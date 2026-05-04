package com.iccuu.general_web_backend.module.oauth.entity;

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
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "oauth_clients", autoResultMap = true)
public class OAuthClient {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String clientId;
    private String clientSecret;
    private String clientName;
    private String clientType;
    @TableField(exist = false)
    private String grantTypes;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> redirectUris;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> allowedScopes;
    private Integer isTrusted;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic(value = "0")
    private Long deletedAt;
}
