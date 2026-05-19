package com.iccuu.general_web_backend.module.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("template_world_gen_bindings")
public class TemplateWorldGenBinding {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long serverTemplateId;
    private Long worldGenPresetId;
    private String shardType;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
