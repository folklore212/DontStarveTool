package com.iccuu.general_web_backend.module.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("world_gen_presets")
public class WorldGenPreset {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long templateId;
    private String name;
    private String description;
    private String previewImage;
    private String worldSize;
    private String branching;
    private String loopMode;
    private String seasonStart;
    private String dayMode;
    private String autumnLength;
    private String winterLength;
    private String springLength;
    private String summerLength;
    private String resourceVariety;
    private String extraSettings;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic(value = "0", delval = "NOW()")
    private Long deletedAt;
}
