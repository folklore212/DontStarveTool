package com.iccuu.general_web_backend.steamcache.service;

import com.iccuu.general_web_backend.infrastructure.steam.SteamApiClient;
import com.iccuu.general_web_backend.infrastructure.steam.WorkshopCacheWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Active cache refresh: periodically fetches top DST mods from Steam Workshop
 * and persists them to the shared dst_templates database.
 *
 * Disabled when dst.scheduled.workshop-cache.enabled=false (multi-instance safety).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "dst.scheduled.workshop-cache.enabled", havingValue = "true", matchIfMissing = false)
public class WorkshopRefreshService {

    private final SteamApiClient steamApiClient;
    private final WorkshopCacheWriter cacheWriter;

    private static final int HOT_MOD_COUNT = 500;
    private static final int PER_PAGE = 50;

    @Scheduled(cron = "0 7,37 * * * *")
    public void refreshHotMods() {
        log.info("Starting scheduled Workshop cache refresh (top {} mods)...", HOT_MOD_COUNT);
        try {
            List<Map<String, Object>> allMods = steamApiClient.fetchHotMods(HOT_MOD_COUNT, PER_PAGE);
            if (allMods.isEmpty()) {
                log.info("Steam API returned no results, skipping refresh");
                return;
            }
            var result = cacheWriter.upsert(allMods);
            log.info("Workshop cache refresh complete: {} mods processed ({} new, {} updated)",
                    allMods.size(), result.get("new"), result.get("updated"));
        } catch (Exception e) {
            log.warn("Workshop cache refresh failed: {}", e.getMessage());
        }
    }
}
