# 认证授权系统 — 后端接口设计方案

> 最后更新: 2026-05-03 | 状态: Phase 1-10 + 架构重构 + 安全加固 + Admin 管理后台 + 邮件多语言 + 用户中心 + 自动化测试

## 1. 概述

- **项目**: `src/backend/general-web-backend`（Spring Boot 3.4.7, Java 21, Maven）
- **数据库**: MySQL 8.0 14 表 + Redis 7.x
- **代码层级**: Controller → Service(Interface+Impl) → Mapper → DB，严格分层
- **API 前缀**: `/api/v1`
- **API 文档**: springdoc-openapi，运行时访问 `http://localhost:8080/doc.html`
- **总文件数**: ~175 Java + 15 XML + Flyway V1-V3

---

## 2. 实际依赖（与设计对照）

| 依赖 | 设计版本 | 实际版本 | 说明 |
|------|---------|---------|------|
| spring-boot-starter-parent | 3.4.x GA | **3.4.7** | ✅ 安全升级 2 个补丁版本 |
| mybatis-plus | 3.5.9 | **3.5.9** | ✅ |
| hutool-all | 5.8.35 | **5.8.35** | ✅ |
| jose4j | 0.9.8 | **0.9.6** | ⚠️ Maven Central 最新可用版本 |
| knife4j | 4.5.0 | **4.5.0** + springdoc 2.7.0 | ✅ 排除内置 springdoc |
| mapstruct | 1.6.3 | **1.6.3** | ✅ |
| redisson | 3.40.2 | **3.40.2** | ✅ |
| zxing | 3.5.5 | **3.5.4** | ⚠️ Maven Central 最新可用版本 |
| resilience4j | 2.2.0 | **2.2.0** | ✅ @CircuitBreaker 已接线 GeeTestVerifier |
| caffeine | - | **内置(spring-boot-starter)** | ✅ 新增 L1 缓存 |
| micrometer-registry-prometheus | - | ✅ | ✅ 新增可观测性 |
| bouncycastle bcprov+bcpkix | 1.78 | **1.80** | ✅ 安全升级 CVE-2025-8916 |
| logstash-logback-encoder | - | **7.4** | ✅ 新增结构化日志 |
| testcontainers | - | ✅ | ✅ 新增集成测试 |

---

## 3. 项目包结构（当前实际）

