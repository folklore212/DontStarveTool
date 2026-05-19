package com.iccuu.general_web_backend.server.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * REST client for template-service Workshop search.
 */
@Slf4j
public class RemoteModSearchProvider {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${platform.template.url:http://template-service:8082}")
    private String templateUrl;

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchCached(String keyword) {
        try {
            String url = templateUrl + "/api/v1/workshop/search" + (keyword != null ? "?keyword=" + keyword : "");
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("data")) {
                return (List<Map<String, Object>>) response.getBody().get("data");
            }
        } catch (Exception e) {
            log.warn("Workshop search failed for keyword '{}': {}", keyword, e.getMessage());
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchFromSteam(String keyword, int page, int size) {
        try {
            String url = templateUrl + "/api/v1/workshop/search?keyword=" + keyword;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("data")) {
                return (List<Map<String, Object>>) response.getBody().get("data");
            }
        } catch (Exception e) {
            log.warn("Workshop fetch failed for keyword '{}': {}", keyword, e.getMessage());
        }
        return Collections.emptyList();
    }
}
