package com.iccuu.general_web_backend.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

public class PasswordComplexityValidator implements ConstraintValidator<PasswordComplexity, Object> {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[@$!%*#?&]");

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String password = getFieldValue(value, "password", "newPassword");
        if (password == null || password.isEmpty()) {
            return true;
        }

        // Disable default violation message
        context.disableDefaultConstraintViolation();

        // Rule 1: length
        if (password.length() < MIN_LENGTH) {
            context.buildConstraintViolationWithTemplate(
                    "密码长度不能少于" + MIN_LENGTH + "个字符")
                    .addConstraintViolation();
            return false;
        }
        if (password.length() > MAX_LENGTH) {
            context.buildConstraintViolationWithTemplate(
                    "密码长度不能超过" + MAX_LENGTH + "个字符")
                    .addConstraintViolation();
            return false;
        }

        // Rule 2: must contain at least 3 of 4 categories
        int categories = 0;
        if (UPPERCASE.matcher(password).find()) categories++;
        if (LOWERCASE.matcher(password).find()) categories++;
        if (DIGIT.matcher(password).find()) categories++;
        if (SPECIAL.matcher(password).find()) categories++;
        if (categories < 3) {
            context.buildConstraintViolationWithTemplate(
                    "密码必须包含大写字母、小写字母、数字、特殊字符(@$!%*#?&)中的至少三类")
                    .addConstraintViolation();
            return false;
        }

        // Rule 3: cannot contain username or email (3+ consecutive chars)
        String username = getFieldValue(value, "username");
        String email = getFieldValue(value, "email");

        if (username != null && !username.isEmpty() && username.length() >= 3) {
            String lowerPassword = password.toLowerCase();
            String lowerUsername = username.toLowerCase();
            for (int i = 0; i <= lowerUsername.length() - 3; i++) {
                String substr = lowerUsername.substring(i, i + 3);
                if (lowerPassword.contains(substr)) {
                    context.buildConstraintViolationWithTemplate(
                            "密码不能包含连续3个及以上与用户名相同的字符")
                            .addConstraintViolation();
                    return false;
                }
            }
        }

        if (email != null && !email.isEmpty()) {
            String localPart = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
            if (localPart.length() >= 3) {
                String lowerPassword = password.toLowerCase();
                String lowerLocal = localPart.toLowerCase();
                for (int i = 0; i <= lowerLocal.length() - 3; i++) {
                    String substr = lowerLocal.substring(i, i + 3);
                    if (lowerPassword.contains(substr)) {
                        context.buildConstraintViolationWithTemplate(
                                "密码不能包含连续3个及以上与邮箱前缀相同的字符")
                                .addConstraintViolation();
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Try to get a field value by trying multiple candidate field names.
     * Returns the value of the first matching field, or null if none found.
     */
    private String getFieldValue(Object obj, String... fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                Field field = findField(obj.getClass(), fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    Object val = field.get(obj);
                    if (val instanceof String s) {
                        return s;
                    }
                }
            } catch (IllegalAccessException ignored) {
                // skip inaccessible field
            }
        }
        return null;
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