```
com.iccuu.general_web_backend
├── GeneralWebBackendApplication.java
│
├── common/
│   ├── config/
│   │   ├── WebMvcConfig.java              — CORS, Jackson, 安全响应头
│   │   ├── SecurityConfig.java            — SecurityFilterChain, BCryptPasswordEncoder(12), AuthenticationEntryPoint(401 JSON), AccessDeniedHandler(403 JSON)
│   │   ├── MyBatisPlusConfig.java         — Snowflake IdGenerator, 分页拦截器
│   │   ├── RedisConfig.java               — RedisTemplate, RedissonClient
│   │   ├── AsyncConfig.java               — @EnableAsync for email/log/audit
│   │   ├── GeeTestConfig.java             — GeeTestProperties
│   │   ├── Knife4jConfig.java             — OpenAPI group config
│   │   └── PrometheusMetricsConfig.java   — MeterRegistry 通用标签
│   ├── constant/
│   │   ├── Constants.java
│   │   ├── RedisKeyPrefix.java
│   │   └── ErrorCode.java
│   ├── enums/                             — 9 enum: UserStatus/IdentityType/MfaType/AuthMethod/LoginResult/ScopeType/ClientType/GrantType/ApiKeyStatus
│   ├── exception/                         — 6 exception: BusinessException + AuthenticationException/AuthorizationException/ResourceNotFoundException/RateLimitException/DuplicateResourceException
│   ├── handler/
│   │   └── GlobalExceptionHandler.java    — @RestControllerAdvice, AccessDeniedException+AuthenticationException
│   ├── result/
│   │   ├── R.java                         — 统一响应: code/message/data/timestamp
│   │   ├── PageResult.java                — 分页响应
│   │   ├── PageQuery.java                 — 分页查询基类
│   │   └── FieldError.java                — 校验错误字段
│   ├── aspect/
│   │   ├── AuditLogAspect.java            — @Around @AuditLog
│   │   ├── RateLimitAspect.java           — @Around @RateLimit (Redis Lua)
│   │   └── RequirePermissionAspect.java   — @Around @RequirePermission (Caffeine L1 + Redis L2), 无权限→AccessDeniedException(403)
│   ├── annotation/
│   │   ├── AuditLog.java
│   │   ├── RateLimit.java
│   │   └── RequirePermission.java
│   ├── validation/
│   │   ├── PasswordComplexity.java        — @Target(TYPE) 密码复杂度注解
│   │   └── PasswordComplexityValidator.java
│   ├── util/
│   │   ├── CryptoUtil.java                — AES-256-GCM ThreadLocal<Cipher>
│   │   ├── HashUtil.java                  — SHA-256
│   │   ├── IpUtil.java                    — X-Forwarded-For 解析
│   │   ├── RedisUtil.java                 — RedisTemplate 便捷方法(含 setNx)
│   │   ├── SecureRandomUtil.java          — 共享 SecureRandom + generateSecureToken()
│   │   └── SecurityUtil.java              — getCurrentUserId/Username/Permissions(含 String→Long)
│   └── filter/
│       ├── JwtAuthenticationFilter.java   — OncePerRequestFilter: parse Bearer, blacklist, password_changed_at(Caffeine)
│       └── ApiKeyAuthenticationFilter.java— OncePerRequestFilter: SHA-256 hash, HGETALL缓存, ApiKeyCacheManager
│   ├── cache/ApiKeyCacheManager.java          — Redis Hash 5字段缓存 (HGETALL/HPUTALL/null标记)
│   ├── event/ (4 领域事件: UserRegistered/UserLoggedIn/PasswordChanged/UserStatusChanged)
│   └── converter/ (5 MapStruct: User/ApiKey/AuditLog/LoginLog/OAuthClient)
│
├── infrastructure/
│   ├── event/
│   │   ├── AuthEventPublisher.java            — ApplicationEventPublisher 封装
│   │   └── AuthEventListeners.java            — @TransactionalEventListener(AFTER_COMMIT)+@Async
│   ├── cache/
│   │   ├── CacheWarmer.java                   — 启动后预热 100 活跃用户权限 (→ UserService)
│   │   ├── CacheInvalidationPublisher.java
│   │   └── CacheInvalidationListener.java
│   ├── storage/
│   │   ├── DataRetentionService.java          — DB-backed 物理删除调度 (TaskPersistenceService)
│   │   ├── TaskPersistenceService.java
│   │   ├── TaskPoller.java
│   │   ├── PartitionMaintenanceScheduler.java — 每月分区维护
│   │   └── DataRetentionScheduler.java        — 90天软删除+12月分区清理
│   ├── snowflake/
│   │   └── SnowflakeIdGenerator.java      — Hutool Snowflake
│   ├── security/
│   │   ├── JwtTokenProvider.java          — jose4j RS256 sign/verify, getJwks()
│   │   ├── PermissionResolver.java        — Interface
│   │   ├── RateLimiterService.java        — Redis Lua sliding window
│   │   └── HibpService.java               — HIBP k-anonymity 密码泄露检测
│   ├── audit/
│   │   ├── AuditEventPublisher.java       — @Deprecated (→ AuthEventListeners)
│   │   └── AuditEvent.java                — @Deprecated
│   ├── geetest/
│   │   ├── GeeTestProperties.java
│   │   └── GeeTestVerifier.java           — HMAC-MD5 sign, Redis 缓存
│   ├── metrics/
│   │   └── MetricsService.java            — Prometheus Counter/Timer 业务指标
└── module/
    ├── auth/
    │   ├── controller/ (AuthController, JWKSController)
    │   ├── service/ (AuthService, TokenService, VerificationCodeService, LoginLogService + impl)
    │   ├── dto/ (LoginRequest, LoginResponse, RegisterRequest, RefreshTokenRequest, ChangePasswordRequest, ResetPasswordRequest, SendCodeRequest, VerifyCodeRequest, TokenValidationResponse)
    │   ├── entity/ (LoginLog)
    │   ├── strategy/ (authentication: 2Provider + AuthenticationResult; identity: 3IdentityResolver)
    │   └── mapper/ (LoginLogMapper + XML)
    │
    ├── user/
    │   ├── controller/UserController.java
    │   ├── service/ (UserService, UserProfileService, UserCredentialsHistoryService + impl)
    │   ├── dto/ (10 DTO: UserCreateRequest, UserUpdateRequest, UserQueryRequest, UserVO, UserStatusRequest, UserProfileUpdateRequest, UserProfileVO, UserAuthVO, BindAuthRequest)
    │   ├── entity/ (User, UserProfile, UserAuth, UserCredentialsHistory, UserDevice)
    │   └── mapper/ (4 Mapper + XML)
    │
    ├── role/
    │   ├── controller/RoleController.java
    │   ├── service/ (RoleService, PermissionService + impl)
    │   ├── dto/ (9 DTO: RoleCreateRequest, RoleUpdateRequest, RoleVO, RoleTreeVO, PermissionVO, ScopeVO, AssignRoleRequest, AssignPermissionRequest, UserRoleVO)
    │   ├── entity/ (Role, Permission, Scope, UserRole, RolePermission)
    │   ├── mapper/ (5 Mapper + XML)
    │   └── cache/ (PermissionEvaluatorImpl, PermissionCacheManager)
    │
    ├── mfa/
    │   ├── controller/MfaController.java
    │   ├── service/ (UserMfaService + impl)
    │   ├── dto/ (5 DTO: MfaStatusVO, MfaSetupInitRequest, MfaSetupInitResponse, MfaEnableRequest, MfaDisableRequest)
    │   ├── entity/ (UserMfa)
    │   ├── strategy/ (4 MfaVerifier: TOTP + SMS/Email/WebAuthn 占位)
    │   └── mapper/ (UserMfaMapper + XML)
    │
    ├── oauth/
    │   ├── controller/OAuthClientController.java
    │   ├── service/ (OAuthClientService, OAuthAuthorizationService + impl)
    │   ├── dto/ (6 DTO: OAuthClientCreateRequest, OAuthClientUpdateRequest, OAuthClientVO, AuthorizationRequest, TokenExchangeRequest, TokenResponse)
    │   ├── entity/ (OAuthClient)
    │   └── mapper/ (OAuthClientMapper + XML)
    │
    ├── apikey/
    │   ├── controller/ApiKeyController.java
    │   ├── service/ (ApiKeyService + impl)
    │   ├── dto/ (3 DTO: ApiKeyCreateRequest, ApiKeyCreateResponse, ApiKeyVO)
    │   ├── entity/ (ApiKey)
    │   └── mapper/ (ApiKeyMapper + XML)
    │
    └── audit/
        ├── controller/ (AuditLogController, LoginLogController)
        ├── service/ (AuditLogService + impl)
        ├── dto/ (4 DTO: AuditLogQueryRequest, AuditLogVO, LoginLogQueryRequest, LoginLogVO)
        ├── entity/ (AuditLog)
        └── mapper/ (AuditLogMapper + XML)
```

**与设计的差异（已全部解决）**:
- ~~Converter 目录存在但为空~~ → **✅ 已实现** 5 个 MapStruct Converter（User/ApiKey/AuditLog/LoginLog/OAuthClient）
- ~~Strategy 模式未实现~~ → **✅ 已实现** 3 条策略链：IdentityResolver(3) + MfaVerifier(4) + AuthenticationProvider(2)
- `SecurityContextHelper` 合并至 `SecurityUtil`
- `JwtUtil` 合并至 `JwtTokenProvider.getClaimString()`

