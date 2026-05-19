package com.iccuu.general_web_backend.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum LoginResult {
    SUCCESS("success"), FAILED_CREDENTIAL("failed_credential"),
    FAILED_MFA("failed_mfa"), FAILED_LOCKED("failed_locked"),
    FAILED_DISABLED("failed_disabled");
    @EnumValue private final String value;
    LoginResult(String value) { this.value = value; }
}
