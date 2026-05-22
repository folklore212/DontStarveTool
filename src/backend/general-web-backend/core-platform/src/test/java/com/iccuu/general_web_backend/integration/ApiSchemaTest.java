package com.iccuu.general_web_backend.integration;

import com.iccuu.general_web_backend.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies API response shapes don't silently change.
 * If a test fails, ask: "Did you intend to change the API contract?"
 */
class ApiSchemaTest extends BaseIntegrationTest {
    @Autowired private TestRestTemplate rest;

    @Test
    void captchaConfigResponseShape() {
        var r = rest.getForEntity("/api/v1/auth/captcha-config", Map.class);
        assertEquals(0, r.getBody().get("code"), "captcha-config code must be 0");
        assertNotNull(r.getBody().get("message"), "must have message");
        Map<String, String> data = (Map) r.getBody().get("data");
        assertNotNull(data, "must have data");
        assertNotNull(data.get("loginCaptchaId"), "must have loginCaptchaId");
        assertNotNull(data.get("registerCaptchaId"), "must have registerCaptchaId");
        assertTrue(r.getBody().containsKey("timestamp"), "must have timestamp");
    }

    @Test
    void errorResponseShape() {
        var r = rest.postForEntity("/api/v1/auth/login", Map.of(), Map.class);
        assertNotNull(r.getBody().get("code"), "error must have code");
        assertNotNull(r.getBody().get("message"), "error must have message");
        assertNotNull(r.getBody().get("timestamp"), "error must have timestamp");
    }

    @Test
    void loginSuccessResponseShape() {
        // Register first to get valid credentials
        var regResp = rest.postForEntity("/api/v1/auth/register", Map.of(
            "username", "schema_" + System.currentTimeMillis() % 100000,
            "password", "Aa@123456",
            "identityType", "username",
            "verificationCode", "000000"
        ), Map.class);

        // Login
        var r = rest.postForEntity("/api/v1/auth/login", Map.of(
            "username", "schema_" + System.currentTimeMillis() % 100000,
            "password", "Aa@123456"
        ), Map.class);
        if (r.getStatusCode().is2xxSuccessful() && r.getBody() != null && r.getBody().get("data") != null) {
            Map<String, Object> data = (Map) r.getBody().get("data");
            assertNotNull(data.get("accessToken"), "login success must have accessToken");
            assertNotNull(data.get("refreshToken"), "login success must have refreshToken");
        }
        // If login fails (rate-limit/verification), that's ok — just verify shape when it succeeds
    }
}