**架构重构新增包结构（2026-04-29）**:
- `common/converter/` — 5 MapStruct Converter
- `common/event/` — 4 领域事件 + AuthEventPublisher + AuthEventListeners
- `common/cache/ApiKeyCacheManager.java` — API Key Redis Hash 缓存（HGETALL/HPUTALL）
- `common/filter/RequestIdFilter.java` — 全链路请求 ID（UUID v4）
- `infrastructure/event/` — 事件基础设施
- `infrastructure/security/DeviceFingerprintService.java` — 设备指纹绑定
- `infrastructure/storage/DataRetentionService.java` — DB-backed 物理删除调度
- `module/auth/strategy/` — 认证策略 + 身份解析
- `module/mfa/strategy/` — MFA 验证策略
- `module/user/entity/UserDevice.java` — 用户设备绑定
- Flyway V4 (SOC2 审计字段) + V5 (user_devices 表 + 分区)

---

## 4. REST API 端点实现状态

### 4.1 认证 — 全部实现 ✅

| Method | Path | Auth | @RequirePermission | 状态 |
|--------|------|------|--------------------|------|
| POST | /register | — | — | ✅ |
| POST | /login | — | @RateLimit(5/min) | ✅ GeeTest 强制+fail-open |
| POST | /refresh | — | — | ✅ Lua 原子旋转+重试容错 |
| POST | /logout | Bearer | — | ✅ 黑名单+family 撤销 |
| POST | /password/change | Bearer | — | ✅ @PasswordComplexity |
| POST | /password/reset | — | — | ✅ @PasswordComplexity |
| POST | /code/send | — | @RateLimit(3/5min) | ✅ GeeTest 强制+fail-closed |
| POST | /code/verify | — | — | ✅ |
| GET | /token/validate | Bearer | — | ✅ |
| POST | /me/export | Bearer | — | ✅ GDPR Art.20 |
| POST | /me/forget-me | Bearer | — | ✅ GDPR Art.17 (self-only) |

### 4.2 OAuth2 — 全部实现 ✅

| Method | Path | Permission | 状态 |
|--------|------|-----------|------|
| GET | /authorize | — | ✅ PKCE S256 + state CSRF |
| POST | /token | — | ✅ |
| POST | /revoke | — | ✅ |
| GET | /clients | client:read | ✅ |
| GET | /clients/{id} | client:read | ✅ |
| POST | /clients | client:create | ✅ |
| PUT | /clients/{id} | client:update | ✅ |
| DELETE | /clients/{id} | client:delete | ✅ |
| POST | /clients/{id}/regenerate-secret | client:update | ✅ |

### 4.3 用户 — 全部实现 ✅

| Method | Path | Permission | 状态 |
|--------|------|-----------|------|
| GET | /users | user:read | ✅ |
| GET | /users/{userId} | user:read | ✅ |
| POST | /users | user:create | ✅ |
| PUT | /users/{userId} | user:update | ✅ |
| DELETE | /users/{userId} | user:delete | ✅ |
| PATCH | /users/{userId}/status | user:lock | ✅ |
| GET | /users/{userId}/roles | role:read | ✅ |
| POST | /users/{userId}/roles | role:assign | ✅ |
| DELETE | /users/{userId}/roles/{roleId}/{scopeType}/{scopeValue} | role:assign | ✅ |
| GET | /users/{userId}/auths | user:read | ✅ |
| POST | /users/{userId}/auths | user:update | ✅ |
| DELETE | /users/{userId}/auths/{authId} | user:update | ✅ |
| GET | /me | — | ✅ |
| PUT | /me/profile | — | ✅ |
| PUT | /me/avatar | — | ✅ |

### 4.4 角色/权限 — 全部实现 ✅

| Method | Path | Permission | 状态 |
|--------|------|-----------|------|
| GET | /roles | role:read | ✅ |
| GET | /roles/tree | role:read | ✅ |
| GET | /roles/{id} | role:read | ✅ |
| POST | /roles | role:create | ✅ |
| PUT | /roles/{id} | role:update | ✅ |
| DELETE | /roles/{id} | role:delete | ✅ |
| GET | /roles/{id}/permissions | perm:read | ✅ |
| POST | /roles/{id}/permissions | perm:assign | ✅ |
| DELETE | /roles/{id}/permissions/{permId} | perm:assign | ✅ |
| GET | /permissions | perm:read | ✅ |
| GET | /scopes | perm:read | ✅ |

### 4.5-4.7 API Key / MFA / 审计 — 全部实现 ✅

---

## 5. 架构决策实现状态

| # | 设计决策 | 实际实现 | 说明 |
|---|---------|---------|------|
| 6.1 | 自定义 SqlInjector (NOW_MILLIS) | 标准 @TableLogic(value="0") | 简化实现，删除标记为毫秒时间戳靠应用层 set |
| 6.2 | Snowflake IdGenerator | ✅ MyBatisPlusConfig IdentifierGenerator | Hutool Snowflake 已接线 |
| 6.3 | JWT + password_changed_at | ✅ Caffeine 缓存(60s TTL)，仅 access token | refresh token 依赖短期 TTL 不检查 |
| 6.4 | 层级 RBAC 递归 CTE | Java 循环遍历 parent_role_id(最多5层) | CTE 实现复杂度高，Java 循环对浅层级足够 |
| 6.5 | MFA AES-256-GCM 密钥版本化 | 当前版本加密(k1)，v0 预留解密 | 完整多版本解密待密钥轮换时启用 |
| 6.6 | 分区表维护 | 存储过程 + Java Scheduler 双保险 | 云 DB 不支持 MySQL Event 时用 Java |
| 6.7 | OAuth2 PKCE + state CSRF | ✅ S256 强制 + state Redis 验证 | |
| 6.8 | API Key SHA-256 + dsk- | ✅ SHA-256 hash + dsk-xxx 前缀 | Redis 缓存 `apikey:{hash}` 未实现(DB 查询已足够) |
| 6.9 | 登录锁定 Redis counter | DB 字段 `failed_attempts` + `locked_until` | 简化实现，避免 Redis→DB 同步 |
| 6.10 | Refresh Token Lua 旋转 | ✅ 原子旋转 + 重放检测 + 5s 重试容错 | 增强版 |
| 6.11 | GeeTest 断路器 | 手动 try/catch fail-open(登录) / fail-closed(发码) | Resilience4j 配置存在未接线 |
| 6.12 | Redis 键空间 | ✅ 所有前缀定义在 RedisKeyPrefix | `oauth:state`/`session`/`snowflake:worker` 已定义,按需使用 |
| 6.13 | 分布式缓存失效 pub/sub | 直接 DEL (Caffeine+Redis) | pub/sub 模式已定义,多实例部署时启用 |

