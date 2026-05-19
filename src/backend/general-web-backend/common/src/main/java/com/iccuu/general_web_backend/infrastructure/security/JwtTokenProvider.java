package com.iccuu.general_web_backend.infrastructure.security;

import cn.hutool.crypto.PemUtil;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final long accessTokenTtl;
    private final long refreshTokenTtl;

    public JwtTokenProvider(@Value("${jwt.private-key:classpath:jwt-private.pem}") String privateKeyPath,
                            @Value("${jwt.public-key:classpath:jwt-public.pem}") String publicKeyPath,
                            @Value("${jwt.access-token-ttl:900}") long accessTokenTtl,
                            @Value("${jwt.refresh-token-ttl:604800}") long refreshTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
        try {
            byte[] privateKeyBytes = readKeyBytes(privateKeyPath);
            this.privateKey = (RSAPrivateKey) PemUtil.readPemPrivateKey(
                    new ByteArrayInputStream(privateKeyBytes));
            byte[] publicKeyBytes = readKeyBytes(publicKeyPath);
            this.publicKey = (RSAPublicKey) PemUtil.readPemPublicKey(
                    new ByteArrayInputStream(publicKeyBytes));
        } catch (Exception e) {
            log.error("Failed to load RSA keys", e);
            throw new RuntimeException("Failed to initialize JWT key material", e);
        }
    }

    private byte[] readKeyBytes(String path) throws Exception {
        if (path.startsWith("classpath:")) {
            var resource = new org.springframework.core.io.ClassPathResource(path.substring("classpath:".length()));
            return resource.getInputStream().readAllBytes();
        }
        return Files.readAllBytes(Path.of(path));
    }

    public String createAccessToken(Long userId, String username, List<String> permissions) {
        JwtClaims claims = new JwtClaims();
        claims.setSubject(String.valueOf(userId));
        claims.setClaim("username", username);
        claims.setClaim("perm", permissions != null ? permissions.toArray(new String[0]) : new String[0]);
        claims.setIssuedAtToNow();
        claims.setExpirationTimeMinutesInTheFuture(accessTokenTtl / 60.0f);
        claims.setJwtId(UUID.randomUUID().toString());
        claims.setClaim("type", ACCESS_TOKEN_TYPE);
        return sign(claims);
    }

    public String createRefreshToken(Long userId, String tokenFamily) {
        JwtClaims claims = new JwtClaims();
        claims.setSubject(String.valueOf(userId));
        claims.setIssuedAtToNow();
        claims.setExpirationTimeMinutesInTheFuture(refreshTokenTtl / 60.0f);
        claims.setJwtId(UUID.randomUUID().toString());
        claims.setClaim("type", REFRESH_TOKEN_TYPE);
        claims.setClaim("family", tokenFamily);
        return sign(claims);
    }

    public JwtClaims parseToken(String token) {
        try {
            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setVerificationKey(publicKey)
                    .setJwsAlgorithmConstraints(
                            AlgorithmConstraints.ConstraintType.PERMIT, AlgorithmIdentifiers.RSA_USING_SHA256)
                    .setRequireExpirationTime()
                    .setAllowedClockSkewInSeconds(30)
                    .build();
            return jwtConsumer.processToClaims(token);
        } catch (InvalidJwtException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }

    public boolean isTokenExpired(String token) {
        JwtClaims claims = parseToken(token);
        if (claims == null) return true;
        try {
            return claims.getExpirationTime().getValueInMillis() <= System.currentTimeMillis();
        } catch (Exception e) {
            return true;
        }
    }

    public static String getClaimString(JwtClaims claims, String name) {
        try {
            return claims.getStringClaimValue(name);
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Object> getJwks() {
        BigInteger modulus = publicKey.getModulus();
        BigInteger exponent = publicKey.getPublicExponent();

        String n = base64UrlEncode(modulus.toByteArray());
        String e = base64UrlEncode(exponent.toByteArray());

        Map<String, Object> key = Map.of(
                "kty", "RSA",
                "kid", "key-2026-04",
                "use", "sig",
                "alg", "RS256",
                "n", n,
                "e", e
        );

        return Map.of("keys", List.of(key));
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sign(JwtClaims claims) {
        try {
            JsonWebSignature jws = new JsonWebSignature();
            jws.setPayload(claims.toJson());
            jws.setKey(privateKey);
            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
            return jws.getCompactSerialization();
        } catch (Exception e) {
            log.error("Failed to sign JWT", e);
            throw new RuntimeException("Failed to create JWT token", e);
        }
    }
}
