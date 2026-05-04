package com.iccuu.general_web_backend.module.mfa.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaDisableRequest {
    @NotBlank
    private String password;
}
