package com.iccuu.general_web_backend.common.filter;

import com.iccuu.general_web_backend.infrastructure.metrics.MetricsService;
import com.iccuu.general_web_backend.infrastructure.security.JwtTokenProvider;
import com.iccuu.general_web_backend.module.auth.service.TokenService;
import com.iccuu.general_web_backend.module.user.dto.UserVO;
import com.iccuu.general_web_backend.module.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.JwtClaims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String COOKIE_NAME = "session_id";

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final UserService userService;
    private final MetricsService metricsService;
    private final Cache<Long, Long> passwordChangedAtCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long validationStart = System.currentTimeMillis();
        try {
            JwtClaims claims = jwtTokenProvider.parseToken(token);
            if (claims == null) {
                metricsService.recordTokenValidation(false,
                        System.currentTimeMillis() - validationStart);
                filterChain.doFilter(request, response);
                return;
            }

            // Check expiration from already-parsed claims
            try {
                if (claims.getExpirationTime() != null
                        && claims.getExpirationTime().getValueInMillis() <= System.currentTimeMillis()) {
                    metricsService.recordTokenValidation(false,
                            System.currentTimeMillis() - validationStart);
                    filterChain.doFilter(request, response);
                    return;
                }
            } catch (Exception e) {
                metricsService.recordTokenValidation(false,
                        System.currentTimeMillis() - validationStart);
                filterChain.doFilter(request, response);
                return;
            }

            // Check JWT blacklist for access tokens
            String jti = claims.getJwtId();
            String tokenType = claims.getStringClaimValue("type");
            if ("access".equals(tokenType) && jti != null && tokenService.isBlacklisted(jti)) {
                filterChain.doFilter(request, response);
                return;
            }

            Long userId = Long.parseLong(claims.getSubject());
            String username = claims.getStringClaimValue("username");
            List<String> perms = claims.getStringListClaimValue("perm");

            // Check password_changed_at for access tokens (with TTL cache)
            if ("access".equals(tokenType)) {
                long iatEpochSeconds = claims.getIssuedAt().getValueInMillis() / 1000;
                Long cachedPwdChangedAt = passwordChangedAtCache.getIfPresent(userId);
                if (cachedPwdChangedAt == null) {
                    UserVO userVO = userService.getUserById(userId);
                    if (userVO != null && userVO.getPasswordChangedAt() != null) {
                        long passwordChangedAtEpochSeconds =
                                Timestamp.valueOf(userVO.getPasswordChangedAt()).getTime() / 1000;
                        passwordChangedAtCache.put(userId, passwordChangedAtEpochSeconds);
                        if (iatEpochSeconds < passwordChangedAtEpochSeconds) {
                            metricsService.recordTokenValidation(false,
                                    System.currentTimeMillis() - validationStart);
                            filterChain.doFilter(request, response);
                            return;
                        }
                    } else {
                        passwordChangedAtCache.put(userId, 0L);
                    }
                } else if (cachedPwdChangedAt > 0 && iatEpochSeconds < cachedPwdChangedAt) {
                    metricsService.recordTokenValidation(false,
                            System.currentTimeMillis() - validationStart);
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            List<SimpleGrantedAuthority> authorities = (perms != null ? perms : List.<String>of())
                    .stream().map(SimpleGrantedAuthority::new).toList();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, username, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            metricsService.recordTokenValidation(true,
                    System.currentTimeMillis() - validationStart);
        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            metricsService.recordTokenValidation(false,
                    System.currentTimeMillis() - validationStart);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
