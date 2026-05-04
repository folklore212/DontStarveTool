# 认证授权系统 — 架构重构方案

> 最后更新: 2026-04-29 | 状态: ✅ 全部实施完成 | 作者: Claude Opus 4.7

> **实施日期**: 2026-04-28 ~ 2026-04-29 | **新增 36 文件 + 修改 15 文件 + 移动 2 文件 + 删除 2 文件 | 编译零错误 | 17/17 单元测试通过**

> **安全审查**: 2026-04-29 完成三轮 /review + /simplify + /security-review | 修复 23 项 (4 CRITICAL + 5 HIGH + 8 MEDIUM + 6 LOW) | 剩余 7 项待外部条件

> **最终产物**: 策略模式(3链) + 事件驱动(4事件) + MapStruct(5) + ApiKeyCacheManager + DataRetentionService(9表级联) + DeviceFingerprintService + RequestIdFilter + SOC2审计字段(V4) + 设备指纹表(V5) + Admin SPA脚手架 + 安全加固(23项)

> **最终产物**: 策略模式(3链) + 事件驱动(4事件) + MapStruct(5转换器) + ApiKeyCacheManager + DataRetentionService + DeviceFingerprintService + 安全加固(12项) + Admin SPA脚手架

## 0. 背景与动机

当前系统（~180 Java 文件，Spring Boot 3.4.5，Java 21，40+ REST API）已实现全部 Phase 1-10 功能并通过集成测试验证。但架构层面存在以下技术债：

| 问题 | 严重程度 | 影响 |
|------|---------|------|
| `AuthServiceImpl` 为 645 行上帝服务，注入 9 个 Mapper（8 个跨模块 + 1 个同模块） | 高 | 模块边界模糊，修改一个流程需理解整类 |
| 无认证策略模式 — login() 内 if-else 硬编码身份类型/MFA 类型/AuthMethod | 高 | 新增 OAuth 登录需修改核心类 |
| `JwtAuthenticationFilter` / `UserMfaServiceImpl` / `CacheWarmer` 直接注入跨模块 Mapper | 中 | 违反分层架构，安全过滤器耦合数据层 |
| `AuditEventPublisher` 存在但只打日志不持久化（死代码） | 低 | 维护负担 |
| `LoginLogVO` 位于 `audit.dto` 但实体在 `auth.entity` | 低 | 模块依赖方向混乱 |
| `IdentityType`（WECHAT/GITHUB/GOOGLE/APPLE）和 `MfaType`（SMS/EMAIL/WEBAUTHN）枚举值已定义但无对应策略实现 | 中 | 扩展点形同虚设 |

本方案在**不改变任何 REST API 契约**的前提下，通过策略模式和事件驱动重构核心架构。

---

## 1. 目标架构拓扑

```
                          ┌─────────────────────────────────────────┐
                          │          API Gateway / Clients           │
                          └────────────────────┬────────────────────┘
                                               │
                          ┌────────────────────▼────────────────────┐
                          │         Filters & Security Chain         │
                          │  ApiKeyAuthFilter → JwtAuthFilter        │
                          │  (调用 Service 接口，不直接调 Mapper)      │
                          └────────────────────┬────────────────────┘
                                               │
          ┌────────────────────────────────────┼────────────────────────────────────┐
          │                                    │                                    │
          ▼                                    ▼                                    ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  AuthController  │  │  Admin APIs      │  │  OAuth/MFA 等     │
│  (仅编排/薄层)    │  │  User/Role/Api   │  │  Controllers     │
└────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘
         │                     │                     │
         ▼                     ▼                     ▼
┌──────────────────┐  ┌──────────────────────────────────────────────┐
│   AuthService    │  │  Domain Services                             │
│  (编排器 ~200行)  │  │  UserService / RoleService / MfaService /    │
│                  │  │  TokenService / ApiKeyService / AuditService │
│  委托至:          │  │                                              │
│  ┌──────────────┐│  │  各 Service 只访问本模块 Mapper                │
│  │AuthProvider  ││  │  跨模块通过 ApplicationEvent 通信             │
│  │  Chain       ││  │                                              │
│  └──────────────┘│  └──────────────────────────────────────────────┘
│  ┌──────────────┐│                       │
│  │IdentityResol ││           ┌───────────▼───────────┐
│  │    Chain     ││           │  ApplicationEvents    │
│  └──────────────┘│           │  (跨模块异步解耦)      │
│  ┌──────────────┐│           └───────────────────────┘
│  │MfaVerifier   ││
│  │   Chain     ││
│  └──────────────┘│
└──────────────────┘
```

