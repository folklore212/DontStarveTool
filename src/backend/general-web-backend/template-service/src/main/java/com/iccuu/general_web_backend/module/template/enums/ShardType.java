package com.iccuu.general_web_backend.module.template.enums;

public enum ShardType {
    MASTER("master"),
    CAVES("caves");

    private final String value;

    ShardType(String value) { this.value = value; }

    public String getValue() { return value; }
}
