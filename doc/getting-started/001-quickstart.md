---
name: 5 分钟快速体验
description: 使用 Docker Compose 一键启动 DST 管理平台
status: approved
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [quickstart, docker, getting-started]
---

# 5 分钟快速体验

本文档将帮助你在 **5 分钟内** 通过 Docker Compose 启动 DST 管理平台，快速体验核心功能。

## 前置条件

- ✅ Docker 24+ 已安装
- ✅ Docker Compose v2+ 已安装
- ✅ 端口 80, 3306-3308, 6379, 8081-8090 未被占用
- ✅ 至少 4GB 可用内存
- ✅ 至少 10GB 可用磁盘空间

## 快速启动

### 步骤 1: 克隆项目

```bash
git clone https://github.com/your-org/DontStarveTool.git
cd DontStarveTool/deploy/docker
```

### 步骤 2: 配置环境变量

```bash
# 复制环境变量示例文件
cp .env.example .env

# 编辑环境变量（可选，使用默认值也可）
vim .env
```

**关键配置项**:
```bash
# 服务器规模（small=1.8GB, normal=4GB+）
SERVER_SIZE=small

# 数据库密码（生产环境请修改）
MYSQL_ROOT_PASSWORD=change_me
REDIS_PASSWORD=change_me_too

# AES 加密密钥（生产环境必须修改）
AES_KEY_V0=change_me_aes_v0
AES_KEY_V1=change_me_aes_v1
```

### 步骤 3: 一键启动

```bash
# 启动所有服务（11 个容器）
./start-all.sh

# 或者使用 docker compose 命令
docker compose up -d
```

### 步骤 4: 检查服务状态

```bash
# 查看所有容器状态
docker compose ps

# 预期输出：
# NAME                          STATUS         PORTS
# api-gateway                   Up (healthy)   0.0.0.0:80->80/tcp
# auth-admin-container          Up             0.0.0.0:3000->3000/tcp
# auth-customer-container       Up             0.0.0.0:80->80/tcp
# core-platform-container       Up (healthy)   8081/tcp
# node-gateway-container        Up             8090/tcp
# server-mysql-container        Up             3308/tcp
# server-service-container      Up (healthy)   8083/tcp
# steam-cache-service-container Up             8084/tcp
# template-mysql-container      Up             3307/tcp
# template-service-container    Up (healthy)   8082/tcp
# auth-mysql-container          Up             3306/tcp
# auth-redis-container          Up (healthy)   6379/tcp
```

### 步骤 5: 访问平台

打开浏览器访问：

| 服务 | URL | 说明 |
|------|-----|------|
| Customer 前端 | http://localhost | 用户界面 |
| Admin 管理后台 | http://localhost:3000 | 管理员界面 |
| API Gateway | http://localhost/api | API 入口 |

**默认账号**（开发环境）:
- 用户名：`admin`
- 密码：查看数据库初始化脚本或 `.env` 文件

## 核心功能体验

### 1. 创建服务器模板

1. 登录 Admin 管理后台
2. 导航到 **模板管理** → **创建模板**
3. 填写模板信息：
   - 模板名称：`我的第一个服务器`
   - 世界预设：选择预设配置
   - 模组配置：添加需要的模组

### 2. 部署服务器

1. 导航到 **服务器管理** → **部署向导**
2. 选择刚创建的模板
3. 配置服务器参数：
   - 服务器名称
   - 最大玩家数
   - 密码（可选）
4. 点击 **部署**，等待部署完成

### 3. 查看服务器状态

1. 导航到 **服务器列表**
2. 查看服务器运行状态
3. 可以执行操作：
   - 启动/停止/重启
   - 查看玩家列表
   - 查看服务器日志
   - 备份/回档

## 常见问题

### Q1: 容器启动失败

**问题**: `docker compose up` 报错

**解决方案**:
```bash
# 1. 检查 Docker 是否运行
docker info

# 2. 检查端口是否被占用
netstat -tlnp | grep -E '80|3306|6379|808'

# 3. 查看容器日志
docker compose logs -f <容器名>

# 4. 清理并重启
docker compose down -v
docker compose up -d
```

### Q2: 数据库连接失败

**问题**: 后端服务无法连接数据库

**解决方案**:
```bash
# 等待数据库完全启动（首次启动需要 2-3 分钟）
docker compose logs -f mysql

# 看到 "ready for connections" 后再启动其他服务
docker compose restart core-platform template-service server-service
```

### Q3: 前端页面无法访问

**问题**: 浏览器显示无法连接

**解决方案**:
```bash
# 1. 检查前端容器状态
docker compose ps admin customer

# 2. 查看前端日志
docker compose logs admin customer

# 3. 检查 Nginx 配置
docker compose exec nginx nginx -t

# 4. 重启 Nginx
docker compose restart nginx
```

## 下一步

- 📖 [本地开发环境搭建](002-local-setup.md) - 搭建完整的开发环境
- 📖 [架构概览](003-architecture-overview.md) - 了解系统架构
- 📖 [部署指南](../dev-guide/deployment/006-docker-guide.md) - 详细部署文档

## 资源清理

```bash
# 停止所有服务
docker compose down

# 停止并删除数据卷（谨慎使用）
docker compose down -v

# 查看磁盘使用
docker system df
```

---

**参考链接**:
- [Docker Compose 官方文档](https://docs.docker.com/compose/)
- [部署拓扑图](../architecture/design/005-deployment-topology.md)
- [数据库设计](../reference/database/001-schema-reference.md)

**最后更新**: 2026-05-22
