package com.iccuu.general_web_backend.module.role.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleCreateRequest {
    @NotBlank(message = "角色名称不能为空")
    private String roleName;
    private String description;
    private Integer parentRoleId;
    private Integer isSystem;
}
