package com.iccuu.general_web_backend.modworker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iccuu.general_web_backend.module.template.entity.SteamWorkshopCache;
import com.iccuu.general_web_backend.module.template.mapper.SteamWorkshopCacheMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * One-time data import from collected Steam JSON samples.
 * Only runs when data directory exists and DB is empty.
 * Enable with: --data.import.enabled=true or set DATA_IMPORT_ENABLED=true
 *
 * Usage: Place collected data at tools/data/steam_samples/workshop/hot_mods_combined.json
 *        Then run mod-worker with DATA_IMPORT_ENABLED=true
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "data.import.enabled", havingValue = "true")
public class DataImportRunner implements CommandLineRunner {

    private final SteamWorkshopCacheMapper cacheMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DataImportRunner(SteamWorkshopCacheMapper cacheMapper) {
        this.cacheMapper = cacheMapper;
    }

    @Override
    public void run(String... args) {
        log.info("=== DataImportRunner: seeding workshop cache from collected JSON ===");

        // Check if DB already has data
        long count = cacheMapper.selectCount(new LambdaQueryWrapper<>());
        if (count > 0) {
            log.info("DB already has {} cached mods, skipping import (use --data.import.force=true to override)", count);
            return;
        }

        int imported = 0;
        for (Path path : findDataFiles()) {
            try {
                imported += importFile(path);
            } catch (Exception e) {
                log.error("Failed to import {}: {}", path, e.getMessage());
            }
        }
        log.info("=== Data import complete: {} mods imported ===", imported);
    }

    private List<Path> findDataFiles() {
        List<Path> paths = new ArrayList<>();
        // Search common locations
        String[] searchPaths = {
                "tools/data/steam_samples/workshop/hot_mods_combined.json",
                "data/steam_samples/workshop/hot_mods_combined.json",
                "../tools/data/steam_samples/workshop/hot_mods_combined.json",
        };
        for (String sp : searchPaths) {
            Path p = Path.of(sp);
            if (Files.exists(p)) {
                paths.add(p);
                log.info("Found data file: {}", p.toAbsolutePath());
                break; // Only need one combined file
            }
        }
        return paths;
    }

    @SuppressWarnings("unchecked")
    private int importFile(Path path) throws Exception {
        log.info("Importing {}...", path);
        Map<String, Object> root = objectMapper.readValue(path.toFile(), Map.class);
        Map<String, Object> response = (Map<String, Object>) root.get("response");
        if (response == null) {
            log.warn("No 'response' key in JSON, skipping");
            return 0;
        }
        List<Map<String, Object>> mods = (List<Map<String, Object>>) response.get("publishedfiledetails");
        if (mods == null || mods.isEmpty()) {
            log.warn("No 'publishedfiledetails' in JSON");
            return 0;
        }

        int count = 0;
        for (var mod : mods) {
            String wid = String.valueOf(mod.getOrDefault("publishedfileid", ""));
            if (wid.isBlank()) continue;

            var existing = cacheMapper.selectOne(
                    new LambdaQueryWrapper<SteamWorkshopCache>()
                            .eq(SteamWorkshopCache::getWorkshopId, wid));
            if (existing != null) continue;

            var entity = new SteamWorkshopCache();
            entity.setWorkshopId(wid);
            entity.setTitle((String) mod.getOrDefault("title", ""));
            entity.setDescription((String) mod.getOrDefault("file_description", ""));
            entity.setPreviewUrl((String) mod.getOrDefault("preview_url", ""));
            entity.setAuthorName((String) mod.getOrDefault("creator", ""));
            entity.setSubscriptions(intVal(mod, "subscriptions"));
            entity.setFavorited(intVal(mod, "favorited"));
            var tags = (List<Map<String, String>>) mod.get("tags");
            if (tags != null) {
                entity.setTags(tags.stream()
                        .map(t -> t.getOrDefault("tag", t.getOrDefault("display_name", "")))
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.joining(",")));
            }
            entity.setLastUpdated(LocalDateTime.now());
            entity.setCreatedAt(LocalDateTime.now());
            cacheMapper.insert(entity);
            count++;
        }
        log.info("  Imported {} mods from {}", count, path.getFileName());
        return count;
    }

    private int intVal(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof Number n ? n.intValue() : 0;
    }
}
