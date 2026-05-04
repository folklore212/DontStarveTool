package com.iccuu.general_web_backend.module.auth.dto;

import com.iccuu.general_web_backend.common.validation.PasswordComplexity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@PasswordComplexity
public class ChangePasswordRequest {
    @NotBlank
    private String oldPassword;
    @NotBlank
    private String newPassword;
}
