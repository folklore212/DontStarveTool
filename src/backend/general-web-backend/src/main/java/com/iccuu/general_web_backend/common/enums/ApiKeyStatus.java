package com.iccuu.general_web_backend.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ApiKeyStatus {
    DISABLED(0), NORMAL(1);
    @EnumValue private final int value;
    ApiKeyStatus(int value) { this.value = value; }
}
