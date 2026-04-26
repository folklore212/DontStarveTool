#!/usr/bin/env python3
"""数据库功能验证测试套件"""

import os, sys, time
from datetime import date

# ---- 配置 ----
MYSQL_HOST = os.environ["MYSQL_HOST"]
MYSQL_PORT = int(os.environ["MYSQL_PORT"])
MYSQL_USER = os.environ["MYSQL_USER"]
MYSQL_PASSWORD = os.environ["MYSQL_PASSWORD"]
MYSQL_DB = os.environ["MYSQL_DATABASE"]

REDIS_HOST = os.environ["REDIS_HOST"]
REDIS_PORT = int(os.environ["REDIS_PORT"])
REDIS_PASSWORD = os.environ["REDIS_PASSWORD"]

passed = 0
failed = 0

def test(name, fn):
    global passed, failed
    try:
        fn()
        print(f"  ✅ {name}")
        passed += 1
    except Exception as e:
        print(f"  ❌ {name}: {e}")
        failed += 1

def assert_eq(a, b, msg=""):
    assert a == b, f"expected {b}, got {a} {msg}"

def assert_ge(a, b, msg=""):
    assert a >= b, f"expected >= {b}, got {a} {msg}"

# ---- MySQL ----
print("\n" + "=" * 60)
print("MySQL 功能测试")
print("=" * 60)

import mysql.connector

mysql_conn = None
def get_mysql():
    global mysql_conn
    if mysql_conn is None:
        for _ in range(30):
            try:
                mysql_conn = mysql.connector.connect(
                    host=MYSQL_HOST, port=MYSQL_PORT, user=MYSQL_USER,
                    password=MYSQL_PASSWORD, database=MYSQL_DB,
                    charset="utf8mb4"
                )
                return mysql_conn
            except Exception:
                time.sleep(2)
        raise Exception("MySQL 连接超时")
    return mysql_conn

def mysql_query(sql, params=None):
    c = get_mysql().cursor(dictionary=True)
    c.execute(sql, params)
    return c.fetchall()

def mysql_exec(sql, params=None):
    conn = get_mysql()
    c = conn.cursor()
    c.execute(sql, params)
    conn.commit()

# 1. 表数量
def t_tables_count():
    rows = mysql_query("SHOW TABLES")
    assert_eq(len(rows), 17, f"tables: {[list(r.values())[0] for r in rows]}")

# 2. 预期表名
def t_expected_tables():
    expected = {
        "users", "user_profiles", "user_auths", "user_credentials_history",
        "user_mfa", "roles", "permissions", "scopes",
        "user_roles", "role_permissions", "oauth_clients", "api_keys",
        "login_logs", "audit_logs", "client_grant_types",
        "password_reset_tokens", "refresh_tokens"
    }
    actual = {list(r.values())[0] for r in mysql_query("SHOW TABLES")}
    assert_eq(actual, expected, f"missing={expected-actual}, extra={actual-expected}")

# 3. login_logs 分区
def t_login_logs_partitions():
    rows = mysql_query("""
        SELECT PARTITION_NAME FROM information_schema.PARTITIONS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME='login_logs' AND PARTITION_NAME IS NOT NULL
        ORDER BY PARTITION_ORDINAL_POSITION
    """, (MYSQL_DB,))
    names = [r["PARTITION_NAME"] for r in rows]
    assert_eq(len(names), 4, str(names))
    assert "p202604" in names
    assert "p_future" in names

# 4. audit_logs 分区
def t_audit_logs_partitions():
    rows = mysql_query("""
        SELECT PARTITION_NAME FROM information_schema.PARTITIONS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME='audit_logs' AND PARTITION_NAME IS NOT NULL
        ORDER BY PARTITION_ORDINAL_POSITION
    """, (MYSQL_DB,))
    assert_eq(len([r for r in rows]), 4)

