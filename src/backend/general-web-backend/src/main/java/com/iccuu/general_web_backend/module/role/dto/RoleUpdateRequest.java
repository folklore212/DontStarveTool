package com.iccuu.general_web_backend.module.role.dto;

import lombok.Data;

@Data
public class RoleUpdateRequest {
    private String description;
    private Integer parentRoleId;
}
