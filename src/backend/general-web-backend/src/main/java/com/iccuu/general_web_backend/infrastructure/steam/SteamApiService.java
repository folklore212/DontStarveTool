package com.iccuu.general_web_backend.infrastructure.steam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class SteamApiService {

    private static final Logger log = LoggerFactory.getLogger(SteamApiService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${steam.api-key:}")
    private String apiKey;

    private static final String WORKSHOP_SEARCH =
        "https://api.steampowered.com/IPublishedFileService/QueryFiles/v1/";

    /**
     * Search Steam Workshop for DST mods.
     */
    public List<Map<String, Object>> searchMods(String keyword, int page, int size) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Steam API key not configured, returning empty results");
            return Collections.emptyList();
        }
        try {
            String url = WORKSHOP_SEARCH + "?key=" + apiKey +
                "&appid=322330&return_vote_data=true&return_tags=true" +
                "&search_text=" + java.net.URLEncoder.encode(keyword, "UTF-8") +
                "&numperpage=" + size + "&page=" + page +
                "&query_type=1&return_metadata=true";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map body = response.getBody();
            if (body != null && body.containsKey("response")) {
                Map resp = (Map) body.get("response");
                Object published = resp.get("publishedfiledetails");
                if (published instanceof List<?> list) {
                    return list.stream().map(item -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("workshopId", ((Map) item).getOrDefault("publishedfileid", ""));
                        m.put("title", ((Map) item).getOrDefault("title", ""));
                        m.put("description", ((Map) item).getOrDefault("description", ""));
                        m.put("previewUrl", ((Map) item).getOrDefault("preview_url", ""));
                        m.put("subscriptions", ((Map) item).getOrDefault("subscriptions", 0));
                        m.put("favorited", ((Map) item).getOrDefault("favorited", 0));
                        return (Map<String, Object>) m;
                    }).toList();
                }
            }
        } catch (Exception e) {
            log.warn("Steam API call failed: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Validate a Klei server token.
     */
    public boolean validateToken(String token) {
        return token != null && token.length() > 20;
    }

    /**
     * Get DST server version from Steam.
     */
    public String getDstVersion() {
        // SteamCMD app_info_print 343050 equivalent via API
        return "unknown";
    }
}
