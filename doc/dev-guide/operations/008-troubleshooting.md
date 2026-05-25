---
name: 故障排查指南
description: 故障排查指南
status: approved
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [troubleshooting, operations, debugging,常见问题]
---

# 故障排查指南

本文档提供常见问题和故障的诊断步骤及解决方案。

---

## 排查流程

### 标准排查流程

```
1. 收集信息
   ├─ 错误消息/日志
   ├─ 复现步骤
   └─ 环境信息

2. 定位问题
   ├─ 确定影响范围
   ├─ 查看相关日志
   └─ 检查监控指标

3. 分析问题
   ├─ 对比正常情况
   ├─ 查找变更历史
   └─ 假设验证

4. 解决问题
   ├─ 实施修复
   ├─ 验证效果
   └─ 记录总结
```

---

## 常见问题分类

### 1. 启动问题

#### 问题 1: 后端服务启动失败

**症状**:
```
ERROR: Failed to start service
Port 8081 is already in use
```

**排查步骤**:
```bash
# 1. 检查端口占用
lsof -i :8081
netstat -tlnp | grep 8081

# 2. 查看完整日志
tail -f logs/app.log

# 3. 检查数据库连接
mysql -h localhost -P 3306 -u root -p
```

**解决方案**:
```bash
# 方案 1: 停止占用端口的进程
kill -9 $(lsof -t -i :8081)

# 方案 2: 修改服务端口
# 编辑 application.yml
server:
  port: 8082

# 方案 3: 重启 Docker 容器
docker compose restart core-platform
```

#### 问题 2: 前端页面无法访问

**症状**:
```
浏览器显示：无法访问此网站
ERR_CONNECTION_REFUSED
```

**排查步骤**:
```bash
# 1. 检查 Nginx 状态
docker compose ps nginx
docker compose logs nginx

# 2. 检查前端容器
docker compose ps admin customer

# 3. 测试端口连通性
curl -I http://localhost

# 4. 检查防火墙
sudo ufw status
```

**解决方案**:
```bash
# 重启 Nginx
docker compose restart nginx

# 重新加载配置
docker compose exec nginx nginx -s reload

# 检查配置文件
docker compose exec nginx nginx -t
```

---

### 2. 数据库问题

#### 问题 1: 数据库连接失败

**症状**:
```
com.mysql.cj.jdbc.exceptions.CommunicationsException:
Communications link failure
```

**排查步骤**:
```bash
# 1. 检查 MySQL 是否运行
docker compose ps mysql

# 2. 查看 MySQL 日志
docker compose logs mysql

# 3. 测试连接
mysql -h localhost -P 3306 -u root -p

# 4. 检查网络连接
ping mysql
telnet mysql 3306
```

**常见原因和解决**:

| 原因 | 解决方案 |
|------|----------|
| MySQL 未启动 | `docker compose up -d mysql` |
| 密码错误 | 检查 `.env` 文件中的 `MYSQL_ROOT_PASSWORD` |
| 端口冲突 | 修改 docker-compose.yml 中的端口映射 |
| 磁盘空间不足 | `df -h` 检查并清理空间 |

#### 问题 2: Flyway 迁移失败

**症状**:
```
org.flywaydb.core.internal.exception.FlywayMigrationException:
Migration V1__init_schema.sql failed
```

**排查步骤**:
```bash
# 1. 查看 Flyway 历史
mysql -u root -p
USE auth_system;
SELECT * FROM flyway_schema_history;

# 2. 检查迁移脚本
cat src/backend/general-web-backend/core-platform/src/main/resources/db/migration/V*.sql

# 3. 查看应用日志
grep "Flyway" logs/app.log
```

**解决方案**:
```bash
# 方案 1: 修复失败的迁移
# 手动执行失败的 SQL
mysql -u root -p < V1__init_schema.sql

# 方案 2: 重置 Flyway 历史（开发环境）
TRUNCATE TABLE flyway_schema_history;

# 方案 3: 跳过特定版本
# 在 application.yml 添加
spring:
  flyway:
    ignore-migration-patterns: "*:failed"
```

