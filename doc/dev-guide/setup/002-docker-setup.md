---
name: Docker 开发环境
description: Docker 开发环境
status: approved
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [setup, docker, development, container]
---

# Docker 开发环境

本文档介绍如何使用 Docker 进行容器化开发，实现开发环境的一致性和可移植性。

## 前置条件

- Docker 24+ 已安装
- Docker Compose v2+ 已安装
- 已完成 [本地开发环境搭建](001-local-setup.md)

---

## Docker 开发模式

### 模式对比

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| **全容器化** | 所有服务运行在容器中 | 快速体验、CI/CD |
| **混合模式** | 数据库/缓存用 Docker，应用本地运行 | 日常开发、调试 |
| **本地模式** | 所有服务本地运行 | 深度调试、性能测试 |

**推荐**: 混合模式（数据库/缓存容器化，应用本地运行）

---

## 快速启动数据库和缓存

### 启动命令

```bash
cd deploy/docker

# 只启动数据库和 Redis
docker compose up -d mysql mysql-template mysql-server redis

# 查看状态
docker compose ps

# 查看日志
docker compose logs -f mysql
```

### 连接信息

```yaml
MySQL (auth_system):
  host: localhost
  port: 3306
  username: root
  password: ${MYSQL_ROOT_PASSWORD}
  database: auth_system

MySQL (dst_templates):
  host: localhost
  port: 3307
  username: root
  password: ${MYSQL_ROOT_PASSWORD}
  database: dst_templates

MySQL (dst_servers):
  host: localhost
  port: 3308
  username: root
  password: ${MYSQL_ROOT_PASSWORD}
  database: dst_servers

Redis:
  host: localhost
  port: 6379
  password: ${REDIS_PASSWORD}
```

---

## 容器化开发配置

### 开发环境 Docker Compose

创建 `deploy/docker/docker-compose.dev.yml`:

```yaml
version: '3.8'

services:
  # 开发数据库（复用生产配置）
  mysql:
    extends:
      file: docker-compose.yml
      service: mysql
    ports:
      - "3306:3306"
  
  mysql-template:
    extends:
      file: docker-compose.yml
      service: mysql-template
    ports:
      - "3307:3307"
  
  mysql-server:
    extends:
      file: docker-compose.yml
      service: mysql-server
    ports:
      - "3308:3308"
  
  redis:
    extends:
      file: docker-compose.yml
      service: redis
    ports:
      - "6379:6379"
```

### 启动开发环境

```bash
# 启动开发服务
docker compose -f docker-compose.dev.yml up -d

# 停止开发服务
docker compose -f docker-compose.dev.yml down
```

---

## 应用容器化运行

### 构建后端镜像

```bash
cd src/backend/general-web-backend

# 构建所有服务镜像
docker compose -f ../../deploy/docker/docker-compose.yml build
```

### 本地运行后端服务（推荐）

**优势**:
- 调试方便（IDE 断点）
- 热更新（Spring DevTools）
- 日志查看方便

```bash
# 启动核心平台
cd core-platform
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 启动模板服务
cd ../template-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 启动服务器服务
cd ../server-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 容器运行后端服务

**优势**:
- 环境一致性
- 资源隔离
- 接近生产环境

```bash
cd deploy/docker

# 启动所有后端服务
docker compose up -d core-platform template-service server-service

# 查看日志
docker compose logs -f core-platform

# 进入容器调试
docker compose exec core-platform-container sh
```

---

## 前端容器化开发

### 开发模式（推荐）

本地运行前端，通过代理连接到后端：

```bash
cd src/frontend/admin

# 修改 vite.config.ts
export default defineConfig({
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})

# 启动开发服务器
npm run dev
```

### 容器运行前端

```bash
cd deploy/docker

# 构建并启动前端容器
docker compose up -d admin customer

# 查看日志
docker compose logs -f admin
```

---

## 开发工作流

### 典型开发流程

```bash
# 1. 启动数据库和缓存
cd deploy/docker
docker compose up -d mysql redis

# 2. 启动后端服务（IDE 运行）
# 在 IDEA 中启动 core-platform, template-service, server-service

# 3. 启动前端服务
cd src/frontend/admin
npm run dev

# 4. 开发调试
# - 修改代码，IDE 自动热更新
# - 浏览器访问 http://localhost:3000

# 5. 提交前测试
./mvnw test
npm run test

# 6. 清理环境（可选）
docker compose down
```

---

## 数据库管理

### 使用 Docker 管理数据库

```bash
# 查看数据库容器
docker compose ps mysql

# 进入 MySQL 容器
docker compose exec mysql mysql -u root -p

# 导入 SQL 文件
docker compose exec -T mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} auth_system < init.sql

# 备份数据库
docker compose exec mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} auth_system > backup.sql
```

### 使用本地 MySQL 客户端

```bash
# 连接 MySQL
mysql -h 127.0.0.1 -P 3306 -u root -p

# 使用 MySQL Workbench / DataGrip / Navicat
# Host: localhost
# Port: 3306
# Username: root
# Password: ${MYSQL_ROOT_PASSWORD}
```

---

## 常见问题

### Q1: Docker 容器无法启动

**排查步骤**:
```bash
# 1. 查看日志
docker compose logs mysql

# 2. 检查端口占用
lsof -i :3306

# 3. 检查磁盘空间
df -h

# 4. 清理并重启
docker compose down -v
docker compose up -d
```

### Q2: 数据库连接超时

**解决方案**:
```bash
# 等待 MySQL 完全启动（首次启动需要 2-3 分钟）
docker compose logs -f mysql | grep "ready for connections"

# 重启后端服务
docker compose restart core-platform
```

### Q3: Docker 占用磁盘空间过大

**清理命令**:
```bash
# 查看 Docker 磁盘使用
docker system df

# 清理未使用的容器、网络、镜像
docker system prune

# 清理所有未使用的镜像
docker image prune -a

# 清理卷（谨慎使用，会删除数据）
docker volume prune
```

### Q4: Mac/Windows 性能问题

**优化建议**:
```bash
# 1. 增加 Docker 资源分配
# Docker Desktop → Settings → Resources
# - CPU: 4+ cores
# - Memory: 8+ GB
# - Swap: 1+ GB

# 2. 使用命名卷而非 bind mount
# 在 docker-compose.yml 中使用 volumes

# 3. 使用 .dockerignore 减少上下文
```

---

## 性能优化

### 构建优化

**Dockerfile 优化**:
```dockerfile
# 多阶段构建
FROM eclipse-temurin:21-jre-alpine AS builder
COPY . .
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine
COPY --from=builder target/*.jar app.jar
EXPOSE 8081
CMD ["java", "-jar", "app.jar"]
```

**构建缓存**:
```bash
# 使用构建缓存
docker build --cache-from=myapp:latest -t myapp:latest .

# 使用 BuildKit
export DOCKER_BUILDKIT=1
docker build -t myapp:latest .
```

### 运行优化

**资源限制**:
```yaml
services:
  core-platform:
    deploy:
      resources:
        limits:
          cpus: '1.5'
          memory: 768M
        reservations:
          cpus: '0.5'
          memory: 256M
```

---

## 下一步

- 📖 [编码规范](../guides/003-coding-standards.md)
- 📖 [调试指南](../guides/004-debugging.md)
- 📖 [测试指南](../guides/005-testing.md)
- 📖 [部署指南](../deployment/006-docker-guide.md)

---

**参考链接**:
- [Docker 官方文档](https://docs.docker.com/)
- [Docker Compose 官方文档](https://docs.docker.com/compose/)
- [Docker 最佳实践](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)

**最后更新**: 2026-05-22
