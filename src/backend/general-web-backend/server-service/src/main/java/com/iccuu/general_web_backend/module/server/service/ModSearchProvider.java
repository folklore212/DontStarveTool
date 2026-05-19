package com.iccuu.general_web_backend.module.server.service;

import java.util.List;
import java.util.Map;

/**
 * Abstraction for searching Steam Workshop mods.
 * In monolith mode: delegates to SteamWorkshopCacheService (local call).
 * In service-split mode: can be implemented as REST client to template-service.
 */
public interface ModSearchProvider {
    List<Map<String, Object>> searchCached(String keyword);
    List<Map<String, Object>> fetchFromSteam(String keyword, int page, int size);
}
