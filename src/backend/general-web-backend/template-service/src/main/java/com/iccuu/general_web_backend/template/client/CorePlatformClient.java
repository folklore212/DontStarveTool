package com.iccuu.general_web_backend.template.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * REST client for core-platform service.
 * In monolith mode: not used (direct DB access).
 * In service-split mode: calls core-platform for user profile info.
 */
@Slf4j
@Component
public class CorePlatformClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${platform.core.url:http://core-platform:8081}")
    private String coreUrl;

    /**
     * Get user display info for template author display.
     * Falls back to userId-only if core-platform is unreachable.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserProfile(Long userId) {
        try {
            String url = coreUrl + "/api/v1/internal/users/" + userId + "/profile";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("data")) {
                return (Map<String, Object>) response.getBody().get("data");
            }
        } catch (Exception e) {
            log.warn("Failed to get user profile for userId={}: {}", userId, e.getMessage());
        }
        return Map.of("userId", userId, "username", "User " + userId);
    }

    /**
     * Batch get user profiles.
     */
    @SuppressWarnings("unchecked")
    public Map<Long, Map<String, Object>> getUserProfiles(List<Long> userIds) {
        try {
            String url = coreUrl + "/api/v1/internal/users/profiles";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<Long>> request = new HttpEntity<>(userIds, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("data")) {
                return (Map<Long, Map<String, Object>>) response.getBody().get("data");
            }
        } catch (Exception e) {
            log.warn("Failed to get user profiles batch ({} ids): {}", userIds.size(), e.getMessage());
        }
        return Collections.emptyMap();
    }
}
