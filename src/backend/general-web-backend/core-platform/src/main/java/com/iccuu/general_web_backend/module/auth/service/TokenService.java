package com.iccuu.general_web_backend.module.auth.service;

import com.iccuu.general_web_backend.module.auth.dto.LoginResponse;
import org.jose4j.jwt.JwtClaims;

import java.util.List;

public interface TokenService {
    LoginResponse createTokens(Long userId, String username, List<String> permissions);
    LoginResponse refreshToken(String refreshToken);
    void logout(String accessToken, String refreshToken);
    boolean isBlacklisted(String jti);
    JwtClaims parseAccessToken(String token);
}