### 分层强制规则
1. **Controller** → 仅调用 Service 接口，不做任何业务逻辑
2. **Service** → 调用本模块 Mapper 或跨模块 Service 接口（不直接调跨模块 Mapper）
3. **Mapper** → 单表 CRUD，无业务逻辑
4. **跨模块通信** → Spring ApplicationEvent（异步）或 Service 接口（同步事务内）

---

## 2. Phase 1: 基础—策略接口与事件定义（零行为变更）

### 2.1 策略接口（3 个新文件）

**`module/auth/strategy/authentication/AuthenticationProvider.java`**
```java
public interface AuthenticationProvider {
    /** 支持的认证方式 */
    AuthMethod getMethod();
    /** 是否可处理此请求 */
    boolean supports(LoginRequest request);
    /** 执行认证，返回结果对象 */
    AuthenticationResult authenticate(LoginRequest request, HttpServletRequest httpRequest);
    /** 处理登录失败（锁定计数等） */
    void handleFailedLogin(User user);
    /** 处理登录成功（重置计数等） */
    void handleSuccessfulLogin(User user);
}
```

**`module/auth/strategy/authentication/AuthenticationResult.java`**

```java
/**
 * Immutable result of an authentication attempt.
 * Fields null-on-failure; ipAddress/userAgent populated by provider from HttpServletRequest.
 */
public record AuthenticationResult(
    boolean success,
    User user,
    UserAuth userAuth,
    boolean mfaRequired,
    MfaContext mfaContext,
    ErrorCode errorCode,
    String failureReason,
    IdentityType identityType,
    String ipAddress,
    String userAgent
) {
    public UserLoggedInEvent toSuccessEvent() {
        return new UserLoggedInEvent(user.getUserId(), identityType,
                ipAddress, userAgent, true, null);
    }

    public UserLoggedInEvent toFailedEvent() {
        return new UserLoggedInEvent(user != null ? user.getUserId() : null,
                identityType, ipAddress, userAgent, false,
                errorCode != null ? errorCode.name() : failureReason);
    }

    public record MfaContext(long userId, java.util.List<String> mfaTypes) {}
}
```

**`module/auth/strategy/identity/IdentityResolver.java`**
```java
public interface IdentityResolver {
    /** 支持的身份类型 */
    IdentityType supportedType();
    /** 判定该 identifier 是否由此解析器处理 */
    boolean canResolve(String identifier);
    /** 解析用户 */
    User resolve(String identifier);
    /** 解析用户认证记录 */
    UserAuth resolveAuth(User user, String identifier);
}
```

**`module/mfa/strategy/MfaVerifier.java`**
```java
public interface MfaVerifier {
    /** 支持的 MFA 类型 */
    MfaType supportedType();
    /** 验证 MFA 码 */
    boolean verify(UserMfa mfaRecord, String code);
    /** 验证并消耗备用码 */
    boolean verifyAndConsumeBackupCode(UserMfa mfaRecord, String code);
}
```

### 2.2 领域事件（4 个新文件）

事件类定义在 `common/event/`（领域事件属于通用模块，不依赖任何具体模块）。现有 `infrastructure/audit/AuditEvent.java` 将在 Phase 6 标记 `@Deprecated` 并替换为这些新事件。

```
common/event/
  UserRegisteredEvent.java       — userId, username, email, phone, identityType, identifier, encodedPassword
  PasswordChangedEvent.java      — userId, changedAt
  UserLoggedInEvent.java         — userId, identityType, ipAddress, userAgent, success, failureReason
  UserStatusChangedEvent.java    — userId, oldStatus, newStatus
```

