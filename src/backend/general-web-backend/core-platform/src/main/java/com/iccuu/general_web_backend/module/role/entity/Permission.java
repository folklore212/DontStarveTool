package com.iccuu.general_web_backend.module.role.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("permissions")
public class Permission {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String code;
    private String name;
    private String resourceType;
    private String action;
    private String description;
    private LocalDateTime createdAt;
}
