package com.iccuu.general_web_backend.common.constant;

public enum ErrorCode {
    SUCCESS(0, "ok"),

    // 10xxx: Auth / Token
    UNAUTHORIZED(10001, "auth.unauthorized"),
    INVALID_CREDENTIALS(10002, "auth.invalid_credentials"),
    TOKEN_EXPIRED(10003, "auth.token_expired"),
    TOKEN_BLACKLISTED(10004, "auth.token_blacklisted"),
    MFA_REQUIRED(10005, "auth.mfa_required"),
    MFA_INVALID(10006, "auth.mfa_invalid"),
    ACCOUNT_LOCKED(10007, "auth.account_locked"),
    ACCOUNT_DISABLED(10008, "auth.account_disabled"),
    ACCOUNT_PENDING(10009, "auth.account_pending"),
    GEE_TEST_FAILED(10010, "auth.gee_test_failed"),
    REFRESH_TOKEN_REPLAY(10011, "auth.refresh_token_replay"),
    TOKEN_INVALID(10012, "auth.token_invalid"),
    CAPTCHA_REQUIRED(10013, "auth.captcha_required"),

    // 11xxx: Validation
    VALIDATION_ERROR(11001, "validation.error"),

    // 40xxx: Business
    USER_NOT_FOUND(40001, "user.not_found"),
    USERNAME_EXISTS(40002, "user.username_exists"),
    EMAIL_EXISTS(40003, "user.email_exists"),
    PHONE_EXISTS(40004, "user.phone_exists"),
    IDENTITY_TAKEN(40005, "user.identity_taken"),
    LAST_IDENTITY(40006, "user.last_identity"),
    USER_ALREADY_ACTIVATED(40007, "user.already_activated"),
    ROLE_NOT_FOUND(40010, "role.not_found"),
    ROLE_SYSTEM_PROTECTED(40011, "role.system_protected"),
    ROLE_NAME_EXISTS(40012, "role.name_exists"),
    ROLE_IN_USE(40013, "role.in_use"),
    PERMISSION_NOT_FOUND(40020, "permission.not_found"),
    CLIENT_NOT_FOUND(40030, "client.not_found"),
    CLIENT_ID_EXISTS(40031, "client.id_exists"),
    OAUTH_CLIENT_DISABLED(40032, "oauth.client_disabled"),
    OAUTH_CODE_INVALID(40033, "oauth.code_invalid"),
    API_KEY_NOT_FOUND(40040, "api_key.not_found"),
    AUDIT_LOG_NOT_FOUND(40080, "audit_log.not_found"),
    VERIFICATION_CODE_INVALID(40050, "verification.code_invalid"),
    VERIFICATION_CODE_EXPIRED(40051, "verification.code_expired"),
    PASSWORD_REUSED(40060, "password.reused"),
    PASSWORD_SAME(40061, "password.same"),
    MFA_ALREADY_ENABLED(40070, "mfa.already_enabled"),
    MFA_NOT_ENABLED(40071, "mfa.not_enabled"),

    // 50xxx: System
    INTERNAL_ERROR(50001, "system.internal_error"),
    SERVICE_UNAVAILABLE(50002, "system.service_unavailable"),
    RATE_LIMITED(50003, "system.rate_limited");

    private final int code;
    private final String messageKey;

    ErrorCode(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }

    public int getCode() {
        return code;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public static ErrorCode fromCode(int code) {
        for (ErrorCode ec : values()) {
            if (ec.code == code) return ec;
        }
        return INTERNAL_ERROR;
    }
}
