package com.iccuu.general_web_backend.module.oauth.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OAuthClientVO {

    private Long id;

    private String clientId;

    private String clientName;

    private String clientType;

    private String grantTypes;

    private List<String> redirectUris;

    private List<String> allowedScopes;

    private Integer isTrusted;

    private Integer status;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
