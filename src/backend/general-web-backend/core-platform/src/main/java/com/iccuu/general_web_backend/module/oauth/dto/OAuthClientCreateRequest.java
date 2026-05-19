package com.iccuu.general_web_backend.module.oauth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class OAuthClientCreateRequest {

    @NotBlank(message = "clientId不能为空")
    private String clientId;

    @NotBlank(message = "clientName不能为空")
    private String clientName;

    private String clientType;

    private String grantTypes;

    private List<String> redirectUris;

    private List<String> allowedScopes;

    private Integer isTrusted;
}
