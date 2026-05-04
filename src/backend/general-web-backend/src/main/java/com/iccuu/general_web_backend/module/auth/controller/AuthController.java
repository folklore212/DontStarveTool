package com.iccuu.general_web_backend.module.auth.controller;

import com.iccuu.general_web_backend.common.annotation.RateLimit;
import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.common.util.SecurityUtil;
import com.iccuu.general_web_backend.infrastructure.geetest.GeeTestProperties;
import com.iccuu.general_web_backend.module.auth.dto.*;
import com.iccuu.general_web_backend.module.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final GeeTestProperties geeTestProperties;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return R.ok();
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    @RateLimit(key = "login", permits = 5, windowSeconds = 60)
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public R<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return R.ok(authService.refresh(request));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader(value = "Authorization", required = false) String auth,
                          @RequestBody(required = false) RefreshTokenRequest request) {
        String accessToken = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : "";
        authService.logout(accessToken, request != null ? request.getRefreshToken() : "");
        return R.ok();
    }

    @Operation(summary = "修改密码")
    @PostMapping("/password/change")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        authService.changePassword(userId, request);
        return R.ok();
    }

    @Operation(summary = "重置密码")
    @PostMapping("/password/reset")
    public R<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return R.ok();
    }

    @Operation(summary = "发送验证码")
    @PostMapping("/code/send")
    @RateLimit(key = "code_send", permits = 3, windowSeconds = 300)
    public R<Void> sendCode(@Valid @RequestBody SendCodeRequest request,
                            @RequestHeader(value = "Accept-Language", defaultValue = "zh-CN") String locale) {
        authService.sendCode(request, locale);
        return R.ok();
    }

    @Operation(summary = "验证验证码")
    @PostMapping("/code/verify")
    public R<Boolean> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        return R.ok(authService.verifyCode(request));
    }

    @Operation(summary = "获取验证码配置（公开接口）")
    @GetMapping("/captcha-config")
    public R<Map<String, String>> captchaConfig() {
        return R.ok(Map.of(
            "loginCaptchaId", geeTestProperties.getLogin().getCaptchaId(),
            "registerCaptchaId", geeTestProperties.getRegister().getCaptchaId()
        ));
    }

    @Operation(summary = "验证令牌")
    @GetMapping("/token/validate")
    public R<TokenValidationResponse> validateToken(@RequestHeader("Authorization") String auth) {
        String token = auth.startsWith("Bearer ") ? auth.substring(7) : auth;
        return R.ok(authService.validateToken(token));
    }

    @Operation(summary = "GDPR数据导出")
    @PostMapping("/me/export")
    public R<Map<String, Object>> exportData() {
        return R.ok(authService.exportUserData());
    }

    @Operation(summary = "GDPR删除")
    @PostMapping("/me/forget-me")
    public R<Void> forgetMe() {
        authService.forgetMe(SecurityUtil.getCurrentUserId());
        return R.ok();
    }
}
