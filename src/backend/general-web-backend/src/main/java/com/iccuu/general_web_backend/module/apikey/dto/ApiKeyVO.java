package com.iccuu.general_web_backend.module.apikey.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiKeyVO {

    private Long id;

    private String keyName;

    private String keyPrefix;

    private String allowedScopes;

    private LocalDateTime expiresAt;

    private LocalDateTime lastUsedAt;

    private Integer status;

    private LocalDateTime createdAt;
}
