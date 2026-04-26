# 认证授权系统 — 后端接口设计方案

## 1. 概述

- **项目**: `src/backend/general-web-backend`（Spring Boot 4.1.0-SNAPSHOT, Java 21, Maven）
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
| cn.hutool : hutool-all 5.8.34 | Snowflake, crypto, TOTP (Hutool 替代 de.taimos), code gen |
| org.bitbucket.b_c : jose4j 0.9.6 | JWT RS256 (no Jackson conflicts) |
| com.github.xiaoymin : knife4j-openapi3-jakarta-spring-boot-starter 4.5.0 | Swagger UI |
| org.mapstruct : mapstruct 1.6.3 (+ processor) | DTO/entity 编译期映射 |
| org.projectlombok : lombok | Boilerplate reduction |
| com.google.zxing : core + javase 3.5.3 | TOTP QR codes |
| org.testcontainers : testcontainers/mysql/junit-jupiter (test) | Integration tests |
| com.h2database : h2 (test) | In-memory test DB |

---

## 3. 配置文件 (application.yml)

```yaml
spring:
  application.name: general-web-backend
  datasource:
    url: jdbc:mysql://localhost:3306/auth_system?useSSL=true&requireSSL=true&serverTimezone=Asia/Shanghai
    # 本地开发若 MySQL 未配置 SSL 证书，可临时改为 useSSL=false
    username: root
    password: ${DB_PASSWORD}
  data.redis:
    host: localhost
    port: 6379
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
  private-key: classpath:jwt-private.pem
  public-key: classpath:jwt-public.pem
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
│   │   ├── GrantType.java                 — authorization_code, client_credentials, refresh_token, implicit
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
│   │   ├── IpUtil.java                    — Extract client IP from request
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
| POST | /refresh | — (body传refresh_token) | 刷新 access token |
| POST | /logout | Bearer token | 黑名单当前 access token JTI, 吊销 refresh token family |
| POST | /password/change | Bearer token | 修改密码 (验旧+查历史+更新password_changed_at) |
| POST | /password/reset | — | 验证码重置密码 |
| POST | /code/send | — | GeeTest → 发邮箱验证码 (5min TTL, rate-limited) |
| POST | /code/verify | — | 验证邮箱验证码 |
| GET | /token/validate | Bearer token | token 有效性校验（供网关使用） |

**注册流程**:
```
前端: 填表(username/email/phone/password) → 点"发送验证码" → GeeTest滑块 → POST /code/send {identifier, identity_type, purpose} → 收到邮件
前端: 输入验证码 → POST /register {..., identity_type, verification_code}
后端: 根据 identity_type + identifier 查找 Redis vc:purpose:{identifier} 校验验证码 → 通过后 Snowflake user_id → bcrypt密码 → 事务写入 users + user_auths + user_profiles → 分配默认user角色
```

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

`login_logs` 和 `audit_logs` 使用 `RANGE COLUMNS(created_date)` + 复合主键 `(id, created_date)`。MySQL 事件 `evt_add_partitions` 每月自动创建新分区。Java 侧 `PartitionMaintenanceScheduler` 作为安全网。

### 6.7 OAuth2 — 完整 Provider

支持 `authorization_code` / `client_credentials` / `refresh_token` / `implicit` 四种 grant type。授权码 Redis 存储 (10min TTL)，PKCE 支持 (`oauth:code:{code}:pkce`)。用户同意记录按 user+client 存储。可信客户端 (`is_trusted=1`) 跳过同意页。

### 6.8 API Key — 作用域限制

`api_keys.allowed_scopes` (JSON) 可限定某个 API Key 的有效权限集。验证时如果设了此字段，则覆盖用户角色派生的权限。

### 6.9 登录锁定

1. 失败 → `lockout:failed:{userId}` Redis Hash: `HINCRBY attempts 1`, `EXPIRE 1800`
2. attempts ≥ 5 → users.status=3 (LOCKED), locked_until=now+30min
3. 成功 → 重置计数器 + failed_attempts
4. LoginLog 异步写入对应的 result + failure_reason

### 6.10 Refresh Token 轮换 + 盗用检测

每次登录创建 "token family" (UUID)。refresh token 存入 `refresh:family:{familyId}` Redis Hash。轮换时新旧交替——如果检测到已被撤销的旧 refresh token 被重放 → 整个 family 撤销 (`refresh:family:{familyId}:revoked`)，用户必须重新登录。

### 6.11 GeeTest 极验 v4

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
12. `AuthController.register()` → 校验验证码 → 事务写入 users + user_auths + user_profiles → 分配默认 user 角色
13. `AuthController.login()` → GeeTest 校验 → .identifier lookup → 状态检查 → bcrypt → MFA → 签发 token → 记录 login_log
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

### 9.6 OAuth2 PKCE — 强制 S256

- 仅接受 `code_challenge_method=S256`，拒绝 `plain`
- 验证逻辑：`BASE64URL-ENCODE(SHA-256(code_verifier)) == code_challenge`
- code_challenge 存储在 Redis `oauth:code:{code}:pkce`，与授权码同时过期 (600s)

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

双重维度：IP 维度防分布式攻击，identifier 维度防定向攻击。MFA code 校验阶段单独限流，防止 TOTP 6 位数字的暴力尝试（30s 窗口内约 1M 组合，限流将有效尝试次数降到可忽略级别）。

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

Phase 1 即配置 Actuator：
```yaml
management:
  endpoints.web.exposure.include: health,info
  endpoint.health:
    probes.enabled: true
    show-details: when-authorized
```

- `/actuator/health/liveness` — 容器存活探针 (仅检查应用是否存活)
- `/actuator/health/readiness` — 就绪探针 (检查 MySQL + Redis 连通性)

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
           -- 将作用域转为数字排名：self=1 dept=2 org=3 all=4，rank 越小越严格
           CASE ur.scope_type WHEN 'self' THEN 1 WHEN 'dept' THEN 2
                              WHEN 'org' THEN 3 WHEN 'all' THEN 4 END,
           4  -- 祖先角色无 user_roles 行，默认为 all(4)
       ) AS ur_rank,
       CASE COALESCE(s.scope_key, 'all')
           WHEN 'self' THEN 1 WHEN 'dept' THEN 2
           WHEN 'org' THEN 3 WHEN 'all' THEN 4
       END AS rp_rank
FROM role_chain rc
JOIN role_permissions rp ON rc.id = rp.role_id
LEFT JOIN scopes s ON rp.scope_id = s.id
LEFT JOIN user_roles ur ON rc.id = ur.role_id AND ur.user_id = <userId> AND ur.deleted_at = 0
WHERE rc.is_direct = 0 OR ur.deleted_at = 0;
-- effective_scope_rank = MAX(rp_rank, ur_rank) → 取较严格的那个
-- 最终合并: 对每个 permission_id 取 MIN(effective_scope_rank) → 跨所有角色取最宽松的
```

作用域合并分两步：
1. **角色内收窄**：每个角色分配的作用域 = `MOST_RESTRICTIVE(role_permissions_scope, user_roles_scope)`。祖先角色无 `user_roles` 行，视为 `scope_type='all'`（不做额外限制）。`LEFT JOIN user_roles` 确保祖先角色的权限不会被丢弃。
2. **角色间合并**：对同一 `permission_id`，取 `MOST_PERMISSIVE(effective_scope_across_all_roles)`。
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