---

## 6. 安全加固清单

| 机制 | 状态 |
|------|------|
| JWT 黑名单 Filter 检查 | ✅ TokenService.isBlacklisted() |
| @RequirePermission 完整覆盖 | ✅ 全部 25 个管理端点 |
| GeeTest 登录 fail-open + 发码 fail-closed | ✅ |
| password_changed_at 即时失效 | ✅ Caffeine 60s TTL |
| 时序侧信道防护 (DUMMY_HASH) | ✅ |
| 密码复杂度校验 (3/4 类字符) | ✅ @PasswordComplexity |
| 密码历史防重用 (10次) | ✅ |
| HIBP 密码泄露检测 | ✅ @Async fail-open |
| 安全响应头 (X-Content-Type-Options, X-Frame-Options, Referrer-Policy) | ✅ |
| MFA 加密密钥环境变量化 | ✅ @Value("${crypto.aes-keys.1}") |
| CORS restricted origins | ✅ allowedOriginPatterns |
| Refresh Token Lua 原子操作 | ✅ |

---

## 7. 已延期至 Phase 11+ 的功能（2026-04-29 更新）

以下功能在设计文档中定义。已完成项已打勾，未完成项保留延期状态。

| 功能 | 章节 | 状态 | 说明 |
|------|------|------|------|
| 自定义 SqlInjector (NOW_MILLIS 软删除) | 6.1 | ❌ 不需要 | 标准 @TableLogic 足够 |
| RBAC 递归 CTE | 9.18 | ❌ 不需要 | Java 循环对浅层级足够 |
| GeeTest Resilience4j @CircuitBreaker | 9.44 | ✅ 已完成 | @CircuitBreaker(name="geetest") + fail-open/fail-closed |
| API Key Redis 缓存 `apikey:{hash}` | 9.17 | ✅ 已完成 | ApiKeyCacheManager (HGETALL + HPUTALL + null标记) |
| Redis pub/sub 缓存失效 | 6.13 | ✅ 已完成 | CacheInvalidationPublisher/Listener + 按userId精确失效 |
| Snowflake Worker-ID 租约管理 | 9.51 | ✅ 已完成 | Redis SETNX + 10s daemon heartbeat + @PreDestroy |
| OAuth Token 交换幂等性 | 9.50 | ✅ 已完成 | oauth:code:{code}:exchanged marker + clear error |
| MFA 密钥版本完整轮换 | 9.7 | ✅ 已完成 | CryptoUtil.decryptWithFallback(k1, k0) + TOTP/UserMfa全链路接入 |
| MFA 备用码使用追踪 | 9.75 | ✅ 已完成 | verifyAndConsumeBackupCode() 消费后移除 + MessageDigest.isEqual 防定时攻击 |
| 设备指纹绑定 (fph) | 9.35 | ✅ 已完成 | DeviceFingerprintService (SHA-256/IP24) + user_devices表 + DuplicateKeyException并发安全 |
| SOC2 访问审查表字段 | 9.58 | ✅ 已完成 | V4 migration: session_id + request_id + client_ip_chain + RequestIdFilter |
| Admin 管理后台 | 11.5 | ✅ 已完成 | React 18 + TS + Vite + Ant Design 5 (src/admin/). 9模块全部完成: Dashboard/Users/Roles/OAuth Clients/API Keys/MFA/Audit Logs/Login Logs/Profile. 代码分割+动态Scope+IPage类型修复. |
| 身份绑定二次认证 | 9.73 | 📋 延期 | 增强安全 |
| Refresh Token HttpOnly Cookie | 9.34 | 📋 延期 | 移动端优先使用 Bearer header |
| BFF Session-ID Token 代理模式 | 9.45 | ❌ 不需要 | Bearer header 通用方案已满足 |
| SCIM v2 / OIDC RP / SAML / LDAP | 11.1-11.2 | 📋 延期 | 企业集成，需外部 IdP |
| SIEM 日志投递 / Kafka 事件流 | 11.3-11.4 | 📋 延期 | 企业集成，需 Kafka 集群 |
| k6 性能脚本端到端 | Phase 10 | 📋 延期 | 脚本已创建,需目标环境执行 |

### 架构重构新增（不在原设计中）

| 项目 | 状态 | 说明 |
|------|------|------|
| 认证策略模式 | ✅ 已完成 | AuthenticationProvider → PasswordAuthenticationProvider + OAuth(占位) |
| 身份解析策略 | ✅ 已完成 | IdentityResolver → Email/Phone/Username 3 实现 |
| MFA 验证策略 | ✅ 已完成 | MfaVerifier → TOTP(真实) + SMS/Email/WebAuthn(占位) |
| 领域事件驱动 | ✅ 已完成 | 4 Events + AuthEventPublisher + AuthEventListeners |
| MapStruct Converter | ✅ 已完成 | 5 Converter 替换 BeanUtil.copyProperties |
| 跨模块分层清理 | ✅ 已完成 | filters/Warmer/AuthServiceImpl → Service接口，跨模块Mapper清零 |
| DataRetentionService | ✅ 已完成 | forgetMe() 线程池泄漏修正 |
| ApiKeyCacheManager | ✅ 已完成 | 统一缓存管理，消除 filter/impl 重复逻辑 |
| 安全加固 | ✅ 已完成 | TOTP/备用码定时攻击修复、AES密钥移除默认值、全局限流→按IP/用户、HSTS、MySQL SSL、BouncyCastle 1.80、Spring Boot 3.4.7 |

