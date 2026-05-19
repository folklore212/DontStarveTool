package com.iccuu.general_web_backend.infrastructure.steam;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Steam Web API client for IPublishedFileService/QueryFiles.
 * Used by both template-service (cold-miss) and mod-worker (scheduled refresh).
 * Falls back gracefully when STEAM_API_KEY is not configured (offline/dev mode).
 */
@Slf4j
@Component
public class SteamApiClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${steam.api.key:}")
    private String apiKey;

    private static final String API_URL = "https://api.steampowered.com/IPublishedFileService/QueryFiles/v1/";
    private static final int DST_APP_ID = 322330;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Query Workshop files by popularity (subscriptions).
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> queryFiles(int page, int perPage, String searchText, int queryType) {
        if (!isConfigured()) {
            log.debug("Steam API key not configured, skipping live query");
            return Collections.emptyList();
        }
        try {
            String url = API_URL + "?key=" + apiKey
                    + "&appid=" + DST_APP_ID
                    + "&return_vote_data=true"
                    + "&return_tags=true"
                    + "&return_metadata=true"
                    + "&numperpage=" + perPage
                    + "&page=" + page
                    + "&query_type=" + queryType
                    + "&search_text=" + (searchText != null ? searchText : "");
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("response")) {
                Map<String, Object> resp = (Map<String, Object>) response.get("response");
                return (List<Map<String, Object>>) resp.getOrDefault("publishedfiledetails", Collections.emptyList());
            }
        } catch (Exception e) {
            log.warn("Steam API call failed (page={}, perPage={}): {}", page, perPage, e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Fetch top DST mods by all-time subscriptions. Paginated.
     */
    public List<Map<String, Object>> fetchHotMods(int total, int perPage) {
        List<Map<String, Object>> all = new ArrayList<>();
        int pages = (total + perPage - 1) / perPage;
        for (int p = 1; p <= pages; p++) {
            List<Map<String, Object>> page = queryFiles(p, perPage, "", 9); // 9 = ranked by subscriptions
            all.addAll(page);
            if (page.size() < perPage) break;
        }
        log.info("Fetched {} hot mods from Steam ({} pages)", all.size(), pages);
        return all;
    }

    /**
     * Search mods by keyword.
     */
    public List<Map<String, Object>> searchMods(String keyword, int page, int perPage) {
        return queryFiles(page, perPage, keyword != null ? keyword : "", 0); // 0 = ranked by last updated
    }
}
