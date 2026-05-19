package com.iccuu.general_web_backend.module.apikey.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "api_keys", autoResultMap = true)
public class ApiKey {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String keyName;
    private String keyHash;
    private String keyPrefix;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String allowedScopes;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private Integer status;
    private LocalDateTime createdAt;
    @TableLogic(value = "0")
    private Long deletedAt;
}
