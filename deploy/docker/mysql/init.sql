CREATE DATABASE IF NOT EXISTS auth_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE auth_system;

-- ============================================================
-- 1. 用户主表
-- ============================================================
CREATE TABLE users (
    user_id    BIGINT       NOT NULL COMMENT 'Snowflake分布式ID',
    username   VARCHAR(64)  NOT NULL COMMENT '唯一用户名',
    email      VARCHAR(128) NULL     COMMENT '主邮箱，未填时存 NULL（勿用空字符串）',
    phone      VARCHAR(20)  NULL     COMMENT '主手机号，未填时存 NULL（勿用空字符串，避免 UNIQUE KEY 冲突）',
    nickname   VARCHAR(64)  NULL     COMMENT '显示昵称',
    avatar     VARCHAR(255) NULL     COMMENT '头像URL',
    status     TINYINT      NOT NULL DEFAULT 0 COMMENT '0:正常 1:禁用 2:待激活 3:锁定(locked_until 过期后自动解锁)',
    locked_until BIGINT     NULL     COMMENT '锁定截止毫秒时间戳',
    failed_attempts INT     NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    last_login_at       TIMESTAMP NULL,
    last_login_ip       VARCHAR(45) NULL,
    password_changed_at TIMESTAMP NULL COMMENT '最后修改密码时间，JWT签发需晚于此时间',
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  BIGINT      NOT NULL DEFAULT 0 COMMENT '软删除毫秒时间戳, 0=正常',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_username (username, deleted_at),
    UNIQUE KEY uk_email    (email, deleted_at),
    UNIQUE KEY uk_phone    (phone, deleted_at),
    INDEX idx_status (status),
    INDEX idx_created (created_at),
    INDEX idx_status_created (status, deleted_at, created_at)    -- 分页查询过滤+排序
) ENGINE=InnoDB;