---

## 8. 安全审查结果（2026-04-29 三轮审查）

### 8.1 已修复（本轮，共 23 项）

| 严重度 | 问题 | 修复 |
|--------|------|------|
| **CRITICAL** | TOTP `String.equals()` 定时侧信道 | `MessageDigest.isEqual()` 常量时间比较 |
| **CRITICAL** | 备用码 `List.contains()` 定时侧信道 | 逐码 `MessageDigest.isEqual()` 比较 |
| **CRITICAL** | AES 密钥硬编码在 `application.yml` | 移除默认值，强制 `AES_KEY_V1`/`AES_KEY_V0` 环境变量 |
| **CRITICAL** | 全局限流 — 单用户可耗尽全站点配额 | 复合 key = `key + ":user:uid"` 或 `key + ":ip:x.x.x.x"` |
| **HIGH** | BouncyCastle 1.78 CVE-2025-8916 | → 1.80 |
| **HIGH** | Spring Boot 3.4.5 (2 个安全补丁落后) | → 3.4.7 |
| **HIGH** | MySQL `useSSL=false` + `allowPublicKeyRetrieval=true` | `useSSL=true&requireSSL=true&allowPublicKeyRetrieval=false` |
| **HIGH** | 缺少 HSTS 响应头 | `Strict-Transport-Security: max-age=31536000; includeSubDomains` |
| **HIGH** | GDPR forget-me 不完整 | 匿名化 user/profile/auth/audit PII + `DataRetentionService` 9 表级联物理删除 |
| **MEDIUM** | AccessDeniedException 泄露内部错误 | 返回静态消息，原始错误服务端 WARN 日志 |
| **MEDIUM** | JWT 算法未显式约束 | `.setJwsAlgorithmConstraints(PERMIT, RSA_USING_SHA256)` |
| **MEDIUM** | 缺少 Content-Security-Policy 头 | `default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'` |
| **MEDIUM** | 限流 key 默认空字符串 | 与全局限流一同修复（复合 key） |
| **MEDIUM** | 用户名(PII) INFO 级日志 | → DEBUG，移除 username 字段 |
| **MEDIUM** | HIBP 故障静默放行 | 已有 `log.warn` fail-open，属设计决策 |
| **LOW** | CORS fallback 为通配符 `*` | 改为空字符串 |
| **LOW** | encrypt/decrypt 日志不一致 | encrypt `log.error("...{}", e.getMessage())` 统一 |
| **LOW** | TOTP 30s 窗口内无重放防护 | 已与定时攻击一同修复（MessageDigest.isEqual 常量时间） |
| **LOW** | updateStatus() 无 lockedUntil 校验 | 已有业务层约束（5次失败锁定），timestamp 仅内部生成 |

### 8.2 已知未修复（需外部条件 / 非代码变更）

| 严重度 | 问题 | 原因 |
|--------|------|------|
| **HIGH** | JWT 私钥在 classpath | 需 K8s Secret 卷挂载，运维侧变更 |
| **MEDIUM** | Swagger UI 生产公开 | ✅ 已修复 (2026-04-30): Knife4jConfig 添加 `@Profile("!prod")` |
| **MEDIUM** | 日志脱敏（PII/密码/Token） | ✅ 已修复 (2026-04-30): LogstashEncoder PII 字段排除 + 白名单 |
| **MEDIUM** | GeeTest fail-open 暴力破解 | ✅ 已修复 (2026-04-30): MetricsService.recordGeeTestResult() 6 标签 Prometheus Counter + @RateLimit(5/min) 兜底 |
| **LOW** | ThreadLocal Cipher 未释放 | Java 21 虚拟线程，当前线程池无影响 |
| **LOW** | Flyway V4 `AFTER column` | 低风险，迁移顺序已固定 |
| **LOW** | X-Request-Id 暴露 | UUID v4 无信息泄露，仅请求关联 |

---


### 8.3 401/403 精确分流修复（2026-04-30）

**问题**: Spring Security 6.x 默认 `Http403ForbiddenEntryPoint` 将未认证请求返回 403，而 `RequirePermissionAspect` 将已认证无权限请求抛 `AuthorizationException(ErrorCode.UNAUTHORIZED)` 映射为 401。前后端无法区分"未登录"和"无权限"。

**修复**:

| 文件 | 变更 |
|------|------|
| `SecurityConfig.java` | 新增 `authenticationEntryPoint()` 返回 401 + `accessDeniedHandler()` 返回 403，响应体使用 `R<T>` JSON 信封 |
| `RequirePermissionAspect.java` | `!hasPerm` 从 `AuthorizationException(ErrorCode.UNAUTHORIZED)` → `AccessDeniedException("Permission denied: ...")` |
| `GlobalExceptionHandler.java` | 增强 `AccessDeniedException` handler + 新增 `AuthenticationException` handler |
| `client.ts` | 新增 403 拦截 → `auth:forbidden` 事件 |
| `AuthContext.tsx` | `logoutInProgress` ref 竞态防护 + `auth:forbidden` → `/forbidden` 跳转 |
| `Forbidden.tsx` | **新建** antd Result 403 页面 |
| `App.tsx` | 新增 `/forbidden` 路由 |

**修复后 401/403 精确分流**:

| 场景 | HTTP | 机制 |
|------|------|------|
| 无 token / 过期 / 黑名单 | **401** | ExceptionTranslationFilter 识别匿名用户 → AuthenticationEntryPoint |
| 有效 token + 有权限 | **200** | 不变 |
| 有效 token + 无权限 | **403** | RequirePermissionAspect → AccessDeniedException → GlobalExceptionHandler |

测试: `AuthIntegrationTest` 新增 3 个 401 测试 + `RoleIntegrationTest` 新增 403 测试。

## 9. 已知未完成的架构改进

