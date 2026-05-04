package com.iccuu.general_web_backend.module.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "audit_logs", autoResultMap = true)
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String clientId;
    private String action;
    private String resourceType;
    private String resourceId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object detail;
    private String ipAddress;
    private String userAgent;
    private String sessionId;
    private String requestId;
    private String clientIpChain;
    private LocalDateTime createdAt;
    private LocalDate createdDate;
}
