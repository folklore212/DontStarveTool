package com.iccuu.general_web_backend.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度在3-50之间")
    private String username;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^[1-9]\\d{4,14}$", message = "手机号格式不正确")
    private String phone;

    private String nickname;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度在6-100之间")
    private String password;
}
