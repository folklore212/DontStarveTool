package com.iccuu.general_web_backend.integration;

import com.iccuu.general_web_backend.BaseIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkflowIntegrationTest extends BaseIntegrationTest {
    @Autowired private TestRestTemplate rest;

    @Test @Order(1)
    void captchaConfigIsPublic() {
        var resp = rest.getForEntity("/api/v1/auth/captcha-config", Map.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(0, resp.getBody().get("code"));
    }

    @Test @Order(2)
    void invalidLoginFailsWith4xx() {
        var resp = rest.postForEntity("/api/v1/auth/login", Map.of(
            "username", "nonexistent_user_99999", "password", "wrong"
        ), Map.class);
        assertTrue(resp.getStatusCode().is4xxClientError(), "wrong login: " + resp.getBody());
    }

    @Test @Order(3)
    void loginEmptyBodyRejected() {
        var resp = rest.postForEntity("/api/v1/auth/login", Map.of(), Map.class);
        assertTrue(resp.getStatusCode().is4xxClientError());
    }

    @Test @Order(4)
    void templatesEndpointResponds() {
        var resp = rest.getForEntity("/api/v1/templates", Map.class);
        assertNotNull(resp.getBody());
    }

    @Test @Order(5)
    void workshopSearchResponds() {
        var resp = rest.getForEntity("/api/v1/workshop/search?keyword=boss", Map.class);
        assertNotNull(resp.getBody());
    }
}
