# 后端接口分批实施计划

> 基于 DESIGN.md，逐文件列出待创建/修改内容。每批完成后可独立编译验证。

---

## 总体统计

| 类型 | 数量 |
|------|------|
| 配置文件 | 3 (pom.xml, application.yml, logback-spring.xml) |
| 实体类 (entity) | 14 |
| Mapper 接口 + XML | 14+14 |
| 枚举 (enums) | 9 |
| 异常类 | 6 |
| 配置类 (config) | 7 |
| 工具类 (util) | 6 |
| Filter | 2 |
| DTO | 45+ |
| Service 接口 + Impl | 12+12 |
| Controller | 8 |
| Converter (MapStruct) | 6 |
| Aspect | 3 |
| Annotation | 3 |
| infrastructure | 8 |
| **总计** | **~170 文件** |

---

## 第 0 批：项目初始化 (2 文件)

### 0.1 pom.xml
**路径**: `pom.xml`
**操作**: 替换 — 加入全部 19 个依赖
**关键依赖**: web, security, validation, data-redis, aop, actuator, mail, mybatis-plus 3.5.9, mysql-connector-j, redisson 3.40.2, hutool 5.8.35, jose4j 0.9.8, knife4j 4.5.0, mapstruct 1.6.3, lombok, zxing 3.5.5, caffeine, resilience4j, micrometer-registry-prometheus
**验证**: `./mvnw dependency:resolve` 无冲突

### 0.2 application.yml
**路径**: `src/main/resources/application.yml`
**包含**: datasource(含readonly), redis, mail, mybatis-plus, jwt, snowflake, geetest(login+register), crypto(aes-keys v0/v1), server.shutdown, app.cors, app.cache.warmup, management(health+prometheus), resilience4j, spring.task.execution, spring.servlet.multipart, spring.flyway
**验证**: `./mvnw validate` 属性绑定正确

---

## 第 1 批：common 基础层 (22 文件)

### 1.1 枚举 (9 files)
| 文件 | 路径 |
|------|------|
| UserStatus.java | common/enums/ |
| IdentityType.java | common/enums/ |
| MfaType.java | common/enums/ |
| AuthMethod.java | common/enums/ |
| LoginResult.java | common/enums/ |
| ScopeType.java | common/enums/ |
| ClientType.java | common/enums/ |
| GrantType.java | common/enums/ (不含 implicit) |
| ApiKeyStatus.java | common/enums/ |

### 1.2 异常 (6 files)
| 文件 | 父类 | HTTP 状态 |
|------|------|-----------|
| BusinessException.java | RuntimeException | 400 |
| AuthenticationException.java | BusinessException | 401 |
| AuthorizationException.java | BusinessException | 403 |
| ResourceNotFoundException.java | BusinessException | 404 |
| RateLimitException.java | BusinessException | 429 |
| DuplicateResourceException.java | BusinessException | 409 |

### 1.3 统一响应 (2 files)
| 文件 | 要点 |
|------|------|
| R.java | `code`, `message`, `data`, `timestamp`; static `ok()`, `fail()`, `page()` |
| PageResult.java | extends R: `total`, `page`, `size`, `list` |
| PageQuery.java | `page=1`, `size=20`(max 100), `sortBy`, `sortOrder` |

### 1.4 ErrorCode (1 file)
**路径**: `common/constant/ErrorCode.java`
**分段**: `0` 成功, `10xxx` 认证, `11xxx` 参数校验, `40xxx` 业务, `50xxx` 系统

### 1.5 全局异常处理 (1 file)
**路径**: `common/handler/GlobalExceptionHandler.java`
**处理**: BusinessException(含子类), MethodArgumentNotValidException(422), BindException, AccessDeniedException, HttpMessageNotReadableException, ConstraintViolationException, Exception(500 兜底)

### 1.6 常量 (2 files)
| 文件 | 内容 |
|------|------|
| Constants.java | 通用常量 |
| RedisKeyPrefix.java | Redis key 模板字符串 |

