package com.iccuu.general_web_backend.infrastructure.storage;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("scheduled_tasks")
public class ScheduledTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskType;

    private String taskKey;

    private String payloadJson;

    private LocalDateTime executeAt;

    /** 0=PENDING, 1=RUNNING, 2=COMPLETED, 3=FAILED */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime executedAt;

    private String errorMessage;
}
