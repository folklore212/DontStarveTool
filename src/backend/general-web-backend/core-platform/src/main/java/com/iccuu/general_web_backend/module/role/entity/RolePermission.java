package com.iccuu.general_web_backend.module.role.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("role_permissions")
public class RolePermission {
    private Integer roleId;
    private Integer permissionId;
    private Integer scopeId;
}
