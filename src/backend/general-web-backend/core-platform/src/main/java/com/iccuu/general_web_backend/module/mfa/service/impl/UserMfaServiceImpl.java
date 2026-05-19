package com.iccuu.general_web_backend.module.mfa.service.impl;

import cn.hutool.core.codec.Base32;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.iccuu.general_web_backend.common.constant.ErrorCode;
import com.iccuu.general_web_backend.common.enums.MfaType;
import com.iccuu.general_web_backend.common.exception.BusinessException;
import com.iccuu.general_web_backend.common.util.CryptoUtil;
import com.iccuu.general_web_backend.common.util.SecureRandomUtil;
import com.iccuu.general_web_backend.module.mfa.dto.*;
import com.iccuu.general_web_backend.module.mfa.entity.UserMfa;
import com.iccuu.general_web_backend.module.mfa.mapper.UserMfaMapper;
import com.iccuu.general_web_backend.module.mfa.service.UserMfaService;
import com.iccuu.general_web_backend.module.mfa.strategy.MfaVerifier;
import com.iccuu.general_web_backend.module.user.entity.User;
import com.iccuu.general_web_backend.module.user.entity.UserAuth;
import com.iccuu.general_web_backend.module.user.mapper.UserAuthMapper;
import com.iccuu.general_web_backend.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserMfaServiceImpl implements UserMfaService {

    @Value("${crypto.aes-keys.1}")
    private String mfaKeyCurrent;

    @Value("${crypto.aes-keys.0}")
    private String mfaKeyFallback;

    private static final String TOTP_ISSUER = "GeneralWeb";
    private static final int BACKUP_CODE_COUNT = 10;
    private static final int BACKUP_CODE_LENGTH = 8;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final UserMfaMapper userMfaMapper;
    private final UserMapper userMapper;
    private final UserAuthMapper userAuthMapper;
    private final PasswordEncoder passwordEncoder;
    private final List<MfaVerifier> mfaVerifiers;

    @Override
    public List<MfaStatusVO> getStatus(Long userId) {
        List<UserMfa> mfas = userMfaMapper.selectList(
                new LambdaQueryWrapper<UserMfa>().eq(UserMfa::getUserId, userId));
        return mfas.stream()
                .map(m -> MfaStatusVO.builder()
                        .mfaType(m.getMfaType())
                        .enabled(m.getIsEnabled() != null && m.getIsEnabled() == 1)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public MfaSetupInitResponse setupInit(Long userId, MfaSetupInitRequest request) {
        // Check if TOTP already enabled for this user
        UserMfa existing = findEnabledMfa(userId, request.getMfaType());
        if (existing != null) {
            throw new BusinessException(ErrorCode.MFA_ALREADY_ENABLED);
        }

        // Remove any unverified pending setup for this type
        UserMfa pending = userMfaMapper.selectOne(
                new LambdaQueryWrapper<UserMfa>()
                        .eq(UserMfa::getUserId, userId)
                        .eq(UserMfa::getMfaType, request.getMfaType())
                        .eq(UserMfa::getIsEnabled, 0));
        if (pending != null) {
            userMfaMapper.deleteById(pending.getId());
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // Generate TOTP secret (20 random bytes, base32 encoded)
        byte[] secretBytes = new byte[20];
        SecureRandomUtil.INSTANCE.nextBytes(secretBytes);
        String secret = Base32.encode(secretBytes);

        // Build otpauth:// URI
        String account = user.getEmail() != null ? user.getEmail() : user.getUsername();
        String otpauthUri = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                TOTP_ISSUER, account, secret, TOTP_ISSUER);

        // Generate QR code image via ZXing
        String qrCodeBase64;
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(otpauthUri, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);
            qrCodeBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("Failed to generate QR code for userId={}", userId, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "QR code generation failed");
        }

        // Generate backup codes (plaintext shown once)
        List<String> backupCodes = IntStream.range(0, BACKUP_CODE_COUNT)
                .mapToObj(i -> String.format("%0" + BACKUP_CODE_LENGTH + "d",
                        SecureRandomUtil.INSTANCE.nextInt((int) Math.pow(10, BACKUP_CODE_LENGTH))))
                .collect(Collectors.toList());

        // AES encrypt secret and backupCodes for DB storage
        String encryptedSecret = CryptoUtil.encrypt(secret, mfaKeyCurrent);
        String backupCodesJson;
        String encryptedBackupCodes;
        try {
            backupCodesJson = OBJECT_MAPPER.writeValueAsString(backupCodes);
            encryptedBackupCodes = CryptoUtil.encrypt(backupCodesJson, mfaKeyCurrent);
        } catch (Exception e) {
            log.error("Failed to serialize backup codes for userId={}", userId, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Backup codes serialization failed");
        }

        // Insert pending record (isEnabled=0) so setupVerify can retrieve it later
        UserMfa userMfa = new UserMfa();
        userMfa.setUserId(userId);
        userMfa.setMfaType(request.getMfaType().toLowerCase());
        userMfa.setSecret(encryptedSecret);
        userMfa.setBackupCodes(encryptedBackupCodes);
        userMfa.setIsEnabled(0);
        userMfa.setKeyVersion(1);
        userMfaMapper.insert(userMfa);

        // Return plaintext data to the caller (shown once to the user)
        return MfaSetupInitResponse.builder()
                .secret(secret)
                .qrCodeUri("data:image/png;base64," + qrCodeBase64)
                .backupCodes(backupCodes)
                .build();
    }

    @Override
    public void setupVerify(Long userId, MfaEnableRequest request) {
        // Find the pending (isEnabled=0) MFA record
        UserMfa userMfa = userMfaMapper.selectOne(
                new LambdaQueryWrapper<UserMfa>()
                        .eq(UserMfa::getUserId, userId)
                        .eq(UserMfa::getIsEnabled, 0));
        if (userMfa == null) {
            throw new BusinessException(ErrorCode.MFA_NOT_ENABLED, "No pending MFA setup found");
        }

        // Verify the TOTP code via verifier
        MfaVerifier verifier = getTotpVerifier();
        if (verifier == null || !verifier.verify(userMfa, request.getCode())) {
            throw new BusinessException(ErrorCode.MFA_INVALID);
        }

        // Code verified — enable the MFA
        userMfa.setIsEnabled(1);
        userMfaMapper.updateById(userMfa);
    }

    @Override
    public void disable(Long userId, MfaDisableRequest request) {
        // Verify password before disabling MFA
        List<UserAuth> auths = userAuthMapper.selectList(
                new LambdaQueryWrapper<UserAuth>()
                        .eq(UserAuth::getUserId, userId)
                        .eq(UserAuth::getVerified, 1));
        if (auths.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        boolean passwordMatch = auths.stream()
                .anyMatch(a -> passwordEncoder.matches(request.getPassword(), a.getCredential()));
        if (!passwordMatch) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Delete all enabled MFA rows for this user
        userMfaMapper.delete(new LambdaQueryWrapper<UserMfa>()
                .eq(UserMfa::getUserId, userId)
                .eq(UserMfa::getIsEnabled, 1));
    }

    @Override
    public List<String> getBackupCodes(Long userId) {
        UserMfa userMfa = findEnabledMfa(userId, MfaType.TOTP.getValue());
        if (userMfa == null) {
            throw new BusinessException(ErrorCode.MFA_NOT_ENABLED);
        }

        // Decrypt existing backup codes
        String backupCodesJson = CryptoUtil.decryptWithFallback(userMfa.getBackupCodes(), mfaKeyCurrent, mfaKeyFallback);

        List<String> backupCodes;
        try {
            backupCodes = OBJECT_MAPPER.readValue(backupCodesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize backup codes for userId={}", userId, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Backup codes deserialization failed");
        }

        // Regenerate backup codes
        List<String> newBackupCodes = IntStream.range(0, BACKUP_CODE_COUNT)
                .mapToObj(i -> String.format("%0" + BACKUP_CODE_LENGTH + "d",
                        SecureRandomUtil.INSTANCE.nextInt((int) Math.pow(10, BACKUP_CODE_LENGTH))))
                .collect(Collectors.toList());

        // Re-encrypt and persist new backup codes
        try {
            String newBackupCodesJson = OBJECT_MAPPER.writeValueAsString(newBackupCodes);
            String encryptedNewBackupCodes = CryptoUtil.encrypt(newBackupCodesJson, mfaKeyCurrent);
            userMfa.setBackupCodes(encryptedNewBackupCodes);
            userMfaMapper.updateById(userMfa);
        } catch (Exception e) {
            log.error("Failed to serialize new backup codes for userId={}", userId, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Backup codes regeneration failed");
        }

        // Return plaintext codes (shown once)
        return newBackupCodes;
    }

    @Override
    public boolean verifyAndConsumeBackupCode(Long userId, String code) {
        UserMfa userMfa = findEnabledMfa(userId, MfaType.TOTP.getValue());
        if (userMfa == null) {
            return false;
        }
        MfaVerifier verifier = getTotpVerifier();
        if (verifier == null) {
            return false;
        }
        return verifier.verifyAndConsumeBackupCode(userMfa, code);
    }

    @Override
    public boolean verifyTotp(Long userId, String code) {
        UserMfa userMfa = findEnabledMfa(userId, MfaType.TOTP.getValue());
        if (userMfa == null) {
            return false;
        }
        MfaVerifier verifier = getTotpVerifier();
        if (verifier == null) {
            return false;
        }
        return verifier.verify(userMfa, code);
    }

    private MfaVerifier getTotpVerifier() {
        return mfaVerifiers.stream()
                .filter(v -> v.supportedType() == MfaType.TOTP)
                .findFirst()
                .orElse(null);
    }

    private UserMfa findEnabledMfa(Long userId, String mfaType) {
        return userMfaMapper.selectOne(
                new LambdaQueryWrapper<UserMfa>()
                        .eq(UserMfa::getUserId, userId)
                        .eq(UserMfa::getMfaType, mfaType)
                        .eq(UserMfa::getIsEnabled, 1));
    }

}
