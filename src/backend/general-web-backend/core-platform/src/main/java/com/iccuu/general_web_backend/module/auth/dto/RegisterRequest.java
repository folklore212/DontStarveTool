package com.iccuu.general_web_backend.module.auth.dto;

import com.iccuu.general_web_backend.common.validation.PasswordComplexity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@PasswordComplexity
public class RegisterRequest {
    @NotBlank @Size(min = 3, max = 64)
    private String username;
    @Email
    private String email;
    private String phone;
    @NotBlank @Size(min = 8, max = 128)
    private String password;
    @NotBlank
    private String identityType;
    @NotBlank
    private String verificationCode;
}