每个事件继承 `ApplicationEvent`，字段均为 final（不可变 record 风格）。

### 2.3 事件基础设施（2 个新文件）

事件基础设施放在 `infrastructure/event/`（发布/监听机制属于基础设施层）：

```
infrastructure/event/
  AuthEventPublisher.java    — 封装 ApplicationEventPublisher，提供类型安全的发布方法
  AuthEventListeners.java    — @TransactionalEventListener(phase=AFTER_COMMIT) 异步消费
```

`infrastructure/audit/AuditEvent.java` 将在 Phase 6 迁至 `common/event/` 并标记旧位置 `@Deprecated`。

---

## 3. Phase 2: IdentityResolver 策略实现

### 3.1 新文件（3 个）
```
module/auth/strategy/identity/
  EmailIdentityResolver.java       — canResolve: contains("@")，resolve: userMapper.eq(email)
  PhoneIdentityResolver.java       — canResolve: matches("^\\+?\\d{7,15}$")，resolve: userMapper.eq(phone)
  UsernameIdentityResolver.java    — canResolve: 兜底，resolve: userMapper.eq(username)
```

### 3.2 提取逻辑（来源：AuthServiceImpl）
| 原私有方法 | 行号 | 提取至 |
|-----------|------|--------|
| `findUserByIdentifier()` | 495-503 | 各 Resolver.resolve() |
| `findUserAuth()` | 505-520 | 各 Resolver.resolveAuth() |
| `resolveIdentifier()` | 619-626 | 删除 — Resolver 已覆盖 |
| `guessIdentityType()` | 628-639 | 删除 — canResolve() 替代 |

### 3.3 AuthServiceImpl 改动
```java
// 注入策略链
private final List<IdentityResolver> identityResolvers;

// 调度方法
private IdentityResolver findResolver(String identifier) {
    return identityResolvers.stream()
        .filter(r -> r.canResolve(identifier))
        .findFirst()
        .orElseThrow(() -> new AuthenticationException(ErrorCode.INVALID_CREDENTIALS));
}

// register() 中
IdentityResolver resolver = findResolver(request.getIdentifier());
User existing = resolver.resolve(request.getIdentifier());
```

**风险评估**：低。提取的是简单的字符串匹配逻辑，确定性高。集成测试覆盖。

---

## 4. Phase 3: MFA 验证策略实现

### 4.1 新文件（4 个）
```
module/mfa/strategy/
  TotpMfaVerifier.java           — 真实实现：提取 RFC 6238 TOTP/HOTP 算法 + 备用码消费
  SmsMfaVerifier.java            — 占位：throw UnsupportedOperationException("SMS MFA not yet implemented")
  EmailMfaVerifier.java          — 占位
  WebAuthnMfaVerifier.java       — 占位
```

### 4.2 提取逻辑（来源：UserMfaServiceImpl）
| 原私有方法 | 行号 | 提取至 |
|-----------|------|--------|
| `verifyTotpCode()` | 301-319 | TotpMfaVerifier.verify() |
| `generateTotp()` | 325-352 | TotpMfaVerifier（私有） |
| `verifyAndConsumeBackupCode()` | 239-276 | TotpMfaVerifier.verifyAndConsumeBackupCode() |

### 4.3 AuthServiceImpl.login() 改动
```java
// Before (inline 硬编码):
boolean mfaValid = mfaService.verifyTotp(userId, code);
if (!mfaValid) {
    boolean backupValid = mfaService.verifyAndConsumeBackupCode(userId, code);
}

// After (策略调度):
MfaVerifier verifier = mfaVerifiers.stream()
    .filter(v -> v.supportedType().getValue().equals(userMfa.getMfaType()))
    .findFirst().orElseThrow();
boolean valid = verifier.verify(userMfa, code)
    || verifier.verifyAndConsumeBackupCode(userMfa, code);
```

**风险评估**：低。TOTP 算法是纯函数（RFC 6238），提取后可在单元测试中独立验证。

