package com.iccuu.general_web_backend.common.constant;

public final class Constants {
    private Constants() {}

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    public static final int BCRYPT_COST = 12;
    public static final int MAX_PASSWORD_HISTORY = 10;
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    public static final long LOCKOUT_DURATION_MS = 30 * 60 * 1000L;
    public static final long SOFT_DELETE_RETENTION_MS = 90L * 24 * 3600 * 1000;
    public static final long LOG_RETENTION_MONTHS = 12;
    public static final String API_KEY_PREFIX = "dsk-";
    public static final int API_KEY_RAW_LENGTH = 43;
    public static final int VERIFICATION_CODE_LENGTH = 6;
    public static final int VERIFICATION_CODE_TTL_SECONDS = 300;
    public static final int BACKUP_CODE_COUNT = 10;
    public static final int BACKUP_CODE_LENGTH = 8;
    public static final int ACCESS_TOKEN_TTL_SECONDS = 900;
    public static final int REFRESH_TOKEN_TTL_SECONDS = 604800;
    public static final int JWT_BLACKLIST_TTL_BUFFER = 60;
    public static final int MAX_EXPORT_RECORDS = 10000;
}
