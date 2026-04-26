# 认证授权系统 — 后端接口设计方案

## 1. 概述

- **项目**: `src/backend/general-web-backend`（Spring Boot 3.4.x GA, Java 21, Maven；⚠ 若使用 4.x 需待 GA 发布，SNAPSHOT 不可上生产）
- **数据库**: MySQL 8.0 14 表（`deploy/docker/mysql/init.sql`）+ Redis 7.x
- **代码层级**: Controller → Service(Interface+Impl) → Mapper → DB，严格分层
- **API 前缀**: `/api/v1`
- **API 文档**: Knife4j/OpenAPI 3，运行时访问 `http://localhost:8080/doc.html`

---

## 2. Maven 依赖

| groupId : artifactId | 用途 |
|---|---|
| org.springframework.boot : spring-boot-starter-web | REST + Tomcat + Jackson |
| org.springframework.boot : spring-boot-starter-security | Auth filter chain + BCrypt |
| org.springframework.boot : spring-boot-starter-validation | Jakarta Bean Validation |
| org.springframework.boot : spring-boot-starter-data-redis | Redis + Lettuce |
| org.springframework.boot : spring-boot-starter-aop | AOP for audit/rate-limit |
| org.springframework.boot : spring-boot-starter-actuator | Health probes |
| org.springframework.boot : spring-boot-starter-mail | Email verification codes |
| com.baomidou : mybatis-plus-spring-boot3-starter 3.5.9 | ORM + Snowflake + pagination |
| com.mysql : mysql-connector-j | MySQL 8.x JDBC |
| org.redisson : redisson-spring-boot-starter 3.40.2 | Redis ops, distributed locks |
| cn.hutool : hutool-all 5.8.35 | Snowflake, crypto, TOTP, code gen (≥5.8.35 修复 CVE-2023-42277) |
| org.bitbucket.b_c : jose4j 0.9.8 | JWT RS256 (≥0.9.7 修复 CVE-2023-51701, CVE-2024-21635) |
| com.github.xiaoymin : knife4j-openapi3-jakarta-spring-boot-starter 4.5.0 | Swagger UI |
| org.mapstruct : mapstruct 1.6.3 (+ processor) | DTO/entity 编译期映射 |
| org.projectlombok : lombok | Boilerplate reduction |
| com.google.zxing : core + javase 3.5.5 | TOTP QR codes |
| com.github.ben-manes.caffeine : caffeine | Caffeine L1 本地权限缓存 |
| io.github.resilience4j : resilience4j-spring-boot3 | GeeTest 断路器降级 |
| io.micrometer : micrometer-registry-prometheus | Prometheus 指标暴露 |
| org.testcontainers : testcontainers/mysql/junit-jupiter (test) | Integration tests |
| com.h2database : h2 (test) | In-memory test DB |

---

## 3. 配置文件 (application.yml)

```yaml
server:
  forward-headers-strategy: NATIVE          # 信任 X-Forwarded-For/Proto 等代理头
  shutdown: graceful
spring:
  application.name: general-web-backend
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:3306/auth_system?useSSL=true&requireSSL=true&serverTimezone=Asia/Shanghai&sslMode=VERIFY_CA&trustCertificateKeyStoreUrl=file:${DB_TRUSTSTORE_PATH}&trustCertificateKeyStorePassword=${DB_TRUSTSTORE_PASSWORD}
    # 本地开发可临时改为 useSSL=false&requireSSL=false&sslMode=PREFERRED
    # 云数据库（RDS/PolarDB）通过 sslMode=VERIFY_CA 验证 CA 证书，可不指定 truststore 使用 JVM 默认 cacerts
    username: ${DB_USERNAME:app_user}
    password: ${DB_PASSWORD}
    # 生产环境使用低权限应用账号，禁用 root
  data.redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    # 生产环境必须设置 REDIS_PASSWORD，配合 Redis protected-mode yes
  mail:
    host: smtp.example.com
    port: 465
    username: noreply@example.com
    password: ${MAIL_PASSWORD}
    properties.mail.smtp.auth: true
    properties.mail.smtp.ssl.enable: true

mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  global-config.db-config:
    logic-delete-field: deletedAt
    logic-delete-value: "NOW_MILLIS"
    logic-not-delete-value: "0"
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

jwt:
  private-key: ${JWT_PRIVATE_KEY_PATH:/etc/secrets/jwt-private.pem}   # 生产环境外部挂载，勿放 classpath
  public-key: ${JWT_PUBLIC_KEY_PATH:/etc/secrets/jwt-public.pem}
  # 开发环境可临时覆盖为 classpath:jwt-private.pem
  access-token-ttl: 900            # 15 min
  refresh-token-ttl: 604800        # 7 days

snowflake:
  datacenter-id: 1

geetest:
  login:
    captcha-id: ${GEETEST_LOGIN_CAPTCHA_ID}
    captcha-key: ${GEETEST_LOGIN_CAPTCHA_KEY}
  register:
    captcha-id: ${GEETEST_REGISTER_CAPTCHA_ID}
    captcha-key: ${GEETEST_REGISTER_CAPTCHA_KEY}

crypto:
  aes-keys:
    1: ${AES_KEY_V1}    # 当前 AES-256 加密密钥 (Base64 编码的 32 字节)
    0: ${AES_KEY_V0}    # 仅用于解密旧版本数据，详见 9.7
```

---

## 4. 项目包结构

```
com.iccuu.general_web_backend
├── GeneralWebBackendApplication.java
│
├── common/
│   ├── config/
│   │   ├── WebMvcConfig.java              — CORS, Jackson, converters
│   │   ├── SecurityConfig.java            — SecurityFilterChain, BCryptPasswordEncoder(12)
│   │   ├── MyBatisPlusConfig.java         — Snowflake IdGenerator, pagination, soft-delete injector
│   │   ├── RedisConfig.java               — RedisTemplate, RedissonClient
│   │   ├── AsyncConfig.java               — @EnableAsync for email/log/audit
│   │   ├── GeeTestConfig.java             — GeeTestProperties 注册为 @ConfigurationProperties("geetest")
│   │   └── Knife4jConfig.java             — OpenAPI group config
│   ├── constant/
│   │   ├── Constants.java
│   │   ├── RedisKeyPrefix.java            — Redis key naming convention
│   │   └── ErrorCode.java                 — Enum: code + i18n message key
│   ├── enums/
│   │   ├── UserStatus.java                — NORMAL(0), DISABLED(1), PENDING(2), LOCKED(3)
│   │   ├── IdentityType.java              — phone, email, wechat, github, google, apple, username
│   │   ├── MfaType.java                   — totp, sms, email, webauthn
│   │   ├── AuthMethod.java                — password, totp, sms, oauth, api_key, sso
│   │   ├── LoginResult.java               — success, failed_credential, failed_mfa, failed_locked, failed_disabled
│   │   ├── ScopeType.java                 — self, dept, org, all
│   │   ├── ClientType.java                — confidential, public
│   │   ├── GrantType.java                 — authorization_code, client_credentials, refresh_token (implicit 已废弃)
│   │   └── ApiKeyStatus.java              — DISABLED(0), NORMAL(1)
│   ├── exception/
│   │   ├── BusinessException.java         — extends RuntimeException(code, message)
│   │   ├── AuthenticationException.java   — 401
│   │   ├── AuthorizationException.java    — 403
│   │   ├── ResourceNotFoundException.java — 404
│   │   ├── RateLimitException.java        — 429
│   │   └── DuplicateResourceException.java— 409
│   ├── handler/
│   │   └── GlobalExceptionHandler.java    — @RestControllerAdvice, handles all above + validation errors
│   ├── result/
│   │   ├── R.java                         — Generic response: code, message, data, timestamp. Static: R.ok(), R.fail()
│   │   └── PageResult.java                — Extends R: total, page, size, list
│   ├── aspect/
│   │   ├── AuditLogAspect.java            — @Around @AuditLog, publishes AuditEvent
│   │   └── RateLimitAspect.java           — @Around @RateLimit, Redis sliding window
│   ├── annotation/
│   │   ├── AuditLog.java                  — action, resourceType, resourceIdExpression(SpEL)
│   │   ├── RateLimit.java                 — key(SpEL), permits, window(seconds)
│   │   └── RequirePermission.java         — value("user:create"), scopeType
│   ├── util/
│   │   ├── JwtUtil.java                   — createAccessToken, createRefreshToken, parseToken, getJti
│   │   ├── CryptoUtil.java                — AES-256-GCM encrypt/decrypt
│   │   ├── HashUtil.java                  — SHA-256, BCrypt convenience
│   │   ├── IpUtil.java                    — Extract client IP from request (解析 X-Forwarded-For 代理链)
│   │   ├── RedisUtil.java                 — Convenience over RedisTemplate
│   │   └── SecurityUtil.java              — getUserId(), getPermissions() from SecurityContext
│   └── filter/
│       ├── JwtAuthenticationFilter.java   — OncePerRequestFilter: parse Bearer, verify jose4j, check blacklist
│       └── ApiKeyAuthenticationFilter.java— OncePerRequestFilter: parse X-API-Key, SHA-256 hash, lookup
│
├── infrastructure/
│   ├── snowflake/
│   │   └── SnowflakeIdGenerator.java      — Hutool Snowflake wrapper
│   ├── security/
│   │   ├── JwtTokenProvider.java          — jose4j core: sign/verify/parse RS256 JWT
│   │   ├── SecurityContextHelper.java     — Static: getUserId(), getPermissions(), hasPermission()
│   │   ├── PermissionResolver.java        — Interface: resolvePermissions(userId) → Set<EffectivePermission>
│   │   └── RateLimiterService.java        — Redis sliding-window counter with Lua script
│   ├── audit/
│   │   └── AuditEventPublisher.java       — Spring event publisher + @Async listener
│   ├── geetest/
│   │   ├── GeeTestProperties.java         — @ConfigurationProperties("geetest"), 两个模块的 captcha-id/key
│   │   └── GeeTestVerifier.java           — 调用极验 v4 /validate 接口 HMAC-MD5 签名校验
│   └── storage/
│       └── PartitionMaintenanceScheduler.java — @Scheduled monthly, calls stored procedure
│
└── module/
    ├── auth/
    │   ├── controller/AuthController.java
    │   ├── service/
    │   │   ├── AuthService.java + impl/AuthServiceImpl.java
    │   │   ├── TokenService.java + impl/TokenServiceImpl.java
    │   │   ├── MfaService.java + impl/MfaServiceImpl.java
    │   │   ├── VerificationCodeService.java + impl/VerificationCodeServiceImpl.java
    │   │   └── LoginLogService.java + impl/LoginLogServiceImpl.java
    │   ├── dto/
    │   │   ├── LoginRequest.java           — identifier, credential, mfa_code, captcha_output, lot_number, pass_token, gen_time
    │   │   ├── LoginResponse.java          — access_token, refresh_token, expires_in, user_info, mfa_required, mfa_types
    │   │   ├── RegisterRequest.java        — username, email, phone, password, identity_type, verification_code
    │   │   ├── RefreshTokenRequest.java    — refresh_token
    │   │   ├── ChangePasswordRequest.java  — old_password, new_password
    │   │   ├── ResetPasswordRequest.java   — identifier, code, new_password
    │   │   ├── SendCodeRequest.java        — identifier, identity_type, purpose, captcha_output, lot_number, pass_token, gen_time
    │   │   ├── VerifyCodeRequest.java      — identifier, code, purpose
    │   │   └── TokenValidationResponse.java— valid, user_id, username, permissions[], expires_at
    │   └── strategy/
    │       ├── AuthStrategy.java           — supports(IdentityType), authenticate(AuthContext)
    │       ├── PasswordAuthStrategy.java   — bcrypt verify
    │       ├── OAuthAuthStrategy.java      — OAuth delegate
    │       └── ApiKeyAuthStrategy.java     — SHA-256 hash compare
    │
    ├── user/
    │   ├── controller/UserController.java
    │   ├── service/
    │   │   ├── UserService.java + impl/UserServiceImpl.java
    │   │   ├── UserProfileService.java + impl/UserProfileServiceImpl.java
    │   │   └── UserCredentialsHistoryService.java + impl
    │   ├── dto/
    │   │   ├── UserCreateRequest.java / UserUpdateRequest.java / UserQueryRequest.java
    │   │   ├── UserVO.java / UserStatusRequest.java
    │   │   ├── UserProfileUpdateRequest.java / UserProfileVO.java
    │   │   ├── UserAuthVO.java / BindAuthRequest.java
    │   ├── mapper/ (User, UserAuth, UserProfile, UserCredentialsHistory) + .xml
    │   └── converter/UserConverter.java    — MapStruct
    │
    ├── role/
    │   ├── controller/RoleController.java
    │   ├── service/
    │   │   ├── RoleService.java + impl/RoleServiceImpl.java
    │   │   └── PermissionService.java + impl/PermissionServiceImpl.java
    │   ├── dto/
    │   │   ├── RoleCreateRequest.java / RoleUpdateRequest.java
    │   │   ├── RoleVO.java / RoleTreeVO.java / PermissionVO.java
    │   │   ├── AssignRoleRequest.java / AssignPermissionRequest.java / UserRoleVO.java
    │   ├── mapper/ (Role, Permission, UserRole, RolePermission, Scope) + .xml
    │   ├── converter/ (RoleConverter, PermissionConverter)
    │   └── cache/
│       ├── PermissionEvaluatorImpl.java    — implements infra PermissionResolver, recursive CTE + Redis cache
│       └── PermissionCacheManager.java     — invalidation + TTL management
    │
    ├── oauth/
    │   ├── controller/OAuthClientController.java
    │   ├── service/
    │   │   ├── OAuthClientService.java + impl
    │   │   └── OAuthAuthorizationService.java + impl
    │   ├── dto/
    │   │   ├── OAuthClientCreateRequest.java / OAuthClientUpdateRequest.java / OAuthClientVO.java
    │   │   ├── AuthorizationRequest.java / TokenExchangeRequest.java / TokenResponse.java
    │   ├── mapper/OAuthClientMapper.java + .xml
    │   └── converter/OAuthClientConverter.java
    │
    ├── apikey/
    │   ├── controller/ApiKeyController.java
    │   ├── service/ (ApiKeyService.java + impl)
    │   ├── dto/
    │   │   ├── ApiKeyCreateRequest.java / ApiKeyCreateResponse.java / ApiKeyVO.java
    │   ├── mapper/ApiKeyMapper.java + .xml
    │   └── converter/ApiKeyConverter.java
    │
    ├── audit/
    │   ├── controller/ (AuditLogController.java, LoginLogController.java)
    │   ├── service/ (AuditLogService.java + impl)
    │   ├── dto/ (AuditLogQueryRequest.java, AuditLogVO.java, LoginLogQueryRequest.java, LoginLogVO.java)
    │   ├── mapper/ (AuditLogMapper, LoginLogMapper) + .xml
    │   └── converter/AuditLogConverter.java
    │
    └── mfa/
        ├── controller/MfaController.java
        ├── service/ (UserMfaService.java + impl)
        ├── dto/
        │   ├── MfaSetupInitRequest.java / MfaSetupInitResponse.java
        │   ├── MfaEnableRequest.java / MfaDisableRequest.java / MfaStatusVO.java
        ├── mapper/UserMfaMapper.java + .xml
        └── converter/MfaConverter.java
```

