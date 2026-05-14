package com.iccuu.general_web_backend.module.template.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iccuu.general_web_backend.common.constant.RedisKeyPrefix;
import com.iccuu.general_web_backend.common.util.RedisUtil;
import com.iccuu.general_web_backend.infrastructure.steam.SteamApiService;
import com.iccuu.general_web_backend.module.template.entity.SteamWorkshopCache;
import com.iccuu.general_web_backend.module.template.mapper.SteamWorkshopCacheMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SteamWorkshopCacheService {

    private final SteamWorkshopCacheMapper cacheMapper;
    private final SteamApiService steamApiService;
    private final RedisUtil redisUtil;

    private static final String CACHE_KEY = RedisKeyPrefix.STEAM_WORKSHOP_HOT;
    private static final long CACHE_TTL_SECONDS = Duration.ofHours(6).toSeconds();

    public List<Map<String, Object>> getHotMods() {
        List<Map<String, Object>> cached = redisUtil.get(CACHE_KEY);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        var dbResults = cacheMapper.selectList(
                new LambdaQueryWrapper<SteamWorkshopCache>()
                        .orderByDesc(SteamWorkshopCache::getSubscriptions)
                        .last("LIMIT 50"));
        var results = dbResults.stream().map(this::toMap).toList();
        if (!results.isEmpty()) {
            redisUtil.set(CACHE_KEY, results, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        }
        return results;
    }

    public List<Map<String, Object>> searchCached(String keyword) {
        var all = getHotMods();
        if (keyword == null || keyword.isBlank()) return all;
        String kw = keyword.toLowerCase();
        return all.stream()
                .filter(m -> {
                    String title = String.valueOf(m.getOrDefault("title", "")).toLowerCase();
                    String desc = String.valueOf(m.getOrDefault("description", "")).toLowerCase();
                    return title.contains(kw) || desc.contains(kw);
                })
                .limit(50)
                .toList();
    }

    /**
     * Scheduled refresh: fetches popular mods from Steam Workshop.
     * Runs at :07 and :37 past each hour to avoid the :00/:30 thundering herd.
     */
    @Scheduled(cron = "0 7,37 * * * *")
    public void refreshCache() {
        log.info("Refreshing Steam Workshop cache...");
        try {
            var popularMods = steamApiService.searchMods("", 1, 30);
            if (popularMods.isEmpty()) {
                log.debug("Steam API returned no results, skipping cache refresh");
                return;
            }

            Map<String, Map<String, Object>> steamModMap = new LinkedHashMap<>();
            for (var mod : popularMods) {
                String workshopId = String.valueOf(mod.getOrDefault("workshopId", ""));
                if (!workshopId.isBlank()) steamModMap.put(workshopId, mod);
            }
            if (steamModMap.isEmpty()) return;

            var existingList = cacheMapper.selectList(
                    new LambdaQueryWrapper<SteamWorkshopCache>()
                            .in(SteamWorkshopCache::getWorkshopId, steamModMap.keySet()));
            var existingMap = existingList.stream()
                    .collect(Collectors.toMap(SteamWorkshopCache::getWorkshopId, e -> e));

            List<SteamWorkshopCache> toInsert = new ArrayList<>();
            List<SteamWorkshopCache> toUpdate = new ArrayList<>();

            for (var entry : steamModMap.entrySet()) {
                String wid = entry.getKey();
                var mod = entry.getValue();
                var existing = existingMap.get(wid);
                if (existing != null) {
                    existing.setTitle((String) mod.getOrDefault("title", existing.getTitle()));
                    existing.setDescription((String) mod.getOrDefault("description", existing.getDescription()));
                    existing.setPreviewUrl((String) mod.getOrDefault("previewUrl", existing.getPreviewUrl()));
                    existing.setSubscriptions((Integer) mod.getOrDefault("subscriptions", existing.getSubscriptions()));
                    existing.setFavorited((Integer) mod.getOrDefault("favorited", existing.getFavorited()));
                    existing.setLastUpdated(LocalDateTime.now());
                    toUpdate.add(existing);
                } else {
                    var entry2 = new SteamWorkshopCache();
                    entry2.setWorkshopId(wid);
                    entry2.setTitle((String) mod.getOrDefault("title", ""));
                    entry2.setDescription((String) mod.getOrDefault("description", ""));
                    entry2.setPreviewUrl((String) mod.getOrDefault("previewUrl", ""));
                    entry2.setSubscriptions((Integer) mod.getOrDefault("subscriptions", 0));
                    entry2.setFavorited((Integer) mod.getOrDefault("favorited", 0));
                    entry2.setLastUpdated(LocalDateTime.now());
                    entry2.setCreatedAt(LocalDateTime.now());
                    toInsert.add(entry2);
                }
            }

            if (!toInsert.isEmpty()) {
                for (var e : toInsert) cacheMapper.insert(e);
            }
            if (!toUpdate.isEmpty()) {
                for (var e : toUpdate) cacheMapper.updateById(e);
            }

            var dbResults = cacheMapper.selectList(
                    new LambdaQueryWrapper<SteamWorkshopCache>()
                            .orderByDesc(SteamWorkshopCache::getSubscriptions)
                            .last("LIMIT 50"));
            redisUtil.set(CACHE_KEY,
                    dbResults.stream().map(this::toMap).toList(),
                    CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            log.info("Steam Workshop cache refreshed: {} new, {} updated",
                    toInsert.size(), toUpdate.size());
        } catch (Exception e) {
            log.warn("Failed to refresh Steam Workshop cache: {}", e.getMessage());
        }
    }

    private Map<String, Object> toMap(SteamWorkshopCache c) {
        return Map.of(
                "workshopId", c.getWorkshopId(),
                "title", c.getTitle() != null ? c.getTitle() : "",
                "description", c.getDescription() != null ? c.getDescription() : "",
                "previewUrl", c.getPreviewUrl() != null ? c.getPreviewUrl() : "",
                "subscriptions", c.getSubscriptions() != null ? c.getSubscriptions() : 0,
                "favorited", c.getFavorited() != null ? c.getFavorited() : 0,
                "tags", c.getTags() != null ? c.getTags() : ""
        );
    }
}
