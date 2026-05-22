package com.iccuu.general_web_backend.module.auth.dto;
import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    @jakarta.validation.constraints.NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