-- ============================================================
-- 2. 用户扩展资料
-- ============================================================
CREATE TABLE user_profiles (
    user_id   BIGINT       NOT NULL,
    real_name VARCHAR(64)  NULL     COMMENT '真实姓名',
    locale    VARCHAR(10)  NOT NULL DEFAULT 'zh-CN',
    timezone  VARCHAR(32)  NOT NULL DEFAULT 'Asia/Shanghai',
    metadata  JSON         NULL     COMMENT '扩展属性（避免频繁改表）',
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 3. 用户多身份认证
-- ============================================================
CREATE TABLE user_auths (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    identity_type ENUM('phone','email','wechat','github','google','apple','username') NOT NULL,
    identifier    VARCHAR(128) NOT NULL COMMENT '手机号/邮箱/OpenID',
    credential    VARCHAR(256) NOT NULL COMMENT 'bcrypt哈希后的密码',
    is_verified   TINYINT      NOT NULL DEFAULT 0,
    is_primary    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否主认证方式',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    BIGINT       NOT NULL DEFAULT 0 COMMENT '软删除毫秒时间戳',
    PRIMARY KEY (id),
    UNIQUE KEY uk_identity (identity_type, identifier, deleted_at),
    INDEX idx_user_id (user_id),
    CONSTRAINT fk_auth_user FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 4. 密码历史（防止重用）
-- ============================================================
CREATE TABLE user_credentials_history (
    id         BIGINT     NOT NULL AUTO_INCREMENT,
    user_id    BIGINT     NOT NULL,
    credential VARCHAR(256) NOT NULL COMMENT '历史bcrypt哈希',
    created_at TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_user_time (user_id, created_at DESC),
    CONSTRAINT fk_pwdhist_user FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 5. MFA 配置
-- ============================================================
CREATE TABLE user_mfa (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL,
    mfa_type     ENUM('totp','sms','email','webauthn') NOT NULL,
    secret       VARCHAR(256) NOT NULL COMMENT 'AES加密存储',
    key_version  TINYINT     NOT NULL DEFAULT 1 COMMENT 'AES密钥版本号',
    is_enabled   TINYINT     NOT NULL DEFAULT 0,
    backup_codes JSON        NULL     COMMENT 'AES加密的备用恢复码',
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_mfa (user_id, mfa_type),
    CONSTRAINT fk_mfa_user FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 6. 角色表（支持层级继承）
-- ============================================================
CREATE TABLE roles (
    id             INT          NOT NULL AUTO_INCREMENT,
    role_name      VARCHAR(50)  NOT NULL,
    description    VARCHAR(200) NULL,
    parent_role_id INT          NULL     COMMENT '父角色ID，NULL=顶级角色',
    is_system      TINYINT      NOT NULL DEFAULT 0 COMMENT '系统角色不可删除',
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     BIGINT       NOT NULL DEFAULT 0 COMMENT '软删除毫秒时间戳',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_name (role_name, deleted_at),
    INDEX idx_parent (parent_role_id),
    CONSTRAINT fk_role_parent FOREIGN KEY (parent_role_id) REFERENCES roles(id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
-- 7. 权限表
-- ============================================================
CREATE TABLE permissions (
    id            INT          NOT NULL AUTO_INCREMENT,
    code          VARCHAR(100) NOT NULL COMMENT 'resource:action 如 user:create',
    name          VARCHAR(100) NOT NULL COMMENT '权限显示名',
    resource_type VARCHAR(50)  NOT NULL COMMENT '资源类型 user/role/server 等',
    action        VARCHAR(50)  NOT NULL COMMENT '操作 create/read/update/delete/assign',
    description   VARCHAR(200) NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB;

-- ============================================================
-- 8. 权限作用域
-- ============================================================
CREATE TABLE scopes (
    id          INT          NOT NULL AUTO_INCREMENT,
    scope_key   VARCHAR(64)  NOT NULL COMMENT 'self/dept/org/all',
    description VARCHAR(200) NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scope (scope_key)
) ENGINE=InnoDB;

-- ============================================================
-- 9. 用户-角色映射（含临时授权）
-- ============================================================
CREATE TABLE user_roles (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    role_id     INT         NOT NULL,
    scope_type  VARCHAR(20) NOT NULL DEFAULT 'self' COMMENT '作用域类型 (self/dept/org/all)',
    scope_value VARCHAR(64) NOT NULL DEFAULT '' COMMENT '作用域值（如部门ID），无具体值时为空字符串',
    granted_by  BIGINT      NULL     COMMENT '授权人',
    expires_at  TIMESTAMP   NULL     COMMENT '临时授权过期时间',
    last_reviewed_at TIMESTAMP NULL  COMMENT 'SOC2 CC6.3 最近审查时间',
    last_reviewed_by BIGINT   NULL   COMMENT 'SOC2 CC6.3 审查人 user_id',
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  BIGINT      NOT NULL DEFAULT 0 COMMENT '软删除毫秒时间戳',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role_scope (user_id, role_id, scope_type, scope_value, deleted_at),
    INDEX idx_role_user (role_id),
    INDEX idx_expires (expires_at),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ur_grantor FOREIGN KEY (granted_by) REFERENCES users(user_id)
        ON DELETE SET NULL,
    CONSTRAINT chk_scope_type CHECK (scope_type IN ('self','dept','org','all'))
) ENGINE=InnoDB;

-- ============================================================
-- 10. 角色-权限映射
-- ============================================================
CREATE TABLE role_permissions (
    role_id       INT NOT NULL,
    permission_id INT NOT NULL,
    scope_id      INT NULL COMMENT '默认作用域',
    PRIMARY KEY (role_id, permission_id),
    INDEX idx_perm_role (permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES roles(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_rp_perm FOREIGN KEY (permission_id) REFERENCES permissions(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_rp_scope FOREIGN KEY (scope_id) REFERENCES scopes(id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
-- 11. OAuth2 客户端注册
-- ============================================================
CREATE TABLE oauth_clients (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    client_id     VARCHAR(128) NOT NULL COMMENT 'OAuth2 client_id',
    client_secret VARCHAR(256) NOT NULL COMMENT 'bcrypt哈希',
    client_name   VARCHAR(128) NOT NULL,
    client_type   ENUM('confidential','public') NOT NULL DEFAULT 'confidential',
    grant_types   SET('authorization_code','client_credentials','refresh_token') NOT NULL DEFAULT 'authorization_code',
    redirect_uris JSON         NULL,
    allowed_scopes JSON       NULL,
    is_trusted    TINYINT      NOT NULL DEFAULT 0 COMMENT '跳过用户确认',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '0:禁用 1:正常',
    created_by    BIGINT       NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_client_id (client_id, deleted_at),
    INDEX idx_created_by (created_by),
    CONSTRAINT fk_oauth_creator FOREIGN KEY (created_by) REFERENCES users(user_id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
-- 12. API Key 表
-- ============================================================
CREATE TABLE api_keys (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    key_name        VARCHAR(128) NOT NULL COMMENT 'Key用途标识',
    key_hash        VARCHAR(256) NOT NULL COMMENT 'SHA-256哈希',
    key_prefix      VARCHAR(12)  NOT NULL COMMENT '展示用前缀 dsk-xxxxxxxx',
    allowed_scopes  JSON         NULL,
    expires_at      TIMESTAMP    NULL,
    last_used_at    TIMESTAMP    NULL,
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '0:禁用 1:正常',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_key_hash (key_hash, deleted_at),
    INDEX idx_user_key (user_id),
    INDEX idx_expires (expires_at),
    CONSTRAINT fk_apikey_user FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 13. 登录审计日志（按月分区）
-- 使用 RANGE COLUMNS + DATE 列避免 TIMESTAMP 分区函数的时区依赖限制
-- ============================================================
CREATE TABLE login_logs (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NULL     COMMENT '用户未注册时可为空',
    identifier_hash VARCHAR(128) NOT NULL COMMENT 'SHA-256(identifier)，隐私保护',
    identity_type   VARCHAR(20)  NOT NULL,
    auth_method     ENUM('password','totp','sms','oauth','api_key','sso') NOT NULL DEFAULT 'password',
    ip_address      VARCHAR(45)  NULL COMMENT '仅存 /24 前缀 (GDPR 伪匿名化)',
    user_agent      VARCHAR(512) NULL COMMENT '仅存主版本号 e.g. Chrome/126',
    result          ENUM('success','failed_credential','failed_mfa','failed_locked','failed_disabled') NOT NULL,
    failure_reason  VARCHAR(200) NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_date    DATE         NOT NULL DEFAULT (CURRENT_DATE) COMMENT '分区键',
    PRIMARY KEY (id, created_date),
    INDEX idx_user_time (user_id, created_date),
    INDEX idx_result_time (result, created_date),
    INDEX idx_identifier_hash (identifier_hash, created_date)
) ENGINE=InnoDB
-- 初始分区基于 2026-05 部署。若部署月份晚于此，首次运行维护任务时会自动拆分 p_future
PARTITION BY RANGE COLUMNS(created_date) (
    PARTITION p202604 VALUES LESS THAN ('2026-05-01'),
    PARTITION p202605 VALUES LESS THAN ('2026-06-01'),
    PARTITION p202606 VALUES LESS THAN ('2026-07-01'),
    PARTITION p_future VALUES LESS THAN (MAXVALUE)
);

-- ============================================================
-- 14. 操作审计日志（按月分区）
-- ============================================================
CREATE TABLE audit_logs (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    user_id       BIGINT        NULL,
    client_id     VARCHAR(128)  NULL     COMMENT 'OAuth客户端操作来源',
    action        VARCHAR(100)  NOT NULL COMMENT '如 user.create / role.assign',
    resource_type VARCHAR(50)   NOT NULL,
    resource_id   VARCHAR(128)  NULL,
    detail        JSON          NULL,
    resource_owner_id BIGINT     NULL     COMMENT 'OAuth授权用户，SOC2 CC6.1 资源所有者追踪',
    ip_address    VARCHAR(45)   NULL,
    user_agent    VARCHAR(512)  NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_date  DATE          NOT NULL DEFAULT (CURRENT_DATE) COMMENT '分区键',
    PRIMARY KEY (id, created_date),
    INDEX idx_user_time (user_id, created_date),
    INDEX idx_action_time (action, created_date),
    INDEX idx_resource (resource_type, resource_id, created_date)
) ENGINE=InnoDB
-- 初始分区基于 2026-05 部署。若部署月份晚于此，首次运行维护任务时会自动拆分 p_future
PARTITION BY RANGE COLUMNS(created_date) (
    PARTITION p202604 VALUES LESS THAN ('2026-05-01'),
    PARTITION p202605 VALUES LESS THAN ('2026-06-01'),
    PARTITION p202606 VALUES LESS THAN ('2026-07-01'),
    PARTITION p_future VALUES LESS THAN (MAXVALUE)
);

-- ============================================================
-- 种子数据 (幂等，可重复执行)
-- ============================================================

-- 角色
INSERT IGNORE INTO roles (role_name, description, is_system) VALUES
('super_admin', '超级管理员（拥有所有权限）', 1),
('admin',       '管理员',                       1),
('user',        '普通注册用户',                  1);

-- 权限
INSERT IGNORE INTO permissions (code, name, resource_type, action) VALUES
('user:create', '创建用户',     'user',  'create'),
('user:read',   '查看用户',     'user',  'read'),
('user:update', '编辑用户',     'user',  'update'),
('user:delete', '删除用户',     'user',  'delete'),
('user:lock',   '锁定/解锁用户', 'user',  'lock'),
('role:create', '创建角色',     'role',  'create'),
('role:read',   '查看角色',     'role',  'read'),
('role:update', '编辑角色',     'role',  'update'),
('role:delete', '删除角色',     'role',  'delete'),
('role:assign', '分配角色',     'role',  'assign'),
('perm:read',   '查看权限列表',  'perm',  'read'),
('perm:assign', '分配权限',     'perm',  'assign'),
('client:create', '注册OAuth客户端', 'client', 'create'),
('client:read',   '查看OAuth客户端', 'client', 'read'),
('client:update', '编辑OAuth客户端', 'client', 'update'),
('client:delete', '删除OAuth客户端', 'client', 'delete'),
('apikey:create', '创建API Key',    'apikey', 'create'),
('apikey:revoke', '吊销API Key',    'apikey', 'revoke'),
('apikey:rotate', '轮换API Key',    'apikey', 'rotate'),
('audit:read',    '查看审计日志',     'audit',  'read');

-- 作用域
INSERT IGNORE INTO scopes (scope_key, description) VALUES
('self', '仅限自己的资源'),
('dept', '部门范围内'),
('org',  '组织范围内'),
('all',  '全局无限制');

-- super_admin 拥有所有权限(all作用域)
INSERT IGNORE INTO role_permissions (role_id, permission_id, scope_id)
SELECT r.id, p.id, s.id
FROM roles r
CROSS JOIN permissions p
JOIN scopes s ON s.scope_key = 'all'
WHERE r.role_name = 'super_admin';

-- admin 拥有除 audit:read 和 client:delete 外的所有权限
INSERT IGNORE INTO role_permissions (role_id, permission_id, scope_id)
SELECT r.id, p.id, s.id
FROM roles r
CROSS JOIN permissions p
JOIN scopes s ON s.scope_key = 'all'
WHERE r.role_name = 'admin'
  AND p.code NOT IN ('audit:read', 'client:delete');

-- user 仅有 self 作用域的基本查看权限
INSERT IGNORE INTO role_permissions (role_id, permission_id, scope_id)
SELECT r.id, p.id, s.id
FROM roles r
CROSS JOIN permissions p
JOIN scopes s ON s.scope_key = 'self'
WHERE r.role_name = 'user'
  AND p.code IN ('user:read', 'role:read', 'perm:read', 'user:update');

-- ============================================================
-- 分区自动维护：每月自动创建下月分区
-- ============================================================
DELIMITER $$

CREATE PROCEDURE add_next_month_partitions()
BEGIN
    DECLARE v_last_boundary DATE;
    DECLARE v_p1_name VARCHAR(20);
    DECLARE v_p2_name VARCHAR(20);
    DECLARE v_p1_boundary DATE;
    DECLARE v_p2_boundary DATE;

    -- 从现有分区中动态推算下个边界（排除 MAXVALUE 的 p_future）
    SELECT MAX(STR_TO_DATE(PARTITION_DESCRIPTION, '%Y-%m-%d'))
    INTO v_last_boundary
    FROM information_schema.PARTITIONS
    WHERE TABLE_SCHEMA = 'auth_system'
      AND TABLE_NAME = 'login_logs'
      AND PARTITION_DESCRIPTION != 'MAXVALUE';

    IF v_last_boundary IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No existing partition boundary found';
    END IF;

    SET v_p1_boundary = DATE_ADD(v_last_boundary, INTERVAL 1 MONTH);
    SET v_p2_boundary = DATE_ADD(v_last_boundary, INTERVAL 2 MONTH);
    SET v_p1_name = CONCAT('p', DATE_FORMAT(v_p1_boundary, '%Y%m'));
    SET v_p2_name = CONCAT('p', DATE_FORMAT(v_p2_boundary, '%Y%m'));

    SET @sql = CONCAT(
        'ALTER TABLE login_logs REORGANIZE PARTITION p_future INTO (',
        'PARTITION ', v_p1_name, ' VALUES LESS THAN (''', v_p1_boundary, '''), ',
        'PARTITION ', v_p2_name, ' VALUES LESS THAN (''', v_p2_boundary, '''), ',
        'PARTITION p_future VALUES LESS THAN (MAXVALUE))'
    );
    PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

    SET @sql = CONCAT(
        'ALTER TABLE audit_logs REORGANIZE PARTITION p_future INTO (',
        'PARTITION ', v_p1_name, ' VALUES LESS THAN (''', v_p1_boundary, '''), ',
        'PARTITION ', v_p2_name, ' VALUES LESS THAN (''', v_p2_boundary, '''), ',
        'PARTITION p_future VALUES LESS THAN (MAXVALUE))'
    );
    PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
END$$

DELIMITER ;

-- 每月 1 号凌晨自动追加分区
CREATE EVENT IF NOT EXISTS evt_add_partitions
ON SCHEDULE EVERY 1 MONTH
STARTS '2026-05-01 02:00:00'
ON COMPLETION PRESERVE
ENABLE
COMMENT '每月自动为 login_logs/audit_logs 追加两个分区并保留 p_future'
DO CALL add_next_month_partitions();
