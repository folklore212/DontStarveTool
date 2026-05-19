package com.iccuu.general_web_backend.module.audit.controller;

import com.iccuu.general_web_backend.common.annotation.RequirePermission;
import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.module.auth.dto.LoginLogQueryRequest;
import com.iccuu.general_web_backend.module.auth.service.LoginLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/login-logs")
@RequiredArgsConstructor
public class LoginLogController {

    private final LoginLogService loginLogService;

    @GetMapping
    @RequirePermission("audit:read")
    public R<?> query(@Valid LoginLogQueryRequest request) {
        return R.ok(loginLogService.queryPage(request));
    }
}
