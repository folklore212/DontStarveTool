package com.iccuu.general_web_backend.module.oauth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenExchangeRequest {

    @NotBlank(message = "grantType不能为空")
    private String grantType;

    @NotBlank(message = "code不能为空")
    private String code;

    @NotBlank(message = "clientId不能为空")
    private String clientId;

    @NotBlank(message = "clientSecret不能为空")
    private String clientSecret;

    private String redirectUri;

    private String codeVerifier;
}
