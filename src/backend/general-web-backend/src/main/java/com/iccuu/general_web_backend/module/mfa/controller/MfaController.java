package com.iccuu.general_web_backend.module.mfa.controller;

import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.common.util.SecurityUtil;
import com.iccuu.general_web_backend.module.mfa.dto.*;
import com.iccuu.general_web_backend.module.mfa.service.UserMfaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "MFA")
@RestController
@RequestMapping("/api/v1/mfa")
@RequiredArgsConstructor
public class MfaController {

    private final UserMfaService userMfaService;

    @Operation(summary = "列出已启用的MFA方式")
    @GetMapping("/status")
    public R<List<MfaStatusVO>> getStatus() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return R.fail(10001, "未登录");
        return R.ok(userMfaService.getStatus(userId));
    }

    @Operation(summary = "开始TOTP设置")
    @PostMapping("/setup/init")
    public R<MfaSetupInitResponse> setupInit(@Valid @RequestBody MfaSetupInitRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return R.fail(10001, "未登录");
        return R.ok(userMfaService.setupInit(userId, request));
    }

    @Operation(summary = "验证并启用MFA")
    @PostMapping("/setup/verify")
    public R<Void> setupVerify(@Valid @RequestBody MfaEnableRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return R.fail(10001, "未登录");
        userMfaService.setupVerify(userId, request);
        return R.ok();
    }

    @Operation(summary = "禁用MFA")
    @PostMapping("/disable")
    public R<Void> disable(@Valid @RequestBody MfaDisableRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return R.fail(10001, "未登录");
        userMfaService.disable(userId, request);
        return R.ok();
    }

    @Operation(summary = "获取/重新生成备用恢复码")
    @GetMapping("/backup-codes")
    public R<List<String>> getBackupCodes() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return R.fail(10001, "未登录");
        return R.ok(userMfaService.getBackupCodes(userId));
    }
}