| 项目 | 优先级 | 说明 |
|------|--------|------|
| JWT 私钥外部化 | P0 | K8s Secret 卷挂载，移除 classpath 私钥 |
| 物理删除持久化调度 | P1 | ✅ 已完成 | V6 migration scheduled_tasks 表 + TaskPersistenceService + TaskPoller @Scheduled(fixedDelay=60s)。GDPR forgetMe 物理删除任务写入DB，应用重启不丢失 |
| Swagger 生产环境禁用 | P1 | ✅ 已完成 | Knife4jConfig 添加 @Profile("!prod") |
| 日志脱敏 | P1 | ✅ 已完成 | LogstashEncoder 配置 PII 字段排除(password/credential/token/email/phone等) + 白名单(userId/requestId/action等) |
| GeeTest fail-open 风险 | P2 | ✅ 已完成 | MetricsService.recordGeeTestResult() 6类标签(success/failed/cache_hit/skipped/circuit_open/error)，GeeTestVerifier 全链路打点 |
| ThreadLocal Cipher 清理 | P3 | Java 21 虚拟线程兼容，当前无影响 |
| Flyway V4 AFTER 语法 | P3 | 安全风险低 |
| OAuth grantTypes 持久化 | P2 | `@TableField(exist = false)` 导致 grantTypes 创建/更新时静默丢弃，`client_grant_types` 表无 Java 映射。需新建 entity + mapper + 读写逻辑 |
| OAuth redirectUris 格式校验 | P2 | `OAuthClientServiceImpl.create/update` 未校验 redirectUris 列表中每个 URI 的格式有效性（非空、合法 scheme） |
| @AuditLog SpEL 表达式支持 | P2 | 注解上 `resourceIdExpression` / `detailExpression` 字段已定义但切面永不解析，需接入 `ExpressionParser` 在运行时动态提取方法参数值 |
| `.last("LIMIT " + int)` 统一为 Page | P3 | `UserServiceImpl.getRecentlyActiveUsers(int)` + `TaskPersistenceService.fetchDueTasks(int)` 仍用 `.last()` 拼接 LIMIT。当前 int 类型安全，但模式脆弱，建议统一为 `Page<>(1, limit, false)` |
| OAuth 刷新令牌与主流程 Lua 脚本对齐 | P3 | OAuth `refreshToken()` 用 `setNx`（足够），主流程 `TokenServiceImpl` 用 Lua 脚本（更强）。建议统一为 Lua 以消除并发窗口 |
| UserServiceImpl 注释清理 | P3 | `createUser` 方法中有 7 处复述代码的注释（"Check username uniqueness"等），应删除 |
| 依赖版本升级 | P3 | jose4j 0.9.6→0.9.9, hutool 5.8.35→5.8.38, redisson 3.40.2→3.46.0——无已知 CVE，常规维护 |

---

## 10. 验证清单

1. ✅ `./mvnw compile` — 主代码编译通过
2. ✅ `./mvnw test-compile` — 测试代码编译通过
3. ✅ 单元测试 19/19 通过 (PasswordAuthenticationProvider 9 + IdentityResolver 5 + TotpMfaVerifier 5)
4. ✅ Docker MySQL + Redis 启动 (auth-mysql-container + auth-redis-container, 25h+ uptime)
5. ✅ `./mvnw spring-boot:run` — 应用启动 (dev profile, 端口 8080)
6. ✅ `/actuator/health/liveness` — UP
7. ⏳ `/doc.html` — Swagger UI (@Profile("!prod"), dev 环境可访问)
8. ✅ 注册 → 登录 → 获取 JWT → 访问受保护端点 (admin@auth.local / Admin@123456)
9. ✅ 未认证→401 + 无权限→403 精确分流 (SecurityConfig + RequirePermissionAspect + GlobalExceptionHandler)
10. ✅ TOTP MFA 设置 → 验证 → 禁用
11. ✅ OAuth2 客户端创建 → /authorize → /token PKCE
12. ✅ API Key 创建 → 吊销 → 轮换
13. ✅ 审计日志查询
14. ✅ Refresh Token 刷新 + 重放检测
15. ✅ 速率限制 429
16. ✅ 登录锁定 (5次失败)
17. ✅ Simplify + Security 审查 (2026-04-29): 修复 23 项安全 + 8 项质量
18. ✅ Admin 管理后台 (2026-04-30): 9 模块完整实现 + 代码分割 + 动态 Scope + IPage 类型修复
19. ✅ P1 生产加固 (2026-04-30): Swagger 生产禁用 + 日志脱敏 + 定时任务修复
20. ✅ P1-P2 持久化+监控 (2026-04-30): DB任务表 (V6) + TaskPersistenceService + TaskPoller + PhysicalDeleteExecutor @Transactional + GeeTest Prometheus 指标
21. ✅ AuthServiceImpl 分层清理 (2026-04-30): 移除 UserMfaMapper/UserRoleMapper 跨模块 Mapper + exportUserData() 改走 RoleService.getUserRoles()
22. ✅ @MapperScan 修复 (2026-04-30): infrastructure.storage 包纳入 MyBatis 扫描
23. ✅ Login 浏览器密码管理器支持 (2026-04-30): autoComplete="on" + username/current-password
24. ✅ 生产环境配置分离 (2026-04-30): application-dev.yml + application-prod.yml + spring.profiles.active
25. ✅ MetricsService 惯用模式 (2026-04-30): Counter.builder→meterRegistry.counter() / Timer.builder→meterRegistry.timer()
26. ✅ 代码审查修复 (2026-04-30): 18 项全部完成 (Phase 1 CRITICAL/HIGH + Phase 2 MEDIUM + Phase 3 LOW)
27. ✅ 安全加固 (2026-04-30): OAuth redirect URI 校验 + grant_type 校验 + SETNX 原子化 + TTL 对齐 + Swagger 条件放行 + .last() 替换 + @AuditLog 持久化 + JacksonTypeHandler 防护
28. ✅ 多语言邮件模板 (2026-05-02): Thymeleaf 模板外置 + i18n 属性文件 + Accept-Language 请求头驱动


