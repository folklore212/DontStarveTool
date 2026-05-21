package com.iccuu.general_web_backend.common.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HashUtilTest {

    @Test
    void sha256ShouldProduceHexString() {
        String result = HashUtil.sha256("hello");
        assertNotNull(result);
        assertEquals(64, result.length(), "SHA-256 should produce 64 hex chars");
        assertTrue(result.matches("^[0-9a-f]+$"), "should be lowercase hex");
    }

    @Test
    void sha256ShouldBeDeterministic() {
        assertEquals(HashUtil.sha256("hello"), HashUtil.sha256("hello"));
    }

    @Test
    void sha256ShouldDifferForDifferentInputs() {
        assertNotEquals(HashUtil.sha256("a"), HashUtil.sha256("b"));
    }

    @Test
    void sha256ShouldHandleEmptyString() {
        String result = HashUtil.sha256("");
        assertNotNull(result);
        assertEquals(64, result.length());
    }

    @Test
    void sha256KnownVector() {
        // echo -n "hello" | sha256sum
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                HashUtil.sha256("hello"));
    }
}
