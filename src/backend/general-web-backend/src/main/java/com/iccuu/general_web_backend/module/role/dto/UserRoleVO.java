package com.iccuu.general_web_backend.module.role.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRoleVO {
    private Integer roleId;
    private String roleName;
    private String scopeType;
    private String scopeValue;
    private Long grantedBy;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
