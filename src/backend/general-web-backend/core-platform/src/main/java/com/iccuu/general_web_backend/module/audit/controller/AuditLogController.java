package com.iccuu.general_web_backend.module.audit.controller;

import com.iccuu.general_web_backend.common.annotation.RequirePermission;
import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.module.audit.dto.AuditLogQueryRequest;
import com.iccuu.general_web_backend.module.audit.service.AuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @RequirePermission("audit:read")
    public R<?> query(@Valid AuditLogQueryRequest request) {
        return R.ok(auditLogService.query(request));
    }

    @GetMapping("/{id}")
    public R<?> getById(@PathVariable Long id) {
        return R.ok(auditLogService.getById(id));
    }

    @GetMapping("/export")
    @RequirePermission("audit:read")
    public R<?> exportCsv(@Valid AuditLogQueryRequest request) {
        return R.ok("CSV export placeholder - filtering by: " + request);
    }
}
