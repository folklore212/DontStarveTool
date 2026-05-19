package com.iccuu.general_web_backend.module.role.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleTreeVO {
    private Integer id;
    private String roleName;
    private String description;
    private Integer parentRoleId;
    private List<RoleTreeVO> children;
}
