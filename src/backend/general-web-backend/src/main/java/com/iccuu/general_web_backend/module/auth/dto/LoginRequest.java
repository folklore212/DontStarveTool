package com.iccuu.general_web_backend.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "标识符不能为空")
    private String identifier;
    @NotBlank(message = "密码不能为空")
    private String credential;
    private String mfaCode;
    private String captchaOutput;
    private String lotNumber;
    private String passToken;
    private String genTime;
}
