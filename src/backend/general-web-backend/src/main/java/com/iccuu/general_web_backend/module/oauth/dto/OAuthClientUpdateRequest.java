package com.iccuu.general_web_backend.module.oauth.dto;

import lombok.Data;

import java.util.List;

@Data
public class OAuthClientUpdateRequest {

    private String clientName;

    private String grantTypes;

    private List<String> redirectUris;

    private List<String> allowedScopes;

    private Integer isTrusted;

    private Integer status;
}
