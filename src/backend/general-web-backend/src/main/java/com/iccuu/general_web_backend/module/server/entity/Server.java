package com.iccuu.general_web_backend.module.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("servers")
public class Server {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String name;
    private String host;
    private Integer port;
    private String username;
    private String authType;
    private String password;
    private Long sshKeyId;
    private String tags;
    private Integer sortOrder;
    private String osInfo;
    private Integer cpuCores;
    private java.math.BigDecimal memGb;
    private java.math.BigDecimal diskGb;
    private Integer steamcmdInstalled;
    private String dstVersion;
    private String status;
    private LocalDateTime lastTestAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic(value = "0", delval = "NOW()")
    private Long deletedAt;
}
