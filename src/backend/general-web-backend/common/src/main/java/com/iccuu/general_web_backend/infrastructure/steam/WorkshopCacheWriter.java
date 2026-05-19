package com.iccuu.general_web_backend.infrastructure.steam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iccuu.general_web_backend.common.constant.RedisKeyPrefix;
import com.iccuu.general_web_backend.common.util.RedisUtil;
import com.iccuu.general_web_backend.module.template.entity.SteamWorkshopCache;
import com.iccuu.general_web_backend.module.template.mapper.SteamWorkshopCacheMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Shared writer for steam_workshop_cache table.
 * Used by both mod-worker (scheduled refresh) and template-service (cold-miss fallback).
 * Single implementation avoids upsert logic drift between modules.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkshopCacheWriter {

    private final SteamWorkshopCacheMapper cacheMapper;
    private final RedisUtil redisUtil;

    private static final String CACHE_KEY = RedisKeyPrefix.STEAM_WORKSHOP_HOT;
    private static final long CACHE_TTL = Duration.ofHours(6).toSeconds();
    private static final int REDIS_TOP_COUNT = 50;

    /**
     * Bulk upsert Steam Workshop items into DB, then refresh Redis top list.
     * @return {new: N, updated: N}
     */
    public Map<String, Integer> upsert(List<Map<String, Object>> steamItems) {
        if (steamItems == null || steamItems.isEmpty()) {
            return Map.of("new", 0, "updated", 0);
        }

        // Deduplicate by publishedfileid
        Map<String, Map<String, Object>> steamMap = new LinkedHashMap<>();
        for (var mod : steamItems) {
            String wid = String.valueOf(mod.getOrDefault("publishedfileid", ""));
            if (!wid.isBlank()) steamMap.put(wid, mod);
        }
        if (steamMap.isEmpty()) return Map.of("new", 0, "updated", 0);

        var existingList = cacheMapper.selectList(
                new LambdaQueryWrapper<SteamWorkshopCache>()
                        .in(SteamWorkshopCache::getWorkshopId, steamMap.keySet()));
        var existingMap = existingList.stream()
                .collect(Collectors.toMap(SteamWorkshopCache::getWorkshopId, e -> e));

        List<SteamWorkshopCache> toInsert = new ArrayList<>();
        List<SteamWorkshopCache> toUpdate = new ArrayList<>();

        for (var entry : steamMap.entrySet()) {
            String wid = entry.getKey();
            var mod = entry.getValue();
            var existing = existingMap.get(wid);
            if (existing != null) {
                apply(mod, existing);
                toUpdate.add(existing);
            } else {
                var entity = new SteamWorkshopCache();
                entity.setWorkshopId(wid);
                apply(mod, entity);
                entity.setCreatedAt(LocalDateTime.now());
                toInsert.add(entity);
            }
        }

        for (var e : toInsert) cacheMapper.insert(e);
        for (var e : toUpdate) cacheMapper.updateById(e);

        // Refresh Redis
        refreshRedis();

        log.info("Workshop cache upserted: {} new, {} updated", toInsert.size(), toUpdate.size());
        return Map.of("new", toInsert.size(), "updated", toUpdate.size());
    }

    public void refreshRedis() {
        var top = cacheMapper.selectList(
                new LambdaQueryWrapper<SteamWorkshopCache>()
                        .orderByDesc(SteamWorkshopCache::getSubscriptions)
                        .last("LIMIT " + REDIS_TOP_COUNT));
        redisUtil.set(CACHE_KEY,
                top.stream().map(WorkshopCacheWriter::toMap).toList(),
                CACHE_TTL, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    private void apply(Map<String, Object> mod, SteamWorkshopCache e) {
        e.setTitle((String) mod.getOrDefault("title", e.getTitle()));
        e.setDescription((String) mod.getOrDefault("file_description", e.getDescription()));
        e.setPreviewUrl((String) mod.getOrDefault("preview_url", e.getPreviewUrl()));
        e.setAuthorName((String) mod.getOrDefault("creator", e.getAuthorName()));
        e.setSubscriptions(intVal(mod, "subscriptions"));
        e.setFavorited(intVal(mod, "favorited"));
        var tags = (List<Map<String, String>>) mod.get("tags");
        if (tags != null) {
            e.setTags(tags.stream()
                    .map(t -> t.getOrDefault("tag", t.getOrDefault("display_name", "")))
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining(",")));
        }
        e.setLastUpdated(LocalDateTime.now());
    }

    public static int intVal(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof Number n ? n.intValue() : 0;
    }

    public static Map<String, Object> toMap(SteamWorkshopCache c) {
        return Map.of(
                "workshopId", c.getWorkshopId(),
                "title", c.getTitle() != null ? c.getTitle() : "",
                "description", c.getDescription() != null ? c.getDescription() : "",
                "previewUrl", c.getPreviewUrl() != null ? c.getPreviewUrl() : "",
                "authorName", c.getAuthorName() != null ? c.getAuthorName() : "",
                "subscriptions", c.getSubscriptions() != null ? c.getSubscriptions() : 0,
                "favorited", c.getFavorited() != null ? c.getFavorited() : 0,
                "tags", c.getTags() != null ? c.getTags() : ""
        );
    }
}
