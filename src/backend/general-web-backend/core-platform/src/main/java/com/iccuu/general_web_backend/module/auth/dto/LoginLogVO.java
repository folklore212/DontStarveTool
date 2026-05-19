package com.iccuu.general_web_backend.module.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginLogVO {

    private Long id;

    private Long userId;

    private String identifierHash;

    private String identityType;

    private String authMethod;

    private String ipAddress;

    private String result;

    private String failureReason;

    private LocalDateTime createdAt;
}
