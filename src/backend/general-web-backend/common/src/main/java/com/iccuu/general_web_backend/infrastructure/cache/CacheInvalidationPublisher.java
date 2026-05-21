package com.iccuu.general_web_backend.core.warmer;

import com.iccuu.general_web_backend.common.constant.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CacheInvalidationPublisher {

    public static final String FULL_FLUSH_SENTINEL = "*";

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationPublisher.class);

    private final StringRedisTemplate redisTemplate;

    public void publishPermissionInvalidation(Long userId) {
        try {
            String channel = RedisKeyPrefix.CACHE_INVALIDATE_PERMISSIONS;
            redisTemplate.convertAndSend(channel, String.valueOf(userId));
            log.debug("Published cache invalidation for userId={} to channel={}", userId, channel);
        } catch (Exception e) {
            log.warn("Failed to publish cache invalidation for userId={}: {}", userId, e.getMessage());
        }
    }

    public void publishFullFlush() {
        try {
            String channel = RedisKeyPrefix.CACHE_INVALIDATE_PERMISSIONS;
            redisTemplate.convertAndSend(channel, FULL_FLUSH_SENTINEL);
            log.debug("Published full cache flush to channel={}", channel);
        } catch (Exception e) {
            log.warn("Failed to publish full cache flush: {}", e.getMessage());
        }
    }
}
