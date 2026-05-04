package com.iccuu.general_web_backend.module.user.dto;

import lombok.Data;

@Data
public class UserProfileUpdateRequest {

    private String realName;

    private String locale;

    private String timezone;

    private String metadata;
}
