package com.iccuu.general_web_backend.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iccuu.general_web_backend.common.constant.Constants;
import com.iccuu.general_web_backend.common.constant.ErrorCode;
import com.iccuu.general_web_backend.common.enums.*;
import com.iccuu.general_web_backend.common.exception.AuthenticationException;
import com.iccuu.general_web_backend.common.exception.BusinessException;
import com.iccuu.general_web_backend.common.util.HashUtil;
import com.iccuu.general_web_backend.common.util.IpUtil;
import com.iccuu.general_web_backend.common.util.SecurityUtil;
import com.iccuu.general_web_backend.infrastructure.geetest.GeeTestVerifier;
import com.iccuu.general_web_backend.infrastructure.metrics.MetricsService;
import com.iccuu.general_web_backend.infrastructure.snowflake.SnowflakeIdGenerator;
import com.iccuu.general_web_backend.common.event.PasswordChangedEvent;
import com.iccuu.general_web_backend.common.event.UserRegisteredEvent;
import com.iccuu.general_web_backend.infrastructure.event.AuthEventPublisher;
import com.iccuu.general_web_backend.infrastructure.storage.DataRetentionService;
import com.iccuu.general_web_backend.module.audit.service.AuditLogService;
import com.iccuu.general_web_backend.module.auth.dto.*;
import com.iccuu.general_web_backend.module.auth.entity.LoginLog;
import com.iccuu.general_web_backend.module.auth.mapper.LoginLogMapper;
import com.iccuu.general_web_backend.module.auth.service.AuthService;
import com.iccuu.general_web_backend.module.auth.service.LoginLogService;
import com.iccuu.general_web_backend.module.auth.service.TokenService;
import com.iccuu.general_web_backend.module.auth.service.VerificationCodeService;
import com.iccuu.general_web_backend.module.auth.service.VerifyResult;
import com.iccuu.general_web_backend.module.role.dto.UserRoleVO;
import com.iccuu.general_web_backend.module.auth.strategy.authentication.AuthenticationProvider;
import com.iccuu.general_web_backend.module.auth.strategy.authentication.AuthenticationResult;
import com.iccuu.general_web_backend.module.auth.strategy.identity.IdentityResolver;
import com.iccuu.general_web_backend.module.user.entity.User;
import com.iccuu.general_web_backend.module.user.entity.UserAuth;
import com.iccuu.general_web_backend.module.user.entity.UserCredentialsHistory;
import com.iccuu.general_web_backend.module.user.entity.UserProfile;
import com.iccuu.general_web_backend.module.user.mapper.UserAuthMapper;
import com.iccuu.general_web_backend.module.user.mapper.UserCredentialsHistoryMapper;
import com.iccuu.general_web_backend.module.user.mapper.UserMapper;
import com.iccuu.general_web_backend.module.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserMapper userMapper;
    private final UserAuthMapper userAuthMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserCredentialsHistoryMapper userCredentialsHistoryMapper;
    private final AuditLogService auditLogService;
    private final LoginLogMapper loginLogMapper;

    private final com.iccuu.general_web_backend.module.role.service.RoleService roleService;

    private final VerificationCodeService verificationCodeService;
    private final TokenService tokenService;
    private final LoginLogService loginLogService;
    private final PasswordEncoder passwordEncoder;
    private final GeeTestVerifier geeTestVerifier;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final MetricsService metricsService;
    private final DataRetentionService dataRetentionService;
    private final AuthEventPublisher eventPublisher;

    private final com.iccuu.general_web_backend.module.mfa.service.UserMfaService mfaService;

    private final List<IdentityResolver> identityResolvers;
    private final List<AuthenticationProvider> authProviders;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        String identifier = resolveIdentifier(request);
        switch (verificationCodeService.verify(identifier, request.getVerificationCode(), "register")) {
            case EXPIRED:
                throw new AuthenticationException(ErrorCode.VERIFICATION_CODE_EXPIRED);
            case INVALID:
                throw new AuthenticationException(ErrorCode.VERIFICATION_CODE_INVALID);
            case VALID:
                break;
        }

        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())) > 0) {
            // Auto-append suffix on conflict
            String base = request.getUsername();
            String unique = base;
            int suffix = 1;
            while (userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, unique)) > 0 && suffix < 10000) {
                unique = base + "_" + suffix;
                suffix++;
            }
            request.setUsername(unique);
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, request.getEmail())) > 0) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, request.getPhone())) > 0) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }

        long userId = snowflakeIdGenerator.nextId();

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User();
        user.setUserId(userId);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setNickname(request.getUsername());
        user.setStatus(UserStatus.NORMAL.getValue());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        UserAuth userAuth = new UserAuth();
        userAuth.setUserId(userId);
        userAuth.setIdentityType(request.getIdentityType().toLowerCase());
        userAuth.setIdentifier(identifier);
        userAuth.setCredential(encodedPassword);
        userAuth.setVerified(1);
        userAuth.setIsPrimary(1);
        userAuth.setCreatedAt(LocalDateTime.now());
        userAuth.setUpdatedAt(LocalDateTime.now());
        userAuthMapper.insert(userAuth);

        // Also create a username-based UserAuth so the user can log in with username
        UserAuth usernameAuth = new UserAuth();
        usernameAuth.setUserId(userId);
        usernameAuth.setIdentityType(IdentityType.USERNAME.getValue());
        usernameAuth.setIdentifier(request.getUsername().toLowerCase());
        usernameAuth.setCredential(encodedPassword);
        usernameAuth.setVerified(1);
        usernameAuth.setIsPrimary(0);
        usernameAuth.setCreatedAt(LocalDateTime.now());
        usernameAuth.setUpdatedAt(LocalDateTime.now());
        userAuthMapper.insert(usernameAuth);

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        userProfileMapper.insert(profile);

        roleService.assignDefaultRole(userId);

        eventPublisher.publishUserRegistered(new UserRegisteredEvent(this, userId,
                request.getUsername(), request.getEmail(), request.getPhone(),
                request.getIdentityType().toLowerCase(), identifier, encodedPassword));

        log.info("User registered: userId={}, username={}", userId, request.getUsername());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        AuthenticationProvider provider = authProviders.stream()
                .filter(p -> p.supports(request))
                .findFirst()
                .orElseThrow(() -> new AuthenticationException(ErrorCode.INVALID_CREDENTIALS));

        AuthenticationResult result = provider.authenticate(request, SecurityUtil.getCurrentRequest());

        if (!result.success()) {
            eventPublisher.publishUserLoggedIn(result.toFailedEvent());
            metricsService.recordLogin("failure",
                    result.identityType() != null ? result.identityType().getValue() : "unknown");
            throw new AuthenticationException(result.errorCode(), result.failureReason());
        }

        if (result.mfaRequired()) {
            List<String> mfaTypes = result.mfaContext().mfaTypes();
            return LoginResponse.builder()
                    .mfaRequired(true)
                    .mfaTypes(mfaTypes)
                    .build();
        }

        eventPublisher.publishUserLoggedIn(result.toSuccessEvent());
        LoginResponse response = createLoginResponse(result.user());
        response.setNewDevice(result.newDevice());
        metricsService.recordLogin("success",
                result.identityType() != null ? result.identityType().getValue() : "unknown");
        return response;
    }

    @Override
    public LoginResponse refresh(RefreshTokenRequest request) {
        return tokenService.refreshToken(request.getRefreshToken());
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        tokenService.logout(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        UserAuth userAuth = userAuthMapper.selectOne(new LambdaQueryWrapper<UserAuth>()
                .eq(UserAuth::getUserId, userId)
                .eq(UserAuth::getIsPrimary, 1));
        if (userAuth == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(request.getOldPassword(), userAuth.getCredential())) {
            throw new AuthenticationException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_SAME);
        }

        Page<UserCredentialsHistory> historyPage = new Page<>(1, Constants.MAX_PASSWORD_HISTORY, false);
        List<UserCredentialsHistory> historyList = userCredentialsHistoryMapper.selectPage(
                historyPage,
                new LambdaQueryWrapper<UserCredentialsHistory>()
                        .eq(UserCredentialsHistory::getUserId, userId)
                        .orderByDesc(UserCredentialsHistory::getCreatedAt))
                .getRecords();
        for (UserCredentialsHistory history : historyList) {
            if (passwordEncoder.matches(request.getNewPassword(), history.getCredential())) {
                throw new BusinessException(ErrorCode.PASSWORD_REUSED);
            }
        }

        String newEncoded = passwordEncoder.encode(request.getNewPassword());
        userAuth.setCredential(newEncoded);
        userAuth.setUpdatedAt(LocalDateTime.now());
        userAuthMapper.updateById(userAuth);

        UserCredentialsHistory history = new UserCredentialsHistory();
        history.setUserId(userId);
        history.setCredential(newEncoded);
        history.setCreatedAt(LocalDateTime.now());
        userCredentialsHistoryMapper.insert(history);

        LocalDateTime now = LocalDateTime.now();
        user.setPasswordChangedAt(now);
        user.setUpdatedAt(now);
        userMapper.updateById(user);

        eventPublisher.publishPasswordChanged(new PasswordChangedEvent(this, userId, now));

        log.info("Password changed for userId={}", userId);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        switch (verificationCodeService.verify(request.getIdentifier(), request.getCode(), "reset_password")) {
            case EXPIRED:
                throw new AuthenticationException(ErrorCode.VERIFICATION_CODE_EXPIRED);
            case INVALID:
                throw new AuthenticationException(ErrorCode.VERIFICATION_CODE_INVALID);
            case VALID:
                break;
        }

        User user = findUserByIdentifier(request.getIdentifier());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        UserAuth userAuth = findUserAuth(user, request.getIdentifier());
        if (userAuth == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (passwordEncoder.matches(request.getNewPassword(), userAuth.getCredential())) {
            throw new BusinessException(ErrorCode.PASSWORD_SAME);
        }

        // Check password history for reuse (matching changePassword behavior)
        Page<UserCredentialsHistory> historyPage = new Page<>(1, Constants.MAX_PASSWORD_HISTORY, false);
        List<UserCredentialsHistory> historyList = userCredentialsHistoryMapper.selectPage(
                historyPage,
                new LambdaQueryWrapper<UserCredentialsHistory>()
                        .eq(UserCredentialsHistory::getUserId, user.getUserId())
                        .orderByDesc(UserCredentialsHistory::getCreatedAt))
                .getRecords();
        for (UserCredentialsHistory history : historyList) {
            if (passwordEncoder.matches(request.getNewPassword(), history.getCredential())) {
                throw new BusinessException(ErrorCode.PASSWORD_REUSED);
            }
        }

        String newEncoded = passwordEncoder.encode(request.getNewPassword());
        userAuth.setCredential(newEncoded);
        userAuth.setUpdatedAt(LocalDateTime.now());
        userAuthMapper.updateById(userAuth);

        UserCredentialsHistory history = new UserCredentialsHistory();
        history.setUserId(user.getUserId());
        history.setCredential(newEncoded);
        history.setCreatedAt(LocalDateTime.now());
        userCredentialsHistoryMapper.insert(history);

        LocalDateTime now = LocalDateTime.now();
        user.setPasswordChangedAt(now);
        user.setUpdatedAt(now);
        userMapper.updateById(user);

        eventPublisher.publishPasswordChanged(new PasswordChangedEvent(this, user.getUserId(), now));

        verificationCodeService.consume(request.getIdentifier(), "reset_password");

        log.info("Password reset for userId={}", user.getUserId());
    }

    @Override
    public TokenValidationResponse validateToken(String token) {
        JwtClaims claims = tokenService.parseAccessToken(token);
        if (claims == null) {
            return TokenValidationResponse.builder().valid(false).build();
        }

        String jti;
        try {
            jti = claims.getJwtId();
        } catch (MalformedClaimException e) {
            return TokenValidationResponse.builder().valid(false).build();
        }

        if (tokenService.isBlacklisted(jti)) {
            return TokenValidationResponse.builder().valid(false).build();
        }

        try {
            String userIdStr = claims.getSubject();
            String username = (String) claims.getClaimValue("username");
            @SuppressWarnings("unchecked")
            List<String> permissions = (List<String>) claims.getClaimValue("perm");
            long expiresAt = claims.getExpirationTime().getValueInMillis() / 1000;

            return TokenValidationResponse.builder()
                    .valid(true)
                    .userId(userIdStr != null ? Long.parseLong(userIdStr) : null)
                    .username(username)
                    .permissions(permissions)
                    .expiresAt(expiresAt)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse token claims", e);
            return TokenValidationResponse.builder().valid(false).build();
        }
    }

    @Override
    public void sendCode(SendCodeRequest request, String locale) {
        // GeeTest: mandatory for sendCode — reject if missing or failed (prevent spam)
        if (request.getCaptchaOutput() == null || request.getCaptchaOutput().isBlank()) {
            throw new AuthenticationException(ErrorCode.CAPTCHA_REQUIRED, "Captcha is required");
        }
        geeTestVerifier.verify(request.getCaptchaOutput(), request.getLotNumber(),
                request.getPassToken(), request.getGenTime(), false);

        // Pre-validate identifier before sending code
        if ("register".equalsIgnoreCase(request.getPurpose())) {
            if (IdentityType.EMAIL.getValue().equalsIgnoreCase(request.getIdentityType())
                    && request.getIdentifier() != null && !request.getIdentifier().isBlank()) {
                if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, request.getIdentifier())) > 0) {
                    throw new BusinessException(ErrorCode.EMAIL_EXISTS);
                }
            } else if (IdentityType.PHONE.getValue().equalsIgnoreCase(request.getIdentityType())
                    && request.getIdentifier() != null && !request.getIdentifier().isBlank()) {
                if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, request.getIdentifier())) > 0) {
                    throw new BusinessException(ErrorCode.PHONE_EXISTS);
                }
            }
        } else if ("reset_password".equalsIgnoreCase(request.getPurpose())) {
            // For password reset: user must exist
            User user = findUserByIdentifier(request.getIdentifier());
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
        }

        verificationCodeService.send(request.getIdentifier(), request.getIdentityType(), request.getPurpose(), locale);
    }

    @Override
    @Transactional
    public boolean verifyCode(VerifyCodeRequest request) {
        return verificationCodeService.verify(request.getIdentifier(), request.getCode(), request.getPurpose()) == VerifyResult.VALID;
    }

    @Override
    public Map<String, Object> exportUserData() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new AuthenticationException(ErrorCode.INVALID_CREDENTIALS);
        }

        Map<String, Object> data = new LinkedHashMap<>();

        User user = userMapper.selectById(currentUserId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        data.put("user", user);

        UserProfile profile = userProfileMapper.selectById(currentUserId);
        data.put("profile", profile);

        List<UserAuth> auths = userAuthMapper.selectList(
                new LambdaQueryWrapper<UserAuth>().eq(UserAuth::getUserId, currentUserId));
        data.put("auths", auths);

        List<UserRoleVO> roles = roleService.getUserRoles(currentUserId);
        data.put("roles", roles);

        var loginLogs = loginLogService.exportByUserId(currentUserId);
        data.put("loginLogs", loginLogs);

        var auditLogs = auditLogService.exportByUserId(currentUserId);
        data.put("auditLogs", auditLogs);

        log.info("GDPR data export for userId={}", currentUserId);
        return data;
    }

    @Override
    @Transactional
    public void forgetMe(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // Anonymize login logs
        loginLogMapper.update(null, new LambdaUpdateWrapper<LoginLog>()
                .eq(LoginLog::getUserId, userId)
                .set(LoginLog::getIdentifierHash, null)
                .set(LoginLog::getIpAddress, null));

        // Anonymize PII in core tables
        user.setUsername("deleted_" + userId);
        user.setEmail(null);
        user.setPhone(null);
        user.setNickname(null);
        user.setAvatar(null);
        user.setStatus(UserStatus.DISABLED.getValue());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        // Anonymize auth identifiers
        userAuthMapper.update(null, new LambdaUpdateWrapper<UserAuth>()
                .eq(UserAuth::getUserId, userId)
                .set(UserAuth::getIdentifier, null));

        // Anonymize user profile
        userProfileMapper.update(null, new LambdaUpdateWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .set(UserProfile::getRealName, null)
                .set(UserProfile::getMetadata, null));

        // Anonymize audit log PII
        auditLogService.anonymizeByUserId(userId);

        dataRetentionService.schedulePhysicalDeletion(userId, 30);

        log.info("GDPR forget-me processed for userId={}, scheduled physical deletion in 30 days", userId);
    }

    private IdentityResolver findResolver(String identifier) {
        return identityResolvers.stream()
                .filter(r -> r.canResolve(identifier))
                .findFirst()
                .orElseThrow(() -> new AuthenticationException(ErrorCode.INVALID_CREDENTIALS));
    }

    private User findUserByIdentifier(String identifier) {
        return findResolver(identifier).resolve(identifier);
    }

    private UserAuth findUserAuth(User user, String identifier) {
        return findResolver(identifier).resolveAuth(user, identifier);
    }

    private LoginResponse createLoginResponse(User user) {
        List<String> permissions = loadUserPermissions(user.getUserId());
        List<String> roleNames = roleService.getUserRoles(user.getUserId()).stream()
                .map(com.iccuu.general_web_backend.module.role.dto.UserRoleVO::getRoleName)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        LoginResponse tokenResponse = tokenService.createTokens(
                user.getUserId(), user.getUsername(), permissions);

        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .permissions(permissions)
                .roles(roleNames)
                .build();

        return LoginResponse.builder()
                .accessToken(tokenResponse.getAccessToken())
                .refreshToken(tokenResponse.getRefreshToken())
                .expiresIn(tokenResponse.getExpiresIn())
                .tokenType(tokenResponse.getTokenType())
                .mfaRequired(false)
                .userInfo(userInfo)
                .build();
    }

    private List<String> loadUserPermissions(Long userId) {
        return roleService.getPermissionStrings(userId);
    }

    private String resolveIdentifier(RegisterRequest request) {
        if (IdentityType.EMAIL.getValue().equalsIgnoreCase(request.getIdentityType())) {
            return request.getEmail();
        } else if (IdentityType.PHONE.getValue().equalsIgnoreCase(request.getIdentityType())) {
            return request.getPhone();
        }
        return request.getUsername();
    }

    private String guessIdentityType(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "unknown";
        }
        try {
            return findResolver(identifier).supportedType().getValue();
        } catch (Exception e) {
            return IdentityType.USERNAME.getValue();
        }
    }

}
