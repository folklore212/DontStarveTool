package com.iccuu.general_web_backend.module.role;

import com.iccuu.general_web_backend.BaseIntegrationTest;
import com.iccuu.general_web_backend.common.enums.UserStatus;
import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.module.auth.dto.LoginRequest;
import com.iccuu.general_web_backend.module.auth.dto.LoginResponse;
import com.iccuu.general_web_backend.module.role.dto.PermissionVO;
import com.iccuu.general_web_backend.module.role.dto.RoleTreeVO;
import com.iccuu.general_web_backend.module.role.dto.RoleVO;
import com.iccuu.general_web_backend.module.role.entity.Permission;
import com.iccuu.general_web_backend.module.role.entity.Role;
import com.iccuu.general_web_backend.module.role.mapper.PermissionMapper;
import com.iccuu.general_web_backend.module.role.entity.UserRole;
import com.iccuu.general_web_backend.module.role.mapper.RoleMapper;
import com.iccuu.general_web_backend.module.role.mapper.UserRoleMapper;
import com.iccuu.general_web_backend.module.user.entity.User;
import com.iccuu.general_web_backend.module.user.entity.UserAuth;
import com.iccuu.general_web_backend.module.user.mapper.UserAuthMapper;
import com.iccuu.general_web_backend.module.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoleIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserAuthMapper userAuthMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String ADMIN_EMAIL = "admin_role_test@example.com";
    private static final String ADMIN_USERNAME = "admin_role_test";
    private static final String ADMIN_PASSWORD = "Bb@445566";

    private Long adminUserId;
    private String adminAccessToken;

    @BeforeAll
    void seedDataAndLogin() {
        seedRoles();
        seedPermissions();
        seedAdminUser();
        adminAccessToken = loginAsAdmin();
    }

    @Test
    @Order(1)
    void testGetRoles() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);

        ResponseEntity<R<List<RoleVO>>> response = restTemplate.exchange(
                baseUrl() + "/api/v1/roles",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<R<List<RoleVO>>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCode()).isEqualTo(0);

        List<RoleVO> roles = response.getBody().getData();
        assertThat(roles).isNotNull().hasSize(3);
        assertThat(roles).extracting(RoleVO::getRoleName)
                .containsExactlyInAnyOrder("super_admin", "admin", "user");
    }

    @Test
    @Order(2)
    void testRoleTree() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);

        ResponseEntity<R<List<RoleTreeVO>>> response = restTemplate.exchange(
                baseUrl() + "/api/v1/roles/tree",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<R<List<RoleTreeVO>>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCode()).isEqualTo(0);

        List<RoleTreeVO> tree = response.getBody().getData();
        assertThat(tree).isNotNull().hasSize(1);

        // Root should be super_admin
        RoleTreeVO root = tree.get(0);
        assertThat(root.getRoleName()).isEqualTo("super_admin");
        assertThat(root.getParentRoleId()).isNull();

        // super_admin → admin
        assertThat(root.getChildren()).isNotNull().hasSize(1);
        RoleTreeVO admin = root.getChildren().get(0);
        assertThat(admin.getRoleName()).isEqualTo("admin");
        assertThat(admin.getParentRoleId()).isEqualTo(root.getId());

        // admin → user
        assertThat(admin.getChildren()).isNotNull().hasSize(1);
        RoleTreeVO user = admin.getChildren().get(0);
        assertThat(user.getRoleName()).isEqualTo("user");
        assertThat(user.getParentRoleId()).isEqualTo(admin.getId());
    }

    @Test
    @Order(3)
    void testPermissionDeniedReturns403() {
        // Create a restricted role with no user:read permission
        Role restrictedRole = new Role();
        restrictedRole.setRoleName("restricted_role");
        restrictedRole.setDescription("Role without user:read");
        restrictedRole.setParentRoleId(null);
        restrictedRole.setIsSystem(0);
        restrictedRole.setCreatedAt(LocalDateTime.now());
        restrictedRole.setUpdatedAt(LocalDateTime.now());
        roleMapper.insert(restrictedRole);

        // Create a test user with the restricted role
        String restrictedEmail = "restricted_test@example.com";
        String restrictedPassword = "Cc@556677";
        String restrictedUsername = "restricted_user";

        User restrictedUser = new User();
        restrictedUser.setUsername(restrictedUsername);
        restrictedUser.setEmail(restrictedEmail);
        restrictedUser.setNickname("Restricted Test User");
        restrictedUser.setStatus(UserStatus.NORMAL.getValue());
        restrictedUser.setCreatedAt(LocalDateTime.now());
        restrictedUser.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(restrictedUser);

        UserAuth restrictedAuth = new UserAuth();
        restrictedAuth.setUserId(restrictedUser.getUserId());
        restrictedAuth.setIdentityType("email");
        restrictedAuth.setIdentifier(restrictedEmail);
        restrictedAuth.setCredential(passwordEncoder.encode(restrictedPassword));
        restrictedAuth.setVerified(1);
        restrictedAuth.setIsPrimary(1);
        restrictedAuth.setCreatedAt(LocalDateTime.now());
        restrictedAuth.setUpdatedAt(LocalDateTime.now());
        userAuthMapper.insert(restrictedAuth);

        UserRole restrictedUserRole = new UserRole();
        restrictedUserRole.setUserId(restrictedUser.getUserId());
        restrictedUserRole.setRoleId(restrictedRole.getId());
        restrictedUserRole.setCreatedAt(LocalDateTime.now());
        userRoleMapper.insert(restrictedUserRole);

        // Login as the restricted user
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier(restrictedEmail);
        loginRequest.setCredential(restrictedPassword);

        ResponseEntity<R<LoginResponse>> loginResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<R<LoginResponse>>() {});
        assertThat(loginResponse.getBody().getCode()).isEqualTo(0);

        String restrictedToken = loginResponse.getBody().getData().getAccessToken();
        assertThat(restrictedToken).isNotBlank();

        // Try accessing /api/v1/users without user:read permission
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(restrictedToken);

        ResponseEntity<R<Void>> usersResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/users",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<R<Void>>() {});
        assertThat(usersResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(usersResponse.getBody()).isNotNull();
        assertThat(usersResponse.getBody().getCode()).isEqualTo(403);
        assertThat(usersResponse.getBody().getMessage()).contains("user:read");
    }

    @Test
    @Order(4)
    void testGetPermissions() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);

        ResponseEntity<R<List<PermissionVO>>> response = restTemplate.exchange(
                baseUrl() + "/api/v1/permissions",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<R<List<PermissionVO>>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCode()).isEqualTo(0);

        List<PermissionVO> permissions = response.getBody().getData();
        assertThat(permissions).isNotNull().hasSize(20);

        // Verify all expected permissions are present
        assertThat(permissions).extracting(PermissionVO::getCode).contains(
                "user:read", "user:write", "user:delete",
                "role:read", "role:write", "role:delete",
                "permission:read", "permission:write",
                "apikey:read", "apikey:write", "apikey:delete",
                "oauth:read", "oauth:write", "oauth:delete",
                "audit:read", "audit:export",
                "mfa:manage",
                "system:config", "system:metrics",
                "log:read");
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private void seedRoles() {
        long count = roleMapper.selectCount(null);
        if (count >= 3) {
            return; // already seeded
        }

        // Create super_admin (root, no parent)
        Role superAdmin = new Role();
        superAdmin.setRoleName("super_admin");
        superAdmin.setDescription("Super Administrator");
        superAdmin.setParentRoleId(null);
        superAdmin.setIsSystem(1);
        superAdmin.setCreatedAt(LocalDateTime.now());
        superAdmin.setUpdatedAt(LocalDateTime.now());
        roleMapper.insert(superAdmin);

        // Create admin (child of super_admin)
        Role admin = new Role();
        admin.setRoleName("admin");
        admin.setDescription("Administrator");
        admin.setParentRoleId(superAdmin.getId());
        admin.setIsSystem(1);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        roleMapper.insert(admin);

        // Create user (child of admin)
        Role userRole = new Role();
        userRole.setRoleName("user");
        userRole.setDescription("Normal User");
        userRole.setParentRoleId(admin.getId());
        userRole.setIsSystem(1);
        userRole.setCreatedAt(LocalDateTime.now());
        userRole.setUpdatedAt(LocalDateTime.now());
        roleMapper.insert(userRole);
    }

    private void seedPermissions() {
        long count = permissionMapper.selectCount(null);
        if (count >= 20) {
            return;
        }

        String[][] permDefs = {
                {"user:read", "Read Users", "user", "read", "View user details"},
                {"user:write", "Write Users", "user", "write", "Create and update users"},
                {"user:delete", "Delete Users", "user", "delete", "Delete users"},
                {"role:read", "Read Roles", "role", "read", "View roles"},
                {"role:write", "Write Roles", "role", "write", "Create and update roles"},
                {"role:delete", "Delete Roles", "role", "delete", "Delete roles"},
                {"permission:read", "Read Permissions", "permission", "read", "View permissions"},
                {"permission:write", "Write Permissions", "permission", "write", "Assign and modify permissions"},
                {"apikey:read", "Read API Keys", "apikey", "read", "View API keys"},
                {"apikey:write", "Write API Keys", "apikey", "write", "Create and update API keys"},
                {"apikey:delete", "Delete API Keys", "apikey", "delete", "Delete API keys"},
                {"oauth:read", "Read OAuth Clients", "oauth", "read", "View OAuth clients"},
                {"oauth:write", "Write OAuth Clients", "oauth", "write", "Create and update OAuth clients"},
                {"oauth:delete", "Delete OAuth Clients", "oauth", "delete", "Delete OAuth clients"},
                {"audit:read", "Read Audit Logs", "audit", "read", "View audit logs"},
                {"audit:export", "Export Audit Logs", "audit", "export", "Export audit log data"},
                {"mfa:manage", "Manage MFA", "mfa", "manage", "Manage MFA settings"},
                {"system:config", "System Config", "system", "config", "Manage system configuration"},
                {"system:metrics", "System Metrics", "system", "metrics", "View system metrics"},
                {"log:read", "Read Logs", "log", "read", "View system logs"},
        };

        for (String[] def : permDefs) {
            Permission perm = new Permission();
            perm.setCode(def[0]);
            perm.setName(def[1]);
            perm.setResourceType(def[2]);
            perm.setAction(def[3]);
            perm.setDescription(def[4]);
            perm.setCreatedAt(LocalDateTime.now());
            permissionMapper.insert(perm);
        }
    }

    private void seedAdminUser() {
        // Check if admin user already exists
        User existing = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getEmail, ADMIN_EMAIL));
        if (existing != null) {
            adminUserId = existing.getUserId();
            return;
        }

        // Create admin user
        User adminUser = new User();
        adminUser.setUsername(ADMIN_USERNAME);
        adminUser.setEmail(ADMIN_EMAIL);
        adminUser.setNickname("Admin Test User");
        adminUser.setStatus(UserStatus.NORMAL.getValue());
        adminUser.setCreatedAt(LocalDateTime.now());
        adminUser.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(adminUser);
        adminUserId = adminUser.getUserId();

        // Create auth record
        UserAuth auth = new UserAuth();
        auth.setUserId(adminUserId);
        auth.setIdentityType("email");
        auth.setIdentifier(ADMIN_EMAIL);
        auth.setCredential(passwordEncoder.encode(ADMIN_PASSWORD));
        auth.setVerified(1);
        auth.setIsPrimary(1);
        auth.setCreatedAt(LocalDateTime.now());
        auth.setUpdatedAt(LocalDateTime.now());
        userAuthMapper.insert(auth);

        // Assign admin role to the user
        Role adminRole = roleMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Role>()
                        .eq(Role::getRoleName, "admin"));
        if (adminRole != null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(adminUserId);
            userRole.setRoleId(adminRole.getId());
            userRole.setCreatedAt(LocalDateTime.now());
            userRoleMapper.insert(userRole);
        }
    }

    private String loginAsAdmin() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier(ADMIN_EMAIL);
        loginRequest.setCredential(ADMIN_PASSWORD);

        ResponseEntity<R<LoginResponse>> response = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<R<LoginResponse>>() {});

        assertThat(response.getBody().getCode()).isEqualTo(0);
        assertThat(response.getBody().getData().getAccessToken()).isNotBlank();

        return response.getBody().getData().getAccessToken();
    }
}