# 5. 种子角色
def t_seed_roles():
    rows = mysql_query("SELECT role_name FROM roles ORDER BY id")
    assert_eq([r["role_name"] for r in rows], ["super_admin", "admin", "user"])

# 6. 种子权限数量
def t_seed_permissions():
    rows = mysql_query("SELECT COUNT(*) AS cnt FROM permissions")
    assert_eq(rows[0]["cnt"], 20)

# 7. super_admin 拥有所有权限
def t_superadmin_has_all_perms():
    rows = mysql_query("""
        SELECT COUNT(*) AS cnt FROM role_permissions rp
        JOIN roles r ON r.id = rp.role_id
        WHERE r.role_name = 'super_admin'
    """)
    assert_eq(rows[0]["cnt"], 20)

# 8. 外键约束数量
def t_foreign_keys():
    rows = mysql_query("""
        SELECT COUNT(*) AS cnt FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA=%s AND CONSTRAINT_TYPE='FOREIGN KEY'
    """, (MYSQL_DB,))
    assert_eq(rows[0]["cnt"], 17)

# 9. 软删除字段
def t_soft_delete():
    rows = mysql_query("""
        SELECT TABLE_NAME FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA=%s AND COLUMN_NAME='deleted_at'
        ORDER BY TABLE_NAME
    """, (MYSQL_DB,))
    tables = [r["TABLE_NAME"] for r in rows]
    assert_eq(tables, ["api_keys", "oauth_clients", "roles", "user_auths", "user_roles", "users"])

# 10. 角色继承
def t_role_hierarchy():
    rows = mysql_query("""
        SELECT COLUMN_NAME FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME='roles' AND COLUMN_NAME='parent_role_id'
    """, (MYSQL_DB,))
    assert_eq(len(rows), 1)

# 11. 用户 CRUD（含软删除验证）
def t_user_crud():
    uid = int(time.time() * 1e6)  # 微秒级伪 Snowflake
    mysql_exec(
        "INSERT INTO users (user_id, username, nickname) VALUES (%s, %s, %s)",
        (uid, f"test_{uid}", "测试用户")
    )
    row = mysql_query("SELECT username, status, deleted_at FROM users WHERE user_id=%s", (uid,))
    assert_eq(row[0]["username"], f"test_{uid}")
    assert_eq(row[0]["status"], 0)
    assert_eq(row[0]["deleted_at"], 0)

    mysql_exec("UPDATE users SET nickname=%s WHERE user_id=%s", ("已更新", uid))
    row = mysql_query("SELECT nickname FROM users WHERE user_id=%s", (uid,))
    assert_eq(row[0]["nickname"], "已更新")

    # 使用软删除而非硬删除
    mysql_exec("UPDATE users SET deleted_at=%s WHERE user_id=%s",
               (int(time.time() * 1000), uid))
    row = mysql_query("SELECT user_id FROM users WHERE user_id=%s AND deleted_at=0", (uid,))
    assert_eq(len(row), 0, "软删除后 deleted_at=0 条件应查不到")

# 12. 分区写入和裁剪
def t_partition_insert_and_prune():
    mysql_exec("""
        INSERT INTO login_logs (identifier_hash, identity_type, result)
        VALUES (SHA2('perf_test@test.com', 256), 'email', 'success')
    """)
    row = mysql_query("SELECT * FROM login_logs ORDER BY id DESC LIMIT 1")
    # 允许 ±1 天偏差（容器 UTC vs MySQL Asia/Shanghai 时区差）
    delta = abs((row[0]["created_date"] - date.today()).days)
    assert delta <= 1, f"created_date too far from today: {row[0]['created_date']}"
    assert_eq(row[0]["result"], "success")

    plan = mysql_query("EXPLAIN SELECT * FROM login_logs WHERE created_date=%s",
                       (row[0]["created_date"],))
    partitions = plan[0].get("partitions", "")
    assert partitions == "p202604", f"partition pruning failed: {partitions}"

