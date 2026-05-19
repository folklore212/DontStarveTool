package com.iccuu.general_web_backend.server.client;

import com.iccuu.general_web_backend.module.server.service.ModSearchProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * REST implementation of ModSearchProvider.
 * Calls template-service for workshop mod search.
 */
public class RemoteModSearchProvider implements ModSearchProvider {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${platform.template.url:http://template-service:8082}")
    private String templateUrl;

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchCached(String keyword) {
        try {
            String url = templateUrl + "/api/v1/workshop/search" + (keyword != null ? "?keyword=" + keyword : "");
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("data")) {
                return (List<Map<String, Object>>) response.getBody().get("data");
            }
        } catch (Exception e) {
            // Fallback: empty results
        }
        return Collections.emptyList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchFromSteam(String keyword, int page, int size) {
        try {
            String url = templateUrl + "/api/v1/workshop/search?keyword=" + keyword;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("data")) {
                return (List<Map<String, Object>>) response.getBody().get("data");
            }
        } catch (Exception e) {
            // Fallback
        }
        return Collections.emptyList();
    }
}
