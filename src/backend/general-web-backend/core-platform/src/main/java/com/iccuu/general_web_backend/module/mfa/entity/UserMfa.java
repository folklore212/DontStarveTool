package com.iccuu.general_web_backend.module.mfa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "user_mfa", autoResultMap = true)
public class UserMfa {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String mfaType;
    private String secret;
    private Integer isEnabled;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String backupCodes;
    private Integer keyVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
