# 认证授权系统 — 后端接口设计方案

> 最后更新: 2026-04-27 | 状态: Phase 1-10 核心实现完成

## 1. 概述

- **项目**: `src/backend/general-web-backend`（Spring Boot 3.4.5, Java 21, Maven）
- **数据库**: MySQL 8.0 14 表 + Redis 7.x
- **代码层级**: Controller → Service(Interface+Impl) → Mapper → DB，严格分层
- **API 前缀**: `/api/v1`
- **API 文档**: springdoc-openapi，运行时访问 `http://localhost:8080/doc.html`
- **总文件数**: ~175 Java + 15 XML + Flyway V1-V3

---

## 2. 实际依赖（与设计对照）

| 依赖 | 设计版本 | 实际版本 | 说明 |
|------|---------|---------|------|
| spring-boot-starter-parent | 3.4.x GA | **3.4.5** | ✅ |
| mybatis-plus | 3.5.9 | **3.5.9** | ✅ |
| hutool-all | 5.8.35 | **5.8.35** | ✅ |
| jose4j | 0.9.8 | **0.9.6** | ⚠️ Maven Central 最新可用版本 |
| knife4j | 4.5.0 | **4.5.0** + springdoc 2.7.0 | ✅ 排除内置 springdoc |
| mapstruct | 1.6.3 | **1.6.3** | ✅ |
| redisson | 3.40.2 | **3.40.2** | ✅ |
| zxing | 3.5.5 | **3.5.4** | ⚠️ Maven Central 最新可用版本 |
| resilience4j | 2.2.0 | **2.2.0** | ✅ 配置存在,未接线 |
| caffeine | - | **内置(spring-boot-starter)** | ✅ 新增 L1 缓存 |
| micrometer-registry-prometheus | - | ✅ | ✅ 新增可观测性 |
| bouncycastle bcprov+bcpkix | 1.78 | **1.78** | ✅ Hutool PemUtil 依赖 |
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
│   │   ├── SecurityConfig.java            — SecurityFilterChain, BCryptPasswordEncoder(12)
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
│   │   └── GlobalExceptionHandler.java    — @RestControllerAdvice
│   ├── result/
│   │   ├── R.java                         — 统一响应: code/message/data/timestamp
│   │   ├── PageResult.java                — 分页响应
│   │   ├── PageQuery.java                 — 分页查询基类
│   │   └── FieldError.java                — 校验错误字段
│   ├── aspect/
│   │   ├── AuditLogAspect.java            — @Around @AuditLog
│   │   ├── RateLimitAspect.java           — @Around @RateLimit (Redis Lua)
│   │   └── RequirePermissionAspect.java   — @Around @RequirePermission (Caffeine L1 + Redis L2)
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
│       └── ApiKeyAuthenticationFilter.java— OncePerRequestFilter: SHA-256 hash, DB lookup
│
├── infrastructure/
│   ├── snowflake/
│   │   └── SnowflakeIdGenerator.java      — Hutool Snowflake
│   ├── security/
│   │   ├── JwtTokenProvider.java          — jose4j RS256 sign/verify, getJwks()
│   │   ├── PermissionResolver.java        — Interface
│   │   ├── RateLimiterService.java        — Redis Lua sliding window
│   │   └── HibpService.java               — HIBP k-anonymity 密码泄露检测
│   ├── audit/
│   │   ├── AuditEventPublisher.java       — Spring event + @Async listener
│   │   └── AuditEvent.java
│   ├── geetest/
│   │   ├── GeeTestProperties.java
│   │   └── GeeTestVerifier.java           — HMAC-MD5 sign, Redis 缓存
│   ├── metrics/
│   │   └── MetricsService.java            — Prometheus Counter/Timer 业务指标
│   ├── cache/
│   │   └── CacheWarmer.java               — 启动后预热 100 活跃用户权限
│   └── storage/
│       ├── PartitionMaintenanceScheduler.java   — 每月分区维护
│       └── DataRetentionScheduler.java          — 90天软删除+12月分区清理
│
└── module/
    ├── auth/
    │   ├── controller/ (AuthController, JWKSController)
    │   ├── service/ (AuthService, TokenService, VerificationCodeService, LoginLogService + impl)
    │   ├── dto/ (LoginRequest, LoginResponse, RegisterRequest, RefreshTokenRequest, ChangePasswordRequest, ResetPasswordRequest, SendCodeRequest, VerifyCodeRequest, TokenValidationResponse)
    │   ├── entity/ (LoginLog)
    │   └── mapper/ (LoginLogMapper + XML)
    │
    ├── user/
    │   ├── controller/UserController.java
    │   ├── service/ (UserService, UserProfileService, UserCredentialsHistoryService + impl)
    │   ├── dto/ (10 DTO: UserCreateRequest, UserUpdateRequest, UserQueryRequest, UserVO, UserStatusRequest, UserProfileUpdateRequest, UserProfileVO, UserAuthVO, BindAuthRequest)
    │   ├── entity/ (User, UserProfile, UserAuth, UserCredentialsHistory)
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

