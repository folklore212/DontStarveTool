package com.iccuu.general_web_backend.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BindAuthRequest {

    @NotBlank(message = "身份类型不能为空")
    private String identityType;

    @NotBlank(message = "标识符不能为空")
    private String identifier;

    private String credential;

    private Integer isPrimary;
}
