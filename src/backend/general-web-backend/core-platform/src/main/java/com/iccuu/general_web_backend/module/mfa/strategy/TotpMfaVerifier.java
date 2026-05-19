package com.iccuu.general_web_backend.module.mfa.strategy;

import cn.hutool.core.codec.Base32;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iccuu.general_web_backend.common.enums.MfaType;
import com.iccuu.general_web_backend.common.util.CryptoUtil;
import com.iccuu.general_web_backend.module.mfa.entity.UserMfa;
import com.iccuu.general_web_backend.module.mfa.mapper.UserMfaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TotpMfaVerifier implements MfaVerifier {

    @Value("${crypto.aes-keys.1:change_me_aes_v1}")
    private String mfaKeyCurrent;

    @Value("${crypto.aes-keys.0:change_me_aes_v0}")
    private String mfaKeyFallback;

    private static final int TOTP_PERIOD = 30;
    private static final int TOTP_DIGITS = 6;
    private static final String HASH_ALGORITHM = "HmacSHA1";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final UserMfaMapper userMfaMapper;

    @Override
    public MfaType supportedType() {
        return MfaType.TOTP;
    }

    @Override
    public boolean verify(UserMfa mfaRecord, String code) {
        String secret = CryptoUtil.decryptWithFallback(mfaRecord.getSecret(), mfaKeyCurrent, mfaKeyFallback);
        return verifyTotpCode(secret, code);
    }

    @Override
    public boolean verifyAndConsumeBackupCode(UserMfa mfaRecord, String code) {
        String backupCodesJson = CryptoUtil.decryptWithFallback(mfaRecord.getBackupCodes(), mfaKeyCurrent, mfaKeyFallback);

        List<String> backupCodes;
        try {
            backupCodes = OBJECT_MAPPER.readValue(backupCodesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize backup codes for mfaId={}", mfaRecord.getId(), e);
            return false;
        }

        if (backupCodes == null) {
            return false;
        }
        boolean matched = false;
        for (String backupCode : backupCodes) {
            if (MessageDigest.isEqual(
                    backupCode.getBytes(StandardCharsets.UTF_8),
                    code.getBytes(StandardCharsets.UTF_8))) {
                matched = true;
                backupCodes.remove(backupCode);
                break;
            }
        }
        if (!matched) {
            return false;
        }

        try {
            String updatedJson = OBJECT_MAPPER.writeValueAsString(backupCodes);
            String encrypted = CryptoUtil.encrypt(updatedJson, mfaKeyCurrent);
            mfaRecord.setBackupCodes(encrypted);
            userMfaMapper.updateById(mfaRecord);
        } catch (Exception e) {
            log.error("Failed to persist consumed backup code for mfaId={}", mfaRecord.getId(), e);
            return false;
        }

        log.info("Backup code consumed for mfaId={}, {} codes remaining", mfaRecord.getId(), backupCodes.size());
        return true;
    }

    private boolean verifyTotpCode(String secret, String code) {
        try {
            byte[] key = Base32.decode(secret);
            long currentTimeSeconds = System.currentTimeMillis() / 1000;
            long counter = currentTimeSeconds / TOTP_PERIOD;

            for (long offset = -1; offset <= 1; offset++) {
                String expected = generateTotp(key, counter + offset);
                if (MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8),
                        code.getBytes(StandardCharsets.UTF_8))) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("TOTP verification error", e);
            return false;
        }
    }

    private String generateTotp(byte[] key, long counter) {
        try {
            byte[] counterBytes = new byte[8];
            long c = counter;
            for (int i = 7; i >= 0; i--) {
                counterBytes[i] = (byte) (c & 0xFF);
                c >>= 8;
            }

            Mac mac = Mac.getInstance(HASH_ALGORITHM);
            mac.init(new SecretKeySpec(key, HASH_ALGORITHM));
            byte[] hash = mac.doFinal(counterBytes);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, TOTP_DIGITS);
            return String.format("%0" + TOTP_DIGITS + "d", otp);
        } catch (Exception e) {
            throw new RuntimeException("TOTP generation failed", e);
        }
    }
}
