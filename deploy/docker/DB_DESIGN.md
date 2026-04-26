# 认证授权系统数据库设计文档

## 概述

- **MySQL 8.0**: 持久化存储（14 张表），InnoDB 引擎，utf8mb4 字符集
- **Redis 7.x**: 临时/高频数据（JWT 黑名单、会话、速率限制等），自动过期

## MySQL 数据模型

```
users ──1:1── user_profiles
  │
  ├──1:N── user_auths           (多身份: 手机/邮箱/微信/GitHub/...)
  ├──1:N── user_credentials_history (密码历史, 防重用)
  ├──1:N── user_mfa              (多因素认证, TOTP/SMS/WebAuthn)
  ├──M:N── roles ──M:N── permissions ──M:1── scopes
  │         (user_roles)          (role_permissions)
  ├──1:N── api_keys              (API Key 管理)
  ├──1:N── login_logs            (登录审计, 按月分区)
  └──1:N── audit_logs            (操作审计, 按月分区)

oauth_clients ──N:1── users(created_by)
```

### 1. users — 用户主表

| 列 | 类型 | 说明 |
|----|------|------|
| user_id | BIGINT PK | Snowflake 分布式 ID |
| username | VARCHAR(64) UNIQUE | 唯一用户名 |
| email | VARCHAR(128) | 主邮箱 |
| phone | VARCHAR(20) | 主手机号 |
| nickname | VARCHAR(64) | 显示昵称 |
| avatar | VARCHAR(255) | 头像 URL |
| status | TINYINT | 0:正常 1:禁用 2:待激活 3:永久锁定 |
| locked_until | BIGINT | 锁定截止毫秒时间戳 |
| failed_attempts | INT | 连续失败次数 |
| last_login_at | TIMESTAMP | 最后登录时间 |
| last_login_ip | VARCHAR(45) | 最后登录 IP |
| created_at / updated_at | TIMESTAMP | 时间戳 |
| deleted_at | BIGINT DEFAULT 0 | 软删除（0=正常） |

### 2. user_auths — 多身份认证

| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | 自增 |
| user_id | BIGINT FK→users | 关联用户 |
| identity_type | ENUM | phone/email/wechat/github/google/apple/username |
| identifier | VARCHAR(128) | 手机号/邮箱/OpenID |
| credential | VARCHAR(256) | bcrypt 哈希 |
| verified | TINYINT | 是否已验证 |
| is_primary | TINYINT | 是否主认证方式 |
| UK | (identity_type, identifier, deleted_at) | 含 deleted_at 支持软删除后可重新绑定 |

### 3. roles — 角色表（层级继承）

| 列 | 类型 | 说明 |
|----|------|------|
| id | INT PK | 自增 |
| role_name | VARCHAR(50) UNIQUE | 角色名 |
| description | VARCHAR(200) | 描述 |
| parent_role_id | INT FK→roles | 父角色（NULL=顶级） |
| is_system | TINYINT | 系统角色不可删除 |
| deleted_at | BIGINT | 软删除 |

种子角色: `super_admin` → `admin` → `user`

### 4. permissions — 权限表

| 列 | 类型 | 说明 |
|----|------|------|
| id | INT PK | 自增 |
| code | VARCHAR(100) UNIQUE | `resource:action` 如 `user:create` |
| name | VARCHAR(100) | 显示名 |
| resource_type | VARCHAR(50) | 资源: user/role/client/apikey/audit |
| action | VARCHAR(50) | 操作: create/read/update/delete/assign/lock/revoke |

种子权限（19 个）:

```
user:create/read/update/delete/lock
role:create/read/update/delete/assign
perm:read/assign
client:create/read/update/delete
apikey:create/revoke
audit:read
```

### 5. scopes — 权限作用域

| id | scope_key | 说明 |
|----|-----------|------|
| 1 | self | 仅限自己的资源 |
| 2 | dept | 部门范围 |
| 3 | org | 组织范围 |
| 4 | all | 全局无限制 |

