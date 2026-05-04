package com.iccuu.general_web_backend.common.constant;

public final class RedisKeyPrefix {
    private RedisKeyPrefix() {}

    public static final String BLACKLIST_JTI = "blacklist:jti:%s";
    public static final String REFRESH_FAMILY = "refresh:family:%s";
    public static final String REFRESH_FAMILY_REVOKED = "refresh:family:%s:revoked";
    public static final String SESSION = "session:%s:%s";
    public static final String OAUTH_CODE = "oauth:code:%s";
    public static final String OAUTH_CODE_PKCE = "oauth:code:%s:pkce";
    public static final String OAUTH_CODE_EXCHANGED = "oauth:code:%s:exchanged";
    public static final String OAUTH_STATE = "oauth:state:%s";
    public static final String OAUTH_CONSENT = "oauth:consent:%s:%s";
    public static final String OAUTH_CLIENT = "oauth:client:%s";
    public static final String OAUTH_LOCK = "oauth:lock:%s";
    public static final String LOCKOUT_FAILED = "lockout:failed:%s";
    public static final String RATELIMIT = "ratelimit:%s:%s";
    public static final String VC = "vc:%s:%s";
    public static final String VC_RL = "vc:rl:%s";
    public static final String GEETEST_RESULT = "geetest:result:%s:%s";
    public static final String PERM_EFFECTIVE = "perm:effective:%s";
    public static final String PERM_ROLES = "perm:roles:%s";
    public static final String APIKEY = "apikey:%s";
    public static final String SNOWFLAKE_WORKER_COUNTER = "snowflake:worker:counter";
    public static final String SNOWFLAKE_WORKER = "snowflake:worker:%s";
    public static final String SNOWFLAKE_AVAILABLE_IDS = "snowflake:available:ids";
    public static final String CACHE_INVALIDATE_PERMISSIONS = "cache:invalidate:permissions";
    public static final String PARTITION_MAINT_LOCK = "partition:maintenance:lock";

    public static String fmt(String pattern, Object... args) {
        return String.format(pattern, args);
    }
}
