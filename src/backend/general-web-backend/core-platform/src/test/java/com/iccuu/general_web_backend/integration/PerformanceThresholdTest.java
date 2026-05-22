package com.iccuu.general_web_backend.integration;

import com.iccuu.general_web_backend.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import java.time.Duration;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Fails if key endpoints suddenly get much slower (N+1 queries, missing indexes, etc.).
 * Uses generous thresholds — only catches major regressions.
 */
class PerformanceThresholdTest extends BaseIntegrationTest {
    @Autowired private TestRestTemplate rest;

    @Test
    void captchaConfigRespondsWithin10Seconds() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            var r = rest.getForEntity("/api/v1/auth/captcha-config", Map.class);
            assertEquals(HttpStatus.OK, r.getStatusCode());
        }, "captcha-config should respond in <10s");
    }

    @Test
    void loginRejectionRespondsWithin10Seconds() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            var r = rest.postForEntity("/api/v1/auth/login", Map.of("username", "test"), Map.class);
            assertTrue(r.getStatusCode().is4xxClientError());
        }, "login rejection should respond in <10s");
    }
}
