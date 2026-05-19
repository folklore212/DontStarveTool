package com.iccuu.general_web_backend.module.template.enums;

public enum TemplateType {
    SERVER_TEMPLATE("server_template"),
    WORLD_GEN("world_gen");

    private final String value;

    TemplateType(String value) { this.value = value; }

    public String getValue() { return value; }
}