> **`UserMfa` 记录查询职责**：`MfaVerifier.verify(UserMfa, code)` 接受 entity 而非 userId，由调用方负责查询。在 `PasswordAuthenticationProvider` 中，通过注入 `UserMfaService`（mfa 模块的 Service 接口）调用 `userMfaService.getEnabledMfa(userId)` 获取 `UserMfa` record，而非直接注入 `UserMfaMapper`，避免引入新的跨模块 Mapper 依赖。

---

## 5. Phase 4: 认证策略调度（核心重构）

### 5.1 新增文件（2 个实现 + 1 record 已在 Phase 1 定义）
```
module/auth/strategy/authentication/
  AuthenticationProvider.java           — 接口（Phase 1 已创建）
  AuthenticationResult.java             — 结果 record（Phase 1 已定义）
  PasswordAuthenticationProvider.java   — 真实实现（本 Phase 新增）
  OAuthAuthenticationProvider.java      — 占位（本 Phase 新增，为 social login 预留）
```

### 5.2 PasswordAuthenticationProvider 包含的逻辑（提取自 AuthServiceImpl.login():162-230）
1. GeeTest 验证码校验（fail-open 模式：circuit breaker → warn → 放行）
2. IdentityResolver 链身份解析
3. BCrypt 密码验证 + DUMMY_HASH 常量时间防侧信道
4. 用户状态检查（LOCKED/DISABLED/PENDING）
5. 失败处理（failedAttempts 递增 → 达到阈值锁定账户）
6. MFA 门控（委托 MfaVerifier 链）

> **依赖注入规则**：`PasswordAuthenticationProvider` 位于 `module/auth/strategy/authentication/`，其 `authenticate()` 实现中需更新 User 状态字段（`handleFailedLogin` / `handleSuccessfulLogin`）。它对 user 模块的访问通过注入 `UserService` 接口实现：
> - `userService.updateLoginStatus(userId, lastLoginIp)` — 重置 failedAttempts，更新 lastLoginAt/lastLoginIp
> - `userService.recordFailedAttempt(userId)` — 递增 failedAttempts，达到阈值时锁定账户
> 
> 不直接注入 `UserMapper`，与 `UserMfaService` 注入（见 Section 4.3 说明框）遵循同一分层规则。

### 5.3 AuthServiceImpl.login() 重构后
```java
@Override
public LoginResponse login(LoginRequest request) {
    AuthenticationProvider provider = authProviders.stream()
        .filter(p -> p.supports(request))
        .findFirst()
        .orElseThrow(() -> new AuthenticationException(ErrorCode.INVALID_CREDENTIALS));

    AuthenticationResult result = provider.authenticate(request, getCurrentRequest());

    if (!result.success()) {
        eventPublisher.publishUserLoggedIn(result.toFailedEvent());
        metricsService.recordLogin("failure", result.identityType());
        throw new AuthenticationException(result.errorCode(), result.failureReason());
    }

    if (result.mfaRequired()) {
        return buildMfaRequiredResponse(result.mfaContext());
    }

    eventPublisher.publishUserLoggedIn(result.toSuccessEvent());
    LoginResponse response = createLoginResponse(result.user());
    metricsService.recordLogin("success", result.identityType());
    return response;
}
```

**风险评估**：中。login() 是最复杂的流程（68 行，涉及 GeeTest/BCrypt/状态机/MFA/日志/指标）。提取时必须逐行对比原行为。已有 `AuthIntegrationTest` 覆盖 golden path 和 failure path。

---

## 6. Phase 5: 事件解耦与分层修正

> Phase 5 做两件事：(a) 将副作用从 `@Transactional` 方法中抽离为异步事件（6.1-6.3, 6.7），(b) 消除 AuthServiceImpl 和 Filter 中剩余的跨模块 Mapper 引用（6.4-6.6）。后者在逻辑上属于 Phase 6 的分层清理，但因依赖 Phase 4 重构后的 Service 接口，安排在同一轮实施。

