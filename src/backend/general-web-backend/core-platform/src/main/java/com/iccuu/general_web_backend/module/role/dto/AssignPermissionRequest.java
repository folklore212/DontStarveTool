package com.iccuu.general_web_backend.module.role.dto;
import jakarta.validation.constraints.NotEmpty;

import lombok.Data;

import java.util.List;

@Data
public class AssignPermissionRequest {
    private List<Integer> permissionIds;
    private Integer scopeId;
}