### 6. user_roles — 用户角色映射

| 列 | 类型 | 说明 |
|----|------|------|
| user_id | BIGINT FK→users | 用户 |
| role_id | INT FK→roles | 角色 |
| scope_type | VARCHAR(20) | 作用域类型 |
| scope_value | VARCHAR(64) | 作用域值（如部门 ID） |
| granted_by | BIGINT FK→users | 授权人 |
| expires_at | TIMESTAMP | 临时授权过期时间 |

### 7. role_permissions — 角色权限映射

| 列 | 类型 | 说明 |
|----|------|------|
| role_id | INT FK→roles | 角色 |
| permission_id | INT FK→permissions | 权限 |
| scope_id | INT FK→scopes | 默认作用域 |

种子映射: super_admin 拥有所有权限(all 作用域)

### 8. oauth_clients — OAuth2 客户端

client_type: confidential / public
grant_types: authorization_code / client_credentials / refresh_token / implicit

### 9. api_keys — API Key

key_hash: SHA-256 哈希存储
key_prefix: 展示用前缀（dsk-xxxxxxxx）
支持过期时间、作用域限制

### 10. login_logs — 登录审计日志

按月 RANGE COLUMNS 分区（created_date）, 预建 p202604/p202605/p202606/p_future。
identifier_hash: SHA-256 保护隐私。
result: success / failed_credential / failed_mfa / failed_locked / failed_disabled

### 11. audit_logs — 操作审计日志

按月分区，记录所有管理操作（user.create / role.assign 等）。

---

## Redis 数据键空间

| Key 模式 | 类型 | 说明 | TTL |
|----------|------|------|-----|
| `jwt:blacklist:<jti>` | String | JWT 撤销列表 | JWT exp |
| `refresh:<family>` | Hash | Refresh Token 家族 | refresh token TTL |
| `sessions:<user_id>` | Hash | 用户活跃会话 | 30d |
| `oauth:code:<code>` | Hash | OAuth2 授权码 | 10min |
| `lockout:<user_id>` | Hash | 登录失败计数 | 30min |
| `ratelimit:<action>:<key>:<window>` | String | 速率限制 | window |
| `verify:<type>:<identifier>` | Hash | 验证码/OTP | 5min |

### JWT 黑名单

```
SET jwt:blacklist:<jti> "revoked" EX <remaining_ttl>

# 验证时检查
EXISTS jwt:blacklist:<jti>  → 1 = 已撤销
```

### 会话管理

```
# 用户登录时
HSET sessions:<user_id> <session_id> '{"device":"...","ip":"...","user_agent":"..."}'
EXPIRE sessions:<user_id> 2592000

# 列出用户所有会话
HGETALL sessions:<user_id>

# 强制下线（删除某会话）
HDEL sessions:<user_id> <session_id>
```

### 登录锁定

```
# 失败时递增
HINCRBY lockout:<user_id> attempts 1
HSET lockout:<user_id> first_fail <timestamp>
EXPIRE lockout:<user_id> 1800

# 检查是否锁定: attempts >= 5 → 锁定
```

---

## 外键关系总览

| 子表 | 父表 | 键 | 删除策略 |
|------|------|-----|----------|
| user_profiles | users | user_id | CASCADE |
| user_auths | users | user_id | CASCADE |
| user_credentials_history | users | user_id | CASCADE |
| user_mfa | users | user_id | CASCADE |
| user_roles | users | user_id | CASCADE |
| user_roles | roles | role_id | CASCADE |
| user_roles | users | granted_by | SET NULL |
| role_permissions | roles | role_id | CASCADE |
| role_permissions | permissions | permission_id | CASCADE |
| role_permissions | scopes | scope_id | SET NULL |
| roles | roles | parent_role_id | SET NULL |
| oauth_clients | users | created_by | SET NULL |
| api_keys | users | user_id | CASCADE |
