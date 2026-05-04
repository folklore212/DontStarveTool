package com.iccuu.general_web_backend.module.auth.strategy.authentication;

import com.iccuu.general_web_backend.common.constant.ErrorCode;
import com.iccuu.general_web_backend.common.enums.AuthMethod;
import com.iccuu.general_web_backend.common.enums.IdentityType;
import com.iccuu.general_web_backend.common.enums.UserStatus;
import com.iccuu.general_web_backend.common.exception.AuthenticationException;
import com.iccuu.general_web_backend.infrastructure.geetest.GeeTestVerifier;
import com.iccuu.general_web_backend.infrastructure.security.DeviceFingerprintService;
import com.iccuu.general_web_backend.module.auth.dto.LoginRequest;
import com.iccuu.general_web_backend.module.auth.service.LoginLogService;
import com.iccuu.general_web_backend.module.auth.strategy.identity.IdentityResolver;
import com.iccuu.general_web_backend.module.mfa.entity.UserMfa;
import com.iccuu.general_web_backend.module.mfa.mapper.UserMfaMapper;
import com.iccuu.general_web_backend.module.mfa.strategy.MfaVerifier;
import com.iccuu.general_web_backend.module.user.entity.User;
import com.iccuu.general_web_backend.module.user.entity.UserAuth;
import com.iccuu.general_web_backend.module.user.mapper.UserAuthMapper;
import com.iccuu.general_web_backend.module.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordAuthenticationProviderTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private UserAuthMapper userAuthMapper;
    @Mock
    private UserMfaMapper userMfaMapper;
    @Mock
    private GeeTestVerifier geeTestVerifier;
    @Mock
    private LoginLogService loginLogService;
    @Mock
    private IdentityResolver identityResolver;
    @Mock
    private DeviceFingerprintService deviceFingerprintService;

    @Mock
    private MfaVerifier mfaVerifier;

    private PasswordEncoder passwordEncoder;
    private PasswordAuthenticationProvider provider;
    private MockHttpServletRequest httpRequest;

    @BeforeEach
    void setUp() throws Exception {
        passwordEncoder = new BCryptPasswordEncoder(4);

        lenient().when(geeTestVerifier.verify(any(), any(), any(), any(), anyBoolean())).thenReturn(true);
        lenient().when(identityResolver.supportedType()).thenReturn(IdentityType.EMAIL);
        lenient().when(deviceFingerprintService.checkDevice(any(), any()))
                .thenReturn(new DeviceFingerprintService.DeviceCheckResult(false, true));

        provider = new PasswordAuthenticationProvider(
                userMapper, userAuthMapper, userMfaMapper,
                geeTestVerifier, passwordEncoder, loginLogService,
                deviceFingerprintService,
                List.of(identityResolver), List.of(mfaVerifier)
        );

        httpRequest = new MockHttpServletRequest();
        httpRequest.setRemoteAddr("127.0.0.1");
        httpRequest.addHeader("User-Agent", "TestAgent/1.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpRequest));
    }

    @Test
    void shouldSupportPasswordAuthMethod() {
        assertThat(provider.getMethod()).isEqualTo(AuthMethod.PASSWORD);
    }

    @Test
    void shouldSupportLoginRequestWithCredentials() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("test@example.com");
        request.setCredential("password123");
        assertThat(provider.supports(request)).isTrue();
    }

    @Test
    void shouldNotSupportEmptyIdentifier() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("");
        request.setCredential("password123");
        assertThat(provider.supports(request)).isFalse();
    }

    @Test
    void shouldReturnFailureWhenUserNotFound() {
        LoginRequest request = buildLoginRequest("unknown@test.com", "any");
        when(identityResolver.canResolve("unknown@test.com")).thenReturn(true);

        AuthenticationResult result = provider.authenticate(request, httpRequest);

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void shouldReturnMfaRequiredWhenMfaEnabled() {
        User user = buildUser(1L, "test@test.com", UserStatus.NORMAL);
        UserAuth userAuth = buildUserAuth(1L, "email", "test@test.com", passwordEncoder.encode("correct"));
        UserMfa userMfa = buildUserMfa(1L);

        LoginRequest request = buildLoginRequest("test@test.com", "correct");

        when(identityResolver.canResolve("test@test.com")).thenReturn(true);
        when(identityResolver.resolve("test@test.com")).thenReturn(user);
        when(identityResolver.resolveAuth(user, "test@test.com")).thenReturn(userAuth);
        when(userMfaMapper.selectOne(any())).thenReturn(userMfa);

        AuthenticationResult result = provider.authenticate(request, httpRequest);

        assertThat(result.success()).isTrue();
        assertThat(result.mfaRequired()).isTrue();
        assertThat(result.mfaContext().mfaTypes()).contains("totp");
    }

    @Test
    void shouldRejectLockedAccount() {
        User user = buildUser(1L, "locked@test.com", UserStatus.LOCKED);
        user.setLockedUntil(System.currentTimeMillis() + 3600_000);
        UserAuth userAuth = buildUserAuth(1L, "email", "locked@test.com", passwordEncoder.encode("correct"));

        LoginRequest request = buildLoginRequest("locked@test.com", "correct");

        when(identityResolver.canResolve("locked@test.com")).thenReturn(true);
        when(identityResolver.resolve("locked@test.com")).thenReturn(user);
        when(identityResolver.resolveAuth(user, "locked@test.com")).thenReturn(userAuth);

        AuthenticationResult result = provider.authenticate(request, httpRequest);

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).contains("locked");
    }

    @Test
    void shouldRejectDisabledAccount() {
        User user = buildUser(1L, "disabled@test.com", UserStatus.DISABLED);
        UserAuth userAuth = buildUserAuth(1L, "email", "disabled@test.com", passwordEncoder.encode("correct"));

        LoginRequest request = buildLoginRequest("disabled@test.com", "correct");

        when(identityResolver.canResolve("disabled@test.com")).thenReturn(true);
        when(identityResolver.resolve("disabled@test.com")).thenReturn(user);
        when(identityResolver.resolveAuth(user, "disabled@test.com")).thenReturn(userAuth);

        AuthenticationResult result = provider.authenticate(request, httpRequest);

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo(ErrorCode.ACCOUNT_DISABLED);
    }

    @Test
    void shouldRejectPendingAccount() {
        User user = buildUser(1L, "pending@test.com", UserStatus.PENDING);
        UserAuth userAuth = buildUserAuth(1L, "email", "pending@test.com", passwordEncoder.encode("correct"));

        LoginRequest request = buildLoginRequest("pending@test.com", "correct");

        when(identityResolver.canResolve("pending@test.com")).thenReturn(true);
        when(identityResolver.resolve("pending@test.com")).thenReturn(user);
        when(identityResolver.resolveAuth(user, "pending@test.com")).thenReturn(userAuth);

        AuthenticationResult result = provider.authenticate(request, httpRequest);

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo(ErrorCode.ACCOUNT_PENDING);
    }

    @Test
    void shouldRejectWrongPassword() {
        User user = buildUser(1L, "test@test.com", UserStatus.NORMAL);
        UserAuth userAuth = buildUserAuth(1L, "email", "test@test.com", passwordEncoder.encode("correct"));

        LoginRequest request = buildLoginRequest("test@test.com", "wrong_password");

        when(identityResolver.canResolve("test@test.com")).thenReturn(true);
        when(identityResolver.resolve("test@test.com")).thenReturn(user);
        when(identityResolver.resolveAuth(user, "test@test.com")).thenReturn(userAuth);

        AuthenticationResult result = provider.authenticate(request, httpRequest);

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    private LoginRequest buildLoginRequest(String identifier, String credential) {
        LoginRequest request = new LoginRequest();
        request.setIdentifier(identifier);
        request.setCredential(credential);
        request.setCaptchaOutput("fake");
        request.setLotNumber("lot");
        request.setPassToken("pass");
        request.setGenTime("123");
        return request;
    }

    private User buildUser(Long id, String email, UserStatus status) {
        User user = new User();
        user.setUserId(id);
        user.setUsername("testuser");
        user.setEmail(email);
        user.setStatus(status.getValue());
        user.setFailedAttempts(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private UserAuth buildUserAuth(Long userId, String identityType, String identifier, String credential) {
        UserAuth auth = new UserAuth();
        auth.setId(1L);
        auth.setUserId(userId);
        auth.setIdentityType(identityType);
        auth.setIdentifier(identifier);
        auth.setCredential(credential);
        auth.setVerified(1);
        auth.setIsPrimary(1);
        return auth;
    }

    private UserMfa buildUserMfa(Long userId) {
        UserMfa mfa = new UserMfa();
        mfa.setId(1L);
        mfa.setUserId(userId);
        mfa.setMfaType("totp");
        mfa.setIsEnabled(1);
        return mfa;
    }
}