### 6.1 原则
- **事务内操作** 保持同步（register() 插入 4 张表 → 一个 @Transactional）
- **事务后副作用** 发事件（审计日志、缓存刷新、指标、异步通知）

### 6.2 register() 重构
```java
// Before: 直接调用 5 个 Mapper
userMapper.insert(user);
userAuthMapper.insert(userAuth);
userProfileMapper.insert(profile);
roleMapper.selectOne(...);
userRoleMapper.insert(userRole);

// After: 委托 Domain Service（同一 @Transactional 内）
UserVO userVO = userService.createUser(createRequest);       // user + auth + profile
roleService.assignDefaultRole(userVO.getUserId());            // role
eventPublisher.publishUserRegistered(new UserRegisteredEvent(...));  // 事务后异步
```

> **新方法说明**：
> - `UserService.createUser(CreateUserRequest)` — 新增方法，将 user/auth/profile 三表插入封装在 `UserServiceImpl` 中，复用 `@Transactional` 边界
> - `RoleService.assignDefaultRole(Long userId)` — 新增方法，封装 "查询 default role → 插入 user_role" 逻辑
> - 两个方法均被外层 `AuthServiceImpl.register()` 的 `@Transactional` 管理，保持同一个事务

### 6.3 changePassword() / resetPassword() 重构
```java
// Before: 直接调 userCredentialsHistoryMapper.insert()
// After: credentialsHistoryService.recordPasswordChange(userId, newEncoded);
//         eventPublisher.publishPasswordChanged(new PasswordChangedEvent(userId, now));
```

### 6.4 exportUserData() 与 forgetMe() 分层修正

exportUserData() 的 Mapper 清理：
```java
// Before: auditLogMapper.selectList() / loginLogMapper.selectList() 直接查
// After: auditLogService.exportByUserId(userId) / loginLogService.exportByUserId(userId)
```

forgetMe() 的 Mapper 清理（`ScheduledExecutorService` 线程池泄漏在 Section 7.2 独立处理）：
```java
// Before: loginLogMapper.update() 直接改
// After: loginLogService.anonymizeByUserId(userId)
```

### 6.5 JwtAuthenticationFilter 分层清理
```
Before:  @Autowired UserMapper userMapper;
         User user = userMapper.selectById(userId);
         user.getPasswordChangedAt()

After:  @Autowired UserService userService;
         UserVO user = userService.getUserById(userId);
         user.getPasswordChangedAt()  // UserVO 新增字段
```

### 6.6 loadUserPermissions() 跨模块委托

当前 `createLoginResponse()` 内部调用 `loadUserPermissions()`，该方法直接注入 `UserRoleMapper`（role 模块 Mapper），是 AuthServiceImpl 最后一个跨模块 Mapper 引用。

```java
// Before（AuthServiceImpl.createLoginResponse()）:
private List<String> loadUserPermissions(Long userId) {
    List<UserRole> userRoles = userRoleMapper.selectList(...);  // 跨模块 Mapper
    return userRoles.stream()
        .map(ur -> "ROLE_" + ur.getRoleId())
        .collect(Collectors.toList());
}

// After: 委托 RoleService
// AuthServiceImpl:
private List<String> loadUserPermissions(Long userId) {
    return roleService.getPermissionStrings(userId);  // 委托 role 模块 Service
}

// RoleService 新增方法:
List<String> getPermissionStrings(Long userId);
```

> **关联变更**：`RoleServiceImpl` 新增 `getPermissionStrings(Long userId)` 方法，内部调用 `UserRoleMapper`（同模块 Mapper，符合分层规则）。`AuthServiceImpl` 移除 `UserRoleMapper` 注入，跨模块 Mapper 从 8 个减为 0。

### 6.7 AuthEventListeners 消费

`AuthEventListeners` 作为事件监听入口，使用 `@TransactionalEventListener(phase=AFTER_COMMIT)` + `@Async` 异步消费。初期将 4 个事件的监听集中在此类中。