# 13. password_changed_at 字段
def t_password_changed_at():
    rows = mysql_query("""
        SELECT COLUMN_NAME FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME='users' AND COLUMN_NAME='password_changed_at'
    """, (MYSQL_DB,))
    assert_eq(len(rows), 1)

# 14. 事件调度器开启
def t_event_scheduler_on():
    rows = mysql_query("SELECT @@event_scheduler AS val")
    assert rows[0]["val"] == "ON", f"event_scheduler: {rows[0]['val']}"

# 15. 分区维护事件存在
def t_partition_event():
    rows = mysql_query("SHOW EVENTS IN auth_system WHERE Name='evt_add_partitions'")
    assert_eq(len(rows), 1)
    assert_eq(rows[0]["Status"], "ENABLED")

# 16. user_mfa.key_version
def t_mfa_key_version():
    rows = mysql_query("""
        SELECT COLUMN_NAME, COLUMN_DEFAULT FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME='user_mfa' AND COLUMN_NAME='key_version'
    """, (MYSQL_DB,))
    assert_eq(len(rows), 1)
    assert_eq(rows[0]["COLUMN_DEFAULT"], "1")

# 17. user_roles 含 id PK + updated_at + deleted_at
def t_user_roles_restructured():
    rows = mysql_query("""
        SELECT COLUMN_NAME FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME='user_roles'
        AND COLUMN_NAME IN ('id','updated_at','deleted_at')
        ORDER BY COLUMN_NAME
    """, (MYSQL_DB,))
    assert_eq([r["COLUMN_NAME"] for r in rows], ["deleted_at","id","updated_at"])

# 18. user_roles uk_user_role_scope
def t_user_roles_uk():
    rows = mysql_query("""
        SELECT INDEX_NAME FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME='user_roles' AND INDEX_NAME='uk_user_role_scope'
        GROUP BY INDEX_NAME
    """, (MYSQL_DB,))
    assert_eq(len(rows), 1)

# 19. login_logs/audit_logs/api_keys 新增索引
def t_new_indexes():
    checks = {
        "api_keys": "idx_expires",
        "login_logs": "idx_identifier_hash",
        "user_roles": "idx_expires",
    }
    for tbl, idx in checks.items():
        rows = mysql_query("""
            SELECT INDEX_NAME FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s AND INDEX_NAME=%s
            GROUP BY INDEX_NAME
        """, (MYSQL_DB, tbl, idx))
        assert_eq(len(rows), 1, f"{tbl}.{idx} missing")

# 20. audit_logs.idx_resource 含 created_date(3列)
def t_audit_logs_idx_resource():
    cols = mysql_query("""
        SELECT COLUMN_NAME FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA=%s AND TABLE_NAME='audit_logs' AND INDEX_NAME='idx_resource'
        ORDER BY SEQ_IN_INDEX
    """, (MYSQL_DB,))
    assert_eq([c["COLUMN_NAME"] for c in cols],
              ["resource_type", "resource_id", "created_date"])

# 21. INSERT IGNORE 幂等
def t_insert_ignore_idempotent():
    mysql_exec("INSERT IGNORE INTO roles (role_name, description, is_system) VALUES ('super_admin', 'dup', 1)")
    rows = mysql_query("SELECT COUNT(*) AS cnt FROM roles WHERE role_name='super_admin'")
    assert_eq(rows[0]["cnt"], 1)

# 22. 软删除级联触发器
def t_soft_delete_triggers():
    triggers = mysql_query("""
        SELECT TRIGGER_NAME FROM information_schema.TRIGGERS
        WHERE TRIGGER_SCHEMA=%s AND TRIGGER_NAME IN ('trg_users_soft_delete','trg_roles_soft_delete')
    """, (MYSQL_DB,))
    assert_eq(len(triggers), 2, f"expected 2 triggers: {[t['TRIGGER_NAME'] for t in triggers]}")

