package com.iccuu.general_web_backend.module.apikey.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiKeyCreateRequest {

    @NotBlank(message = "keyName不能为空")
    private String keyName;

    private String allowedScopes;

    private LocalDateTime expiresAt;
}