**验证**: `./mvnw compile` 通过

---

## 第 2 批：配置类 (7 files)

### 2.1 MyBatisPlusConfig.java
- Snowflake `IdentifierGenerator` (Hutool Snowflake, datacenterId + workerId 从配置)
- `MybatisPlusInterceptor` + `PaginationInnerInterceptor`
- 自定义 `DeletedAtSqlInjector` (重写逻辑删除 SQL 为 `deleted_at = UNIX_TIMESTAMP(NOW(3))*1000`)

### 2.2 SecurityConfig.java
- `SecurityFilterChain`: 初始 permit-all
- `BCryptPasswordEncoder(12)` bean
- `AuthenticationManager` bean

### 2.3 RedisConfig.java
- `RedisTemplate<String, Object>` (Jackson2JsonRedisSerializer)
- `RedissonClient` (单机模式，配置从 application.yml)

### 2.4 AsyncConfig.java
- `@EnableAsync`
- ThreadPoolTaskExecutor: core=4, max=8, queue=1000, CALLER_RUNS

### 2.5 GeeTestConfig.java
- `GeeTestProperties` `@ConfigurationProperties("geetest")`
- GeeTestVerifier bean (RestTemplate)

### 2.6 WebMvcConfig.java
- CORS: `/api/**`, allowed-origins 从配置，allowCredentials=true
- Jackson 配置: `PropertyNamingStrategies.SNAKE_CASE`, 时区 Asia/Shanghai
- 安全响应头

### 2.7 Knife4jConfig.java
- OpenAPI group "认证授权系统"
- 扫描包路径

**验证**: `./mvnw compile` — 所有 @Bean 方法编译通过

---

## 第 3 批：实体类 (14 files)

全部位于 `module/{domain}/entity/`

| # | 文件 | 表名 | PK 策略 | 逻辑删除 |
|---|------|------|---------|----------|
| 3.1 | User.java | users | ASSIGN_ID | deletedAt |
| 3.2 | UserProfile.java | user_profiles | INPUT (同user_id) | — |
| 3.3 | UserAuth.java | user_auths | AUTO | deletedAt |
| 3.4 | UserCredentialsHistory.java | user_credentials_history | AUTO | — |
| 3.5 | UserMfa.java | user_mfa | AUTO | — |
| 3.6 | Role.java | roles | AUTO | deletedAt |
| 3.7 | Permission.java | permissions | AUTO | — |
| 3.8 | Scope.java | scopes | AUTO | — |
| 3.9 | UserRole.java | user_roles | INPUT | deletedAt |
| 3.10 | RolePermission.java | role_permissions | INPUT | — |
| 3.11 | OAuthClient.java | oauth_clients | AUTO | deletedAt |
| 3.12 | ApiKey.java | api_keys | AUTO | deletedAt |
| 3.13 | LoginLog.java | login_logs | AUTO | — |
| 3.14 | AuditLog.java | audit_logs | AUTO | — |

**要点**:
- 字段名驼峰映射下划线
- `LocalDateTime` 对应 `TIMESTAMP`
- `deletedAt` 使用 `Long` 类型 (毫秒时间戳)
- JSON 列使用 `@TableField(typeHandler = JacksonTypeHandler.class)`
- `login_logs`/`audit_logs` 主键含 `createdDate`

**验证**: `./mvnw compile` — MyBatis-Plus 实体扫描通过

---

## 第 4 批：Mapper 接口 + XML (14+14 = 28 files)

全部继承 `BaseMapper<T>`，XML 仅含基础 namespace

| 模块 | Mapper |
|------|--------|
| user | UserMapper, UserAuthMapper, UserProfileMapper, UserCredentialsHistoryMapper |
| role | RoleMapper, PermissionMapper, UserRoleMapper, RolePermissionMapper, ScopeMapper |
| auth | LoginLogMapper |
| oauth | OAuthClientMapper |
| apikey | ApiKeyMapper |
| audit | AuditLogMapper |
| mfa | UserMfaMapper |

