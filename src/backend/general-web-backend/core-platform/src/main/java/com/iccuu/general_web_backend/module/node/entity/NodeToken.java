package com.iccuu.general_web_backend.module.node.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("node_tokens")
public class NodeToken {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long serverId;

    private String tokenHash;

    private String tokenPrefix;

    private Integer status;

    private LocalDateTime lastUsedAt;

    private LocalDateTime createdAt;

    @TableLogic
    private Long deletedAt;
}
