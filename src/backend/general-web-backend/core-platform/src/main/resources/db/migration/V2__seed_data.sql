-- ============================================================
-- V2: Seed data — idempotent (INSERT IGNORE)
-- ============================================================

-- 角色
INSERT IGNORE INTO roles (role_name, description, is_system) VALUES
('super_admin', '超级管理员（拥有所有权限）', 1),
('admin',       '管理员',                       1),
('user',        '普通注册用户',                  1);

-- Set up role hierarchy: super_admin → admin → user
UPDATE roles SET parent_role_id = (SELECT id FROM (SELECT id FROM roles WHERE role_name = 'super_admin') AS t) WHERE role_name = 'admin';
UPDATE roles SET parent_role_id = (SELECT id FROM (SELECT id FROM roles WHERE role_name = 'admin') AS t) WHERE role_name = 'user';

-- 权限 (20 条，含 apikey:rotate)
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
