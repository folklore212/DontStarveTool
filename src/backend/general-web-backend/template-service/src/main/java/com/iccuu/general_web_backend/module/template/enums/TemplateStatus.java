package com.iccuu.general_web_backend.module.template.enums;

public enum TemplateStatus {
    PUBLISHED("published"),
    DRAFT("draft"),
    ARCHIVED("archived");

    private final String value;

    TemplateStatus(String value) { this.value = value; }

    public String getValue() { return value; }
}
