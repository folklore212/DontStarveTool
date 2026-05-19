package com.iccuu.general_web_backend.module.auth.service;

import com.iccuu.general_web_backend.module.auth.dto.*;

import java.util.Map;

public interface AuthService {
    void register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    LoginResponse refresh(RefreshTokenRequest request);
    void logout(String accessToken, String refreshToken);
    void changePassword(Long userId, ChangePasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    void sendCode(SendCodeRequest request, String locale);
    boolean verifyCode(VerifyCodeRequest request);
    TokenValidationResponse validateToken(String token);
    Map<String, Object> exportUserData();
    void forgetMe(Long userId);
}
