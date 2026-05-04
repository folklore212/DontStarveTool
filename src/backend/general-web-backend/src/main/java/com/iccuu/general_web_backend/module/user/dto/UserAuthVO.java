package com.iccuu.general_web_backend.module.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserAuthVO {

    private Long id;

    private String identityType;

    private String identifier;

    private Integer verified;

    private Integer isPrimary;

    private LocalDateTime createdAt;
}
