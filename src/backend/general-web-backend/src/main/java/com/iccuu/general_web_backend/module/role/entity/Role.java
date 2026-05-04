package com.iccuu.general_web_backend.module.role.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("roles")
public class Role {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String roleName;
    private String description;
    private Integer parentRoleId;
    private Integer isSystem;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic(value = "0")
    private Long deletedAt;
}
