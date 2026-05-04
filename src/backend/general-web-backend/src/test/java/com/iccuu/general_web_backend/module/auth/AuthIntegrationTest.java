package com.iccuu.general_web_backend.module.auth;

import com.iccuu.general_web_backend.BaseIntegrationTest;
import com.iccuu.general_web_backend.common.constant.RedisKeyPrefix;
import com.iccuu.general_web_backend.common.enums.UserStatus;
import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.module.auth.dto.*;
import com.iccuu.general_web_backend.module.user.entity.User;
import com.iccuu.general_web_backend.module.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserMapper userMapper;

    private static final String TEST_EMAIL = "testuser_auth_integration@example.com";
    private static final String TEST_USERNAME = "testuser_auth_integration";
    private static final String TEST_PASSWORD = "Aa@112233";

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @BeforeEach
    void setUp() {
        // Clean up any existing user from previous runs
        User existing = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getEmail, TEST_EMAIL));
        if (existing != null) {
            userMapper.deleteById(existing.getUserId());
        }
    }

    @Test
    @Order(1)
    void testUnauthenticatedAccessReturns401() {
        ResponseEntity<R<Void>> response = restTemplate.exchange(
                baseUrl() + "/api/v1/users",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<R<Void>>() {});
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isNotNull().isNotEmpty();
    }

    @Test
    @Order(2)
    void testInvalidTokenReturns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid_token_string");

        ResponseEntity<R<Void>> response = restTemplate.exchange(
                baseUrl() + "/api/v1/users",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<R<Void>>() {});
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(401);
    }

    @Test
    @Order(3)
    void testRegisterAndLogin() {
        // Step 1: Send verification code
        SendCodeRequest sendCodeRequest = new SendCodeRequest();
        sendCodeRequest.setIdentifier(TEST_EMAIL);
        sendCodeRequest.setIdentityType("email");
        sendCodeRequest.setPurpose("register");

        ResponseEntity<R<Void>> sendCodeResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/code/send",
                HttpMethod.POST,
                new HttpEntity<>(sendCodeRequest),
                new ParameterizedTypeReference<R<Void>>() {});
        assertThat(sendCodeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sendCodeResponse.getBody().getCode()).isEqualTo(0);

        // Step 2: Retrieve verification code from Redis
        String codeKey = RedisKeyPrefix.fmt(RedisKeyPrefix.VC, "register", TEST_EMAIL);
        String verificationCode = stringRedisTemplate.opsForValue().get(codeKey);
        assertThat(verificationCode).isNotNull().hasSize(6);

        // Step 3: Register with verification code
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(TEST_USERNAME);
        registerRequest.setEmail(TEST_EMAIL);
        registerRequest.setPassword(TEST_PASSWORD);
        registerRequest.setIdentityType("email");
        registerRequest.setVerificationCode(verificationCode);

        ResponseEntity<R<Void>> registerResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(registerRequest),
                new ParameterizedTypeReference<R<Void>>() {});
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registerResponse.getBody().getCode()).isEqualTo(0);

        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getEmail, TEST_EMAIL));
        assertThat(user).isNotNull();
        assertThat(user.getStatus()).isEqualTo(UserStatus.NORMAL.getValue());

        // Step 5: Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier(TEST_EMAIL);
        loginRequest.setCredential(TEST_PASSWORD);

        ResponseEntity<R<LoginResponse>> loginResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<R<LoginResponse>>() {});
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody().getCode()).isEqualTo(0);

        LoginResponse loginData = loginResponse.getBody().getData();
        assertThat(loginData.getAccessToken()).isNotBlank();
        assertThat(loginData.getRefreshToken()).isNotBlank();
        assertThat(loginData.getTokenType()).isEqualTo("Bearer");
        assertThat(loginData.getUserInfo()).isNotNull();
        assertThat(loginData.getUserInfo().getUsername()).isEqualTo(TEST_USERNAME);

        // Step 6: Call an authenticated endpoint
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(loginData.getAccessToken());

        ResponseEntity<R<TokenValidationResponse>> validateResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/token/validate",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders),
                new ParameterizedTypeReference<R<TokenValidationResponse>>() {});
        assertThat(validateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(validateResponse.getBody().getCode()).isEqualTo(0);
        assertThat(validateResponse.getBody().getData().isValid()).isTrue();
        assertThat(validateResponse.getBody().getData().getUsername()).isEqualTo(TEST_USERNAME);
    }

    @Test
    @Order(4)
    void testLoginFailureWrongPassword() {
        // First ensure a registered and activated user exists
        registerAndActivateUser();

        // Try login with wrong password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier(TEST_EMAIL);
        loginRequest.setCredential("WrongPassword123!");

        ResponseEntity<R<LoginResponse>> loginResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<R<LoginResponse>>() {});
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        // The app returns 200 with error code in body
        assertThat(loginResponse.getBody().getCode()).isNotEqualTo(0);
    }

    @Test
    @Order(5)
    void testRefreshToken() {
        // Register, activate and login
        registerAndActivateUser();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier(TEST_EMAIL);
        loginRequest.setCredential(TEST_PASSWORD);

        ResponseEntity<R<LoginResponse>> loginResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<R<LoginResponse>>() {});
        assertThat(loginResponse.getBody().getCode()).isEqualTo(0);

        String refreshToken = loginResponse.getBody().getData().getRefreshToken();
        assertThat(refreshToken).isNotBlank();

        // Refresh the token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(refreshToken);

        ResponseEntity<R<LoginResponse>> refreshResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(refreshRequest),
                new ParameterizedTypeReference<R<LoginResponse>>() {});
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody().getCode()).isEqualTo(0);

        LoginResponse refreshData = refreshResponse.getBody().getData();
        assertThat(refreshData.getAccessToken()).isNotBlank();
        assertThat(refreshData.getRefreshToken()).isNotBlank();
        // New access token should be different from old one
        assertThat(refreshData.getAccessToken()).isNotEqualTo(loginResponse.getBody().getData().getAccessToken());
    }

    @Test
    @Order(6)
    void testLogout() {
        // Register, activate and login
        registerAndActivateUser();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier(TEST_EMAIL);
        loginRequest.setCredential(TEST_PASSWORD);

        ResponseEntity<R<LoginResponse>> loginResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<R<LoginResponse>>() {});
        assertThat(loginResponse.getBody().getCode()).isEqualTo(0);

        String accessToken = loginResponse.getBody().getData().getAccessToken();
        String refreshToken = loginResponse.getBody().getData().getRefreshToken();

        // Verify token works before logout
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(accessToken);

        ResponseEntity<R<TokenValidationResponse>> validateBefore = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/token/validate",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders),
                new ParameterizedTypeReference<R<TokenValidationResponse>>() {});
        assertThat(validateBefore.getBody().getData().isValid()).isTrue();

        // Logout
        HttpHeaders logoutHeaders = new HttpHeaders();
        logoutHeaders.setBearerAuth(accessToken);

        RefreshTokenRequest logoutRequest = new RefreshTokenRequest();
        logoutRequest.setRefreshToken(refreshToken);

        ResponseEntity<R<Void>> logoutResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(logoutRequest, logoutHeaders),
                new ParameterizedTypeReference<R<Void>>() {});
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(logoutResponse.getBody().getCode()).isEqualTo(0);

        // Try using the old token after logout - should be rejected with 401
        ResponseEntity<R<TokenValidationResponse>> validateAfter = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/token/validate",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders),
                new ParameterizedTypeReference<R<TokenValidationResponse>>() {});
        assertThat(validateAfter.getBody().getData().isValid()).isFalse();

        // Verify blacklisted token returns 401 for secured endpoints
        ResponseEntity<R<Void>> usersResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/users",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders),
                new ParameterizedTypeReference<R<Void>>() {});
        assertThat(usersResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(usersResponse.getBody().getCode()).isEqualTo(401);
    }

    // ============ New Tests ============

    @Test
    @Order(7)
    void testResetPasswordFlow() {
        String email = "reset_test@example.com";
        String username = "reset_test_user";
        String oldPassword = "OldPass@123";
        String newPassword = "NewPass@456!";

        registerAndActivateUser(email, username, oldPassword);

        // Step 1: Send reset password code
        SendCodeRequest sendCodeRequest = new SendCodeRequest();
        sendCodeRequest.setIdentifier(email);
        sendCodeRequest.setIdentityType("email");
        sendCodeRequest.setPurpose("reset_password");
        restTemplate.postForEntity(baseUrl() + "/api/v1/auth/code/send", sendCodeRequest, R.class);

        // Step 2: Read code from Redis
        String codeKey = RedisKeyPrefix.fmt(RedisKeyPrefix.VC, "reset_password", email);
        String code = stringRedisTemplate.opsForValue().get(codeKey);
        assertThat(code).isNotNull().hasSize(6);

        // Step 3: Verify code (should NOT consume it for reset_password)
        VerifyCodeRequest verifyRequest = new VerifyCodeRequest();
        verifyRequest.setIdentifier(email);
        verifyRequest.setCode(code);
        verifyRequest.setPurpose("reset_password");
        ResponseEntity<R<Boolean>> verifyResp = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/code/verify", HttpMethod.POST,
                new HttpEntity<>(verifyRequest), new ParameterizedTypeReference<R<Boolean>>() {});
        assertThat(verifyResp.getBody().getCode()).isEqualTo(0);

        // Step 4: Verify code STILL exists in Redis (non-consumptive verify)
        String codeAfterVerify = stringRedisTemplate.opsForValue().get(codeKey);
        assertThat(codeAfterVerify).isEqualTo(code);

        // Step 5: Reset password with the verified code
        ResetPasswordRequest resetRequest = new ResetPasswordRequest();
        resetRequest.setIdentifier(email);
        resetRequest.setCode(code);
        resetRequest.setNewPassword(newPassword);
        restTemplate.postForEntity(baseUrl() + "/api/v1/auth/password/reset", resetRequest, R.class);

        // Step 6: Code should be consumed after successful reset
        String codeAfterReset = stringRedisTemplate.opsForValue().get(codeKey);
        assertThat(codeAfterReset).isNull();

        // Step 7: Login with new password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier(email);
        loginRequest.setCredential(newPassword);
        ResponseEntity<R<LoginResponse>> loginResp = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/login", HttpMethod.POST,
                new HttpEntity<>(loginRequest), new ParameterizedTypeReference<R<LoginResponse>>() {});
        assertThat(loginResp.getBody().getCode()).isEqualTo(0);

        // Cleanup
        User user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        if (user != null) userMapper.deleteById(user.getUserId());
    }

    @Test
    @Order(8)
    void testPasswordHistoryEnforcement() {
        String email = "history_test@example.com";
        String username = "history_test";
        String password = "Pass@11111";

        registerAndActivateUser(email, username, password);

        // Get a reset code
        sendCode(email, "reset_password");
        String codeKey = RedisKeyPrefix.fmt(RedisKeyPrefix.VC, "reset_password", email);
        String code = stringRedisTemplate.opsForValue().get(codeKey);
        assertThat(code).isNotNull();

        // Reset to SAME password should fail (PASSWORD_SAME)
        ResetPasswordRequest sameRequest = new ResetPasswordRequest();
        sameRequest.setIdentifier(email);
        sameRequest.setCode(code);
        sameRequest.setNewPassword(password);
        ResponseEntity<R<Void>> sameResp = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/password/reset", HttpMethod.POST,
                new HttpEntity<>(sameRequest), new ParameterizedTypeReference<R<Void>>() {});
        assertThat(sameResp.getBody().getCode()).isNotEqualTo(0);

        // Cleanup
        User user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        if (user != null) userMapper.deleteById(user.getUserId());
    }

    @Test
    @Order(9)
    void testCaseInsensitiveIdentityType() {
        String email = "case_test@example.com";
        String username = "case_test_user";

        // Register with lowercase identityType
        registerAndActivateUser(email, username, "Case@Pass1");

        // Login with same email should work (EmailIdentityResolver case-insensitive)
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier(email);
        loginRequest.setCredential("Case@Pass1");
        ResponseEntity<R<LoginResponse>> loginResp = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/login", HttpMethod.POST,
                new HttpEntity<>(loginRequest), new ParameterizedTypeReference<R<LoginResponse>>() {});
        assertThat(loginResp.getBody().getCode()).isEqualTo(0);

        // Login with username should work (via username UserAuth)
        LoginRequest usernameLogin = new LoginRequest();
        usernameLogin.setIdentifier(username);
        usernameLogin.setCredential("Case@Pass1");
        ResponseEntity<R<LoginResponse>> usernameResp = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/login", HttpMethod.POST,
                new HttpEntity<>(usernameLogin), new ParameterizedTypeReference<R<LoginResponse>>() {});
        assertThat(usernameResp.getBody().getCode()).isEqualTo(0);

        // Login with case-mismatched username should work
        LoginRequest caseLogin = new LoginRequest();
        caseLogin.setIdentifier(username.toUpperCase());
        caseLogin.setCredential("Case@Pass1");
        ResponseEntity<R<LoginResponse>> caseResp = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/login", HttpMethod.POST,
                new HttpEntity<>(caseLogin), new ParameterizedTypeReference<R<LoginResponse>>() {});
        assertThat(caseResp.getBody().getCode()).isEqualTo(0);

        // Cleanup
        User user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        if (user != null) userMapper.deleteById(user.getUserId());
    }

    @Test
    @Order(10)
    void testAccountStatusCheckedAfterPassword() {
        // Login with wrong password should return INVALID_CREDENTIALS,
        // not reveal account status (PENDING/DISABLED/LOCKED)
        String email = "status_test@example.com";
        registerAndActivateUser(email, "status_user", "StatusP@ss1");

        LoginRequest wrongPwd = new LoginRequest();
        wrongPwd.setIdentifier(email);
        wrongPwd.setCredential("WrongPassword");
        ResponseEntity<R<LoginResponse>> resp = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/login", HttpMethod.POST,
                new HttpEntity<>(wrongPwd), new ParameterizedTypeReference<R<LoginResponse>>() {});
        // Should return INVALID_CREDENTIALS (10002), not ACCOUNT_PENDING/DISABLED/LOCKED
        assertThat(resp.getBody().getCode()).isEqualTo(10002);

        // Login with correct password should succeed
        LoginRequest correct = new LoginRequest();
        correct.setIdentifier(email);
        correct.setCredential("StatusP@ss1");
        ResponseEntity<R<LoginResponse>> correctResp = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/login", HttpMethod.POST,
                new HttpEntity<>(correct), new ParameterizedTypeReference<R<LoginResponse>>() {});
        assertThat(correctResp.getBody().getCode()).isEqualTo(0);

        // Cleanup
        User user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        if (user != null) userMapper.deleteById(user.getUserId());
    }

    // ============ Helpers ============

    private void sendCode(String email, String purpose) {
        SendCodeRequest req = new SendCodeRequest();
        req.setIdentifier(email);
        req.setIdentityType("email");
        req.setPurpose(purpose);
        restTemplate.postForEntity(baseUrl() + "/api/v1/auth/code/send", req, R.class);
    }

    private void registerAndActivateUser(String email, String username, String password) {
        User existing = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getEmail, email));
        if (existing != null) {
            userMapper.deleteById(existing.getUserId());
        }

        sendCode(email, "register");
        String codeKey = RedisKeyPrefix.fmt(RedisKeyPrefix.VC, "register", email);
        String verificationCode = stringRedisTemplate.opsForValue().get(codeKey);

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);
        registerRequest.setIdentityType("email");
        registerRequest.setVerificationCode(verificationCode != null ? verificationCode : "000000");
        restTemplate.postForEntity(baseUrl() + "/api/v1/auth/register", registerRequest, R.class);
    }

    private void registerAndActivateUser() {
        // Check if user already exists and is active
        User existing = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getEmail, TEST_EMAIL));
        if (existing != null && existing.getStatus() == UserStatus.NORMAL.getValue()) {
            return;
        }

        // Send verification code
        SendCodeRequest sendCodeRequest = new SendCodeRequest();
        sendCodeRequest.setIdentifier(TEST_EMAIL);
        sendCodeRequest.setIdentityType("email");
        sendCodeRequest.setPurpose("register");
        restTemplate.postForEntity(baseUrl() + "/api/v1/auth/code/send", sendCodeRequest, R.class);

        // Get verification code from Redis
        String codeKey = RedisKeyPrefix.fmt(RedisKeyPrefix.VC, "register", TEST_EMAIL);
        String verificationCode = stringRedisTemplate.opsForValue().get(codeKey);

        // Register
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(TEST_USERNAME);
        registerRequest.setEmail(TEST_EMAIL);
        registerRequest.setPassword(TEST_PASSWORD);
        registerRequest.setIdentityType("email");
        registerRequest.setVerificationCode(verificationCode != null ? verificationCode : "000000");
        restTemplate.postForEntity(baseUrl() + "/api/v1/auth/register", registerRequest, R.class);

        // Activate user
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getEmail, TEST_EMAIL));
        if (user != null && user.getStatus() != UserStatus.NORMAL.getValue()) {
            user.setStatus(UserStatus.NORMAL.getValue());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        }
    }
}
