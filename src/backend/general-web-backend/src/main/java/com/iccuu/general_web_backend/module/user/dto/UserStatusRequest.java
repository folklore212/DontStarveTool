package com.iccuu.general_web_backend.module.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusRequest {

    @NotNull(message = "状态不能为空")
    private Integer status;

    private Long lockedUntil;
}