---

## 5. REST API 端点设计

### 5.1 认证 (`/api/v1/auth`)

| Method | Path | Auth | 描述 |
|--------|------|------|------|
| POST | /register | — | 用户注册 (GeeTest在 sendCode 环节) |
| POST | /login | — | 登录 (GeeTest → password → MFA → JWT) |
| POST | /refresh | — (HttpOnly Cookie 或 body) | 刷新 access token (refresh token 优先从 Cookie 读取) |
| POST | /logout | Bearer token | 黑名单 access token JTI, 吊销 refresh token family, 清除 refresh cookie |
| POST | /password/change | Bearer token | 修改密码 (验旧+查历史+更新password_changed_at) |
| POST | /password/reset | — | 验证码重置密码 |
| POST | /code/send | — | GeeTest → 发邮箱验证码 (5min TTL, rate-limited) |
| POST | /code/verify | — | 验证邮箱验证码 |
| GET | /token/validate | Bearer token | token 有效性校验（供网关使用） |
| POST | /me/export | Bearer token | GDPR Art.20 数据导出 (1/day/user 限流) |

**注册流程**:
```
前端: 填表(username/email/phone/password) → 点"发送验证码" → GeeTest滑块 → POST /code/send {identifier, identity_type, purpose} → 收到邮件
前端: 输入验证码 → POST /register {..., identity_type, verification_code}
后端: 校验验证码 → Snowflake user_id → bcrypt密码 → 事务写入 users(status=PENDING) + user_auths(is_verified=0) + user_profiles
      → 发激活邮件 → 用户点击链接或输入验证码 → POST /code/verify {identifier, code, purpose=activate}
      → 校验通过 → users.status=NORMAL, user_auths.is_verified=1, 分配默认user角色
```
PENDING 状态的用户无法登录，确保邮箱所有权验证完成后再激活账户。

**登录流程**:
```
前端: 输入账密 → GeeTest滑块 → POST /login {identifier, credential, captcha_*}
后端: 校验GeeTest → 查询user_auths → 校验bcrypt → 检查MFA → 签发JWT → 记录login_log
```

### 5.2 OAuth2 (`/api/v1/oauth`)

| Method | Path | Permission | 描述 |
|--------|------|-----------|------|
| GET | /authorize | — | 授权码流程入口 (code → Redis 10min) |
| POST | /token | — | code/client_credentials/refresh_token 换 token |
| POST | /revoke | — | 吊销 token |
| GET | /clients | client:read | 列出 OAuth2 客户端 |
| GET | /clients/{id} | client:read | 客户端详情 |
| POST | /clients | client:create | 注册客户端 |
| PUT | /clients/{id} | client:update | 更新客户端 |
| DELETE | /clients/{id} | client:delete | 删除客户端 |
| POST | /clients/{id}/regenerate-secret | client:update | 重新生成 secret |

### 5.3 用户管理 (`/api/v1/users`)

| Method | Path | Permission | Scope | 描述 |
|--------|------|-----------|-------|------|
| GET | / | user:read | — | 分页列表 |
| GET | /{userId} | user:read | self/dept/org/all | 用户详情 |
| POST | / | user:create | all | 管理员创建用户 |
| PUT | /{userId} | user:update | self/dept/org/all | 更新用户 |
| DELETE | /{userId} | user:delete | all | 软删除 |
| PATCH | /{userId}/status | user:lock | all | 锁定/禁用/解封 |
| GET | /{userId}/roles | role:read | all | 用户角色列表 |
| POST | /{userId}/roles | role:assign | all | 分配角色 (scope_type + scope_value) |
| DELETE | /{userId}/roles/{roleId}/{scopeType}/{scopeValue} | role:assign | all | 移除角色 (scope_type + scope_value 定位) |
| GET | /{userId}/auths | user:read | self/all | 多身份列表 |
| POST | /{userId}/auths | user:update | self/all | 绑定新身份 |
| DELETE | /{userId}/auths/{authId} | user:update | self/all | 解绑身份 |
| GET | /me | — | — | 当前用户信息 |
| PUT | /me/profile | — | — | 更新自己的 profile |
| PUT | /me/avatar | — | — | 更新头像 |
| POST | /{userId}/forget-me | user:delete 或 self | all/self | GDPR Art.17 即时擦除 |

### 5.4 角色与权限 (`/api/v1/roles`, `/api/v1/permissions`, `/api/v1/scopes`)

| Method | Path | Permission | 描述 |
|--------|------|-----------|------|
| GET | /roles | role:read | 角色列表 |
| GET | /roles/tree | role:read | 角色层级树 |
| GET | /roles/{id} | role:read | 角色详情 |
| POST | /roles | role:create | 创建角色 |
| PUT | /roles/{id} | role:update | 更新角色 |
| DELETE | /roles/{id} | role:delete | 删除 (非system角色) |
| GET | /roles/{id}/permissions | perm:read | 角色权限列表 |
| POST | /roles/{id}/permissions | perm:assign | 分配权限 |
| DELETE | /roles/{id}/permissions/{permId} | perm:assign | 移除权限 |
| GET | /permissions | perm:read | 所有权限列表 |
| GET | /scopes | perm:read | 所有作用域 |

### 5.5 API Key (`/api/v1/api-keys`)

| Method | Path | Permission | 描述 |
|--------|------|-----------|------|
| GET | / | — (own) | 列出当前用户的 key |
| POST | / | apikey:create | 创建 (返回原始 key 一次, SHA-256 存储) |
| DELETE | /{keyId} | apikey:revoke | 吊销 |
| PATCH | /{keyId}/rotate | apikey:rotate | 轮换 (返回新原始 key, 内部同时执行吊销+创建) |

### 5.6 MFA (`/api/v1/mfa`)

| Method | Path | 描述 |
|--------|------|------|
| GET | /status | 列出已启用的 MFA 方式 |
| POST | /setup/init | 开始 TOTP 设置 (AES 加密 secret + QR URI + backup_codes) |
| POST | /setup/verify | 验证并启用 |
| POST | /disable | 禁用 (需密码确认) |
| GET | /backup-codes | 重新生成备用恢复码 |

### 5.7 审计日志 (`/api/v1/audit-logs`, `/api/v1/login-logs`)

| Method | Path | Permission | 描述 |
|--------|------|-----------|------|
| GET | /audit-logs | audit:read | 分页查询 |
| GET | /audit-logs/{id} | audit:read | 单条详情 |
| GET | /audit-logs/export | audit:read | CSV 导出 |
| GET | /login-logs | audit:read | 登录日志查询 |

---

## 6. 关键架构决策

### 6.1 软删除 — deleted_at 复合唯一键

`users`, `roles`, `user_auths`, `oauth_clients`, `api_keys` 使用 `deleted_at BIGINT DEFAULT 0` 配合复合唯一键如 `uk_username (username, deleted_at)`。自定义 MyBatis-Plus SqlInjector 将 `WHERE deleted=0` 替换为 `WHERE deleted_at=0`。

### 6.2 Snowflake ID 生成

`users.user_id` 为 BIGINT Snowflake。MyBatis-Plus `IdentifierGenerator` + Hutool Snowflake 用于高容量表。查找表 (roles/permissions/scopes INT) 使用 AUTO_INCREMENT。

### 6.3 JWT + password_changed_at

JWT 含 `iat` claim。`JwtAuthenticationFilter` 校验 token 时比对 `users.password_changed_at` — 如果 JWT 签发时间早于密码修改时间，token 被拒绝。确保改密码后所有旧 token 失效。

### 6.4 层级 RBAC

