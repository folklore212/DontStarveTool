package com.iccuu.general_web_backend.infrastructure.ssh;

/**
 * Escapes user-supplied values for safe interpolation into shell commands.
 */
public final class ShellEscaper {

    private ShellEscaper() {}

    /**
     * Escape a value for single-quoted shell strings.
     * Replaces ' with '\'' (end quote, escaped quote, restart quote).
     */
    public static String escape(String value) {
        if (value == null) return "";
        return value.replace("'", "'\\''");
    }

    /**
     * Escape for use inside a heredoc body (not the delimiter).
     * Heredocs with quoted delimiters ('EOF') prevent variable expansion,
     * but backticks and $() are still safe. Only the delimiter itself
     * and null bytes need escaping.
     */
    public static String forHeredoc(String value) {
        if (value == null) return "";
        return value.replace("\0", "");
    }
}
