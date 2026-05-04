package com.iccuu.general_web_backend.module.role.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssignRoleRequest {
    private List<Integer> roleIds;
    private String scopeType;
    private String scopeValue;
    private LocalDateTime expiresAt;
}
