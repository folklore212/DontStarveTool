package com.iccuu.general_web_backend.module.role.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.iccuu.general_web_backend.common.constant.RedisKeyPrefix;
import com.iccuu.general_web_backend.common.util.RedisUtil;
import com.iccuu.general_web_backend.infrastructure.cache.CacheInvalidationListener;
import com.iccuu.general_web_backend.infrastructure.cache.CacheInvalidationPublisher;
import com.iccuu.general_web_backend.infrastructure.security.PermissionResolver;
import com.iccuu.general_web_backend.infrastructure.security.PermissionResolver.EffectivePermission;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class PermissionCacheManager {

    private static final Logger log = LoggerFactory.getLogger(PermissionCacheManager.class);

    private static final long REDIS_TTL_SECONDS = 600;
    private static final int CAFFEINE_MAX_SIZE = 10000;
    private static final int CAFFEINE_TTL_SECONDS = 60;

    private final RedisUtil redisUtil;
    private final PermissionResolver permissionResolver;
    private final CacheInvalidationPublisher invalidationPublisher;
    private final CacheInvalidationListener invalidationListener;

    private Cache<Long, Set<EffectivePermission>> localCache;

    @PostConstruct
    public void init() {
        localCache = Caffeine.newBuilder()
                .maximumSize(CAFFEINE_MAX_SIZE)
                .expireAfterWrite(CAFFEINE_TTL_SECONDS, TimeUnit.SECONDS)
                .recordStats()
                .build();

        invalidationListener.registerHandler(RedisKeyPrefix.CACHE_INVALIDATE_PERMISSIONS, body -> {
            if (CacheInvalidationPublisher.FULL_FLUSH_SENTINEL.equals(body)) {
                log.debug("Full cache flush received via pub/sub, clearing local cache");
                localCache.invalidateAll();
            } else {
                try {
                    Long userId = Long.valueOf(body);
                    localCache.invalidate(userId);
                    log.debug("Cache invalidated for userId={} via pub/sub", userId);
                } catch (NumberFormatException e) {
                    log.warn("Unparseable cache invalidation body: {}", body);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    public Set<EffectivePermission> getEffectivePermissions(Long userId) {
        Set<EffectivePermission> cached = localCache.getIfPresent(userId);
        if (cached != null) {
            return cached;
        }

        String redisKey = RedisKeyPrefix.fmt(RedisKeyPrefix.PERM_EFFECTIVE, userId);
        Set<EffectivePermission> permissions = redisUtil.get(redisKey);
        if (permissions != null) {
            localCache.put(userId, permissions);
            return permissions;
        }

        permissions = permissionResolver.resolvePermissions(userId);

        if (permissions != null && !permissions.isEmpty()) {
            localCache.put(userId, permissions);
            redisUtil.set(redisKey, permissions, REDIS_TTL_SECONDS, TimeUnit.SECONDS);
        }

        return permissions;
    }

    public void invalidate(Long userId) {
        localCache.invalidate(userId);
        String redisKey = RedisKeyPrefix.fmt(RedisKeyPrefix.PERM_EFFECTIVE, userId);
        redisUtil.delete(redisKey);
        invalidationPublisher.publishPermissionInvalidation(userId);
    }

    public CacheStats getStats() {
        return localCache.stats();
    }
}
