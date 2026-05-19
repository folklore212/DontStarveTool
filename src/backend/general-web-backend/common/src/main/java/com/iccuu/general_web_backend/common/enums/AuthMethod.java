package com.iccuu.general_web_backend.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum AuthMethod {
    PASSWORD("password"), TOTP("totp"), SMS("sms"), OAUTH("oauth"),
    API_KEY("api_key"), SSO("sso");
    @EnumValue private final String value;
    AuthMethod(String value) { this.value = value; }
}