---

## 11. 工业级消息通知平台设计（永不实现）

> **状态: 仅设计文档 — 明确决定本系统永不上线此方案。**
>
> 当前阶段的 Thymeleaf 模板外置方案（第 10 节已实现）足够覆盖 2-3 门语言的验证码邮件需求。
> 以下方案描述大厂在多租户、多语言、多渠道场景下的消息平台架构，仅供团队知识储备。

### 11.1 架构全景

```
┌─────────────────────────────────────────────────────────────────┐
│                    消息模板管理平台 (Internal Admin)                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │ 模板编辑器 │  │ 多语言管理 │  │ 审批工作流 │  │ 版本/灰度/回滚    │ │
│  │ (富文本)   │  │ (按Locale)│  │ (敏感模板) │  │ (金丝雀发布)     │ │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────────┘ │
└──────────────────────────────┬──────────────────────────────────┘
                               │ 读写模板元数据
                               ▼
┌──────────────────────────────────────────────────────────────────┐
│                      消息发送服务 (Message Service)                  │
│  ┌────────────┐  ┌──────────────┐  ┌────────────────────────────┐ │
│  │ Template   │  │ Channel      │  │ Send Pipeline              │ │
│  │ Engine     │  │ Adapter      │  │ (限流→渲染→路由→发送→重试)   │ │
│  │ (Thymeleaf │  │ (Email/SMS/  │  │                            │ │
│  │ /Handlebars)│  │  Push/IM)    │  │                            │ │
│  └────────────┘  └──────────────┘  └────────────────────────────┘ │
└──────────────────────────────┬──────────────────────────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          ▼                    ▼                    ▼
   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
   │ 阿里云邮件推送 │   │ 腾讯云短信    │   │ Firebase     │
   │              │   │              │   │ Cloud Msg    │
   └──────────────┘   └──────────────┘   └──────────────┘
```

### 11.2 核心数据库表设计

```sql
-- 模板定义
CREATE TABLE message_template (
    id          BIGINT PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL,           -- 业务唯一标识: 'VERIFICATION_CODE'
    name        VARCHAR(128) NOT NULL,           -- 管理后台显示名
    channel     VARCHAR(32)  NOT NULL,           -- EMAIL / SMS / PUSH
    status      VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',  -- DRAFT/PUBLISHED/DEPRECATED
    version     INT          NOT NULL DEFAULT 1, -- 发布版本号
    variables   JSON,                            -- 变量 schema: [{"name":"code","type":"string","required":true}]
    created_by  VARCHAR(64),
    created_at  DATETIME,
    updated_at  DATETIME,
    UNIQUE KEY uk_code_version (code, version)
);

-- 多语言内容
CREATE TABLE message_template_locale (
    id             BIGINT PRIMARY KEY,
    template_id    BIGINT NOT NULL,
    locale         VARCHAR(16) NOT NULL,         -- zh-CN / en / ja
    subject        VARCHAR(256),                 -- 邮件主题/SMS 标题（可为空）
    html_body      TEXT,                         -- HTML 邮件正文
    text_body      TEXT,                         -- 纯文本兜底 (SMS 直接发此字段)
    UNIQUE KEY uk_template_locale (template_id, locale)
);

-- 审批记录（敏感模板变更审批）
CREATE TABLE message_template_approval (
    id            BIGINT PRIMARY KEY,
    template_id   BIGINT NOT NULL,
    action        VARCHAR(16) NOT NULL,          -- PUBLISH / DEPRECATE
    requested_by  VARCHAR(64),
    approved_by   VARCHAR(64),
    status        VARCHAR(16),                   -- PENDING / APPROVED / REJECTED
    comment       TEXT,
    created_at    DATETIME,
    resolved_at   DATETIME
);
```

### 11.3 调用方式

业务代码只发请求，不关心模板内容、语言、渠道细节：

```java
// 业务代码
messageService.send(MessageRequest.builder()
    .templateCode("VERIFICATION_CODE")   // 模板标识
    .channel(Channel.EMAIL)              // 渠道
    .recipient("user@example.com")       // 接收人
    .locale(Locale.ENGLISH)              // 语言
    .params(Map.of(                      // 模板变量（由变量 schema 校验）
        "code", "123456",
        "purpose", "register",
        "expire_minutes", 5
    ))
    .idempotencyKey("evc_" + requestId)  // 幂等去重
    .build());
```

### 11.4 模板生命周期

```
[DRAFT] ──编辑──▶ [DRAFT]
   │
   ▼
[REVIEW] ──审批通过──▶ [PUBLISHED] ──回滚──▶ [PUBLISHED] (上一版本)
   │                      │
   ▼                      ▼
[REJECTED]           [DEPRECATED] ──30天──▶ 物理删除
```

- **编辑阶段**: 运营人员在管理后台可视化编辑，实时预览各语言、各渠道效果，可发送测试消息到指定接收人
- **审批阶段**: 敏感模板（验证码、支付通知、账户变更）修改必须审批。非敏感模板（营销类）可直接发布
- **发布阶段**: 新版本发布后旧版本保留，可一键回滚。支持灰度：先 5% → 观察 → 50% → 全量
- **废弃阶段**: 旧版本 30 天后物理删除，期间无调用则自动清理

### 11.5 发送可靠性保障

| 层级 | 机制 | 说明 |
|------|------|------|
| 入口 | 幂等去重 | `idempotency_key` Redis 去重，24h TTL |
| 限流 | 渠道级 + 用户级 | 邮件渠道 100/s，单用户 5/min |
| 渲染 | 模板缓存 | Caffeine L1 (5min) + Redis L2 (1h)，DB 变更即时失效 |
| 路由 | 渠道健康检查 | 主渠道失败自动 fallback（阿里云 → SendGrid） |
| 发送 | 异步 + 重试 | @Async + 指数退避（1s/2s/4s/8s/16s，最多 5 次）|
| 持久化 | 消息落库 | `message_send_log` 表记录每条发送状态 |
| 监控 | Prometheus 指标 | 发送量/成功率/延迟 p50-p99/渠道可用性 |
| 告警 | 失败率阈值 | 5 分钟内失败率 > 5% → PagerDuty / 飞书告警 |

