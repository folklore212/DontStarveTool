package com.iccuu.general_web_backend.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum IdentityType {
    PHONE("phone"), EMAIL("email"), WECHAT("wechat"), GITHUB("github"),
    GOOGLE("google"), APPLE("apple"), USERNAME("username");
    @EnumValue private final String value;
    IdentityType(String value) { this.value = value; }
}
