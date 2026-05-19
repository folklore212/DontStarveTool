package com.iccuu.general_web_backend.module.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long userId;

    private String username;

    private String email;

    private String phone;

    private String nickname;

    private String avatar;

    private Integer status;

    private Long lockedUntil;

    private LocalDateTime lastLoginAt;

    private String lastLoginIp;

    private LocalDateTime passwordChangedAt;

    private LocalDateTime createdAt;
}
