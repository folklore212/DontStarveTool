package com.iccuu.general_web_backend.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum MfaType {
    TOTP("totp"), SMS("sms"), EMAIL("email"), WEBAUTHN("webauthn");
    @EnumValue private final String value;
    MfaType(String value) { this.value = value; }
}
