package com.iccuu.general_web_backend.module.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("api_tokens")
public class ApiToken {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String token;
    private String scope;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
