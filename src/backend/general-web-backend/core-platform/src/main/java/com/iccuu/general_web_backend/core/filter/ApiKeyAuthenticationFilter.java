package com.iccuu.general_web_backend.core.filter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iccuu.general_web_backend.core.cache.ApiKeyCacheManager;
import com.iccuu.general_web_backend.common.enums.ApiKeyStatus;
import com.iccuu.general_web_backend.common.util.HashUtil;
import com.iccuu.general_web_backend.module.apikey.entity.ApiKey;
import com.iccuu.general_web_backend.module.apikey.mapper.ApiKeyMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyMapper apiKeyMapper;
    private final ApiKeyCacheManager apiKeyCacheManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String rawKey = request.getHeader(API_KEY_HEADER);
        if (rawKey == null || rawKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String keyHash = HashUtil.sha256(rawKey);

            Long userId = null;
            String keyPrefix = null;
            String allowedScopes = null;
            boolean cached = false;

            Map<Object, Object> cachedFields = apiKeyCacheManager.get(keyHash);
            if (!cachedFields.isEmpty()) {
                cached = true;
                String cachedStatus = (String) cachedFields.get(ApiKeyCacheManager.HF_STATUS);
                if (!String.valueOf(ApiKeyStatus.NORMAL.getValue()).equals(cachedStatus)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                String cachedExpiresAt = (String) cachedFields.get(ApiKeyCacheManager.HF_EXPIRES_AT);
                if (cachedExpiresAt != null && !cachedExpiresAt.isEmpty()) {
                    LocalDateTime expiresAt = LocalDateTime.parse(cachedExpiresAt);
                    if (expiresAt.isBefore(LocalDateTime.now())) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                }
                userId = Long.valueOf((String) cachedFields.get(ApiKeyCacheManager.HF_USER_ID));
                keyPrefix = (String) cachedFields.get(ApiKeyCacheManager.HF_KEY_PREFIX);
                allowedScopes = (String) cachedFields.get(ApiKeyCacheManager.HF_ALLOWED_SCOPES);
            } else {
                ApiKey apiKey = apiKeyMapper.selectOne(
                        new LambdaQueryWrapper<ApiKey>()
                                .eq(ApiKey::getKeyHash, keyHash));

                if (apiKey == null) {
                    apiKeyCacheManager.cacheNull(keyHash);
                    filterChain.doFilter(request, response);
                    return;
                }

                if (apiKey.getStatus() == null || apiKey.getStatus() != ApiKeyStatus.NORMAL.getValue()) {
                    apiKeyCacheManager.cache(apiKey);
                    filterChain.doFilter(request, response);
                    return;
                }

                if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
                    apiKeyCacheManager.cache(apiKey);
                    filterChain.doFilter(request, response);
                    return;
                }

                userId = apiKey.getUserId();
                keyPrefix = apiKey.getKeyPrefix();
                allowedScopes = apiKey.getAllowedScopes();
                apiKeyCacheManager.cache(apiKey);
            }

            if (!cached) {
                try {
                    ApiKey update = new ApiKey();
                    update.setLastUsedAt(LocalDateTime.now());
                    apiKeyMapper.update(update, new LambdaQueryWrapper<ApiKey>()
                            .eq(ApiKey::getKeyHash, keyHash));
                } catch (Exception e) {
                    log.debug("Failed to update API key last_used_at: {}", e.getMessage());
                }
            }

            List<SimpleGrantedAuthority> authorities = Collections.emptyList();
            if (allowedScopes != null && !allowedScopes.isBlank()) {
                authorities = List.of(new SimpleGrantedAuthority("SCOPE_" + allowedScopes));
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, keyPrefix, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

            log.debug("API key authenticated: prefix={}, userId={}", keyPrefix, userId);
        } catch (Exception e) {
            log.debug("API key authentication failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
