package com.iccuu.general_web_backend.module.apikey.controller;

import com.iccuu.general_web_backend.common.annotation.RequirePermission;
import com.iccuu.general_web_backend.common.result.PageQuery;
import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.common.util.SecurityUtil;
import com.iccuu.general_web_backend.module.apikey.dto.ApiKeyCreateRequest;
import com.iccuu.general_web_backend.module.apikey.service.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping
    public R<?> list(@Valid PageQuery query) {
        Long userId = SecurityUtil.getCurrentUserId();
        return R.ok(apiKeyService.listByUser(userId, query));
    }

    @PostMapping
    @RequirePermission("apikey:create")
    public R<?> create(@Valid @RequestBody ApiKeyCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return R.ok(apiKeyService.create(userId, request));
    }

    @DeleteMapping("/{keyId}")
    @RequirePermission("apikey:revoke")
    public R<?> revoke(@PathVariable Long keyId) {
        Long userId = SecurityUtil.getCurrentUserId();
        apiKeyService.revoke(keyId, userId);
        return R.ok();
    }

    @PatchMapping("/{keyId}/rotate")
    @RequirePermission("apikey:rotate")
    public R<?> rotate(@PathVariable Long keyId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return R.ok(apiKeyService.rotate(keyId, userId));
    }
}
