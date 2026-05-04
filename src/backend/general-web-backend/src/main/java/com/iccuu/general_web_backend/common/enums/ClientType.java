package com.iccuu.general_web_backend.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ClientType {
    CONFIDENTIAL("confidential"), PUBLIC("public");
    @EnumValue private final String value;
    ClientType(String value) { this.value = value; }
}