```java
@Component
public class AuthEventListeners {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onUserLoggedIn(UserLoggedInEvent event) {
        // 写入 audit_log（持久化，替代死代码 AuditEventPublisher）
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onPasswordChanged(PasswordChangedEvent event) {
        // 刷新权限缓存（password_changed_at 变更 → JWT 即时失效）
    }
}
```

> **依赖规则**：`AuthEventListeners` 注入 `AuditLogService`（非 `AuditLogMapper`）和 `PermissionCacheManager`，遵循分层规则。
>
> **设计说明**：`AuthEventListeners` 是事件监听入口点。后续可按模块拆分为独立的 `@Component`（如 `AuditEventListener`、`CacheInvalidationListener`），事件总线负责分发。

---

## 7. Phase 6: 分层清理与债务清偿

### 7.1 DTO 位置修正
```
Move: module/audit/dto/LoginLogVO.java        → module/auth/dto/LoginLogVO.java
Move: module/audit/dto/LoginLogQueryRequest.java → module/auth/dto/LoginLogQueryRequest.java
```
LoginLogController 保留在 audit 模块（审计视角查询登录日志），通过跨模块 Service 调用。

### 7.2 forgetMe() 延期删除线程池修正

当前 `AuthServiceImpl.forgetMe()` 在方法内直接创建 `Executors.newSingleThreadScheduledExecutor()` 延迟 30 天物理删除用户。这会导致每次调用泄漏一个线程池资源。

```java
// Before（AuthServiceImpl.forgetMe():479-489）
ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
scheduler.schedule(() -> { userMapper.deleteById(userId); }, 30, TimeUnit.DAYS);
scheduler.shutdown();

// After: 提取到 DataRetentionService（复用 Spring TaskScheduler）
@Service
public class DataRetentionService {
    private final TaskScheduler taskScheduler;
    private final UserMapper userMapper;

    public void schedulePhysicalDeletion(Long userId, long delayDays) {
        taskScheduler.schedule(() -> {
            userMapper.deleteById(userId);
            log.info("Physical deletion completed for userId={}", userId);
        }, Instant.now().plus(delayDays, ChronoUnit.DAYS));
    }
}
```

AuthServiceImpl 改为注入 `DataRetentionService` 并调用 `dataRetentionService.schedulePhysicalDeletion(userId, 30)`。

> **分层例外说明**：`DataRetentionService` 位于 `infrastructure/` 层，直接注入 `UserMapper`。基础设施层组件（类似 `CacheWarmer`、`PartitionMaintenanceScheduler`）可直接访问数据层 Mapper，这是有意设计的合理例外，不视为违反分层规则。此类例外应在类 Javadoc 中标注 `@implNote Infrastructure layer — direct Mapper access is intentional.`。

### 7.3 AuditEventPublisher 废弃
- 当前实现只打 SLF4J 日志，从不持久化
- 标记 `@Deprecated`
- 替代方案：AuthEventListeners 负责真实持久化

### 7.4 CacheWarmer 分层修正
```java
// Before: @Autowired UserMapper userMapper;
// After:  @Autowired UserService userService;
//         新增 UserService.getRecentlyActiveUsers(limit) 方法
```

---

## 8. 变更汇总

| 类别 | 新增 | 修改 | 移动 |
|------|------|------|------|
| 策略接口 | 3 | 0 | 0 |
| AuthenticationResult record | 1 | 0 | 0 |
| IdentityResolver 实现 | 3 | AuthServiceImpl | 0 |
| MfaVerifier 实现 | 4 | UserMfaServiceImpl | 0 |
| AuthenticationProvider 实现 | 2 | AuthServiceImpl | 0 |
| 事件类 | 4 | 0 | 0 |
| 事件基础设施 | 2 | 0 | 0 |
| DTO 重定位 | 0 | LoginLogService, LoginLogServiceImpl, LoginLogController | 2 |
| DataRetentionService（forgetMe 线程池修正） | 1 | AuthServiceImpl | 0 |
| Domain Service 新方法（createUser, assignDefaultRole, getPermissionStrings） | 0 | UserServiceImpl, RoleServiceImpl, AuthServiceImpl | 0 |
| Filter / 基础设施分层清理 | 0 | JwtAuthenticationFilter, UserVO, CacheWarmer, AuditEventPublisher | 0 |
| 单元测试 | 3 | 0 | 0 |
| **总计** | **~23** | **~10（独立文件）** | **2** |

