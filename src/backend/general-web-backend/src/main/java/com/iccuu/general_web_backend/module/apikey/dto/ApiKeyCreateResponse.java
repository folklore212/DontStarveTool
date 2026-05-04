package com.iccuu.general_web_backend.module.apikey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyCreateResponse {

    private String keyPrefix;

    private String rawKey;

    private LocalDateTime expiresAt;
}