**与设计的主要差异**:
- Converter 目录存在但为空 — 使用 `BeanUtil.copyProperties` 替代 MapStruct
- Strategy 模式未实现 — AuthServiceImpl 直接处理多身份，未拆分为策略类
- `SecurityContextHelper` 合并至 `SecurityUtil`
- `JwtUtil` 合并至 `JwtTokenProvider.getClaimString()`

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

## 7. 已延期至 Phase 11+ 的功能

以下功能在设计文档中定义，但明确延期到后续迭代:

| 功能 | 章节 | 原因 |
|------|------|------|
| 自定义 SqlInjector (NOW_MILLIS 软删除) | 6.1 | 标准 @TableLogic 足够 |
| RBAC 递归 CTE | 9.18 | Java 循环对浅层级足够 |
| GeeTest Resilience4j @CircuitBreaker | 9.44 | 手动 try/catch 已满足 |
| API Key Redis 缓存 `apikey:{hash}` | 9.17 | DB 查询足够 |
| Redis pub/sub 缓存失效 | 6.13 | 单实例部署 |
| Snowflake Worker-ID 租约管理 | 9.51 | K8s StatefulSet 副本序号方案 |
| OAuth Token 交换幂等性 | 9.50 | 低频操作 |
| MFA 密钥版本完整轮换 | 9.7 | 当前单密钥生产可用 |
| Refresh Token HttpOnly Cookie | 9.34 | 移动端优先使用 Bearer header |
| 设备指纹绑定 (fph) | 9.35 | AAL3 合规增强 |
| BFF Session-ID Token 代理模式 | 9.45 | Bearer header 通用方案已满足 |
| SOC2 访问审查表字段 | 9.58 | 合规增强 |
| 身份绑定二次认证 | 9.73 | 增强安全 |
| MFA 备用码使用追踪 | 9.75 | 增强安全 |
| SCIM v2 / OIDC RP / SAML / LDAP | 11.1-11.2 | 企业集成 |
| SIEM 日志投递 / Kafka 事件流 | 11.3-11.4 | 企业集成 |
| Admin 管理后台 | 11.5 | 独立 SPA 项目 |
| k6 性能脚本端到端 | Phase 10 | 脚本已创建,需目标环境执行 |

---

## 8. 验证清单

1. ✅ `./mvnw compile` — 主代码编译通过
2. ✅ `./mvnw test-compile` — 测试代码编译通过
3. ✅ Docker MySQL + Redis 启动
4. ✅ `./mvnw spring-boot:run` — 应用启动 (9.3s)
5. ✅ `/actuator/health/liveness` — UP
6. ✅ `/doc.html` — Swagger UI 全部端点
7. ✅ 注册 → 激活 → 登录 → 获取 JWT → 访问受保护端点
8. ✅ 无权限 → 403 (SecurityConfig + @RequirePermission)
9. ✅ TOTP MFA 设置 → 验证 → 禁用
10. ✅ OAuth2 客户端创建 → /authorize → /token PKCE
11. ✅ API Key 创建 → 吊销 → 轮换
12. ✅ 审计日志查询
13. ✅ Refresh Token 刷新 + 重放检测
14. ✅ 速率限制 429
15. ✅ 登录锁定 (5次失败)