**验证**: `./mvnw compile` — mapperScan 扫描通过

---

## 第 5 批：infrastructure 层 (8 files)

| 文件 | 路径 | 要点 |
|------|------|------|
| SnowflakeIdGenerator.java | infrastructure/snowflake/ | Hutool Snowflake, datacenterId+workerId |
| JwtTokenProvider.java | infrastructure/security/ | jose4j RS256 sign/verify/parse, 支持多 kid |
| SecurityContextHelper.java | infrastructure/security/ | Static: getUserId/getPermissions/hasPermission |
| PermissionResolver.java | infrastructure/security/ | Interface: `resolvePermissions(userId) → Set<EffectivePermission>` |
| RateLimiterService.java | infrastructure/security/ | Redis Lua sliding window |
| AuditEventPublisher.java | infrastructure/audit/ | Spring event + @Async listener |
| GeeTestProperties.java | infrastructure/geetest/ | @ConfigurationProperties |
| GeeTestVerifier.java | infrastructure/geetest/ | RestTemplate POST gt4.geetest.com/validate, HMAC-MD5 sign, Redis cache, @CircuitBreaker |

**验证**: `./mvnw compile`

---

## 第 6 批：工具类 + Filter + Annotation + Aspect (14 files)

| 文件 | 路径 | 要点 |
|------|------|------|
| JwtUtil.java | common/util/ | 包装 JwtTokenProvider |
| CryptoUtil.java | common/util/ | AES-256-GCM ThreadLocal\<Cipher\>, 多版本密钥 |
| HashUtil.java | common/util/ | SHA-256, BCrypt 便捷方法, DUMMY_HASH 验证 |
| IpUtil.java | common/util/ | X-Forwarded-For 解析 |
| RedisUtil.java | common/util/ | RedisTemplate 便捷方法 |
| SecurityUtil.java | common/util/ | 从 SecurityContext 获取当前用户 |
| JwtAuthenticationFilter.java | common/filter/ | OncePerRequestFilter, 双模(Cookie/Bearer), password_changed_at 检查, MDC |
| ApiKeyAuthenticationFilter.java | common/filter/ | SHA-256, cache lookup, scope 覆盖 |
| @AuditLog.java | common/annotation/ | action, resourceType, resourceIdExpression, detailExpression |
| @RateLimit.java | common/annotation/ | key(SpEL), permits, window |
| @RequirePermission.java | common/annotation/ | value, scopeType |
| AuditLogAspect.java | common/aspect/ | @Around @AuditLog → SpEL → AuditEvent |
| RateLimitAspect.java | common/aspect/ | @Around @RateLimit → RateLimiterService |
| RequirePermissionAspect.java | common/aspect/ | @Around @RequirePermission → PermissionResolver |

**验证**: `./mvnw compile` — AOP 切面织入正确

---

## 第 7-14 批：业务模块 (按模块分批)

### 第 7 批：auth 模块 (~20 files)
DTO: LoginRequest, LoginResponse, RegisterRequest, RefreshTokenRequest, ChangePasswordRequest, ResetPasswordRequest, SendCodeRequest, VerifyCodeRequest, TokenValidationResponse
Service: AuthService+Impl, TokenService+Impl, VerificationCodeService+Impl, LoginLogService+Impl
Controller: AuthController (10 endpoints)
Strategy: AuthStrategy, PasswordAuthStrategy, OAuthAuthStrategy, ApiKeyAuthStrategy
**验证**: `POST /api/v1/auth/register` + `POST /api/v1/auth/login` 可调用

### 第 8 批：user 模块 (~16 files)
DTO: UserCreateRequest, UserUpdateRequest, UserQueryRequest, UserVO, UserStatusRequest, UserProfileUpdateRequest, UserProfileVO, UserAuthVO, BindAuthRequest
Service: UserService+Impl, UserProfileService+Impl, UserCredentialsHistoryService+Impl
Controller: UserController (16 endpoints)
Converter: UserConverter (MapStruct)
**验证**: `GET /api/v1/users` + `GET /api/v1/users/me` 可调用

