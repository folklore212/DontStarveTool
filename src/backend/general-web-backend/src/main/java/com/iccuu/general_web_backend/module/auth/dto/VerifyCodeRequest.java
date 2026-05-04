package com.iccuu.general_web_backend.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyCodeRequest {
    @NotBlank
    private String identifier;
    @NotBlank
    private String code;
    @NotBlank
    private String purpose;
}
