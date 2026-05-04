package com.iccuu.general_web_backend.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^[1-9]\\d{4,14}$", message = "手机号格式不正确")
    private String phone;

    private String nickname;

    private String avatar;
}
