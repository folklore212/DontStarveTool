package com.iccuu.general_web_backend.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendCodeRequest {
    @NotBlank
    private String identifier;
    @NotBlank
    private String identityType;
    @NotBlank
    private String purpose;
    private String captchaOutput;
    private String lotNumber;
    private String passToken;
    private String genTime;
}