---

### 3. Redis 问题

#### 问题 1: Redis 连接超时

**症状**:
```
io.lettuce.core.RedisConnectionTimeoutException:
Unable to connect within 60000 ms
```

**排查步骤**:
```bash
# 1. 检查 Redis 状态
docker compose ps redis

# 2. 测试连接
redis-cli -h localhost -p 6379 -a ${REDIS_PASSWORD} ping

# 3. 查看 Redis 日志
docker compose logs redis

# 4. 检查内存使用
docker compose exec redis redis-cli info memory
```

**解决方案**:
```bash
# 重启 Redis
docker compose restart redis

# 清理 Redis 数据（开发环境）
docker compose exec redis redis-cli FLUSHALL

# 增加内存限制
# docker-compose.yml
redis:
  deploy:
    resources:
      limits:
        memory: 1G
```

---

### 4. Node Agent 问题

#### 问题 1: Node 未连接

**症状**:
```
Node agent not connected
Server status: offline
```

**排查步骤**:
```bash
# 1. 检查 Node Agent 状态
systemctl status dst-node-agent

# 2. 查看 Node Agent 日志
journalctl -u dst-node-agent -f

# 3. 检查配置文件
cat /opt/dst-node/config.json

# 4. 测试 WebSocket 连接
curl -i -N -H "Connection: Upgrade" -H "Upgrade: websocket" \
  -H "Host: your-server:80" \
  -H "Origin: http://your-server" \
  http://your-server/node
```

**解决方案**:
```bash
# 重启 Node Agent
sudo systemctl restart dst-node-agent

# 重新配置
sudo vim /opt/dst-node/config.json
# 确保 platform_url 和 token 正确

# 检查防火墙
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
```

#### 问题 2: 命令执行失败

**症状**:
```
Command execution failed
Timeout waiting for response
```

**排查步骤**:
```bash
# 1. 查看 DST 服务器日志
tail -f /opt/dst-server/MyDediServer/Log.txt

# 2. 检查 DST 进程
ps aux | grep dontstarve_dedicated_server

# 3. 检查资源使用
top
free -h
df -h
```

**解决方案**:
```bash
# 重启 DST 服务器
/opt/dst-server/restart.sh

# 清理缓存
rm -rf /opt/dst-server/cache/*

# 增加资源限制
# 编辑 systemd 服务文件
sudo vim /etc/systemd/system/dst-node-agent.service
# 增加 LimitNOFILE=65535
```

---

### 5. 性能问题

#### 问题 1: 响应缓慢

**症状**:
```
API 响应时间 > 5 秒
页面加载缓慢
```

**排查步骤**:
```bash
# 1. 检查 CPU 使用
top -c

# 2. 检查内存使用
free -h
vmstat 1 5

# 3. 检查磁盘 IO
iostat -x 1 5

# 4. 检查慢查询
mysql -u root -p
SHOW PROCESSLIST;
```

**优化方案**:
```bash
# JVM 优化
# 编辑启动脚本
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# MySQL 优化
# 添加索引
ALTER TABLE servers ADD INDEX idx_status (status);

# Redis 缓存
# 清理过期 key
redis-cli --bigkeys
```

#### 问题 2: 内存泄漏

**症状**:
```
内存使用持续增长
频繁 Full GC
OutOfMemoryError
```

**排查步骤**:
```bash
# 1. 生成堆转储
jmap -dump:format=b,file=heap.hprof <pid>

# 2. 分析堆转储
# 使用 MAT (Memory Analyzer Tool) 打开 heap.hprof

# 3. 查看 GC 日志
jstat -gc <pid> 1000
```

**解决方案**:
```bash
# 临时方案：重启服务
docker compose restart core-platform

# 长期方案：修复代码
# - 检查静态集合类
# - 检查未关闭的资源
# - 检查监听器未注销
```

