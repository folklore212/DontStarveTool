package com.iccuu.general_web_backend.module.role.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_roles")
public class UserRole {
    private Long userId;
    private Integer roleId;
    private String scopeType;
    private String scopeValue;
    private Long grantedBy;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    @TableLogic(value = "0")
    private Long deletedAt;
}
