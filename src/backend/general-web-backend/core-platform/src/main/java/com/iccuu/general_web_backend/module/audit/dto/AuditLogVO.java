package com.iccuu.general_web_backend.module.audit.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogVO {

    private Long id;

    private Long userId;

    private String clientId;

    private String action;

    private String resourceType;

    private String resourceId;

    private Object detail;

    private String ipAddress;

    private String sessionId;

    private String requestId;

    private String clientIpChain;

    private LocalDateTime createdAt;
}
