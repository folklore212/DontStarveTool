package com.iccuu.general_web_backend.infrastructure.ssh;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShellEscaperTest {

    @Test
    void escapeNormalStringShouldPassThrough() {
        assertEquals("my-cluster", ShellEscaper.escape("my-cluster"));
    }

    @Test
    void escapeShouldReplaceSingleQuote() {
        assertEquals("it'\\''s", ShellEscaper.escape("it's"));
    }

    @Test
    void escapeNullShouldReturnEmpty() {
        assertEquals("", ShellEscaper.escape(null));
    }

    @Test
    void escapeEmptyStringShouldReturnEmpty() {
        assertEquals("", ShellEscaper.escape(""));
    }

    @Test
    void escapeShouldPreserveSpecialChars() {
        assertEquals("test; rm -rf /", ShellEscaper.escape("test; rm -rf /"));
    }

    @Test
    void forHeredocNormalStringShouldPassThrough() {
        assertEquals("hello", ShellEscaper.forHeredoc("hello"));
    }

    @Test
    void forHeredocShouldRemoveNullBytes() {
        assertEquals("ab", ShellEscaper.forHeredoc("a\0b"));
    }

    @Test
    void forHeredocNullShouldReturnEmpty() {
        assertEquals("", ShellEscaper.forHeredoc(null));
    }
}
