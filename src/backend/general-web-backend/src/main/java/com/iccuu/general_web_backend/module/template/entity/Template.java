package com.iccuu.general_web_backend.module.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("templates")
public class Template {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long authorId;
    private String name;
    private String description;
    private String templateType;
    private String category;
    private String gameMode;
    private Integer maxPlayers;
    private String tags;
    private String coverImage;
    private String configJson;
    private String modList;
    private Integer version;
    private Integer downloadCount;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private String status;
    private Integer verified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic(value = "0", delval = "NOW()")
    private Long deletedAt;
}
