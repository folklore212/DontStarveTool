package com.iccuu.general_web_backend.module.template.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iccuu.general_web_backend.common.constant.RedisKeyPrefix;
import com.iccuu.general_web_backend.common.util.RedisUtil;
import com.iccuu.general_web_backend.infrastructure.steam.SteamApiClient;
import com.iccuu.general_web_backend.infrastructure.steam.WorkshopCacheWriter;
import com.iccuu.general_web_backend.module.template.entity.SteamWorkshopCache;
import com.iccuu.general_web_backend.module.template.mapper.SteamWorkshopCacheMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Workshop cache read-path: serves hot mods and search from DB/Redis.
 * Write-path (@Scheduled refresh) lives in steam-cache-service.
 * Cold-miss: triggers a live Steam API fetch for the searched keyword.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SteamWorkshopCacheService {

    private final SteamWorkshopCacheMapper cacheMapper;
    private final SteamApiClient steamApiClient;
    private final WorkshopCacheWriter cacheWriter;
    private final RedisUtil redisUtil;

    private static final String CACHE_KEY = RedisKeyPrefix.STEAM_WORKSHOP_HOT;
    private static final long CACHE_TTL_SECONDS = Duration.ofHours(6).toSeconds();
    private static final int MAX_CACHED_RESULTS = 500;

    public List<Map<String, Object>> getHotMods() {
        List<Map<String, Object>> cached = redisUtil.get(CACHE_KEY);
        if (cached != null && !cached.isEmpty()) return cached;

        var dbResults = cacheMapper.selectList(
                new LambdaQueryWrapper<SteamWorkshopCache>()
                        .orderByDesc(SteamWorkshopCache::getSubscriptions)
                        .last("LIMIT 50"));
        var results = dbResults.stream().map(WorkshopCacheWriter::toMap).toList();
        if (!results.isEmpty()) {
            redisUtil.set(CACHE_KEY, results, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        }
        return results;
    }

    public List<Map<String, Object>> searchCached(String keyword) {
        if (keyword == null || keyword.isBlank()) return getHotMods();
        String kw = keyword.toLowerCase();

        // 1. Search local cache
        var dbResults = cacheMapper.selectList(
                new LambdaQueryWrapper<SteamWorkshopCache>()
                        .orderByDesc(SteamWorkshopCache::getSubscriptions)
                        .last("LIMIT " + MAX_CACHED_RESULTS));
        var localHits = dbResults.stream()
                .filter(m -> matchKeyword(m, kw))
                .limit(50)
                .map(WorkshopCacheWriter::toMap)
                .toList();
        if (!localHits.isEmpty()) return localHits;

        // 2. Cache miss — live Steam API
        log.info("Cache miss for '{}', fetching from Steam API", keyword);
        var liveResults = steamApiClient.searchMods(keyword, 1, 20);
        if (liveResults.isEmpty()) return Collections.emptyList();

        cacheWriter.upsert(liveResults);
        return liveResults.stream()
                .map(this::steamItemToMap)
                .limit(20)
                .toList();
    }

    private boolean matchKeyword(SteamWorkshopCache m, String kw) {
        String title = m.getTitle() != null ? m.getTitle().toLowerCase() : "";
        String desc = m.getDescription() != null ? m.getDescription().toLowerCase() : "";
        String tags = m.getTags() != null ? m.getTags().toLowerCase() : "";
        return title.contains(kw) || desc.contains(kw) || tags.contains(kw);
    }

    private Map<String, Object> steamItemToMap(Map<String, Object> mod) {
        return Map.of(
                "workshopId", String.valueOf(mod.getOrDefault("publishedfileid", "")),
                "title", String.valueOf(mod.getOrDefault("title", "")),
                "description", String.valueOf(mod.getOrDefault("file_description", "")),
                "previewUrl", String.valueOf(mod.getOrDefault("preview_url", "")),
                "subscriptions", WorkshopCacheWriter.intVal(mod, "subscriptions"),
                "favorited", WorkshopCacheWriter.intVal(mod, "favorited"),
                "tags", ""
        );
    }
}
