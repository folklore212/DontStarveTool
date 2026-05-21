package com.iccuu.general_web_backend.module.auth.service.impl;

import com.iccuu.general_web_backend.common.constant.Constants;
import com.iccuu.general_web_backend.common.constant.ErrorCode;
import com.iccuu.general_web_backend.common.constant.RedisKeyPrefix;
import com.iccuu.general_web_backend.common.exception.AuthenticationException;
import com.iccuu.general_web_backend.common.util.RedisUtil;
import com.iccuu.general_web_backend.core.security.JwtTokenProvider;
import com.iccuu.general_web_backend.module.auth.dto.LoginResponse;
import com.iccuu.general_web_backend.module.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Primary
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenServiceImpl.class);

    private static final String REFRESH_ROTATION_LUA =
            "local family_key = KEYS[1]\n" +
            "local revoked_key = KEYS[1] .. ':revoked'\n" +
            "local old_jti = ARGV[1]\n" +
            "local new_jti = ARGV[2]\n" +
            "local family_id = ARGV[3]\n" +
            "local retry_window = tonumber(ARGV[4])\n" +
            "\n" +
            "if redis.call('EXISTS', revoked_key) == 1 then\n" +
            "    return {0, 'revoked'}\n" +
            "end\n" +
            "\n" +
            "local current = redis.call('HGET', family_key, 'current')\n" +
            "if current == false then\n" +
            "    redis.call('HSET', family_key, 'current', new_jti, 'previous', old_jti, 'family_id', family_id)\n" +
            "    redis.call('EXPIRE', family_key, 604800)\n" +
            "    return {1, 'ok'}\n" +
            "end\n" +
            "\n" +
            "if current == old_jti then\n" +
            "    local last_rotate = tonumber(redis.call('HGET', family_key, 'last_rotate') or '0')\n" +
            "    if retry_window > 0 and last_rotate > 0 and (retry_window - last_rotate) <= 5 then\n" +
            "        return {1, 'ok'}\n" +
            "    end\n" +
            "    redis.call('HSET', family_key, 'current', new_jti, 'previous', old_jti, 'last_rotate', retry_window)\n" +
            "    return {1, 'ok'}\n" +
            "end\n" +
            "\n" +
            "if redis.call('HEXISTS', family_key, 'previous') == 1 then\n" +
            "    local prev = redis.call('HGET', family_key, 'previous')\n" +
            "    if prev == old_jti then\n" +
            "        local last_rotate = tonumber(redis.call('HGET', family_key, 'last_rotate') or '0')\n" +
            "        if retry_window > 0 and last_rotate > 0 and (retry_window - last_rotate) <= 5 then\n" +
            "            redis.call('HSET', family_key, 'current', new_jti, 'previous', old_jti, 'last_rotate', retry_window)\n" +
            "            return {1, 'ok'}\n" +
            "        end\n" +
            "        redis.call('DEL', family_key)\n" +
            "        redis.call('SET', revoked_key, '1', 'EX', 604800)\n" +
            "        return {2, 'replay_detected'}\n" +
            "    end\n" +
            "end\n" +
            "\n" +
            "return {3, 'unknown'}";

    private static final DefaultRedisScript<List> REFRESH_ROTATION_SCRIPT;
    static {
        REFRESH_ROTATION_SCRIPT = new DefaultRedisScript<>(REFRESH_ROTATION_LUA, List.class);
    }

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisUtil redisUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    public LoginResponse createTokens(Long userId, String username, List<String> permissions) {
        String familyId = UUID.randomUUID().toString();

        String accessToken = jwtTokenProvider.createAccessToken(userId, username, permissions);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId, familyId);

        JwtClaims refreshClaims = jwtTokenProvider.parseToken(refreshToken);
        String refreshJti = JwtTokenProvider.getClaimString(refreshClaims, "jti");

        String familyKey = RedisKeyPrefix.fmt(RedisKeyPrefix.REFRESH_FAMILY, familyId);
        redisTemplate.opsForHash().put(familyKey, "current", refreshJti);
        redisTemplate.opsForHash().put(familyKey, "userId", String.valueOf(userId));
        redisTemplate.expire(familyKey, Constants.REFRESH_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(Constants.ACCESS_TOKEN_TTL_SECONDS)
                .tokenType("Bearer")
                .mfaRequired(false)
                .build();
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        JwtClaims claims = jwtTokenProvider.parseToken(refreshToken);
        if (claims == null) {
            throw new AuthenticationException(ErrorCode.TOKEN_INVALID, "Invalid refresh token");
        }

        String familyId = JwtTokenProvider.getClaimString(claims, "family");
        String oldJti = JwtTokenProvider.getClaimString(claims, "jti");
        String userIdStr = JwtTokenProvider.getClaimString(claims, "sub");

        if (familyId == null || oldJti == null || userIdStr == null) {
            throw new AuthenticationException(ErrorCode.TOKEN_INVALID, "Refresh token missing required claims");
        }

        long userId = Long.parseLong(userIdStr);

        // Generate new tokens
        String newAccessToken = jwtTokenProvider.createAccessToken(userId, "", Collections.emptyList());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId, familyId);
        JwtClaims newRefreshClaims = jwtTokenProvider.parseToken(newRefreshToken);
        String newJti = JwtTokenProvider.getClaimString(newRefreshClaims, "jti");

        // Execute Lua script for atomic rotation
        String familyKey = RedisKeyPrefix.fmt(RedisKeyPrefix.REFRESH_FAMILY, familyId);
        long retryWindow = System.currentTimeMillis() / 1000;

        List<?> result = redisTemplate.execute(
                REFRESH_ROTATION_SCRIPT,
                Collections.singletonList(familyKey),
                oldJti, newJti, familyId, String.valueOf(retryWindow));

        if (result == null || result.isEmpty()) {
            log.error("Refresh token rotation Lua script returned null for family: {}", familyId);
            throw new AuthenticationException(ErrorCode.INTERNAL_ERROR, "Token rotation failed");
        }

        long statusCode = ((Number) result.get(0)).longValue();
        String statusMsg = result.size() > 1 ? (String) result.get(1) : "";

        switch ((int) statusCode) {
            case 0:
                // Family has been revoked
                log.warn("Refresh token family already revoked: {}", familyId);
                throw new AuthenticationException(ErrorCode.REFRESH_TOKEN_REPLAY,
                        "Token family has been revoked");

            case 1:
                // Successful rotation or retry idempotency
                log.debug("Token rotation successful for family: {}, jti: {}", familyId, newJti);
                return LoginResponse.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(newRefreshToken)
                        .expiresIn(Constants.ACCESS_TOKEN_TTL_SECONDS)
                        .tokenType("Bearer")
                        .mfaRequired(false)
                        .build();

            case 2:
                // Replay detected - family has been revoked by the script
                log.warn("Refresh token replay detected for family: {}", familyId);
                throw new AuthenticationException(ErrorCode.REFRESH_TOKEN_REPLAY,
                        "Token replay detected - family revoked");

            case 3:
                // Unknown token
                log.warn("Unknown refresh token presented for family: {}, jti: {}", familyId, oldJti);
                throw new AuthenticationException(ErrorCode.INVALID_CREDENTIALS,
                        "Unknown refresh token");

            default:
                log.error("Unexpected Lua script return code {} for family: {}", statusCode, familyId);
                throw new AuthenticationException(ErrorCode.INTERNAL_ERROR, "Token rotation failed");
        }
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null) {
            JwtClaims accessClaims = jwtTokenProvider.parseToken(accessToken);
            if (accessClaims != null) {
                String jti = JwtTokenProvider.getClaimString(accessClaims, "jti");
                if (jti != null) {
                    long remainingTtl = calculateRemainingTtl(accessClaims);
                    long blacklistTtl = remainingTtl + Constants.JWT_BLACKLIST_TTL_BUFFER;
                    String blacklistKey = RedisKeyPrefix.fmt(RedisKeyPrefix.BLACKLIST_JTI, jti);
                    redisUtil.set(blacklistKey, "1", blacklistTtl, TimeUnit.SECONDS);
                }
            }
        }

        if (refreshToken != null) {
            JwtClaims refreshClaims = jwtTokenProvider.parseToken(refreshToken);
            if (refreshClaims != null) {
                String familyId = JwtTokenProvider.getClaimString(refreshClaims, "family");
                if (familyId != null) {
                    String familyKey = RedisKeyPrefix.fmt(RedisKeyPrefix.REFRESH_FAMILY, familyId);
                    redisUtil.delete(familyKey);
                }
            }
        }
    }

    @Override
    public boolean isBlacklisted(String jti) {
        String blacklistKey = RedisKeyPrefix.fmt(RedisKeyPrefix.BLACKLIST_JTI, jti);
        return redisUtil.exists(blacklistKey);
    }

    @Override
    public JwtClaims parseAccessToken(String token) {
        return jwtTokenProvider.parseToken(token);
    }

    private long calculateRemainingTtl(JwtClaims claims) {
        try {
            long expMillis = claims.getExpirationTime().getValueInMillis();
            long nowMillis = System.currentTimeMillis();
            long remaining = (expMillis - nowMillis) / 1000;
            return Math.max(remaining, 0);
        } catch (MalformedClaimException e) {
            return Constants.ACCESS_TOKEN_TTL_SECONDS;
        }
    }
}
