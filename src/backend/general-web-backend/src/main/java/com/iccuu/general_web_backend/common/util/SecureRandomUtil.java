package com.iccuu.general_web_backend.common.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class SecureRandomUtil {
    private SecureRandomUtil() {}

    public static final SecureRandom INSTANCE = new SecureRandom();

    public static String generateSecureToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        INSTANCE.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
