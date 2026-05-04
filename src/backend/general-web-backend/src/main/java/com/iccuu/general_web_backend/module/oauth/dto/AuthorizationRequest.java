package com.iccuu.general_web_backend.module.oauth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthorizationRequest {

    @NotBlank(message = "responseType不能为空")
    private String responseType;

    @NotBlank(message = "clientId不能为空")
    private String clientId;

    @NotBlank(message = "redirectUri不能为空")
    private String redirectUri;

    private String scope;

    private String state;

    private String codeChallenge;

    private String codeChallengeMethod;
}