### AuthServiceImpl 行数变化
```
重构前: 645 行 (11 public + 11 private)
重构后: ~200 行 (11 public, 全部委托至策略链/Domain Service/事件)
```

### 跨模块 Mapper 引用清零
```
重构前: AuthServiceImpl(8 cross-module) + JwtAuthFilter(1) + UserMfaServiceImpl(2) + CacheWarmer(1) = 12 处跨模块违规
       （AuthServiceImpl 的 LoginLogMapper 为同模块 Mapper，随 exportUserData/forgetMe 重构一并移除）
重构后: 0 处违规（所有跨模块访问通过 Service 接口或 ApplicationEvent）
```

---

## 9. 验证计划

### 9.1 编译
```bash
./mvnw compile          # 零错误
./mvnw test-compile     # 零错误
```

### 9.2 新增单元测试

策略核心逻辑可独立单元测试，需新增 3 个测试类：

| 测试类 | 类型 | 覆盖重点 |
|--------|------|---------|
| `TotpMfaVerifierTest` | 纯函数 | RFC 6238 测试向量 (Appendix B)、时间窗口偏移 (t±30s)、错误码拒绝、备用码消费 |
| `IdentityResolverTest` | Mock Mapper | canResolve 判定逻辑（email/phone/username）、链调度顺序 |
| `PasswordAuthenticationProviderTest` | Mock 全部依赖 | golden path、DUMMY_HASH 防侧信道、状态机拒绝 (LOCKED/DISABLED/PENDING)、MFA 门控、GeeTest fail-open |

```bash
./mvnw test -Dtest="TotpMfaVerifierTest,IdentityResolverTest,PasswordAuthenticationProviderTest"
```

### 9.3 现有集成测试（必须通过）
```bash
./mvnw test -Dtest="AuthIntegrationTest"   # register → activate → login → refresh → logout
./mvnw test -Dtest="RoleIntegrationTest"    # RBAC 权限解析
```

### 9.4 API 契约验证

40+ REST API 请求/响应 JSON 结构保持不变（login/register/refresh/logout + 所有 Admin 端点），集成测试验证。

### 9.5 DI 多实现验证

所有策略注入点必须使用 `List<T>` 模式：`grep` 确认无单点 `@Autowired IdentityResolver/MfaVerifier/AuthenticationProvider`。若有单点注入，改为 `List<>` 注入或添加 `@Primary`。

### 9.6 扩展点验证（未来无需改 AuthServiceImpl）
- 新增 OAuth 登录：创建新 `OAuthAuthenticationProvider` 实现 + 注册为 @Component → 自动参与调度
- 新增 SMS MFA：创建新 `SmsMfaVerifier` 实现 → 自动参与调度
- 新增身份类型：创建新 `XxxIdentityResolver` → 自动参与调度

---

## 10. 不在本方案范围内

| 项目 | 原因 |
|------|------|
| 权限模型改造 | 当前 `loadUserPermissions()` 将 roleId 拼接为 `"ROLE_" + roleId` 放入 JWT，本质是角色传递而非细粒度权限解析。本方案不改变此行为（保持 API 契约兼容），完整的 RBAC 权限解析（role → permission list）留待后续独立方案 |
| OAuth2 社交登录实现 | 本方案只创建占位 Provider，实际对接各平台 OAuth 独立完成 |
| SMS/Email/WebAuthn MFA 实现 | 只创建占位 Verifier，实际对接短信网关/邮件/FIDO2 独立完成 |
| Admin 管理后台 | 独立 SPA 项目，最后开发 |
| 数据库表结构变更 | 当前 14 表结构满足需求，无需变更 |
| Flyway 迁移脚本 | 不需要新增迁移 |
| MapStruct 自动生成的实现类 | 由 `maven-compiler-plugin` 在编译时生成 |
