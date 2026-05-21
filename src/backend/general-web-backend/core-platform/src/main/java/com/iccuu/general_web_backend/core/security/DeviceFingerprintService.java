package com.iccuu.general_web_backend.core.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.dao.DuplicateKeyException;
import com.iccuu.general_web_backend.common.util.HashUtil;
import com.iccuu.general_web_backend.common.util.IpUtil;
import com.iccuu.general_web_backend.module.user.entity.UserDevice;
import com.iccuu.general_web_backend.module.user.mapper.UserDeviceMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceFingerprintService {

    private static final int UNTRUSTED = 0;
    private static final int TRUSTED = 1;

    private final UserDeviceMapper userDeviceMapper;

    public String generateFingerprint(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String acceptLanguage = request.getHeader("Accept-Language");
        String ipPrefix = IpUtil.getClientIpPrefix24(request);

        String raw = (userAgent != null ? userAgent : "") + "|"
                + (acceptLanguage != null ? acceptLanguage : "") + "|"
                + (ipPrefix != null ? ipPrefix : "");
        return HashUtil.sha256(raw);
    }

    public DeviceCheckResult checkDevice(Long userId, HttpServletRequest request) {
        String fingerprint = generateFingerprint(request);
        String userAgent = request.getHeader("User-Agent");

        UserDevice device = findByUserAndHash(userId, fingerprint);

        if (device != null) {
            userDeviceMapper.update(null,
                    new LambdaUpdateWrapper<UserDevice>()
                            .eq(UserDevice::getId, device.getId())
                            .set(UserDevice::getLastSeenAt, LocalDateTime.now())
                            .set(UserDevice::getUserAgent, userAgent));
            return new DeviceCheckResult(false, isTrusted(device));
        }

        device = new UserDevice();
        device.setUserId(userId);
        device.setDeviceHash(fingerprint);
        device.setUserAgent(userAgent);
        device.setIpAddress(IpUtil.getClientIp(request));
        device.setFirstSeenAt(LocalDateTime.now());
        device.setLastSeenAt(LocalDateTime.now());
        device.setCreatedDate(LocalDate.now());
        device.setIsTrusted(UNTRUSTED);

        long deviceCount = userDeviceMapper.selectCount(
                new LambdaQueryWrapper<UserDevice>().eq(UserDevice::getUserId, userId));
        if (deviceCount == 0) {
            device.setIsTrusted(TRUSTED);
        }

        try {
            userDeviceMapper.insert(device);
        } catch (DuplicateKeyException e) {
            // Race condition: another request inserted the same device
            device = findByUserAndHash(userId, fingerprint);
            if (device != null) {
                userDeviceMapper.update(null,
                        new LambdaUpdateWrapper<UserDevice>()
                                .eq(UserDevice::getId, device.getId())
                                .set(UserDevice::getLastSeenAt, LocalDateTime.now())
                                .set(UserDevice::getUserAgent, userAgent));
                log.debug("Device already registered for userId={}, fingerprint={}",
                        userId, fingerprint.substring(0, 16) + "...");
                return new DeviceCheckResult(false, isTrusted(device));
            }
            throw e;
        }

        log.info("New device detected for userId={}, deviceHash={}",
                userId, fingerprint.substring(0, 16) + "...");

        return new DeviceCheckResult(true, isTrusted(device));
    }

    public void trustDevice(Long userId, String deviceHash) {
        UserDevice device = findByUserAndHash(userId, deviceHash);
        if (device != null) {
            userDeviceMapper.update(null,
                    new LambdaUpdateWrapper<UserDevice>()
                            .eq(UserDevice::getId, device.getId())
                            .set(UserDevice::getIsTrusted, TRUSTED));
        }
    }

    public List<UserDevice> getUserDevices(Long userId) {
        return userDeviceMapper.selectList(
                new LambdaQueryWrapper<UserDevice>()
                        .eq(UserDevice::getUserId, userId)
                        .orderByDesc(UserDevice::getLastSeenAt));
    }

    private UserDevice findByUserAndHash(Long userId, String deviceHash) {
        return userDeviceMapper.selectOne(
                new LambdaQueryWrapper<UserDevice>()
                        .eq(UserDevice::getUserId, userId)
                        .eq(UserDevice::getDeviceHash, deviceHash));
    }

    private static boolean isTrusted(UserDevice device) {
        return device.getIsTrusted() != null && device.getIsTrusted() == TRUSTED;
    }

    public record DeviceCheckResult(boolean isNewDevice, boolean isTrusted) {}
}
