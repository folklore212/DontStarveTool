package com.iccuu.general_web_backend.module.oauth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iccuu.general_web_backend.common.constant.Constants;
import com.iccuu.general_web_backend.common.constant.ErrorCode;
import com.iccuu.general_web_backend.common.constant.RedisKeyPrefix;
import com.iccuu.general_web_backend.common.enums.GrantType;
import com.iccuu.general_web_backend.common.exception.AuthenticationException;
import com.iccuu.general_web_backend.common.exception.BusinessException;
import com.iccuu.general_web_backend.common.util.RedisUtil;
import com.iccuu.general_web_backend.common.util.SecureRandomUtil;
import com.iccuu.general_web_backend.core.security.JwtTokenProvider;
import com.iccuu.general_web_backend.module.oauth.dto.*;
import com.iccuu.general_web_backend.module.oauth.entity.OAuthClient;
import com.iccuu.general_web_backend.module.oauth.mapper.OAuthClientMapper;
import com.iccuu.general_web_backend.module.oauth.service.OAuthAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.JwtClaims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class OAuthAuthorizationServiceImpl implements OAuthAuthorizationService {

    private static final long CODE_TTL_SECONDS = 600;
    private static final long ACCESS_TOKEN_TTL = 3600;
    private static final String TOKEN_TYPE_BEARER = "Bearer";

    private static final ThreadLocal<MessageDigest> SHA256 = ThreadLocal.withInitial(() -> {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (Exception e) { throw new RuntimeException(e); }
    });

    private final OAuthClientMapper oauthClientMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisUtil redisUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String generateAuthorizationCode(AuthorizationRequest request, Long userId) {
        OAuthClient client = validateClient(request.getClientId());

        if (request.getRedirectUri() == null || request.getRedirectUri().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "redirectUri is required");
        }
        if (!"code".equals(request.getResponseType())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "unsupported response_type: " + request.getResponseType());
        }

        List<String> registeredUris = client.getRedirectUris();
        if (registeredUris == null || registeredUris.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "client has no registered redirect URIs");
        }
        if (!registeredUris.contains(request.getRedirectUri())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "redirect_uri not registered for this client: " + request.getRedirectUri());
        }

        String code = generateCode();
        String codeKey = RedisKeyPrefix.fmt(RedisKeyPrefix.OAUTH_CODE, code);

        redisUtil.hSet(codeKey, "clientId", request.getClientId());
        redisUtil.hSet(codeKey, "userId", userId != null ? String.valueOf(userId) : "");
        redisUtil.hSet(codeKey, "redirectUri", request.getRedirectUri());
        redisUtil.hSet(codeKey, "scope", request.getScope() != null ? request.getScope() : "");
        redisUtil.expire(codeKey, CODE_TTL_SECONDS, TimeUnit.SECONDS);

        if (request.getCodeChallenge() != null && !request.getCodeChallenge().isBlank()) {
            String pkceKey = RedisKeyPrefix.fmt(RedisKeyPrefix.OAUTH_CODE_PKCE, code);
            redisUtil.hSet(pkceKey, "codeChallenge", request.getCodeChallenge());
            redisUtil.hSet(pkceKey, "codeChallengeMethod",
                    request.getCodeChallengeMethod() != null ? request.getCodeChallengeMethod() : "S256");
            redisUtil.expire(pkceKey, CODE_TTL_SECONDS, TimeUnit.SECONDS);
        }

        if (request.getState() != null && !request.getState().isBlank()) {
            redisUtil.hSet(codeKey, "state", request.getState());
            String stateKey = RedisKeyPrefix.fmt(RedisKeyPrefix.OAUTH_STATE, request.getState());
            redisUtil.set(stateKey, request.getState(), CODE_TTL_SECONDS, TimeUnit.SECONDS);
        }

        return code;
    }

    @Override
    public TokenResponse exchangeCodeForToken(TokenExchangeRequest request) {
        if (request.getGrantType() == null || !GrantType.AUTHORIZATION_CODE.getValue().equalsIgnoreCase(request.getGrantType())) {
            throw new AuthenticationException(ErrorCode.OAUTH_CODE_INVALID,
                    "unsupported grant_type: " + request.getGrantType());
        }

        OAuthClient client = validateClientAndSecret(request.getClientId(), request.getClientSecret());

        String codeKey = RedisKeyPrefix.fmt(RedisKeyPrefix.OAUTH_CODE, request.getCode());
        String exchangedKey = RedisKeyPrefix.fmt(RedisKeyPrefix.OAUTH_CODE_EXCHANGED, request.getCode());

        // Atomic idempotency guard: SETNX prevents concurrent code exchanges
        if (!redisUtil.setNx(exchangedKey, "1", CODE_TTL_SECONDS, TimeUnit.SECONDS)) {
            throw new AuthenticationException(ErrorCode.OAUTH_CODE_INVALID, "Authorization code has already been used");
        }

        String storedClientId = (String) redisUtil.hGet(codeKey, "clientId");
        if (storedClientId == null || !storedClientId.equals(request.getClientId())) {
            throw new AuthenticationException(ErrorCode.OAUTH_CODE_INVALID, "Invalid or expired authorization code");
        }

        String storedRedirectUri = (String) redisUtil.hGet(codeKey, "redirectUri");
        if (storedRedirectUri != null && !storedRedirectUri.isEmpty()
                && !storedRedirectUri.equals(request.getRedirectUri())) {
            throw new AuthenticationException(ErrorCode.OAUTH_CODE_INVALID, "Redirect URI mismatch");
        }

        String pkceKey = RedisKeyPrefix.fmt(RedisKeyPrefix.OAUTH_CODE_PKCE, request.getCode());
        String codeChallenge = (String) redisUtil.hGet(pkceKey, "codeChallenge");
        if (codeChallenge != null && !codeChallenge.isEmpty()) {
            String codeChallengeMethod = (String) redisUtil.hGet(pkceKey, "codeChallengeMethod");
            verifyPkce(codeChallenge, codeChallengeMethod, request.getCodeVerifier());
            redisUtil.delete(pkceKey);
        }

        String userId = (String) redisUtil.hGet(codeKey, "userId");
        String scope = (String) redisUtil.hGet(codeKey, "scope");

        String storedState = (String) redisUtil.hGet(codeKey, "state");
        if (storedState != null && !storedState.isEmpty()) {
            String stateKey = RedisKeyPrefix.fmt(RedisKeyPrefix.OAUTH_STATE, storedState);
            redisUtil.delete(stateKey);
        }

        redisUtil.delete(codeKey);

        long uid = 0L;
        if (userId != null && !userId.isEmpty()) {
            uid = Long.parseLong(userId);
        }

        String tokenFamily = UUID.randomUUID().toString();
        String accessToken = jwtTokenProvider.createAccessToken(uid, "", List.of());
        String refreshToken = jwtTokenProvider.createRefreshToken(uid, tokenFamily);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(TOKEN_TYPE_BEARER)
                .expiresIn(ACCESS_TOKEN_TTL)
                .scope(scope)
                .build();
    }

    @Override
    public TokenResponse refreshToken(String refreshToken, String clientId) {
        if (clientId != null && !clientId.isEmpty()) {
            validateClient(clientId);
        }

        JwtClaims claims = jwtTokenProvider.parseToken(refreshToken);
        if (claims == null) {
            throw new AuthenticationException(ErrorCode.TOKEN_EXPIRED, "Invalid refresh token");
        }

        if (jwtTokenProvider.isTokenExpired(refreshToken)) {
            throw new AuthenticationException(ErrorCode.TOKEN_EXPIRED, "Refresh token has expired");
        }

        String family = JwtTokenProvider.getClaimString(claims, "family");
        String familyKey = RedisKeyPrefix.fmt(RedisKeyPrefix.REFRESH_FAMILY, family);
        String revokedKey = RedisKeyPrefix.fmt(RedisKeyPrefix.REFRESH_FAMILY_REVOKED, family);

        // Atomic revoke: SETNX prevents concurrent refresh token rotation
        if (!redisUtil.setNx(revokedKey, "1", Constants.REFRESH_TOKEN_TTL_SECONDS, TimeUnit.SECONDS)) {
            redisUtil.delete(familyKey);
            throw new AuthenticationException(ErrorCode.REFRESH_TOKEN_REPLAY, "Refresh token replay detected");
        }

        Long userId = Long.parseLong(JwtTokenProvider.getClaimString(claims, "sub"));
        String newFamily = UUID.randomUUID().toString();
        String newAccessToken = jwtTokenProvider.createAccessToken(userId, "", List.of());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId, newFamily);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType(TOKEN_TYPE_BEARER)
                .expiresIn(ACCESS_TOKEN_TTL)
                .build();
    }

    @Override
    public void revokeToken(String token) {
        JwtClaims claims = jwtTokenProvider.parseToken(token);
        if (claims == null) {
            return;
        }

        String jti = JwtTokenProvider.getClaimString(claims, "jti");
        if (jti != null) {
            String blacklistKey = RedisKeyPrefix.fmt(RedisKeyPrefix.BLACKLIST_JTI, jti);
            long expMillis;
            try {
                expMillis = claims.getExpirationTime() != null ?
                        claims.getExpirationTime().getValueInMillis() : System.currentTimeMillis();
            } catch (Exception e) {
                expMillis = System.currentTimeMillis();
            }
            long ttl = expMillis - System.currentTimeMillis();
            if (ttl > 0) {
                redisUtil.set(blacklistKey, "1", ttl, TimeUnit.MILLISECONDS);
            }
        }

        String family = JwtTokenProvider.getClaimString(claims, "family");
        if (family != null) {
            String revokedKey = RedisKeyPrefix.fmt(RedisKeyPrefix.REFRESH_FAMILY_REVOKED, family);
            redisUtil.set(revokedKey, "1", Constants.REFRESH_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
        }
    }

    private OAuthClient validateClient(String clientId) {
        OAuthClient client = oauthClientMapper.selectOne(
                new LambdaQueryWrapper<OAuthClient>().eq(OAuthClient::getClientId, clientId));
        if (client == null) {
            throw new AuthenticationException(ErrorCode.CLIENT_NOT_FOUND, "OAuth client not found: " + clientId);
        }
        if (client.getStatus() != null && client.getStatus() == 0) {
            throw new AuthenticationException(ErrorCode.OAUTH_CLIENT_DISABLED, "OAuth client is disabled: " + clientId);
        }
        return client;
    }

    private OAuthClient validateClientAndSecret(String clientId, String clientSecret) {
        OAuthClient client = validateClient(clientId);
        if (!passwordEncoder.matches(clientSecret, client.getClientSecret())) {
            throw new AuthenticationException(ErrorCode.INVALID_CREDENTIALS, "Invalid client credentials");
        }
        return client;
    }

    private void verifyPkce(String codeChallenge, String codeChallengeMethod, String codeVerifier) {
        if (codeVerifier == null || codeVerifier.isBlank()) {
            throw new AuthenticationException(ErrorCode.INVALID_CREDENTIALS, "PKCE code_verifier is required");
        }

        if ("S256".equalsIgnoreCase(codeChallengeMethod)) {
            try {
                byte[] hash = SHA256.get().digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
                String computedChallenge = Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(hash);
                if (!codeChallenge.equals(computedChallenge)) {
                    throw new AuthenticationException(ErrorCode.INVALID_CREDENTIALS, "PKCE verification failed");
                }
            } catch (RuntimeException e) {
                throw new AuthenticationException(ErrorCode.INTERNAL_ERROR, "SHA-256 not available");
            }
        } else {
            if (!codeChallenge.equals(codeVerifier)) {
                throw new AuthenticationException(ErrorCode.INVALID_CREDENTIALS, "PKCE verification failed");
            }
        }
    }

    private String generateCode() {
        return SecureRandomUtil.generateSecureToken(32);
    }

}
