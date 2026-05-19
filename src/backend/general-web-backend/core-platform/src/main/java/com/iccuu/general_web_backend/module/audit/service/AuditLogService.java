package com.iccuu.general_web_backend.module.audit.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iccuu.general_web_backend.module.audit.dto.*;

public interface AuditLogService {

    record AuditLogRecord(Long userId, String clientId, String action, String resourceType,
                          String resourceId, Object detail, String ipAddress, String userAgent,
                          String sessionId, String requestId, String clientIpChain) {}

    IPage<AuditLogVO> query(AuditLogQueryRequest request);

    AuditLogVO getById(Long id);

    void record(AuditLogRecord record);

    java.util.List<AuditLogVO> exportByUserId(Long userId);

    void anonymizeByUserId(Long userId);
}