### 第 9 批：role 模块 (~18 files)
DTO: RoleCreateRequest, RoleUpdateRequest, RoleVO, RoleTreeVO, PermissionVO, AssignRoleRequest, AssignPermissionRequest, UserRoleVO
Service: RoleService+Impl, PermissionService+Impl
Controller: RoleController (12 endpoints)
PermissionEvaluatorImpl + PermissionCacheManager
Converter: RoleConverter, PermissionConverter
**验证**: `GET /api/v1/roles/tree` 可调用

### 第 10 批：mfa 模块 (~10 files)
DTO: MfaSetupInitRequest, MfaSetupInitResponse, MfaEnableRequest, MfaDisableRequest, MfaStatusVO
Service: UserMfaService+Impl (含 MfaService 整合)
Controller: MfaController (5 endpoints)
Converter: MfaConverter
**验证**: `POST /api/v1/mfa/setup/init` 可调用

### 第 11 批：oauth 模块 (~14 files)
DTO: OAuthClientCreateRequest, OAuthClientUpdateRequest, OAuthClientVO, AuthorizationRequest, TokenExchangeRequest, TokenResponse
Service: OAuthClientService+Impl, OAuthAuthorizationService+Impl
Controller: OAuthClientController (9 endpoints) + AuthController 中 OAuth 端点
Converter: OAuthClientConverter
**验证**: `POST /api/v1/oauth/clients` + `POST /api/v1/oauth/token` 可调用

### 第 12 批：apikey 模块 (~8 files)
DTO: ApiKeyCreateRequest, ApiKeyCreateResponse, ApiKeyVO
Service: ApiKeyService+Impl
Controller: ApiKeyController (4 endpoints)
Converter: ApiKeyConverter
**验证**: `POST /api/v1/api-keys` 可调用

### 第 13 批：audit 模块 (~10 files)
DTO: AuditLogQueryRequest, AuditLogVO, LoginLogQueryRequest, LoginLogVO
Service: AuditLogService+Impl
Controller: AuditLogController + LoginLogController (5 endpoints)
Converter: AuditLogConverter
**验证**: `GET /api/v1/audit-logs` 可调用

### 第 14 批：存储与运维 (~5 files)
PartitionMaintenanceScheduler.java
CacheWarmer.java
DataRetentionScheduler.java (90天软删除物理清理)
SecurityHardeningConfig.java (安全响应头)
JWKSController.java (/.well-known/jwks.json)
**验证**: @Scheduled 方法可编译

---

## 第 15 批：SecurityConfig 收口

- 更新 SecurityFilterChain: 加入 JwtAuthenticationFilter + ApiKeyAuthenticationFilter
- 配置端点权限矩阵 (public/authenticated/permission-based)
- 配置 CSRF 禁用, Session STATELESS

**验证**: `./mvnw spring-boot:run` → 全部端点按权限拦截

---

## 第 16 批：测试与文档

- Knife4j @Tag/@Operation/@Schema 注解 (穿插在各 Controller 中)
- Testcontainers 集成测试 (auth + user + role)
- 单元测试 (PermissionEvaluator, TokenService, RateLimiterService)
- k6 性能脚本

**验证**: `./mvnw test` + `http://localhost:8080/doc.html` 全部端点

---

## 编译验证节点

每完成一批 → `./mvnw compile` → 确认无编译错误 → 进入下一批

| 批次 | 累计文件 | 验证方式 |
|------|----------|----------|
| 0-2 | ~33 | compile |
| 3-4 | ~55 | compile |
| 5-6 | ~77 | compile |
| 7 (auth) | ~97 | compile + POST /register |
| 8 (user) | ~113 | compile + GET /users |
| 9 (role) | ~131 | compile + GET /roles/tree |
| 10-14 | ~165 | compile |
| 15-16 | ~170 | spring-boot:run + full test |
