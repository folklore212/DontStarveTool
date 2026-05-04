package com.iccuu.general_web_backend.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "user_profiles", autoResultMap = true)
public class UserProfile {
    @TableId(type = IdType.INPUT)
    private Long userId;
    private String realName;
    private String locale;
    private String timezone;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
