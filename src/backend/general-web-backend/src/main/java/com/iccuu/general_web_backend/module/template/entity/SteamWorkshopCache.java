package com.iccuu.general_web_backend.module.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("steam_workshop_cache")
public class SteamWorkshopCache {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String workshopId;
    private String title;
    private String description;
    private String previewUrl;
    private String authorName;
    private Integer subscriptions;
    private Integer favorited;
    private String tags;
    private LocalDateTime lastUpdated;
    private LocalDateTime createdAt;
}