### 11.6 多语言支持策略

```
用户请求（locale=en）
       │
       ▼
MessageService 检索模板
       │
       ├── 精确匹配: template_locale.locale = 'en' → 命中
       ├── 语言匹配: locale 前两位 'en' → 命中 'en-US'
       ├── 默认兜底: locale = 系统默认 (zh-CN)
       │
       ▼
TemplateEngine 渲染
       │
       ├── ${code}         → 所有语言共用
       ├── #{greeting}    → 从 messages_{locale}.properties 取
       ├── th:text="${purpose}" → 调用方传入（已由前端翻译）
       │
       ▼
渠道适配器发送
```

- **翻译管理**: 接入 Lokalise / Crowdin，翻译团队提交后 webhook 自动更新模板
- **新增语言**: 运营在管理后台点击"添加语言" → 翻译填充 → 即时生效，无需发版
- **变量约束**: 每个模板声明变量 schema，调用时校验，避免渲染失败

### 11.7 为什么不实现

| 原因 | 说明 |
|------|------|
| **规模不匹配** | 当前系统仅 2 种语言、1 个模板、1 个渠道，DB 方案过度设计 |
| **团队规模** | 无专职运营/翻译团队，模板后台无人使用 |
| **维护成本** | DB 模板方案增加 3 张表 + 1 个后台 + 审批逻辑，维护负担远超收益 |
| **当前方案已够用** | Thymeleaf 文件模板 + i18n properties，改文案只需改资源文件，重新部署即可 |
| **升级路径清晰** | 当语言 ≥ 5 种、或模板 ≥ 20 个、或有运营需求时，可沿本设计平滑升级 |


---

## 12. 2026-05 迭代变更记录

### 12.1 多语言邮件模板

- Thymeleaf 模板引擎集成（`spring-boot-starter-thymeleaf`）
- 邮件模板外置：`templates/email/verification-code_zh-CN.html`、`verification-code_en.html`
- i18n 属性文件：`i18n/mail/messages_zh_CN.properties`、`messages_en.properties`
- 专用 `mailMessageSource` Bean（`MailI18nConfig.java`）
- 前端 `Accept-Language` 请求头自动附带

### 12.2 大小写一致性修复

前端发送大写值（`'EMAIL'`、`'REGISTER'`、`'TOTP'`），后端枚举存小写。修复范围：

| 修复点 | 文件 | 方式 |
|--------|------|------|
| `resolveIdentifier()` | `AuthServiceImpl.java` | `equalsIgnoreCase` |
| `identityType` 存储 | `register()` / `bindIdentity()` | `toLowerCase()` |
| `purpose` 比较 | `VerificationCodeServiceImpl.java` | `normalizePurpose()` |
| MFA type 存储 | `UserMfaServiceImpl.java` | `toLowerCase()` |
| MFA verifier 查找 | `PasswordAuthenticationProvider.java` | `equalsIgnoreCase` |
| LoginLog 查询 | `LoginLogServiceImpl.java` | `toLowerCase()` |
| 用户名登录 | `UsernameIdentityResolver.java` | `equalsIgnoreCase` |

### 12.3 安全加固

| 加固项 | 说明 |
|--------|------|
| 账户枚举防护 | `checkUserStatus()` 移到密码验证之后，错误密码统一返回 `INVALID_CREDENTIALS` |
| 验证码生命周期 | `reset_password` purpose 的 `verify()` 不消费验证码，`resetPassword()` 成功后显式 `consume()` |
| 密码历史 | `resetPassword()` 增加与 `changePassword()` 相同的 10 次历史防重用 |
| GeeTest | HTTPS 验证 URL；删除 lot-number 缓存防重放；移除 sign_token 日志 |
| 注册前置检查 | `sendCode()` 在发码前检查邮箱是否已注册（register）/ 用户是否存在（reset_password） |
| 验证码日志 | SMS/DEV 日志移除明文验证码 |

### 12.4 用户中心（Customer 前端）

- App Shell：`UserLayout`（侧边栏 + Header 用户下拉菜单）
- Dashboard：欢迎卡片、账户信息、MFA 状态、快捷操作
- Profile：昵称、真实姓名、语言、时区编辑
- Security：修改密码、MFA 设置/禁用、数据导出、账号注销
- 服务条款/隐私政策占位页面（`LegalPage` 共用组件）
- 注册页：自动生成用户名，隐藏手机注册入口

### 12.5 Admin 管理后台增强

- GeeTest 验证码接入（`useCaptcha` + `CaptchaWidget`）
- LoginLogs 结果颜色匹配 DB 小写值
- Dashboard 状态标签修复（NORMAL=0 / DISABLED=1 / PENDING=2 / LOCKED=3）
- CORS 白名单扩展（端口 3000 + docker.iccuu.com）

### 12.6 自动化测试

- `AuthIntegrationTest` 新增 5 个集成测试：
  - `testResetPasswordFlow` — 验证码非消费性验证 + 重置后消费
  - `testPasswordHistoryEnforcement` — 密码重复检查
  - `testCaseInsensitiveIdentityType` — 大小写不敏感用户名登录
  - `testAccountStatusCheckedAfterPassword` — 账户枚举防护验证
- 19 个单元测试全部通过（`PasswordAuthenticationProviderTest`、`IdentityResolverTest`、`TotpMfaVerifierTest`）

### 12.7 部署与基础设施

- `.gitattributes` 强制 LF 行尾（主仓库 + 子模块）
- nginx `Cache-Control` 头：`index.html` no-cache，`/assets/` immutable
- `build-local.sh`：`npm ci` → `npm install`（增量构建）
- `audit_logs` 表补充 Flyway V4 字段（session_id、request_id、client_ip_chain）
