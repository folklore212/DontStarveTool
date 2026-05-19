package com.iccuu.general_web_backend.module.auth.strategy.authentication;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iccuu.general_web_backend.common.constant.Constants;
import com.iccuu.general_web_backend.common.constant.ErrorCode;
import com.iccuu.general_web_backend.common.enums.AuthMethod;
import com.iccuu.general_web_backend.common.enums.IdentityType;
import com.iccuu.general_web_backend.common.enums.LoginResult;
import com.iccuu.general_web_backend.common.enums.UserStatus;
import com.iccuu.general_web_backend.common.exception.AuthenticationException;
import com.iccuu.general_web_backend.common.exception.BusinessException;
import com.iccuu.general_web_backend.common.util.HashUtil;
import com.iccuu.general_web_backend.common.util.IpUtil;
import com.iccuu.general_web_backend.common.util.SecurityUtil;
import com.iccuu.general_web_backend.infrastructure.geetest.GeeTestVerifier;
import com.iccuu.general_web_backend.infrastructure.security.DeviceFingerprintService;
import com.iccuu.general_web_backend.module.auth.dto.LoginRequest;
import com.iccuu.general_web_backend.module.auth.entity.LoginLog;
import com.iccuu.general_web_backend.module.auth.service.LoginLogService;
import com.iccuu.general_web_backend.module.auth.strategy.identity.IdentityResolver;
import com.iccuu.general_web_backend.module.mfa.entity.UserMfa;
import com.iccuu.general_web_backend.module.mfa.mapper.UserMfaMapper;
import com.iccuu.general_web_backend.module.mfa.strategy.MfaVerifier;
import com.iccuu.general_web_backend.module.user.entity.User;
import com.iccuu.general_web_backend.module.user.entity.UserAuth;
import com.iccuu.general_web_backend.module.user.mapper.UserAuthMapper;
import com.iccuu.general_web_backend.module.user.mapper.UserMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PasswordAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(PasswordAuthenticationProvider.class);
    private static final String DUMMY_HASH = "$2a$12$LJ3m4ys3Lk0TSwHCpNqrEeY6HhRJZgK0wM3QfR1xP9sV7bN8cD2e";

    private final UserMapper userMapper;
    private final UserAuthMapper userAuthMapper;
    private final UserMfaMapper userMfaMapper;
    private final GeeTestVerifier geeTestVerifier;
    private final PasswordEncoder passwordEncoder;
    private final LoginLogService loginLogService;
    private final DeviceFingerprintService deviceFingerprintService;
    private final List<IdentityResolver> identityResolvers;
    private final List<MfaVerifier> mfaVerifiers;

    @Override
    public AuthMethod getMethod() {
        return AuthMethod.PASSWORD;
    }

    @Override
    public boolean supports(LoginRequest request) {
        return request.getIdentifier() != null && !request.getIdentifier().isBlank()
                && request.getCredential() != null && !request.getCredential().isBlank();
    }

    @Override
    public AuthenticationResult authenticate(LoginRequest request, HttpServletRequest httpRequest) {
        verifyCaptcha(request);

        IdentityResolver resolver = identityResolvers.stream()
                .filter(r -> r.canResolve(request.getIdentifier()))
                .findFirst()
                .orElse(null);

        if (resolver == null) {
            return AuthenticationResult.failure(null, null, ErrorCode.INVALID_CREDENTIALS, "Invalid credentials", null);
        }

        User user = resolver.resolve(request.getIdentifier());
        if (user == null) {
            passwordEncoder.matches(request.getCredential(), DUMMY_HASH);
            return AuthenticationResult.failure(null, null, ErrorCode.INVALID_CREDENTIALS, "Invalid credentials", resolver.supportedType());
        }

        UserAuth userAuth = resolver.resolveAuth(user, request.getIdentifier());
        if (userAuth == null) {
            passwordEncoder.matches(request.getCredential(), DUMMY_HASH);
            return AuthenticationResult.failure(user, null, ErrorCode.INVALID_CREDENTIALS, "Invalid credentials", resolver.supportedType());
        }

        if (!passwordEncoder.matches(request.getCredential(), userAuth.getCredential())) {
            handleFailedLogin(user);
            recordLoginLog(user, resolver.supportedType().getValue(), LoginResult.FAILED_CREDENTIAL, "Invalid credentials");
            String ipAddress = IpUtil.getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            return AuthenticationResult.failure(user, userAuth,
                    ErrorCode.INVALID_CREDENTIALS, "Invalid credentials", resolver.supportedType());
        }

        try {
            checkUserStatus(user);
        } catch (AuthenticationException e) {
            return AuthenticationResult.failure(user, userAuth,
                    ErrorCode.fromCode(e.getCode()), e.getMessage(), resolver.supportedType());
        }

        UserMfa userMfa = userMfaMapper.selectOne(new LambdaQueryWrapper<UserMfa>()
                .eq(UserMfa::getUserId, user.getUserId())
                .eq(UserMfa::getIsEnabled, 1));

        String ipAddress = IpUtil.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        if (userMfa != null && (request.getMfaCode() == null || request.getMfaCode().isBlank())) {
            List<String> mfaTypes = new ArrayList<>();
            mfaTypes.add(userMfa.getMfaType());
            return AuthenticationResult.mfaRequired(user, userAuth,
                    user.getUserId(), mfaTypes, resolver.supportedType(), ipAddress, userAgent);
        }

        if (userMfa != null && request.getMfaCode() != null && !request.getMfaCode().isBlank()) {
            MfaVerifier mfaVerifier = mfaVerifiers.stream()
                    .filter(v -> v.supportedType().getValue().equalsIgnoreCase(userMfa.getMfaType()))
                    .findFirst()
                    .orElse(null);

            if (mfaVerifier == null) {
                return AuthenticationResult.failure(user, userAuth, ErrorCode.INTERNAL_ERROR,
                        "MFA verifier not found", resolver.supportedType());
            }

            boolean mfaValid = mfaVerifier.verify(userMfa, request.getMfaCode())
                    || mfaVerifier.verifyAndConsumeBackupCode(userMfa, request.getMfaCode());

            if (!mfaValid) {
                recordLoginLog(user, resolver.supportedType().getValue(), LoginResult.FAILED_MFA, "Invalid MFA code");
                return AuthenticationResult.failure(user, userAuth,
                        ErrorCode.MFA_INVALID, "Invalid MFA code", resolver.supportedType());
            }
        }

        handleSuccessfulLogin(user);
        recordLoginLog(user, resolver.supportedType().getValue(), LoginResult.SUCCESS, null);

        DeviceFingerprintService.DeviceCheckResult deviceResult =
                deviceFingerprintService.checkDevice(user.getUserId(), httpRequest);
        boolean newDevice = deviceResult.isNewDevice() && !deviceResult.isTrusted();

        log.info("Password auth successful for userId={}, identityType={}", user.getUserId(), resolver.supportedType());
        return AuthenticationResult.success(user, userAuth,
                resolver.supportedType(), ipAddress, userAgent, newDevice);
    }

    @Override
    public void handleFailedLogin(User user) {
        int failedAttempts = user.getFailedAttempts() != null ? user.getFailedAttempts() + 1 : 1;
        user.setFailedAttempts(failedAttempts);
        if (failedAttempts >= Constants.MAX_LOGIN_ATTEMPTS) {
            user.setStatus(UserStatus.LOCKED.getValue());
            user.setLockedUntil(System.currentTimeMillis() + Constants.LOCKOUT_DURATION_MS);
            log.warn("Account locked due to too many failed attempts: userId={}", user.getUserId());
        }
        userMapper.updateById(user);
    }

    @Override
    public void handleSuccessfulLogin(User user) {
        user.setFailedAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(IpUtil.getClientIp(SecurityUtil.getCurrentRequest()));
        user.setStatus(UserStatus.NORMAL.getValue());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private void verifyCaptcha(LoginRequest request) {
        if (request.getCaptchaOutput() == null || request.getCaptchaOutput().isBlank()) {
            throw new AuthenticationException(ErrorCode.CAPTCHA_REQUIRED, "Captcha is required");
        }
        try {
            geeTestVerifier.verify(request.getCaptchaOutput(), request.getLotNumber(),
                    request.getPassToken(), request.getGenTime(), true);
        } catch (CallNotPermittedException e) {
            log.warn("GeeTest circuit breaker open for login — failing open (lockout protection active)");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("GeeTest unavailable for login — failing open (lockout protection active)", e);
        }
    }

    private void checkUserStatus(User user) {
        int status = user.getStatus() != null ? user.getStatus() : UserStatus.NORMAL.getValue();
        if (status == UserStatus.LOCKED.getValue()) {
            if (user.getLockedUntil() != null && user.getLockedUntil() > System.currentTimeMillis()) {
                throw new AuthenticationException(ErrorCode.ACCOUNT_LOCKED);
            }
            user.setStatus(UserStatus.NORMAL.getValue());
            user.setFailedAttempts(0);
            userMapper.updateById(user);
        } else if (status == UserStatus.DISABLED.getValue()) {
            throw new AuthenticationException(ErrorCode.ACCOUNT_DISABLED);
        } else if (status == UserStatus.PENDING.getValue()) {
            throw new AuthenticationException(ErrorCode.ACCOUNT_PENDING);
        }
    }

    private void recordLoginLog(User user, String identityType, LoginResult result, String failureReason) {
        try {
            LoginLog log = new LoginLog();
            log.setUserId(user.getUserId());
            log.setIdentifierHash(HashUtil.sha256(
                    user.getEmail() != null ? user.getEmail() : user.getUsername()));
            log.setIdentityType(identityType);
            log.setAuthMethod(AuthMethod.PASSWORD.getValue());
            var currentRequest = SecurityUtil.getCurrentRequest();
            log.setIpAddress(IpUtil.getClientIp(currentRequest));
            log.setUserAgent(currentRequest.getHeader("User-Agent"));
            log.setResult(result.getValue());
            log.setFailureReason(failureReason);
            log.setCreatedAt(LocalDateTime.now());
            log.setCreatedDate(LocalDate.now());
            loginLogService.record(log);
        } catch (Exception e) {
            log.error("Failed to record login log", e);
        }
    }

}
