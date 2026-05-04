package com.iccuu.general_web_backend.module.mfa.strategy;

import cn.hutool.core.codec.Base32;
import com.iccuu.general_web_backend.common.util.CryptoUtil;
import com.iccuu.general_web_backend.module.mfa.entity.UserMfa;
import com.iccuu.general_web_backend.module.mfa.mapper.UserMfaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TotpMfaVerifierTest {

    @Mock
    private UserMfaMapper userMfaMapper;

    private TotpMfaVerifier verifier;

    private static final String TEST_KEY_BASE64 = "22l9HkgiXovn9NFOuDcfPhwDj34ndUoeSPvsB0txFt4=";

    @BeforeEach
    void setUp() {
        verifier = new TotpMfaVerifier(userMfaMapper);
        ReflectionTestUtils.setField(verifier, "mfaKeyCurrent", TEST_KEY_BASE64);
        ReflectionTestUtils.setField(verifier, "mfaKeyFallback", "");
    }

    @Test
    void shouldReturnTotpAsSupportedType() {
        assertThat(verifier.supportedType().getValue()).isEqualTo("totp");
    }

    @Test
    void shouldVerifyValidTotpCode() {
        byte[] secretBytes = new byte[20];
        new SecureRandom().nextBytes(secretBytes);
        String secret = Base32.encode(secretBytes);
        String encryptedSecret = CryptoUtil.encrypt(secret, TEST_KEY_BASE64);

        UserMfa mfa = new UserMfa();
        mfa.setId(1L);
        mfa.setUserId(100L);
        mfa.setSecret(encryptedSecret);
        mfa.setMfaType("totp");

        // Generate a TOTP code using the current time window
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        long counter = currentTimeSeconds / 30;
        String code = generateTotpForTest(secretBytes, counter);

        assertThat(verifier.verify(mfa, code)).isTrue();
    }

    @Test
    void shouldRejectInvalidTotpCode() {
        byte[] secretBytes = new byte[20];
        new SecureRandom().nextBytes(secretBytes);
        String secret = Base32.encode(secretBytes);
        String encryptedSecret = CryptoUtil.encrypt(secret, TEST_KEY_BASE64);

        UserMfa mfa = new UserMfa();
        mfa.setId(1L);
        mfa.setUserId(100L);
        mfa.setSecret(encryptedSecret);
        mfa.setMfaType("totp");

        assertThat(verifier.verify(mfa, "000000")).isFalse();
    }

    @Test
    void shouldAcceptCodeWithinTimeSkew() {
        byte[] secretBytes = new byte[20];
        new SecureRandom().nextBytes(secretBytes);
        String secret = Base32.encode(secretBytes);
        String encryptedSecret = CryptoUtil.encrypt(secret, TEST_KEY_BASE64);

        UserMfa mfa = new UserMfa();
        mfa.setId(1L);
        mfa.setUserId(100L);
        mfa.setSecret(encryptedSecret);
        mfa.setMfaType("totp");

        // Previous time window (29 seconds in the past)
        long prevTimeSeconds = (System.currentTimeMillis() / 1000) - 29;
        long prevCounter = prevTimeSeconds / 30;
        String prevCode = generateTotpForTest(secretBytes, prevCounter);

        assertThat(verifier.verify(mfa, prevCode))
                .as("Should accept code from previous time window")
                .isTrue();
    }

    @Test
    void shouldRejectCodeOutsideTimeSkew() {
        byte[] secretBytes = new byte[20];
        new SecureRandom().nextBytes(secretBytes);
        String secret = Base32.encode(secretBytes);
        String encryptedSecret = CryptoUtil.encrypt(secret, TEST_KEY_BASE64);

        UserMfa mfa = new UserMfa();
        mfa.setId(1L);
        mfa.setUserId(100L);
        mfa.setSecret(encryptedSecret);
        mfa.setMfaType("totp");

        // 2 steps back (well outside the ±1 step skew window)
        long oldTimeSeconds = (System.currentTimeMillis() / 1000) - 65;
        long oldCounter = oldTimeSeconds / 30;
        String oldCode = generateTotpForTest(secretBytes, oldCounter);

        assertThat(verifier.verify(mfa, oldCode))
                .as("Should reject code from outside time skew window")
                .isFalse();
    }

    private String generateTotpForTest(byte[] key, long counter) {
        try {
            byte[] counterBytes = new byte[8];
            long c = counter;
            for (int i = 7; i >= 0; i--) {
                counterBytes[i] = (byte) (c & 0xFF);
                c >>= 8;
            }
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
            mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(counterBytes);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
