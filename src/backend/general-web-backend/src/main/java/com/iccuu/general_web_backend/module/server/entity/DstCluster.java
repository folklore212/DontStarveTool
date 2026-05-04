package com.iccuu.general_web_backend.module.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dst_clusters")
public class DstCluster {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long serverId;
    private Long userId;
    private String name;
    private String displayName;
    private String gameMode;
    private Integer maxPlayers;
    private String password;
    private String clusterToken;
    private Integer masterPort;
    private Integer steamPort;
    private Integer hasCaves;
    private String status;
    private Integer playerCount;
    private Integer dayCount;
    private String season;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic(value = "0", delval = "NOW()")
    private Long deletedAt;
}
