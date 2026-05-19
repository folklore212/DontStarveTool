package com.iccuu.general_web_backend.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_auths")
public class UserAuth {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String identityType;
    private String identifier;
    private String credential;
    @TableField("is_verified")
    private Integer verified;
    private Integer isPrimary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic(value = "0")
    private Long deletedAt;
}
