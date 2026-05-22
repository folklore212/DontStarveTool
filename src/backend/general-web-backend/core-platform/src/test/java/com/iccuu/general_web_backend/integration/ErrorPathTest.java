package com.iccuu.general_web_backend.integration;

import com.iccuu.general_web_backend.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ErrorPathTest extends BaseIntegrationTest {
    @Autowired private TestRestTemplate rest;

    @Test void loginEmptyBodyReturns4xx() {
        var r = rest.postForEntity("/api/v1/auth/login", Map.of(), Map.class);
        assertTrue(r.getStatusCode().is4xxClientError(),
            "empty body should be 4xx, got " + r.getStatusCode());
        assertNotNull(r.getBody().get("code"));
        assertNotNull(r.getBody().get("message"));
    }
    @Test void registerEmptyBodyReturns4xx() {
        var r = rest.postForEntity("/api/v1/auth/register", Map.of(), Map.class);
        assertTrue(r.getStatusCode().is4xxClientError(),
            "empty body should be 4xx, got " + r.getStatusCode());
    }
    @Test void getUserWithoutTokenReturns401() {
        var r = rest.getForEntity("/api/v1/users/1", Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode());
    }
    @Test void errorResponseContainsCodeAndMessage() {
        var r = rest.postForEntity("/api/v1/auth/login", Map.of(), Map.class);
        assertNotNull(r.getBody().get("code"));
        assertNotNull(r.getBody().get("message"));
        String body = r.getBody().toString();
        assertFalse(body.contains("Exception") || body.contains("\tat "),
            "response MUST NOT leak stack trace: " + body);
    }
    @Test void getOnPostEndpointDoesNotCrash() {
        var r = rest.getForEntity("/api/v1/auth/login", Map.class);
        // Don't crash with 500 — any structured error response is acceptable
        assertNotNull(r.getBody());
        assertTrue(r.getBody().containsKey("code") || r.getBody().containsKey("error"),
            "should return structured error, got: " + r.getBody());
    }
}
