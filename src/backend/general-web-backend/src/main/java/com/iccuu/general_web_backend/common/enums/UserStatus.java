package com.iccuu.general_web_backend.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UserStatus {
    NORMAL(0), DISABLED(1), PENDING(2), LOCKED(3);
    @EnumValue private final int value;
    UserStatus(int value) { this.value = value; }
}
