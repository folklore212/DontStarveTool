package com.iccuu.general_web_backend.infrastructure.security;

import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

class JwtTokenProviderTest {

    private static JwtTokenProvider provider;

    @BeforeAll
    static void setup() {
        provider = new JwtTokenProvider(
            "classpath:jwt-private.pem",
            "classpath:jwt-public.pem",
            900,
            604800
        );
    }

    @Test
    void createAccessTokenShouldReturnNonEmptyString() {
        String token = provider.createAccessToken(1L, "testuser", List.of("user:read"));
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void createAccessTokenShouldHaveThreeParts() {
        String token = provider.createAccessToken(1L, "testuser", List.of());
        assertEquals(3, token.split("\\.").length, "JWT should have header.payload.signature");
    }

    @Test
    void parseValidTokenShouldReturnClaims() {
        String token = provider.createAccessToken(42L, "Alice", List.of("user:read", "user:write"));
        JwtClaims claims = provider.parseToken(token);
        assertNotNull(claims, "valid token should parse successfully");
        try { assertEquals("42", claims.getSubject()); } catch (Exception e) { fail(e); }
    }

    @Test
    void parseTokenShouldGetUsernameClaim() {
        String token = provider.createAccessToken(1L, "Bob", List.of());
        JwtClaims claims = provider.parseToken(token);
        assertEquals("Bob", JwtTokenProvider.getClaimString(claims, "username"));
    }

    @Test
    void createRefreshTokenShouldReturnNonEmptyString() {
        String token = provider.createRefreshToken(1L, "family-1");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void isTokenExpiredShouldReturnFalseForFreshToken() {
        String token = provider.createAccessToken(1L, "user", List.of());
        assertFalse(provider.isTokenExpired(token), "fresh token should not be expired");
    }

    @Test
    void getJwksShouldReturnKeySet() {
        var jwks = provider.getJwks();
        assertTrue(jwks.containsKey("keys"));
    }
}
