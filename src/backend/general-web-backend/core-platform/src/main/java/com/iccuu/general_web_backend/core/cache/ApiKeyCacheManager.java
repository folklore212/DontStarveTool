package com.iccuu.general_web_backend.core.cache;

import com.iccuu.general_web_backend.common.constant.RedisKeyPrefix;
import com.iccuu.general_web_backend.common.util.RedisUtil;
import com.iccuu.general_web_backend.module.apikey.entity.ApiKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyCacheManager {

    public static final long APIKEY_CACHE_TTL_MINUTES = 5;

    public static final String HF_USER_ID = "userId";
    public static final String HF_KEY_PREFIX = "keyPrefix";
    public static final String HF_STATUS = "status";
    public static final String HF_EXPIRES_AT = "expiresAt";
    public static final String HF_ALLOWED_SCOPES = "allowedScopes";

    static final String NULL_MARKER = "-";

    private final RedisUtil redisUtil;

    public void cache(ApiKey apiKey) {
        try {
            String cacheKey = buildCacheKey(apiKey.getKeyHash());
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put(HF_USER_ID, String.valueOf(apiKey.getUserId()));
            fields.put(HF_KEY_PREFIX, apiKey.getKeyPrefix() != null ? apiKey.getKeyPrefix() : "");
            fields.put(HF_STATUS, String.valueOf(apiKey.getStatus() != null ? apiKey.getStatus() : 0));
            fields.put(HF_EXPIRES_AT, apiKey.getExpiresAt() != null ? apiKey.getExpiresAt().toString() : "");
            fields.put(HF_ALLOWED_SCOPES, apiKey.getAllowedScopes() != null ? apiKey.getAllowedScopes() : "");
            redisUtil.hPutAll(cacheKey, fields);
            redisUtil.expire(cacheKey, APIKEY_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.debug("Failed to cache API key: {}", e.getMessage());
        }
    }

    public void cacheNull(String keyHash) {
        try {
            String cacheKey = buildCacheKey(keyHash);
            redisUtil.hSet(cacheKey, HF_STATUS, NULL_MARKER);
            redisUtil.expire(cacheKey, APIKEY_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.debug("Failed to cache null API key marker: {}", e.getMessage());
        }
    }

    public void invalidate(String keyHash) {
        if (keyHash == null) {
            return;
        }
        try {
            redisUtil.delete(buildCacheKey(keyHash));
        } catch (Exception e) {
            log.debug("Failed to invalidate API key cache: {}", e.getMessage());
        }
    }

    public Map<Object, Object> get(String keyHash) {
        return redisUtil.hGetAll(buildCacheKey(keyHash));
    }

    private String buildCacheKey(String keyHash) {
        return RedisKeyPrefix.fmt(RedisKeyPrefix.APIKEY, keyHash);
    }
}