# 23. 新增表存在
def t_new_tables():
    for tbl in ["client_grant_types", "password_reset_tokens", "refresh_tokens"]:
        rows = mysql_query(f"SHOW TABLES LIKE '{tbl}'")
        assert_eq(len(rows), 1, f"{tbl} missing")

# ---- Redis ----
print("\n" + "=" * 60)
print("Redis 功能测试")
print("=" * 60)

import redis

r = None
def get_redis():
    global r
    if r is None:
        r = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, password=REDIS_PASSWORD,
                         decode_responses=True, socket_connect_timeout=5)
        r.ping()
    return r

# 13. PING
def t_redis_ping():
    assert get_redis().ping()

# 14. SET/GET/DEL
def t_redis_crud():
    rr = get_redis()
    rr.set("__test_key__", "hello world", ex=60)
    assert_eq(rr.get("__test_key__"), "hello world")
    rr.delete("__test_key__")
    assert rr.get("__test_key__") is None

# 15. 过期
def t_redis_expire():
    rr = get_redis()
    rr.set("__test_ttl__", "tmp", ex=2)
    assert rr.exists("__test_ttl__")
    time.sleep(3)
    assert not rr.exists("__test_ttl__")

# 16. Hash
def t_redis_hash():
    rr = get_redis()
    rr.hset("__test_hash__", mapping={"field1": "v1", "field2": "v2"})
    assert_eq(rr.hget("__test_hash__", "field1"), "v1")
    assert_eq(rr.hgetall("__test_hash__"), {"field1": "v1", "field2": "v2"})
    rr.delete("__test_hash__")

# 17. 认证失败
def t_redis_auth_fail():
    bad = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, password="wrong",
                       socket_connect_timeout=3)
    try:
        bad.ping()
        raise AssertionError("should have raised")
    except redis.AuthenticationError:
        pass  # expected

# ---- 运行 ----
print("\n" + "=" * 60)
print("开始测试")
print("=" * 60)

# MySQL tests
test("17张表全部存在",    t_tables_count)
test("所有表名正确",      t_expected_tables)
test("login_logs 4个分区", t_login_logs_partitions)
test("audit_logs 4个分区", t_audit_logs_partitions)
test("3个种子角色",        t_seed_roles)
test("20个种子权限",       t_seed_permissions)
test("super_admin拥有全部20权限", t_superadmin_has_all_perms)
test("17条外键约束",       t_foreign_keys)
test("6表含软删除字段",    t_soft_delete)
test("角色继承字段",       t_role_hierarchy)
test("用户CRUD操作",       t_user_crud)
test("分区写入和裁剪",     t_partition_insert_and_prune)
test("password_changed_at字段", t_password_changed_at)
test("事件调度器已开启",  t_event_scheduler_on)
test("分区维护事件存在",  t_partition_event)
test("MFA密钥版本号",     t_mfa_key_version)
test("user_roles重构(id+updated+deleted)", t_user_roles_restructured)
test("user_roles唯一约束", t_user_roles_uk)
test("三表新增索引",      t_new_indexes)
test("audit_logs三列索引", t_audit_logs_idx_resource)
test("INSERT IGNORE幂等", t_insert_ignore_idempotent)
test("软删除触发器",     t_soft_delete_triggers)
test("新表(client_grant/reset/refresh)", t_new_tables)

# Redis tests
test("Redis PING",         t_redis_ping)
test("Redis SET/GET/DEL",  t_redis_crud)
test("Redis Key过期",      t_redis_expire)
test("Redis Hash操作",     t_redis_hash)
test("Redis 认证失败拒绝", t_redis_auth_fail)

# ---- 结果 ----
print("\n" + "=" * 60)
print(f"结果: {passed} 通过, {failed} 失败, 共 {passed+failed} 项")
print("=" * 60)

sys.exit(0 if failed == 0 else 1)