`roles.parent_role_id` 构成自引用层级。`PermissionEvaluator` 解析有效权限：
1. 加载用户的直接角色 (`user_roles`, 过滤过期的)
2. 沿 `parent_role_id` 链上溯收集所有祖先角色
3. 取并集所有 `role_permissions`，每个角色的有效作用域先经 `user_roles.scope_type` 收窄（祖先角色无此限制，视为 `all`），再取最宽松值: all > org > dept > self
4. Redis 缓存 `perm:effective:{userId}`，角色/权限/用户角色变更时失效
具体 SQL 实现参见 [9.18](#918-rbac-层级解析--递归-cte)。

种子数据：`super_admin` → 20权限@all, `admin` → 不含audit:read和client:delete@all, `user` → user:read/role:read/perm:read/user:update@self

### 6.5 MFA — AES-256-GCM 加密存储

TOTP secret 和 backup codes 以 AES-256-GCM 加密后分别存入 `user_mfa.secret` (VARCHAR) 和 `user_mfa.backup_codes` (JSON)。加密密钥从 `application.yml` 读取，支持密钥版本化。

### 6.6 分区表

`login_logs` 和 `audit_logs` 使用 `RANGE COLUMNS(created_date)` + 复合主键 `(id, created_date)`。Java 侧 `PartitionMaintenanceScheduler` 作为主维护方案（云数据库普遍不支持 MySQL Event Scheduler）。MySQL Event `evt_add_partitions` 为可选增强（仅自建 MySQL 支持，开启需 `SUPER` 权限）。

### 6.7 OAuth2 — 完整 Provider

支持 `authorization_code` (强制 PKCE S256 + state CSRF 防护) / `client_credentials` / `refresh_token` 三种 grant type。`implicit` flow 按 OAuth 2.1 规范已废弃且不启用。授权码 Redis 存储 (10min TTL)，PKCE 支持 (`oauth:code:{code}:pkce`)。`state` 参数存 Redis (`oauth:state:{state}`, TTL: 600s) 防登录 CSRF。用户同意记录按 user+client 存储。可信客户端 (`is_trusted=1`) 跳过同意页。

### 6.8 API Key — 作用域限制

`api_keys.allowed_scopes` (JSON) 可限定某个 API Key 的有效权限集。验证时如果设了此字段，则覆盖用户角色派生的权限。

### 6.9 登录锁定

1. 失败 → `lockout:failed:{userId}` Redis Hash: `HINCRBY attempts 1`, `EXPIRE 1800`
2. attempts ≥ 5 → users.status=3 (LOCKED), locked_until=now+30min
3. 成功 → 重置计数器 + failed_attempts
4. LoginLog 异步写入对应的 result + failure_reason

### 6.10 Refresh Token 轮换 + 盗用检测

每次登录创建 "token family" (UUID)。refresh token 默认通过 **HttpOnly + Secure + SameSite=Strict Cookie** 下发（浏览器端）或 POST body（移动端），前端 JavaScript 不可访问，消除 XSS 窃取风险。

```
Set-Cookie: refresh_token=<encrypted>; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth; Max-Age=604800
```

refresh token 存入 `refresh:family:{familyId}` Redis Hash。旋转操作使用 Redis Lua 脚本保证原子性（防并发竞态误触发整族撤销），详见 [9.43](#943-refresh-token-原子旋转--lua-脚本)。登出时除黑名单 access token JTI 外，同时清除 refresh cookie 和吊销 family。

### 6.11 GeeTest 极验 v4 + 断路器降级

GeeTest 服务不可达时通过 Resilience4j 断路器降级为严格限流模式，不阻塞登录。详见 [9.44](#944-geetest-断路器与降级)。

| 模块 | captcha-id | 保护接口 | 防护目标 |
|------|-----------|----------|----------|
| login | geetest.login.captcha-id | POST /api/v1/auth/login | 防撞库、暴力破解 |
| register | geetest.register.captcha-id | POST /api/v1/auth/code/send | 防刷验证码 (注册+重置密码) |

**验证流程**:
1. 前端加载极验 v4 SDK → 用户完成验证 → 获得 `captcha_output`, `lot_number`, `pass_token`, `gen_time`
2. 请求时附带以上参数
3. 后端 `GeeTestVerifier.verify()` → POST `https://gt4.geetest.com/validate` → 服务端 HMAC-MD5 签名
4. 失败返回 `GEE_TEST_VERIFY_FAILED`，不执行业务逻辑
5. Redis 缓存成功结果 (TTL 5min)，避免短时间内重复调用极验接口

### 6.12 Redis 键空间

```
blacklist:jti:{jti}                  — JWT 黑名单 (TTL: 960s, access-token-ttl + 60s 时钟偏差缓冲)
refresh:family:{familyId}            — Refresh Token 家族 (TTL: 604800s, 匹配 refresh-token-ttl)
refresh:family:{familyId}:revoked    — 家族吊销标记 (TTL: 604800s)
session:{userId}:{sessionId}         — 会话元数据 (TTL: 604800s)
oauth:code:{code}                    — 授权码上下文 (TTL: 600s)
oauth:code:{code}:pkce              — PKCE code_challenge (TTL: 600s)
oauth:state:{state}                  — OAuth state 防 CSRF (TTL: 600s)
oauth:consent:{userId}:{clientId}    — OAuth 同意记录 (TTL: 2592000s, 30天)
lockout:failed:{userId}              — 登录失败计数器 (TTL: 1800s)
ratelimit:{purpose}:{identifier}     — API 频率限制 (TTL: 窗口时间 + 1s)
vc:{purpose}:{identifier}            — 邮箱验证码 (TTL: 300s)
vc:rl:{identifier}                   — 验证码发送频率限制 (TTL: 3600s, 实际窗口 300s, TTL 仅用于清理)
geetest:result:{module}:{lot_number} — 极验校验结果缓存 (TTL: 300s)
perm:effective:{userId}              — 有效权限缓存 (TTL: 600s, 变更时主动失效 + TTL 兜底)
perm:roles:{userId}                  — 有效角色缓存 (TTL: 600s, 变更时主动失效 + TTL 兜底)
apikey:{key_hash}                    — API Key 验证缓存 (TTL: 120s, 吊销/轮换时主动 DEL)
snowflake:worker:counter            — Snowflake worker-id 分配计数器 (INCR 自增, 无 TTL)
oauth:client:{clientId}              — OAuth 客户端缓存 (TTL: 300s, 更新/删除时主动 DEL)
oauth:lock:{code}                    — OAuth 授权码交换幂等锁 (TTL: 30s)
snowflake:worker:{id}               — worker-id 租约 (TTL: 30s, 心跳续约)
snowflake:available:ids             — 可用 worker-id 池 (SET, 无 TTL)
```

Pub/sub 通道:
```
cache:invalidate:permissions        — 权限变更通知 (message-based)
```
分布式锁:
```
partition:maintenance:lock          — 分区维护锁 (TTL: 120s)
```

每个 key 必须设置 TTL，防止 Redis 内存无限增长。权限缓存使用"主动失效 + TTL 兜底"双重策略——正常角色/权限变更时主动 DEL，若失效消息丢失，TTL 确保过期数据不会永久保留。

### 6.13 分布式缓存失效

多实例部署时，单实例清除本地缓存无法通知其他实例。方案：
- Redis pub/sub 通道 `cache:invalidate:permissions`，权限变更时发布消息
- 各实例订阅该通道，收到消息后清除本地 `perm:effective:{userId}` 缓存
- Redis 中存储的缓存副本设置 600s TTL，作为发布/订阅失效的兜底

---

## 7. 实施阶段

### Phase 1: 基础设施
1. 更新 `pom.xml` 全部依赖
2. 创建 `application.yml` (datasource / Redis / Snowflake / JWT / mail / geetest / crypto)
3. 构建 common 层: `R<T>`, `ErrorCode`, 6 异常类, `GlobalExceptionHandler`, `PageQuery`
4. `MyBatisPlusConfig` — Snowflake IdGenerator, 分页拦截器, 软删除 SqlInjector
5. `SecurityConfig` — 初始 permit-all, BCryptPasswordEncoder(12)
6. `RedisConfig`, `AsyncConfig`
7. `GeeTestProperties` + `GeeTestConfig` — 极验配置绑定
8. 14 张表的实体类 (严格对应 init.sql 列定义，含 deleted_at 复合注解)
9. 全部 BaseMapper 接口 + XML 存根

### Phase 2: 核心认证
10. `GeeTestVerifier` — 极验 v4 服务端校验 (RestTemplate, HMAC-MD5 签名, Redis 缓存)
11. `JwtTokenProvider` (jose4j RS256) + `JwtAuthenticationFilter`
12. `AuthController.register()` → 校验验证码 → 事务写入 users(status=PENDING) + user_auths(is_verified=0) + user_profiles
13. `AuthController.login()` → GeeTest 校验 → identifier lookup → 状态检查(拒绝PENDING/LOCKED/DISABLED) → bcrypt → MFA → 签发 token → 记录 login_log
14. `AuthController.sendCode()` → GeeTest 校验 → 频率限制 → 生成 6 位验证码 → 发送邮件 → Redis (5min)
15. `TokenService` — access (15min) + refresh (7day), 轮换 + 盗用检测, Redis 黑名单
16. `AuthController.logout()` — blacklist access jti, invalidate refresh family
17. 密码修改/重置 + `password_changed_at` + 历史防重用

### Phase 3: 用户与身份管理
18. `UserController` CRUD — 分页 + 过滤, 软删除
19. 用户状态管理 — lock/unlock/disable + locked_until
20. 多身份绑定/解绑 (UserAuth), 约束: 不能删除最后一个身份
21. UserProfile, 用户自服务接口, 头像

### Phase 4: RBAC
22. `RoleController` + `RoleService` — CRUD, 层级树, 系统角色保护
23. `PermissionController` — 只读列表
24. 角色-权限分配 (批量 upsert)
25. 用户-角色分配 (含 scope_type/scope_value/expires_at)
26. `PermissionEvaluator` — 层级解析 + 作用域合并 + Redis 缓存 + 缓存失效
27. `@RequirePermission` 注解 + AOP

### Phase 5: MFA
28. TOTP setup (RFC 6238, otpauth:// URI, QR code, 10 backup codes)
29. TOTP verify → AES 加密 secret+backup_codes → 写入 user_mfa → is_enabled=1
30. 禁用 (需密码确认) + 备用码重新生成
31. MFA 登录集成

### Phase 6: OAuth2 Provider
32. OAuthClient CRUD + secret 轮换
33. /authorize (code 10min Redis + PKCE)
34. /token (code/client_credentials/refresh_token)
35. /revoke

### Phase 7: API Key
36. create (32 random bytes → Base64url → dsk- 前缀 → SHA-256 → 返回一次原始 key)
37. revoke + rotate
38. `ApiKeyAuthenticationFilter` 集成到 SecurityFilterChain

### Phase 8: 审计与日志
39. `@AuditLog` + AOP → AuditEvent → @Async listener → audit_logs
40. AuditLog/LoginLog 分页查询 + CSV 导出
41. `PartitionMaintenanceScheduler` @Scheduled 安全网

### Phase 9: 频率限制与安全加固
42. `RateLimiterService` (Redis Lua 滑动窗口)
43. `@RateLimit` 注解 + AOP: login(5/min), code/send(3/5min), register(3/hour/IP)
44. CORS + Security Headers

### Phase 10: 文档与测试
45. Knife4j `@Tag`/`@Operation`/`@Schema` → /doc.html
46. Testcontainers 集成测试 (MySQL + Redis)
47. 单元测试: PermissionEvaluator, TokenService, AuthService, MfaService
48. Actuator health check

---

## 8. 验证清单

1. `./mvnw spring-boot:run` → context loads, MySQL + Redis connected
2. `http://localhost:8080/doc.html` → 全部接口可通过 Swagger 调试
3. 注册 → 收到邮箱验证码 → 创建账户 → 登录 → 获取 JWT
4. 权限校验: 无权限访问受保护接口 → 403
5. TOTP MFA: 设置 → 登录需 MFA code → 验证通过
6. OAuth2: 创建客户端 → /authorize → /token → 获得 access token
7. API Key: 创建 → X-API-Key header → 访问接口
8. 审计: 管理操作 → /api/v1/audit-logs → 有记录
9. 速率限制: 频繁请求 → 429
10. 锁定: 5 次错误登录 → 账户锁定 → 下次登录返回 failed_locked

---

## 9. 关键设计细节

### 9.1 密码复杂度策略

- 最小长度 8 字符，最大 128 字符
- 必须包含以下 4 类中的至少 3 类：大写字母、小写字母、数字、特殊字符（`@$!%*#?&`）
- 禁止包含用户名或邮箱的连续 3 个以上字符
- 密码历史保留最近 10 次，禁止重复使用（已在 `user_credentials_history` 中实现）
- 校验层：`RegisterRequest` / `ChangePasswordRequest` 使用 `@PasswordComplexity` 自定义 Jakarta Validator

### 9.2 事务边界

多表写入操作必须使用 `@Transactional`：

| 操作 | 涉及表 | 隔离级别 |
|------|--------|----------|
| 注册 | users + user_auths + user_profiles + user_roles | READ_COMMITTED |
| 密码修改 | users(UPDATE password_changed_at) + user_credentials_history(INSERT) | READ_COMMITTED |
| 角色分配 | user_roles + Redis 权限缓存失效 | READ_COMMITTED |
| 身份绑定 | user_auths(INSERT, 含"最后身份"检查) | READ_COMMITTED |
| MFA 启用 | user_mfa(INSERT/UPDATE) | READ_COMMITTED |
| API Key 创建/轮换 | api_keys(INSERT/UPDATE old+new) | READ_COMMITTED |
| OAuth 客户端注册 | oauth_clients(INSERT) | READ_COMMITTED |
| OAuth token 交换 | oauth_clients(读) + Redis code PKCE(读) + Redis refresh family(写) | READ_COMMITTED |

Redis 缓存失效在生产代码中通过 `@TransactionalEventListener(phase=AFTER_COMMIT)` 延迟到事务成功后执行，避免事务回滚时缓存已清除。该方案存在一个已知失败模式：事务提交成功但 Redis 不可达时，缓存失效静默丢失，用户可能保有已撤销的权限最多 TTL 时长（600s）。高安全场景可将 `perm:effective` TTL 降至 60-120s 以缩小影响窗口。

性能注意：`SecurityConfig` 中 `BCryptPasswordEncoder(12)` 为全局 Bean，OAuth client_secret 验证同样使用 BCrypt(12)（约 250ms/次）。高并发 OAuth token 交换场景下，可通过 `OAuthClientService` 使用独立的 `BCryptPasswordEncoder(10)` 实例（约 62ms/次）降低开销，安全度仍可接受。

### 9.3 错误响应格式

`R<T>` 统一响应，`code` 字段全部使用 ErrorCode 枚举值，HTTP 状态码由 Response 行单独承载：

```json
// 成功 — HTTP 200, code=0
{ "code": 0, "message": "ok", "data": {...}, "timestamp": 1716912000000 }

// 业务异常 (BusinessException) — HTTP 400, code 取自 ErrorCode 枚举
{ "code": 10001, "message": "用户名已存在", "data": null, "timestamp": 1716912000000 }

// 参数校验失败 (MethodArgumentNotValidException) — HTTP 422, code=11001
{ "code": 11001, "message": "参数校验失败", "data": [{"field": "password", "message": "密码长度不能少于8位"}], "timestamp": ... }

// 系统异常 (兜底) — HTTP 500, code=50001
{ "code": 50001, "message": "服务器内部错误", "data": null, "timestamp": ... }
```

ErrorCode 枚举按模块分段：`0` 成功，`1xxxx` 认证/授权错误，`4xxxx` 业务错误，`5xxxx` 系统错误。`data` 字段类型由具体错误类型决定（null / 字段错误数组 / 扩展信息对象），客户端通过 `code` 判断如何解析。

### 9.4 password_changed_at=NULL 的 JWT 校验

新注册用户 `password_changed_at` 为 NULL。JWT 校验逻辑：
- `password_changed_at == NULL` → 视为"未改过密码"，不拒绝任何 token
- `password_changed_at != NULL && jwt.iat < password_changed_at_epoch` → 拒绝 (token 在改密码前签发)
- 比较时：`LocalDateTime passwordChangedAt` → `.atZone(ZoneId.of("Asia/Shanghai")).toEpochSecond()` 转为 epoch 秒，与 `jwt.iat`(long) 比较
- 用户首次修改密码后设置 `password_changed_at = NOW()`，立即失效所有旧 token

### 9.5 locked_until 自动解锁

登录时检查锁定状态：
1. `status = LOCKED(3)` 且 `locked_until` 不为 NULL：
   - 若 `now > locked_until` → 自动恢复 `status = NORMAL(0)`, `failed_attempts = 0`, `locked_until = NULL`，继续正常登录流程
   - 若 `now <= locked_until` → 返回 `failed_locked`
2. `status = LOCKED` 且 `locked_until = NULL` → 永久锁定，需管理员手动解封

### 9.6 OAuth2 PKCE + state — 强制 S256 + 防 CSRF

- 仅接受 `code_challenge_method=S256`，拒绝 `plain`
- 验证逻辑：`BASE64URL-ENCODE(SHA-256(code_verifier)) == code_challenge`
- code_challenge 存储在 Redis `oauth:code:{code}:pkce`，与授权码同时过期 (600s)
- `state` 参数：`/authorize` 时前端生成 `crypto.randomUUID()` → 后端存入 `oauth:state:{state}` (TTL 600s) → 回调时校验是否存在并立即删除，防重放

### 9.7 MFA 密钥版本化

`user_mfa` 表预留 `key_version TINYINT NOT NULL DEFAULT 1` 字段。AES 密钥涉及轮换时：
1. 配置支持多版本密钥：
   ```yaml
   crypto:
     aes-keys:
       1: ${AES_KEY_V1}    # 当前加密密钥 (Base64 编码的 32 字节)
       0: ${AES_KEY_V0}    # 仅用于解密旧版本数据
   ```
2. 新数据使用最新版本加密，`key_version` 记录对应版本号
3. 解密时根据 `key_version` 选择对应密钥，若找不到对应版本则解密失败
4. 后台任务定期用新密钥重新加密旧版本数据，逐步淘汰旧密钥

### 9.8 并发登录策略

初始版本：允许同一用户多设备同时登录，每个登录创建独立的 session/refresh-token-family。
如需强制单设备登录：
- `session:{userId}:*` 前缀扫描现有会话并全部吊销
- 或维护一个 `active_session:{userId}` 计数器，达到上限时拒绝新登录

### 9.9 CORS 配置

```yaml
# 开发环境
app.cors.allowed-origins: http://localhost:3000,http://localhost:5173
# 生产环境通过环境变量注入
app.cors.allowed-origins: ${CORS_ALLOWED_ORIGINS}
```

`WebMvcConfig` 仅对 `/api/**` 路径启用 CORS，`allowCredentials=true`，allowed methods: GET/POST/PUT/PATCH/DELETE，allowed headers: Authorization, Content-Type, X-API-Key, X-Requested-With。

### 9.10 速率限制标识符

| 端点 | 限制维度 | 阈值 |
|------|----------|------|
| POST /login | IP | 5/min |
| POST /login | identifier (用户名/邮箱) | 5/min |
| POST /code/send | IP | 10/hour |
| POST /code/send | identifier (邮箱/手机号) | 1/5min |
| POST /register | IP | 3/hour |
| POST /login (MFA code) | identifier | 5/min |
| GET /audit-logs, /login-logs | user | 10/min |

双重维度：IP 维度防分布式攻击，identifier 维度防定向攻击。MFA code 校验阶段单独限流，防止 TOTP 6 位数字的暴力尝试（30s 窗口内约 1M 组合，限流将有效尝试次数降到可忽略级别）。审计日志查询增加 user 维度限流，防止大数据量分页拖垮 DB 连接池。

### 9.11 数据保留与物理清理

- `login_logs` / `audit_logs`：保留 12 个月。`PartitionMaintenanceScheduler` 每月：
  1. 追加下一个月的分区 (通过存储过程 `add_next_month_partitions`)
  2. 删除 13 个月前分区：`ALTER TABLE ... DROP PARTITION p<YYYYMM>`（Java 侧动态构造 SQL）
- 软删除记录（users/roles/user_auths/oauth_clients/api_keys）：保留 90 天后物理删除。通过 `@Scheduled` 任务清理 `deleted_at > 0 AND deleted_at < NOW_MILLIS - 90*24*3600*1000` 的记录及其级联关联表数据。

### 9.12 SMS 验证码通道

当前实现仅覆盖邮箱验证码（`spring-boot-starter-mail` + SMTP）。`MfaType.sms` 和 `user_mfa.mfa_type` ENUM 中的 `sms` 为预留扩展点。接入 SMS 需要：
1. 添加阿里云短信 / Twilio 等 SDK 依赖
2. 实现 `SmsVerificationCodeSender` 实现 `VerificationCodeSender` 接口
3. `VerificationCodeService` 根据 `identity_type` 分发到不同发送器

### 9.13 健康检查

Phase 1 即配置 Actuator + Prometheus：
```yaml
management:
  endpoints.web.exposure.include: health,info,prometheus
  endpoint.health:
    probes.enabled: true
    show-details: when-authorized
  metrics.tags.application: ${spring.application.name}
```

- `/actuator/health/liveness` — 容器存活探针
- `/actuator/health/readiness` — 就绪探针 (MySQL + Redis)
- `/actuator/prometheus` — Prometheus 抓取端点，指标定义见 [9.39](#939-prometheus-指标暴露)

### 9.14 数据库迁移

使用 Flyway 管理 DDL 版本：
```
src/main/resources/db/migration/
├── V1__init_schema.sql      (DDL — 14 张 CREATE TABLE)
├── V2__seed_data.sql         (种子数据，INSERT IGNORE)
└── V3__procedures.sql        (存储过程 + Event)
```
`application.yml` 中配置 `spring.flyway.enabled=true`。Flyway 替代手动执行 init.sql。

### 9.15 优雅下线

```yaml
server.shutdown: graceful
spring.lifecycle.timeout-per-shutdown-phase: 30s
```

确保下线时：正在处理的 HTTP 请求完成、`@Async` 审计日志写入完成、数据库连接池正常归还。

### 9.16 Snowflake 多实例协调

多实例部署时，每个实例必须拥有唯一的 `(datacenter-id, worker-id)` 组合：
- 方案 A（推荐）：K8s StatefulSet 副本序号作为 worker-id，通过 `HOSTNAME` 环境变量注入
- 方案 B：Redis 原子自增 `snowflake:worker:counter` 分配 worker-id
- `application.yml` 中 `snowflake.datacenter-id: ${SNOWFLAKE_DC_ID:1}`, `snowflake.worker-id: ${SNOWFLAKE_WORKER_ID:1}`

### 9.17 API Key 验证缓存

`ApiKeyAuthenticationFilter` 每次请求计算 SHA-256 + DB 查询开销较大。增加 Redis 缓存层：
- 缓存 key 为 `apikey:{key_hash}`（见 [6.12 Redis 键空间](#612-redis-键空间)），缓存 `{user_id, permissions, allowed_scopes, status, expires_at}` (TTL: 120s)
- API Key 吊销/轮换/删除时主动 DEL
- 缓存未命中时回退到 MySQL 查询

### 9.18 RBAC 层级解析 — 递归 CTE

角色层级树解析使用 MySQL 8.0 递归 CTE 单次查询，避免 N+1 问题：

```sql
WITH RECURSIVE role_chain AS (
    SELECT id, parent_role_id, 1 AS is_direct FROM roles
    WHERE id IN (<user's direct role IDs from user_roles>)
    UNION ALL
    SELECT r.id, r.parent_role_id, 0 FROM roles r
    JOIN role_chain rc ON r.id = rc.parent_role_id
)
SELECT rp.permission_id,
       COALESCE(
           CASE ur.scope_type WHEN 'self' THEN 1 WHEN 'dept' THEN 2
                              WHEN 'org' THEN 3 WHEN 'all' THEN 4 END,
           4
       ) AS ur_rank,
       CASE COALESCE(s.scope_key, 'all')
           WHEN 'self' THEN 1 WHEN 'dept' THEN 2
           WHEN 'org' THEN 3 WHEN 'all' THEN 4
       END AS rp_rank,
       ur.scope_value                    -- 直联角色限定具体部门/组织 ID
FROM role_chain rc
JOIN role_permissions rp ON rc.id = rp.role_id
LEFT JOIN scopes s ON rp.scope_id = s.id
LEFT JOIN user_roles ur ON rc.id = ur.role_id AND ur.user_id = <userId> AND ur.deleted_at = 0
WHERE rc.is_direct = 0 OR ur.deleted_at = 0;
-- effective_scope_rank = MAX(rp_rank, ur_rank) → 取较严格的那个
-- 最终合并: 对每个 permission_id 取 MIN(effective_scope_rank) → 跨所有角色取最宽松的
-- 下游数据访问层: WHERE department_id IN (resolved scope_values) 对 dept/org 作用域实例化过滤
```
作用域合并分两步，第三步为数据过滤：
1. **角色内收窄**：每个角色分配的作用域 = `MOST_RESTRICTIVE(role_permissions_scope, user_roles_scope)`
2. **角色间合并**：对同一 `permission_id`，取 `MOST_PERMISSIVE(effective_scope_across_all_roles)`
3. **数据层过滤**：下游 Service/Mapper 根据 `scope_value` 对 `dept`/`org` 作用域进行实例化过滤（`WHERE department_id IN (scope_values)`）。`scope_value=''`（默认值）表示该作用域类型内无实例限制（如 `scope_type='all'` 时 `scope_value` 恒为空）。祖先角色 `scope_value` 为 NULL，视为无过滤。`LEFT JOIN` 确保祖先角色的权限不会被丢弃。
作用域宽松度排名：`self(1) < dept(2) < org(3) < all(4)`，通过 CASE 表达式显式映射（不可依赖 `LEAST()` 的 ASCII 字典序）。例如：角色权限为 `user:read@all`，但用户角色分配为 `scope_type='dept'`，则有效权限为 `user:read@dept`。

### 9.19 分区维护安全

- MySQL Event 在每月 1 号 02:00 执行
- Java `PartitionMaintenanceScheduler` 在每月 1 号 03:00 (时区 Asia/Shanghai) 作为安全网
- 两者都使用 Redisson 分布式锁 `partition:maintenance:lock` (TTL 120s) 防止并发执行
- 分区维护失败时记录 ERROR 日志 + 健康检查标记为 DEGRADED

### 9.20 结构化日志

- 日志格式：JSON (生产) / 控制台可读 (开发)，通过 `logback-spring.xml` 切换
- MDC 请求追踪：`JwtAuthenticationFilter` 注入 `requestId` (UUID) + `userId` 到 MDC
- 敏感数据脱敏：密码/MFA secret/token/jti 字段在日志中替换为 `***`

### 9.21 安全响应头

`SecurityConfig` 或 `WebMvcConfig` 中配置以下响应头：

| Header | 值 | 说明 |
|--------|-----|------|
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | HSTS，强制 HTTPS |
| `X-Content-Type-Options` | `nosniff` | 禁止 MIME 嗅探 |
| `X-Frame-Options` | `DENY` | 禁止 iframe 嵌套 |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | 跨域时不泄露完整 URL |
| `X-XSS-Protection` | `0` | 禁用已废弃的浏览器 XSS 过滤 |
| `Content-Security-Policy` | `default-src 'self'; frame-ancestors 'none'` | CSP 基线，前后端分离时前端自行扩展 |

### 9.22 请求体大小限制

```yaml
spring.servlet.multipart.max-file-size: 5MB    # 头像上传
spring.servlet.multipart.max-request-size: 5MB
server.tomcat.max-http-form-post-size: 2MB     # 普通 POST 表单
```

### 9.23 AES 密钥格式

AES-256-GCM 要求 32 字节密钥。环境变量 `${AES_KEY_V1}` 等应提供 **Base64 编码的 32 字节随机密钥**。`CryptoUtil` 在初始化时解码：`Base64.getDecoder().decode(keyString)` 得到 32 字节密钥。生成方式：

```bash
openssl rand -base64 32
```

### 9.24 密码复杂度校验器作用域

"禁止包含用户名或邮箱的连续 3 个以上字符" 规则需要访问 DTO 中的 `username`/`email` 字段，因此 `@PasswordComplexity` 必须是**类级别**的 Jakarta Validator（`@Target(TYPE)`），而非字段级别。它接收整个 `RegisterRequest` 或 `ChangePasswordRequest`，同时校验 `password` 与其他字段。

### 9.25 密码历史裁剪策略

`user_credentials_history` 保留最近 10 次。每次 INSERT 新记录后立即执行裁剪：

```sql
DELETE FROM user_credentials_history
WHERE user_id = ? AND id NOT IN (
    SELECT id FROM (
        SELECT id FROM user_credentials_history
        WHERE user_id = ?
        ORDER BY created_at DESC LIMIT 10
    ) AS t
);
```

该操作与密码修改在同一事务内执行，避免并发窗口。

### 9.26 Flyway 与 Docker init.sql 拆分

Docker 部署使用的 `deploy/docker/mysql/init.sql` 包含完整 DDL + 种子数据 + 存储过程。使用 Flyway 时需拆分为：
- `V1__init_schema.sql` — 仅 DDL（14 张 CREATE TABLE）
- `V2__seed_data.sql` — 仅 `INSERT IGNORE` 种子数据
- `V3__procedures.sql` — 存储过程 + Event

Docker 场景可通过挂载 `.sql` 文件到 `/docker-entrypoint-initdb.d/` 保持兼容，或以 `V1.sql` + `V2.sql` + `V3.sql` 串联方式初始化。

### 9.27 API Key rotate 权限

`PATCH /api-keys/{keyId}/rotate` 同时涉及吊销旧 key 和创建新 key，已分配专用权限 `apikey:rotate`（替代此前同时依赖 `apikey:create` + `apikey:revoke` 的方案）。该权限已加入种子数据，super_admin 和 admin 自动获得。

### 9.28 MyBatis-Plus 自定义软删除

`logic-delete-value: "NOW_MILLIS"` 非 MyBatis-Plus 标准支持的值。需要自定义 `SqlInjector` 重写 `SqlMethod.LOGIC_DELETE`，将 `deleted_at = <static_value>` 替换为 `deleted_at = UNIX_TIMESTAMP(NOW(3)) * 1000`。`Phase 1 Step 4` 已提及自定义 SqlInjector。注意 `logic-not-delete-value: "0"` 查询条件需生成为 `WHERE deleted_at = 0`。

### 9.29 JWT 黑名单 TTL 缓冲

黑名单 TTL 配置为 **960s**（access-token-ttl 900s + 60s 时钟偏差缓冲），详见 [6.12 Redis 键空间](#612-redis-键空间)。多出 60s 确保即使在轻微时钟偏差下，已过期 token 不会在黑名单中提前消失。

### 9.30 硬删除策略说明

以下关联表使用**硬删除**（无 `deleted_at` 列），因为它们是用户/角色的级联依赖记录：
- `user_mfa` — MFA 禁用时物理删除行，操作通过 `audit_logs` 记录审计
- `user_credentials_history` — 随用户删除 CASCADE，或按 9.25 策略裁剪
- `user_roles` — 已有 `deleted_at` 支持软删除（本次修改已补充）

### 9.31 初始分区边界说明

`init.sql` 初始分区 `p202604`/`p202605`/`p202606` 基于 2026 年 5 月部署。若在之后部署，`p_future` 分区会承载 2026 年 7 月后的所有数据。首次运行 `PartitionMaintenanceScheduler` 时会自动拆分 `p_future` 为正确的月份分区。部署时需验证维护任务成功执行。

### 9.32 `login_logs` 匿名用户查询

`login_logs.user_id` 可为 NULL（未注册用户的登录尝试）。查询匿名登录记录时需使用 `WHERE user_id IS NULL`，不可使用 `WHERE user_id = NULL`。

### 9.33 `login_logs.identity_type` 宽松型设计

`login_logs.identity_type` 使用 `VARCHAR(20)`（无 ENUM 约束），而 `user_auths.identity_type` 使用严格 `ENUM`。这是有意为之：日志表应能记录超出当前系统已知类型的身份标识（例如将来新增的 OAuth provider），避免因严格 ENUM 导致日志写入失败。

### 9.34 Refresh Token — HttpOnly Cookie 模式

浏览器端 refresh token 通过 **HttpOnly + Secure + SameSite=Strict** Cookie 下发，前端 JavaScript 完全不可访问，消除 XSS 窃取攻击面：

```
Set-Cookie: refresh_token=<token>; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth; Max-Age=604800
```

`/refresh` 端点优先从 Cookie 读取 refresh token（`@CookieValue`），若无则从请求体读取（兼容移动端 / 非浏览器客户端）。`/logout` 时清除该 Cookie（`Max-Age=0`）。

### 9.35 设备指纹绑定（可选安全增强，AAL3 合规）

NIST 800-63B AAL3 要求设备绑定。启用后在 JWT 中增加 `fph` (fingerprint hash) claim：

```java
String ipPrefix = clientIp.substring(0, clientIp.lastIndexOf('.') + 1); // /24 subnet
String fingerprint = DigestUtils.sha256Hex(userAgent + ipPrefix);
// 存入 JWT claim: { "fph": "a1b2c3..." }
```

`JwtAuthenticationFilter` 校验时比对请求指纹与 token 中 `fph`，不匹配 → 拒绝。最多增加 ~50μs 延迟，无额外 Redis/DB 调用。`/refresh` 时更新 `fph`（允许 IP/UA 渐变）。

### 9.36 HIBP 密码泄露检测

注册/改密时调用 Have I Been Pwned k-anonymity API 检查密码是否已知泄露：

```
SHA-1(password) → "A1B2C3D4E5F6G7H8I9J0..." 40 hex
  → 前 5 位 "A1B2C" → GET https://api.pwnedpasswords.com/range/A1B2C
  → 返回含 D4E5F6G7... 后半部分的哈希后缀列表
  → 本地比对: 命中次数 ≥ 100 → 拒绝 (密码已公开泄露)
```

特点：k-anonymity 模型不泄露完整密码哈希，~300ms 延迟可通过 `@Async` 执行，非阻塞注册流程。仅在密码复杂度校验通过后才调用。

### 9.37 Caffeine 本地权限缓存 — L1 层

RBAC 权限解析增加两级缓存：Caffeine (进程内 L1) + Redis (分布式 L2)：

```
请求 → Caffeine L1 (100ns, TTL 60s, max 10000)
   → Miss → Redis L2 (1ms, TTL 600s) → Miss → MySQL CTE (5ms)

cache:invalidate:permissions pub/sub → 多实例广播 → L1 + L2 双向失效
```

依赖：`com.github.ben-manes.caffeine:caffeine`（Spring Cache 原生支持）。预期将 Redis 权限查询流量降低 80-90%。

### 9.38 HikariCP 连接池调优

```yaml
spring.datasource.hikari:
  maximum-pool-size: ${DB_POOL_SIZE:50}  # 生产默认 50，与 9.42 容量规划对齐；开发可覆盖为 20
  connection-timeout: 3000       # 3s，快速失败（默认 30s）
  idle-timeout: 600000           # 10min 闲置回收
  max-lifetime: 1800000          # 30min，低于 MySQL wait_timeout (8h)
  leak-detection-threshold: 10000  # 10s 泄露检测，仅开发/测试开启
```

### 9.39 Prometheus 指标暴露

Micrometer + Prometheus 提供全栈可观测性：

```yaml
management:
  endpoints.web.exposure.include: health,info,prometheus
  metrics.tags.application: ${spring.application.name}
```

```xml
<!-- 依赖 -->
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**业务指标清单**：

| 指标名称 | 类型 | 维度标签 |
|----------|------|----------|
| `auth_login_attempts_total` | Counter | result, identity_type |
| `auth_token_verification_seconds` | Timer | source (jwt/apikey) |
| `auth_rbac_resolution_seconds` | Timer | cache_hit (true/false) |
| `auth_ratelimit_rejected_total` | Counter | endpoint, dimension (ip/id) |
| `auth_mfa_verification_total` | Counter | mfa_type, result |
| `auth_registration_total` | Counter | identity_type |
| `partition_maintenance_total` | Counter | table_name, result |
| `redis_cache_hit_ratio` | Gauge | cache_name |

指标暴露在 `/actuator/prometheus`，Prometheus 定期拉取，配合 Grafana 告警。

### 9.40 性能基线

| 操作 | 目标延迟 | 关键路径 |
|------|----------|----------|
| JWT 验证 (RS256) | < 1ms | OncePerRequestFilter |
| 权限解析 (L1 命中) | < 1μs | Caffeine 本地 |
| 权限解析 (L2 命中) | < 2ms | Redis 往返 |
| 登录 (bcrypt) | < 500ms | BCrypt(12) |
| 注册 (含邮件) | < 1000ms | 邮件发送异步 |
| API Key 验证 (缓存命中) | < 1ms | Redis `apikey:{hash}` |
| OAuth token 交换 | < 500ms | bcrypt(10) + Redis |

### 9.41 登录时序侧信道防护

登录流程中若 `user_auths` 查找不到用户即返回，攻击者可通过响应延迟区分"用户不存在"(~10ms) 与"密码错误"(~260ms)。ASVS V2.1.9 要求认证失败响应不可区分：

```java
// AuthServiceImpl.login()
UserAuth auth = userAuthMapper.selectByIdentifier(identifier);
if (auth == null) {
    // 对抗时序侧信道：对不存在用户也执行 bcrypt(12) 验算
    BCrypt.checkpw(credential, DUMMY_HASH);  // DUMMY_HASH = "$2a$12$..." 预计算
    throw new AuthenticationException(ErrorCode.INVALID_CREDENTIALS);
}
if (!BCrypt.checkpw(credential, auth.getCredential())) {
    throw new AuthenticationException(ErrorCode.INVALID_CREDENTIALS);
}
```

返回统一的 `INVALID_CREDENTIALS` 错误，不区分"用户不存在"与"密码错误"。

### 9.42 HikariCP 扩容与容量规划

9.38 的 `maximum-pool-size: 20` 仅适用于 ~3,000 req/s 以下场景。生产建议按实例规格调整：

```yaml
spring.datasource.hikari:
  maximum-pool-size: ${DB_POOL_SIZE:50}     # 按 CPU 核算: max = (CPU * 2) + 1，上限 100
  minimum-idle: ${DB_POOL_MIN:10}           # 预热减少冷启动
  connection-timeout: 3000
  max-lifetime: 1800000
  leak-detection-threshold: 10000
```

| 并发用户 | 推荐 pool-size | 实例数 | 估算 |
|----------|---------------|--------|------|
| 1K | 20 | 2 | 开发/测试 |
| 10K | 50 | 3-4 | 中等规模 |
| 100K | 80 | 8-12 | 大规模 |

每个实例的 bcrypt(12) 登录容量约 100 次/秒（~25 核），需要按实例数水平扩展。

### 9.43 Refresh Token 原子旋转 — Lua 脚本

防止并发 /refresh 误触发家族撤销，旋转操作使用 Redis Lua 脚本保证原子性：

```lua
-- KEYS[1] = refresh:family:{familyId}
-- ARGV[1] = old_token_hash, ARGV[2] = new_token_hash
local current = redis.call('HGET', KEYS[1], 'current')
if current == false then
    -- 家族已撤销，拒绝
    return {0, 'revoked'}
end
if current == ARGV[1] then
    -- 正常旋转: 旧→新
    redis.call('HSET', KEYS[1], 'current', ARGV[2], 'previous', ARGV[1])
    return {1, 'ok'}
end
if current ~= ARGV[1] and redis.call('HEXISTS', KEYS[1], 'previous') == 1
   and redis.call('HGET', KEYS[1], 'previous') == ARGV[1] then
    -- 重放检测: ARGV[1] 是已被替换的旧值 → 整族撤销
    redis.call('DEL', KEYS[1])
    redis.call('SET', KEYS[1] .. ':revoked', '1', 'EX', 604800)
    return {2, 'replay_detected'}
end
return {3, 'unknown'}
```

Java 侧调用 `RedissonClient.getScript().evalSha(...)` 执行，返回码 `2` 时清除客户端所有 token 并强制重登录。

### 9.44 GeeTest 断路器与降级

GeeTest 不可用时不应阻塞全部登录。使用 Resilience4j 断路器：

```java
@CircuitBreaker(name = "geetest", fallbackMethod = "geetestFallback")
public GeeTestResult verify(String captchaOutput, String lotNumber, ...) { ... }

public GeeTestResult geetestFallback(..., Exception e) {
    log.error("GeeTest unavailable, circuit open", e);
    // 降级策略: fail-open + 最严格限流兜底
    // 仅允许 login: 1/min/IP + code/send: 1/10min/IP
    return GeeTestResult.BYPASS_WITH_STRICT_RATE_LIMIT;
}
```

```yaml
resilience4j.circuitbreaker:
  instances:
    geetest:
      failure-rate-threshold: 50
      wait-duration-in-open-state: 30s
      sliding-window-size: 10
```

断路器开启时触发 CRITICAL 告警，前端提示"人机验证服务暂时不可用，已启用严格限流保护"。

### 9.45 Access Token 客户端安全存储

浏览器端 access token 不使用 localStorage（易受 XSS 窃取）。推荐方案：

| 方案 | XSS 免疫 | 实施难度 | 适用场景 |
|------|----------|----------|----------|
| BFF (Backend-For-Frontend) | ✅ token 存服务端 session，前端仅持 session cookie | 中 | SPA + 同源/同站 API |
| 双 HttpOnly Cookie | ✅ access + refresh 均为 HttpOnly | 中 | 需要 CSRF token 配合 |
| Service Worker + Token 代理 | ✅ SW 拦截请求注入 token | 高 | 复杂 SPA |
| localStorage + strict CSP | ⚠️ CSP `script-src 'self'` 降风险 | 低 | 快速启动（非推荐） |

推荐 BFF 模式：`POST /login` 返回 `Set-Cookie: session_id=<uuid>; HttpOnly; Secure; SameSite=Strict`，access token 存 Redis `session:{sessionId}`。前端所有 `/api/` 请求携带 Cookie，后端 `JwtAuthenticationFilter` 从 Cookie 取 session_id → Redis 取 access token → 注入 SecurityContext。

### 9.46 GDPR 合规

**数据导出** (`GET /api/v1/me/export`，Permission: 无需认证，限流 1/day/user):

```json
{ "user": {...}, "profiles": {...}, "auths": [...], "roles": [...],
  "login_logs": [...], "audit_logs": [...], "mfa": {"totp_enabled": true}, "exported_at": "..." }
```

**删除权** (Art. 17): 用户请求删除时：
1. 立即设置 `users.status=DISABLED`，拒绝登录
2. 审计日志中 `user_id` 和 `identifier_hash` 设为 NULL（保留操作记录结构）
3. `login_logs` 中 `identifier_hash` 和 `ip_address` 设为 NULL
4. 软删除 30 天内完成物理删除（从 90 天缩短）
5. `GET /api/v1/users/{id}/forget-me` 端点即时触发上述流程

**IP/UA 伪匿名化**: `login_logs.ip_address` 仅存储 `/24` 前缀 (如 `192.168.1.0`)，`user_agent` 仅保留主版本号 (如 `Chrome/126`)。

**SOC2 访问审查**: `user_roles` 增加 `last_reviewed_at TIMESTAMP NULL` + `last_reviewed_by BIGINT NULL`。`@Scheduled` 任务标记 90 天未审查的分配为待审查，`GET /api/v1/admin/access-review` 生成审查矩阵。

### 9.47 冷启动缓存预热

新部署/重启时避免所有请求穿透到 CTE 查询：

```java
@Component
public class CacheWarmer implements ApplicationListener<ApplicationReadyEvent> {
    // 启动时异步预热热点用户权限（如最近 100 个活跃用户的 perm:effective）
    // 通过 consumer group 从访问日志中分析热点用户
    // 启动后 30s 内完成预热，覆盖 90% 的登录请求
}
```

```yaml
# 配置项
app.cache.warmup:
  enabled: true
  recent-user-count: 100          # 预热最近活跃用户数
  preload-super-admin: true       # 预热超管
```

### 9.48 @Async 队列约束

```yaml
spring.task.execution:
  pool.core-size: 4
  pool.max-size: 8
  pool.queue-capacity: 1000       # 有界队列
  pool.keep-alive: 60s
  rejection-policy: CALLER_RUNS   # 队列满时降级为同步执行
```

### 9.49 Redis 降级策略

| 组件 | Redis 不可达行为 | 理由 |
|------|-----------------|------|
| JWT 黑名单 (`blacklist:jti`) | **fail-open** — 允许 token 通过 | 15min TTL 限制窗口，不可阻塞合法请求 |
| 速率限制 (`ratelimit:`) | **fail-open** — 不限流 | 不可因限流故障导致 500 |
| 权限缓存 (`perm:`) | **fail-open** — 回退到 MySQL CTE | 性能降级但功能正常 |
| Refresh token family | **fail-closed** — 拒绝 /refresh | 安全优先，不可在无 Redis 时暴露 token |
| GeeTest 结果缓存 | **fail-open** — 重新调 GeeTest | 已有断路器保护 |
| 验证码 (`vc:`) | **fail-closed** — 拒绝 sendCode | 防滥用 |

### 9.50 OAuth Token 交换幂等性

授权码消费后不立即删除，标记为 `exchanged` 并保留 60s 宽限期。客户端重试相同时 code 时返回相同 token：

```
oauth:code:{code} → { status: "exchanged", access_token: "xxx", refresh_token: "xxx", exchanged_at: ts }
```

Redisson 分布式锁 `oauth:lock:{code}` 序列化并发交换请求执行。

### 9.51 Snowflake Worker-ID 租约回收

Redis 分配方案增加租约机制：

```
snowflake:worker:{id} → { instance: "pod-3", lease_until: 1716915000 }
snowflake:worker:lease:pod-3 → {id}  # 反向索引，启动时自动续约
```

`@Scheduled(fixedRate=10000)` 续约心跳。`PartitionMaintenanceScheduler` 同时清理过期租约（`lease_until < now` → DEL worker 键 → SET 回可用池）。worker-id 范围 0-31 用 `SET snowflake:available:ids {0,1,...,31}` 管理，分配时 `SPOP`，释放时 `SADD`。

### 9.52 Prometheus 告警规则

```yaml
groups:
- name: auth-system
  rules:
  - alert: LoginFailureSpike
    expr: rate(auth_login_attempts_total{result!="success"}[5m]) > 50
    for: 2m
    labels: { severity: warning }
  - alert: RedisCacheHitDrop
    expr: redis_cache_hit_ratio < 0.5
    for: 5m
    labels: { severity: warning }
  - alert: DBConnectionPoolHigh
    expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
    for: 1m
    labels: { severity: critical }
  - alert: RedisMemoryHigh
    expr: redis_memory_used_bytes / redis_memory_max_bytes > 0.8
    for: 5m
    labels: { severity: critical }
  - alert: PartitionMaintenanceFailed
    expr: rate(partition_maintenance_total{result="failure"}[1h]) > 0
    labels: { severity: critical }
  - alert: GeeTestCircuitOpen
    expr: resilience4j_circuitbreaker_state{name="geetest",state="open"} == 1
    for: 10s
    labels: { severity: critical }
```

### 9.53 MySQL 高可用

生产使用托管数据库的 HA 方案（RDS Multi-AZ / PolarDB 集群版）。应用层配置读/写分离：

```yaml
spring.datasource:
  primary:
    url: jdbc:mysql://${DB_WRITE_HOST}:3306/auth_system?...  # 写库
  readonly:
    url: jdbc:mysql://${DB_READ_HOST}:3306/auth_system?...   # 只读副本
```

权限解析 CTE（只读）、审计日志查询路由到只读副本，降低写库负载。

### 9.54 JWT 密钥轮换 (JWKS)

支持多密钥共存避免轮换时全员踢下线：

```yaml
jwt:
  keys:
    - kid: key-2026-04           # 当前签名密钥
      private-key: ${JWT_PRIVATE_KEY_PATH}
      public-key: ${JWT_PUBLIC_KEY_PATH}
      active: true
    - kid: key-2026-01           # 旧密钥，仅用于验签
      public-key: ${JWT_PUBLIC_KEY_PREV_PATH}
      active: false
```

新 token 以 `kid: key-2026-04` 签名。验签时按 `kid` 选择公钥，旧 token 在 TTL 内仍有效。对称切换窗口为 2× access-token-ttl (30 min)。

### 9.55 备份与灾备

| 组件 | 备份方法 | RPO | RTO |
|------|----------|-----|-----|
| MySQL | 云快照每小时 + binlog 持续备份 | 1 小时 | 30 分钟 |
| Redis | RDB 快照每小时 + AOF `everysec` | 1 秒 | 5 分钟 |
| JWT 密钥 | 离线安全存储 + KMS HSM | 0 | 即日 |

灾备演练每季度执行一次，RTO 验证为 30 分钟内完整服务恢复。

### 9.56 jose4j 版本安全

```xml
<dependency>
    <groupId>org.bitbucket.b_c</groupId>
    <artifactId>jose4j</artifactId>
    <version>0.9.8</version>  <!-- 修复 CVE-2023-51701, CVE-2024-21635，最低 0.9.7+ -->
</dependency>
```

### 9.57 安全随机数规范

所有加密操作必须使用 `SecureRandom` 实例：

```java
// common/util/SecureRandomHolder.java
public final class SecureRandomHolder {
    public static final SecureRandom INSTANCE = new SecureRandom();
    // 禁止: java.util.Random, Math.random(), ThreadLocalRandom
}
// ArchUnit 规则: @ArchTest 禁止 java.util.Random 出现在 *.security.* / *.auth.* 包
```

适用于：API Key 生成 (32 bytes)、PKCE `code_verifier` (43-128 chars Base64url)、OAuth `state` (UUID)、JWT `jti` (UUID)、TOTP backup codes (10×8 digits)。

### 9.58 SOC2 访问审查表

`user_roles` 增加审查字段：

```sql
ALTER TABLE user_roles ADD COLUMN last_reviewed_at TIMESTAMP NULL;
ALTER TABLE user_roles ADD COLUMN last_reviewed_by BIGINT NULL;
```

`audit_logs` 增加 OAuth 资源所有者追踪：

```sql
ALTER TABLE audit_logs ADD COLUMN resource_owner_id BIGINT NULL COMMENT 'OAuth授权用户';
```

### 9.59 投产前检查清单

1. [ ] JWT 密钥从 classpath 迁移到外部挂载 (`/etc/secrets/`)
2. [ ] MySQL Event Scheduler 确认可用（云 DB 不可用时已配置 Java 调度器为主）
3. [ ] 初始分区按实际部署月份调整（`p_future` 首次运行前手动拆分）
4. [ ] Prometheus 告警规则接入 Alertmanager
5. [ ] HIBP API 超时设为 2s，配置 fail-open
6. [ ] `scope_value NOT NULL DEFAULT ''` 验证直接角色作用域过滤正常工作
7. [ ] bcrypt dummy hash 预计算值在各实例一致
8. [ ] Redis `protected-mode yes` + `requirepass` 确认
9. [ ] 蓝绿部署时 Snowflake worker-id 使用独立计数器 namespace
10. [ ] 云数据库部署: 从 Flyway 迁移中移除 `CREATE EVENT` 语句（`V3_procedures.sql` 仅保留存储过程）
11. [ ] AWS RDS / PolarDB 确认 MySQL Event Scheduler 可用性，若不可用仅依赖 Java 调度器

### 9.60 角色版本化缓存失效

大规模角色（如 100 万用户的 `user` 角色）权限变更时，避免扫描全量 `user_roles` + 逐条 Redis DEL：

```sql
-- roles 表增加版本字段
ALTER TABLE roles ADD COLUMN perm_version INT NOT NULL DEFAULT 1;

-- 角色权限变更时递增版本
UPDATE roles SET perm_version = perm_version + 1 WHERE id = ?;
```

缓存 key 改为版本化：`perm:effective:{userId}:v{role_version_hash}`。角色权限变更后旧版本 key 靠 TTL 自然过期（600s），不必主动逐条删除。`PermissionEvaluator` 计算时取用户所有角色的 `MAX(perm_version)` + 用户角色分配的最新 `updated_at` 拼成缓存 key。

### 9.61 OAuth Client 缓存

OAuth `/token` 端点每次查询 `oauth_clients` 表，增加缓存层：

```
oauth:client:{clientId}   → { client_id, client_secret_hash, grant_types, redirect_uris, allowed_scopes, is_trusted, status }
                             (TTL: 300s, client 更新/删除时主动 DEL)
```

命中时跳过 DB 查询，仅执行 bcrypt(10) 验密。配合 6.12 Redis 键空间。

### 9.62 Redis 容量估算

1M 用户规模下的 Redis 内存预算：

| Key 类型 | 单条估算 | 数量估算 | 小计 |
|----------|----------|----------|------|
| `perm:effective:{userId}` | ~500B | 活跃用户 100K | 50 MB |
| `session:{userId}:{sessionId}` | ~300B | 活跃会话 50K | 15 MB |
| `refresh:family:{familyId}` | ~200B | 活跃 family 50K | 10 MB |
| `ratelimit:*` sliding window | ~100B | 活跃窗口 20K | 2 MB |
| 其他 (vc:, lockout:, geetest:, apikey:, oauth:*) | ~200B avg | 10K | 2 MB |
| **总计** | | | **~80 MB** |

```yaml
# redis.conf / 云 Redis 参数
maxmemory: 256mb              # 留 3x 余量
maxmemory-policy: volatile-lru  # 优先淘汰有 TTL 的 key
```

### 9.63 BFF 模式与 Bearer Token 兼容

9.45 推荐的 BFF 模式不改变 JWT 签发逻辑。`JwtAuthenticationFilter` 支持双模解析：

```java
if (request.getHeader("Authorization") != null) {
    token = parseBearer(request);           // 原生 Bearer (移动端/API)
} else if (request.getCookies() != null) {
    String sessionId = getCookie("session_id");
    token = redisTemplate.opsForValue().get("session:" + sessionId);  // BFF 模式
}
```

移动端/API Key 客户端继续使用 `Authorization: Bearer` header，浏览器端通过 BFF Cookie 获取 session_id → Redis → JWT。两种模式共享同一 SecurityContext 注入路径。

### 9.64 JWKS 配置迁移

从 Section 3 的扁平 key 配置迁移到 9.54 的多密钥模式：

```yaml
# 单密钥 (当前 Section 3) → 多密钥 (9.54)
# 迁移: jwt.keys 列表为空时，JwtTokenProvider 回退读取 jwt.private-key / jwt.public-key
jwt:
  private-key: ${JWT_PRIVATE_KEY_PATH}      # 向后兼容，9.54 keys 列表启用后忽略
  public-key: ${JWT_PUBLIC_KEY_PATH}
  keys: []                                  # 初始为空，首次轮换时追加第一条记录
```

首次轮换时 `JwtTokenProvider` 自动从扁平字段读取当前密钥填充 `keys[0]`，之后的轮换仅在 `keys` 列表中追加。

### 9.65 GDPR 端点补充

5.1 认证 API 增加：

| POST | /me/export | Bearer token | GDPR Art.20 数据导出 (1/day/user 限流) |

5.3 用户管理 API 增加：

| POST | /{userId}/forget-me | user:delete (admin) 或 self | GDPR Art.17 即时擦除 |

### 9.66 补充配置项

Section 3 `application.yml` 增加以下配置块：

```yaml
# 缓存预热 (9.47)
app.cache.warmup:
  enabled: true
  recent-user-count: 100
  preload-super-admin: true

# 读写分离 (9.53) — 仅生产环境启用
spring.datasource.readonly:
  url: jdbc:mysql://${DB_READ_HOST:localhost}:3306/auth_system?useSSL=true&...
  username: ${DB_USERNAME:app_user}
  password: ${DB_PASSWORD}
```

### 9.67 Redis 键空间补充

6.12 增加以下 key：

```
oauth:client:{clientId}              — OAuth 客户端缓存 (TTL: 300s)
oauth:lock:{code}                    — OAuth 授权码交换幂等锁 (TTL: 30s)
snowflake:worker:{id}               — worker-id 租约 (TTL: 30s)
snowflake:available:ids             — 可用 worker-id 池 (SET, 无 TTL)
```

### 9.68 API 网关 JWKS 离线验证

暴露 `/.well-known/jwks.json` 端点供网关（Kong/APISIX/Envoy）离线验证 JWT：

```json
{ "keys": [{"kty":"RSA","kid":"key-2026-04","use":"sig","n":"...","e":"AQAB"}] }
```

网关配置 `jwt.verify_offline=true` 后不再每请求调用 `/token/validate`，将认证延迟从 ~1ms 降到 ~10μs。`/token/validate` 保留供轻量网关或需要实时黑名单检查的场景。

### 9.69 速率限制响应头

429 响应携带 IETF RateLimit 标准头：

```
Retry-After: 42
RateLimit-Limit: 5
RateLimit-Remaining: 0
RateLimit-Reset: 1716912060
```

`Retry-After` 秒数由 `RateLimiterService` 的滑动窗口剩余时间计算，客户端可据此精确退避。

### 9.70 分布式追踪 — X-Request-ID 传播

`JwtAuthenticationFilter` 入站读取 `X-Request-ID` header（无则生成 UUID）。MDC 注入 `requestId` + 出站 `RestTemplate` 自动透传 `X-Request-ID` header 到下游服务。日志中 `requestId` 关联全链路。

### 9.71 多租户隔离

当前设计为单租户模式。若需多租户：`users` 表增加 `tenant_id`，`user_roles.scope_value` 编码租户内部门。全表查询默认带 `WHERE tenant_id = ?` 过滤，租户间数据完全隔离。此为架构扩展点，不在 Phase 1-10 范围内。

### 9.72 WebAuthn/FIDO2 schema 预留

当前仅实现 TOTP。WebAuthn 需新增表：

```sql
CREATE TABLE user_webauthn (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    credential_id VARCHAR(512) NOT NULL,
    public_key TEXT NOT NULL,
    sign_count INT NOT NULL DEFAULT 0,
    transports JSON NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_credential (credential_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
```

### 9.73 身份绑定需二次认证

`POST /users/{id}/auths` 绑定新身份前，服务端校验当前密码或 MFA code（`BindAuthRequest` 增加 `current_credential` 字段）。防止会话劫持后绑定攻击者身份。

### 9.74 邮箱变更流程

变更主邮箱需两步验证：
1. `POST /me/email/change` { new_email, current_password } → 发验证码到新邮箱
2. `POST /me/email/verify` { new_email, code } → 事务更新 `users.email` + 插入/更新 `user_auths` email 身份

### 9.75 MFA 备用码追踪

`user_mfa.backup_codes` JSON 结构从 `["code1","code2",...]` 改为 `[{"code":"...","used":false},{"code":"...","used":true}]`。每次使用标记 `used=true`，同一事务更新 `updated_at`。

### 9.76 API 合约质量标准

**分页约束**: `PageQuery.maxSize=100`，超出截断；`sort` 字段白名单校验 (`@ValidSort(allowed={"createdAt","username","status"})`)。

**版本策略**: 首个破坏性变更引入 `/api/v2`，v1 保留 6 个月共存，`Deprecation: true` 和 `Sunset: <date>` header 通知客户端。

**幂等键**: `POST /register`、`POST /users/{id}/roles` 支持 `Idempotency-Key` header。服务端缓存响应 24h（Redis `idempotent:{key}`），相同 key 重试返回原响应。

**批量上限**: 单次 bulk upsert 最大 500 条，超出返回 422。

**i18n**: `Accept-Language` header 选择 `ErrorCode.message` 语言（zh-CN/en），缺省 zh-CN。响应中 `message` 字段为已解析文本而非 i18n key。

**OpenAPI 完整度**: 每个端点至少 1 个 `@ExampleObject` 示例请求/响应。Phase 10 集成 `springdoc-openapi` 自动生成并部署到内部 API 门户。

### 9.77 Flyway 占位符冲突

Spring 和 Flyway 均使用 `${...}` 占位符语法。配置中覆盖 Flyway 占位符前缀避免冲突：

```yaml
spring.flyway:
  placeholder-prefix: '#{'
  placeholder-suffix: '}'
```

SQL 迁移中使用 `#{key}` 引用 Flyway 占位符，`${key}` 留给 Spring 属性注入。

### 9.78 BCrypt 假哈希版本管理

9.41 预计算的 `DUMMY_HASH` 需与当前 `BCryptPasswordEncoder` 的 cost 保持同步。系统启动时自动验证：

```java
@PostConstruct
void validateDummyHash() {
    if (!BCRYPT_DUMMY_HASH.startsWith("$2a$" + BCRYPT_COST + "$")) {
        log.error("DUMMY_HASH cost factor mismatch, regenerating");
    }
}
```

cost 变更时（12→14）通过 `CryptoUtil` 生成新 dummy hash 并更新配置。

### 9.79 Caffeine L1 堆内存预算

`perm:effective:{userId}` 缓存条目 JVM 内对象 = 权限集合 + scope_value 列表 + 元数据，约 2-5KB/条。10000 条目需 20-50MB 堆内存：

```java
Caffeine.newBuilder()
    .maximumSize(10000)
    .maximumWeight(50 * 1024 * 1024)  // 50MB 上限
    .weigher((key, value) -> estimateWeight(value))
    .expireAfterWrite(60, TimeUnit.SECONDS)
    .recordStats()                    // → /actuator/metrics/cache
    .build();
```

### 9.80 Docker 镜像策略

```dockerfile
# 多阶段构建
FROM eclipse-temurin:21-jdk-alpine AS build
COPY . /src; WORKDIR /src; RUN ./mvnw -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
COPY --from=build /src/target/*.jar /app/app.jar
EXPOSE 8080; USER appuser
ENTRYPOINT ["java", "-XX:+UseZGC", "-jar", "/app/app.jar"]
```

目标镜像 < 200MB。JVM 使用 ZGC (低延迟，适合认证请求场景)。

### 9.81 K8s 启动探针

```yaml
startupProbe:
  httpGet: { path: /actuator/health/liveness, port: 8080 }
  initialDelaySeconds: 10
  periodSeconds: 5
  failureThreshold: 30           # 最多 150s 启动时间
readinessProbe:
  httpGet: { path: /actuator/health/readiness, port: 8080 }
  initialDelaySeconds: 0
  periodSeconds: 5
```

`CacheWarmer` (9.47) 在 startup 探针通过后触发，readiness 在预热完成后标记为就绪。

### 9.82 优雅下线顺序

```
1. SIGTERM 接收
2. 停止接受新请求 (Tomcat connector pause)
3. 等待进行中请求完成 (30s 超时，9.15)
4. @Async 线程池 shutdown → 等待审计日志刷盘
5. Caffeine L1 缓存 stats 导出到 Prometheus
6. HikariCP 连接池关闭
7. Lettuce Redis 连接关闭
8. JVM 退出
```

### 9.83 密钥注入机制

K8s Secret 挂载到 `/etc/secrets/`（tmpfs，不落盘）：

```yaml
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: app
    volumeMounts:
    - name: jwt-keys
      mountPath: /etc/secrets
      readOnly: true
  volumes:
  - name: jwt-keys
    secret: { secretName: auth-jwt-keys }
```

Vault 方案备选：`spring.config.import: vault://secret/auth-system`。

### 9.84 PIPL / 等保 2.0 合规

**数据本地化**: 所有个人数据（`users`、`user_profiles`、`login_logs`）存储于中国境内 MySQL 集群，不跨境传输。

**等保 2.0 三级**: 日志保留 12 月（≥6 月要求）+ 审计表防篡改（`audit_logs` 的 `detail` JSON 字段附加 `checksum: SHA-256(operator_id + action + resource + timestamp)` 可验证）。网络隔离通过 K8s NetworkPolicy 实现。

**实名认证**: `user_profiles.real_name` + `user_auths` 手机号绑定可作为实名基础。接入第三方实名认证 API（如阿里云实名认证）时新增 `identity_type=realname`。

### 9.85 云成本估算 (1M 用户)

| 资源 | 规格 | 月费用 (估算) |
|------|------|-------------|
| PolarDB MySQL 8.0 | 4C16G 集群版 | ¥1,200 |
| Redis 7.x | 2G 标准版 | ¥400 |
| K8s 计算节点 | 4x 4C8G ECS | ¥2,400 |
| 负载均衡 + 公网带宽 | 100Mbps | ¥800 |
| **总计** | | **~¥4,800/月** |

建议使用包年包月（节省 30-40%）+ 非生产环境按量付费。

### 9.86 补充配置外部化

```yaml
mybatis-plus.configuration.log-impl: ${MYBATIS_LOG_IMPL:org.apache.ibatis.logging.slf4j.Slf4jImpl}
snowflake.datacenter-id: ${SNOWFLAKE_DC_ID:1}
snowflake.worker-id: ${SNOWFLAKE_WORKER_ID:1}
spring.mail.host: ${MAIL_HOST:smtp.example.com}
spring.mail.port: ${MAIL_PORT:465}
spring.mail.username: ${MAIL_USERNAME:noreply@example.com}
### 9.87 AES Cipher 线程安全

`javax.crypto.Cipher` 非线程安全。`CryptoUtil` 使用 `ThreadLocal<Cipher>` 池化：

```java
private static final ThreadLocal<Cipher> ENCRYPT_CIPHER = ThreadLocal.withInitial(() -> {
    Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
    c.init(Cipher.ENCRYPT_MODE, deriveKey(currentKeyVersion()));
    return c;
});
private static final ThreadLocal<Cipher> DECRYPT_CIPHER = ThreadLocal.withInitial(() -> { ... });
```

每个线程持独立 `Cipher` 实例，避免并发加密时的 `AEADBadTagException`。密钥轮换时遍历所有 `ThreadLocal` 重新 init。

### 9.88 Filter 链排序与透传

`SecurityFilterChain` 中 filter 顺序和透传约定：

```
1. ApiKeyAuthenticationFilter  (检查 X-API-Key header → 有则验证, 无则透传 filterChain.doFilter())
2. JwtAuthenticationFilter     (检查 Authorization: Bearer → 有则验证, 无则透传)
3. 其余 Spring Security filters
```

两个 filter 均不会因"未携带自身类型的凭证"而拒绝请求——仅验证匹配的凭证类型，不匹配直接放行。同时携带两种凭证时，API Key 优先（per 6.8 作用域覆盖逻辑）。

### 9.89 依赖版本安全更新

| 依赖 | 原版本 | 更新版本 | 原因 |
|------|--------|----------|------|
| hutool-all | 5.8.34 | 5.8.35+ | CVE-2023-42277 任意文件写入 |
| zxing core/javase | 3.5.3 | 3.5.5 | 安全修复 |
| mybatis-plus | 3.5.9 | 3.5.10+ | 保持最新补丁版本 |

Maven Enforcer 插件锁定 Spring Boot BOM 管理的依赖版本，防止传递依赖版本漂移。Renovate 机器人自动提 PR 更新非 BOM 管理的依赖。

### 9.90 供应链安全

**SBOM 生成**: `pom.xml` 配置 `cyclonedx-maven-plugin`，CI 构建时生成 `target/cyclonedx/bom.json`，上传到 Dependency-Track 持续扫描。

**许可证合规**: 所有依赖均为 Apache 2.0 协议，无 GPL/AGPL 传染风险。

**CI 集成**: `mvn verify` → OWASP Dependency-Check Maven 插件 → SBOM → Trivy 容器镜像扫描 → 策略引擎阻断高危 CVE。

### 9.91 测试策略

**覆盖率目标**: JaCoCo 配置 `minimum.instruction=0.80`，security/auth 包必须 100% 分支覆盖。

**专项测试清单**:

| 测试类别 | 覆盖目标 | 框架 |
|----------|----------|------|
| Refresh token Lua 脚本 | 正常旋转 + 重放检测 + 并发竞态 | Testcontainers Redis |
| RBAC CTE 递归查询 | 3 层角色链 + scope_value 过滤 | Testcontainers MySQL |
| 滑动窗口限流精度 | ±10% 误差内 | 集成测试 |
| JWT 算法混淆 (`alg=none`) | 拒绝 | jose4j 内置 |
| PKCE bypass | 拒绝 plain + 不匹配 code_verifier | 集成测试 |
| Concurrent /refresh | 竞态不误触发家族撤销 | `ExecutorService` 并发 |

**性能基线测试**: Phase 10 增加 k6 压测脚本，验证 login 100 req/s < P95 500ms。

**测试隔离**: Testcontainers 每测试类独立 MySQL + Redis 实例。`@TestInstance(Lifecycle.PER_CLASS)` + `@DirtiesContext` 保证隔离。

---

## 10. 应急响应

### 10.1 批量吊销端点

| Method | Path | Permission | 描述 |
|--------|------|-----------|------|
| POST | /admin/revoke-all-tokens/{userId} | user:lock | 吊销指定用户全部 refresh token family + 黑名单当前 access token |
| POST | /admin/force-password-reset | user:lock | 批量强制密码重置 (body: `[userId1, userId2, ...]`)，设置 `password_changed_at=NOW()` |

### 10.2 Break-Glass 紧急访问

`super_admin` 角色 (`is_system=1`) 不可删除，用作紧急访问入口。当 RBAC 配置错误导致所有管理员被锁时：

1. 通过数据库直接操作或紧急配置注入临时 `user_roles` 分配 `super_admin`
2. `super_admin` 拥有全部 20 权限 `@all`，绕过所有作用域限制
3. 操作全量审计（`audit_logs.action=emergency.break_glass`），触发 PagerDuty 告警

### 10.3 安全事件检测告警

9.52 Prometheus 告警规则补充：

```yaml
- alert: TokenReplayDetected
  expr: rate(auth_refresh_replay_total[5m]) > 0
  labels: { severity: critical }
  annotations: { summary: "Refresh token 重放攻击" }
- alert: MassAccountLockout
  expr: rate(auth_login_attempts_total{result="failed_locked"}[5m]) > 100
  labels: { severity: critical }
  annotations: { summary: "可疑批量账户锁定" }
- alert: PrivilegeEscalation
  expr: rate(audit_role_assignment_total[5m]) > 20
  labels: { severity: warning }
  annotations: { summary: "异常频率角色分配操作" }
```

新增指标：`auth_refresh_replay_total` Counter、`audit_role_assignment_total` Counter。

### 10.4 取证查询

`GET /api/v1/audit-logs/forensic` (permission: `audit:read` + `user:lock`，限流 1/min)：

```
?ip=192.168.0.0/16       → 按 IP 前缀关联
&user_id=123             → 按用户关联
&time_window=1h           → 时间窗口
&action_chain=true        → 按 action 链关联 (登录→查询→修改→删除)
```

返回攻击者完整活动轨迹 JSON，包含时间线、IP 变化、用户代理变化。

---

## 11. 企业集成扩展

### 11.1 外部身份源消费 (OIDC RP / SAML / LDAP)

当前系统为 OAuth2 Provider。作为 RP 消费外部 IdP 需增加：

| 协议 | 实现方式 | 优先级 |
|------|----------|--------|
| OpenID Connect | Spring Security `oauth2Login()` + `ClientRegistration` | Phase 11 |
| SAML 2.0 | `spring-security-saml2-service-provider` | Phase 12 |
| LDAP | `spring-security-ldap` + `LdapAuthenticationProvider` | Phase 12 |

新增 `user_auths.identity_type` ENUM 扩展 `oidc`/`saml`/`ldap`。OIDC RP 配置示例：

```yaml
spring.security.oauth2.client.registration.google:
  client-id: ${GOOGLE_CLIENT_ID}
  client-secret: ${GOOGLE_CLIENT_SECRET}
  scope: openid,profile,email
```

### 11.2 SCIM v2 用户生命周期管理

```yaml
# Phase 11 — 供外部身份治理系统 (Okta/Azure AD) 自动同步用户
GET    /scim/v2/Users              → 查询用户列表 (SCIM filter 语法)
POST   /scim/v2/Users              → 创建用户
PUT    /scim/v2/Users/{id}         → 更新用户
PATCH  /scim/v2/Users/{id}         → 部分更新 (RFC 7644)
DELETE /scim/v2/Users/{id}         → 软删除用户
GET    /scim/v2/Groups             → 查询角色 (映射为 SCIM Group)
```

SCIM schema 映射：`users.username` → `userName`, `users.email` → `emails[0].value`, `user_roles` → `groups[].value`。

### 11.3 SIEM 日志投递

9.20 结构化 JSON 日志供 SIEM 消费，增加：

- **Syslog (RFC 5424)**: `logback-syslog4j` appender，发送到 SIEM 采集器
- **Elasticsearch**: `logstash-logback-encoder` → Logstash → Elasticsearch
- **Splunk HEC**: `logback-splunk-http` appender，直接投递到 Splunk HTTP Event Collector

审计事件 `audit_logs.action` 映射为 SIEM 可查询的 `event.action` 字段，配合 `event.outcome` (success/failure)。

### 11.4 领域事件流 (Kafka)

Spring 领域事件 (`AuditEvent`, `LoginEvent`, `RoleAssignedEvent`) 可选投递到 Kafka：

```java
// Spring Kafka + @TransactionalEventListener → KafkaTemplate
@EventListener
public void onLoginSuccess(LoginEvent event) {
    kafkaTemplate.send("auth.events.login", event.getUserId().toString(), event);
}
```

| Topic | 事件 | 消费者 |
|-------|------|--------|
| `auth.events.login` | LoginEvent | 安全监控、用户画像 |
| `auth.events.role` | RoleAssignedEvent, RoleRevokedEvent | IAM 同步 |
| `auth.events.audit` | AuditEvent | SIEM、合规审计 |
| `auth.events.user` | UserCreatedEvent, UserDeletedEvent | SCIM 桥接、CRM |

Kafka 集成在 Phase 11，默认关闭 (`spring.kafka.enabled=false`)。

### 11.5 Admin 管理后台

Phase 11 构建轻量管理后台（Vue 3 SPA）替代 Swagger 运营操作：

| 页面 | 功能 |
|------|------|
| 用户管理 | 搜索/列表/详情/状态变更/角色分配 |
| 角色与权限 | 角色树/权限矩阵/角色分配审计 |
| 审计日志 | 时间线视图/高级搜索/CSV 导出/取证查询 |
| OAuth 客户端 | 注册/密钥轮换/调用量统计 |
| 系统监控 | Prometheus 指标嵌入/健康状态面板/告警历史 |

后台部署为独立 SPA (`admin.example.com`)，通过 CORS + API Key 认证调用管理 API，复用现有 RBAC 权限体系。

---

## 12. 术语表

| 缩写 | 全称 | 首次出现章节 |
|------|------|-------------|
| JWT | JSON Web Token | 2 |
| RS256 | RSA Signature with SHA-256 | 2 |
| TOTP | Time-based One-Time Password (RFC 6238) | 2 |
| PKCE | Proof Key for Code Exchange (RFC 7636) | 6.7 |
| RBAC | Role-Based Access Control | 6.4 |
| CTE | Common Table Expression (SQL 递归查询) | 9.18 |
| AES-GCM | Advanced Encryption Standard - Galois/Counter Mode | 6.5 |
| AEAD | Authenticated Encryption with Associated Data | 9.23 |
| JWKS | JSON Web Key Set | 9.54 |
| JWK | JSON Web Key | 9.54 |
| BFF | Backend-For-Frontend (token 代理模式) | 9.45 |
| DPoP | Demonstration of Proof-of-Possession (RFC 9449) | 9.35 |
| HIBP | Have I Been Pwned (密码泄露检测服务) | 9.36 |
| SBOM | Software Bill of Materials | 9.90 |
| SCIM | System for Cross-domain Identity Management (RFC 7643/7644) | 11.2 |
| SIEM | Security Information and Event Management | 11.3 |
| OIDC | OpenID Connect (OAuth 2.0 扩展身份协议) | 11.1 |
| SAML | Security Assertion Markup Language 2.0 | 11.1 |
| PIPL | 中华人民共和国个人信息保护法 | 9.84 |
| MLPS | 信息安全技术网络安全等级保护 (等保 2.0) | 9.84 |
| GDPR | General Data Protection Regulation | 9.46 |
| SOC2 | Service Organization Control 2 (AICPA 审计标准) | 9.58 |
| RPO | Recovery Point Objective (最大可接受数据丢失量) | 9.55 |
| RTO | Recovery Time Objective (最大可接受恢复时间) | 9.55 |
| SPOF | Single Point of Failure (单点故障) | 9.44 |

### 9.x 章节主题分组

| 分组 | 章节范围 | 主题 |
|------|----------|------|
| 认证安全 | 9.1-9.8, 9.34, 9.41 | 密码/MFA/PKCE/并发登录/Cookie/时序防护 |
| 基础设施 | 9.9, 9.13-9.16, 9.38, 9.42, 9.80-9.83 | CORS/健康检查/Flyway/下线/池化/Docker/K8s |
| 速率与限流 | 9.10, 9.69 | 限流矩阵/标准头 |
| 数据治理 | 9.11, 9.30-9.33, 9.46, 9.65 | 保留/软删除/GDPR/PIPL/日志伪匿名化 |
| 缓存架构 | 9.17, 9.37, 9.47, 9.60-9.62, 9.79 | API Key/RBAC/Caffeine/Redis/预热/版本化/容量 |
| RBAC 与权限 | 9.18, 9.60, 9.88 | CTE/角色版本化/Filter 透传 |
| 运维与告警 | 9.19, 9.29, 9.39, 9.40, 9.44, 9.49, 9.52, 9.55 | 分区/黑名单/指标/性能基线/断路器/降级/DR |
| 测试与供应链 | 9.89-9.91 | 依赖安全/SBOM/测试策略 |
| 集成扩展 | 11.1-11.5 | OIDC/SCIM/SIEM/Kafka/Admin UI |
| 应急响应 | 10.1-10.4 | 批量吊销/Break-Glass/安全告警/取证 |

### 11.6 移动端 SDK 与安全存储

OpenAPI 3.0 JSON 在 `/v3/api-docs` 自动暴露，供 `openapi-generator` 生成 iOS/Android/Flutter SDK。移动端 Token 安全存储：

| 平台 | 技术 | API |
|------|------|-----|
| iOS | Keychain Services | `SecItemAdd`/`SecItemCopyMatching`，`kSecAttrAccessibleAfterFirstUnlock` |
| Android | EncryptedSharedPreferences 或 Android Keystore | `EncryptedSharedPreferences.create()` |
| Flutter | flutter_secure_storage | 封装 iOS Keychain / Android Keystore |

**移动端 PKCE 强制**: `authorization_code` grant 对 `client_type=public`（移动端/SPA）强制 PKCE S256，`GET /authorize` 拒绝无 `code_challenge` 的 public client 请求。

**离线 Token 刷新**: 移动端 SDK 在 401 响应时自动队列 `/refresh`，网络恢复后重放原始请求。离线期间最多缓存 50 个请求，超出 FIFO 丢弃。

### 11.7 特性开关与渐进上线

```yaml
app.feature:
  mfa-required: false              # 强制 MFA（按租户/用户逐步开启）
  pkce-enforcement: true           # PKCE 强制 (已默认开启)
  registration-open: true          # 开放注册 (紧急时可关闭)
  bcrypt-cost-migration: false     # 启用后下次登录以新 cost 重哈希
```

`@FeatureToggle(name="mfa-required")` AOP 注解，从 Redis `feature:mfa-required:{userId}` 读取用户级开关（缺省继承租户级，再缺省取全局默认）。

**BCrypt 渐进升级**: `bcrypt-cost-migration=true` 时，旧 cost 哈希验证通过后自动以新 cost 重哈希并更新 `user_auths.credential`，无需全员重置密码。

**金丝雀部署**: 通过 K8s Ingress `canary.weight` 按比例切流。认证逻辑变更优先 5% → 观察 30min → 50% → 100%。

**限流阈值外部化**: `@RateLimit(permits="${rate.limit.login:5}")` Spring EL 从配置读取，环境差异化无需改代码。

**Kill Switch**: `app.feature.*` 全部可运行时通过 `Redis SET` 即时关闭，无需重启。

---

## 13. 软删除应用层联动

MySQL `ON DELETE CASCADE` 仅在物理 DELETE 时触发。软删除（`deleted_at > 0`）必须在应用层联动：

```java
@Transactional
public void softDeleteUser(Long userId) {
    long now = System.currentTimeMillis();
    // 1. 标记用户
    userMapper.update(null, new UpdateWrapper<User>()
        .set("deleted_at", now).eq("user_id", userId));
    // 2. 级联标记关联表（无 deleted_at 的表物理删除）
    userProfileMapper.deleteById(userId);                          // user_profiles
    userCredentialsHistoryMapper.delete(new QueryWrapper<...>()
        .eq("user_id", userId));                                   // user_credentials_history
    userMfaMapper.delete(new QueryWrapper<...>()
        .eq("user_id", userId));                                   // user_mfa (audit_logs 已记录)
    // 3. 标记有 deleted_at 的表
    userAuthMapper.update(null, new UpdateWrapper<UserAuth>()
        .set("deleted_at", now).eq("user_id", userId));
    apiKeyMapper.update(null, new UpdateWrapper<ApiKey>()
        .set("deleted_at", now).eq("user_id", userId));
    userRoleMapper.update(null, new UpdateWrapper<UserRole>()
        .set("deleted_at", now).eq("user_id", userId));
    // 4. 黑名单当前所有 token + 吊销 refresh family
    tokenService.revokeAllUserTokens(userId);
    // 5. 使 Redis 缓存失效
    redisTemplate.delete("perm:effective:" + userId);
    redisTemplate.delete("apikey:" + keyHash);  // API Key 缓存即时失效（否则 TTL 120s 内仍可认证）
    // 6. 登录日志/审计日志 user_id 保留（9.32 IS NULL 查询），GDPR 擦除时走 forget-me 清空
}
```

**关键**: API Key Redis 缓存 `apikey:{key_hash}` 必须在软删除时同步 DEL，否则 TTL 120s 内已删除用户仍可通过缓存命中认证。

### 13.1 启动时缓存梯度预热

冷启动时避免全量请求穿透到 CTE + bcrypt CPU 风暴：

```java
// 替换 9.47 的简单预热，增加梯度放量
// Phase 1: startupProbe 通过 → 预热 100 热点用户权限 (5s)
// Phase 2: readinessProbe 标记就绪 → 开始接收请求，限流器初始窗口收紧 (10% 正常阈值)
// Phase 3: 30s 后限流器窗口逐步放宽到 100% → 全量服务
```

```yaml
app.cache.warmup:
  gradient: true                   # 梯度放量
  gradient-duration: 30s           # 30s 内从 10% → 100%
  initial-rate-limit-ratio: 0.1    # 启动时限流阈值收紧到 10%
```

### 13.2 Refresh Token 重试容错

9.43 Lua 脚本增加重试容错窗口，避免网络重试误触发家族撤销：

```lua
-- 增加: 如果 same old_token 在 5s 内重放 (而非已被替换的 previous)，视为正常重试
local retry_window = redis.call('HGET', KEYS[1], 'retry_window')
if retry_window and current == ARGV[1] and tonumber(retry_window) > tonumber(ARGV[3]) then
    return {1, 'ok'}  -- 幂等返回，不撤销家族
end
-- ARGV[3] = 当前时间戳(秒)，retry_window = 上次旋转时间 + 5s
```

Java 侧首次旋转时 `HSET retry_window <now+5s>`，5s 内相同 old_token 重放返回成功而非触发撤销。

### 13.3 补充配置项 (Section 3 补全)

```yaml
# === 以下配置项在 9.x 中引用，需补入 Section 3 ===
app.cors.allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
snowflake.datacenter-id: ${SNOWFLAKE_DC_ID:1}
snowflake.worker-id: ${SNOWFLAKE_WORKER_ID:1}
spring.datasource.hikari.maximum-pool-size: ${DB_POOL_SIZE:50}
spring.datasource.hikari.minimum-idle: ${DB_POOL_MIN:10}
spring.mail.host: ${MAIL_HOST:smtp.example.com}
spring.mail.port: ${MAIL_PORT:465}
spring.mail.username: ${MAIL_USERNAME:noreply@example.com}
mybatis-plus.configuration.log-impl: ${MYBATIS_LOG_IMPL:org.apache.ibatis.logging.slf4j.Slf4jImpl}

# 启动校验: 若 MAIL_HOST 仍为 smtp.example.com 则 WARN 日志提醒（非阻塞启动）
```

启动时 `@PostConstruct` 检查 `MAIL_HOST` 是否为占位默认值，若是则 `log.warn("Mail host未配置，验证码邮件将无法发送")`。

### 13.4 GeeTest 断路器防抖动

9.44 断路器增加 `waitDurationInOpenState` 最小化抖动：

```yaml
resilience4j.circuitbreaker.instances.geetest:
  wait-duration-in-open-state: 60s     # 从 30s 增加到 60s，减少抖动
  minimum-number-of-calls: 20          # 至少 20 次调用才评估（防冷启动误判）
  permitted-number-of-calls-in-half-open-state: 5
```

半开状态允许 5 个探测请求，全通过才关闭断路器。避免单次成功即全量切换导致再次熔断。

### 13.5 Redis 高可用 (消除 SPOF)

Redis 单点故障影响 refresh token 家族和验证码功能（9.49 已定义 fail-closed）。生产部署 Redis Sentinel（至少 3 节点）或云托管 Redis 多可用区：

```yaml
spring.data.redis:
  sentinel:
    master: mymaster
    nodes: redis-sentinel-1:26379,redis-sentinel-2:26379,redis-sentinel-3:26379
  password: ${REDIS_PASSWORD}
```

故障转移时 Lettuce 自动重连到新 master，应用层无感知。配合 9.49 降级策略，Sentinel 切换期间（通常 < 30s）：
- Refresh token 在切换窗口内拒绝 → 客户端自动 /refresh 重试 → 新 master 就绪后恢复
- 验证码在切换窗口内不可用 → 前端提示重试
- 权限缓存回退 MySQL CTE → 正常服务