---

### 6. 部署问题

#### 问题 1: Docker 部署失败

**症状**:
```
ERROR: Service 'core-platform' failed to start
Container exited with code 1
```

**排查步骤**:
```bash
# 1. 查看容器日志
docker compose logs core-platform

# 2. 检查配置文件
docker compose config

# 3. 检查环境变量
cat .env

# 4. 测试单个服务
docker compose up -d mysql
docker compose up -d redis
docker compose up -d core-platform
```

**常见错误和解决**:

| 错误 | 解决方案 |
|------|----------|
| Port already in use | 修改端口映射或停止占用进程 |
| Volume mount failed | 检查目录权限和存在性 |
| Out of memory | 增加 Docker 资源限制 |
| Network failed | `docker network prune` |

---

## 诊断工具

### 日志查看

```bash
# 实时查看日志
docker compose logs -f

# 查看特定服务
docker compose logs -f core-platform

# 查看最近 100 行
docker compose logs --tail=100

# 导出日志
docker compose logs > all-logs.txt
```

### 性能监控

```bash
# Spring Boot Actuator
curl http://localhost:8081/actuator/health
curl http://localhost:8081/actuator/metrics

# Prometheus 指标
curl http://localhost:8081/actuator/prometheus

# 数据库连接池
curl http://localhost:8081/actuator/metrics/hikaricp.connections.active
```

### 网络诊断

```bash
# 测试 API
curl -X GET http://localhost/api/v1/users \
  -H "Authorization: Bearer YOUR_TOKEN"

# 测试 WebSocket
wscat -c ws://localhost/node

# 端口扫描
nmap -p 80,443,8081,8082,8083 localhost
```

---

## 应急方案

### 服务完全不可用

```bash
# 1. 快速恢复
cd deploy/docker
./start-all.sh

# 2. 验证服务
docker compose ps

# 3. 检查日志
docker compose logs -f
```

### 数据丢失

```bash
# 1. 从备份恢复
cd /backup/mysql
gunzip auth_system_20260522.sql.gz
mysql -u root -p auth_system < auth_system_20260522.sql

# 2. 重启服务
docker compose restart

# 3. 验证数据
mysql -u root -p -e "SELECT COUNT(*) FROM auth_system.users;"
```

### 安全事件

```bash
# 1. 隔离受影响的服务
docker compose stop affected-service

# 2. 保留现场
docker compose logs > incident-logs.txt

# 3. 重置凭证
# - 修改数据库密码
# - 重置 JWT 密钥
# - 更新 API Key

# 4. 审计日志
grep "Failed login" logs/*.log
```

---

## 预防性维护

### 每日检查

```bash
# 服务状态
docker compose ps

# 磁盘空间
df -h

# 错误日志
grep "ERROR" logs/*.log | tail -20
```

### 每周维护

```bash
# 清理旧日志
find logs -name "*.log" -mtime +7 -delete

# 清理 Docker
docker system prune -f

# 备份数据库
./tools/backup.sh
```

### 每月维护

```bash
# 更新依赖
./mvnw versions:display-dependency-updates

# 性能测试
ab -n 1000 -c 10 http://localhost/api/v1/users

# 安全扫描
./mvnw org.owasp:dependency-check-maven:check
```

---

## 获取帮助

### 内部资源

- [开发环境搭建](../setup/001-local-setup.md)
- [调试指南](../guides/004-debugging.md)
- [架构概览](../../architecture/overview/001-system-overview.md)

### 外部资源

- [Spring Boot 文档](https://spring.io/projects/spring-boot)
- [Docker 故障排查](https://docs.docker.com/config/containers/troubleshoot/)
- [MySQL 错误代码](https://dev.mysql.com/doc/mysql-errors/8.0/en/server-error-reference.html)

---

**最后更新**: 2026-05-22  
**维护人**: @TechLead
