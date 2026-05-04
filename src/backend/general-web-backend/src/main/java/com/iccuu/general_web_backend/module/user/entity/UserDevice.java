package com.iccuu.general_web_backend.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_devices")
public class UserDevice {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String deviceHash;
    private String deviceName;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private LocalDate createdDate;
    private Integer isTrusted;
}
