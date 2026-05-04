package com.iccuu.general_web_backend.module.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("market_configs")
public class MarketConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long authorId;
    private String title;
    private String description;
    private String tags;
    private String screenshots;
    private String configJson;
    private String modList;
    private String category;
    private String gameMode;
    private Integer downloadCount;
    private java.math.BigDecimal ratingAvg;
    private Integer ratingCount;
    private Integer version;
    private String status;
    private Integer verified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
