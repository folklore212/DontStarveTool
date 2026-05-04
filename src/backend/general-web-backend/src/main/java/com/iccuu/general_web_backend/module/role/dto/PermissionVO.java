package com.iccuu.general_web_backend.module.role.dto;

import lombok.Data;

@Data
public class PermissionVO {
    private Integer id;
    private String code;
    private String name;
    private String resourceType;
    private String action;
    private String description;
}
