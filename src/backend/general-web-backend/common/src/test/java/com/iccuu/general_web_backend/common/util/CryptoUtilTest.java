package com.iccuu.general_web_backend.common.util;

import org.junit.jupiter.api.Test;
import java.util.Base64;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilTest {

    private static String generateKey() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        SecretKey key = kg.generateKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    @Test
    void encryptDecryptRoundTrip() throws Exception {
        String key = generateKey();
        String plaintext = "Hello, DST Platform!";
        String encrypted = CryptoUtil.encrypt(plaintext, key);
        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted);
        String decrypted = CryptoUtil.decrypt(encrypted, key);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void encryptWithDifferentKeyShouldFailDecrypt() throws Exception {
        String key1 = generateKey();
        String key2 = generateKey();
        String plaintext = "sensitive data";
        String encrypted = CryptoUtil.encrypt(plaintext, key1);
        assertThrows(Exception.class, () -> CryptoUtil.decrypt(encrypted, key2));
    }

    @Test
    void encryptEmptyString() throws Exception {
        String key = generateKey();
        String encrypted = CryptoUtil.encrypt("", key);
        assertNotNull(encrypted);
        assertEquals("", CryptoUtil.decrypt(encrypted, key));
    }

    @Test
    void encryptRepeatedShouldProduceDifferentOutput() throws Exception {
        String key = generateKey();
        String plaintext = "same input";
        String enc1 = CryptoUtil.encrypt(plaintext, key);
        String enc2 = CryptoUtil.encrypt(plaintext, key);
        assertNotEquals(enc1, enc2, "AES-GCM should use unique IV per encryption");
    }
}
