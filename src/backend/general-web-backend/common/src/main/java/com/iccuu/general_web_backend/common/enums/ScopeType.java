package com.iccuu.general_web_backend.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ScopeType {
    SELF("self"), DEPT("dept"), ORG("org"), ALL("all");
    @EnumValue private final String value;
    ScopeType(String value) { this.value = value; }
}
