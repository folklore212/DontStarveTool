---
name: DST 管理平台
description: DST 管理平台
status: approved
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [documentation]
---

# DST 管理平台

> Don't Starve Together 服务器管理平台 - 专业、高效、易用的 DST 服务器管理解决方案

[![CI](https://github.com/your-org/DontStarveTool/actions/workflows/maven-publish.yml/badge.svg)](https://github.com/your-org/DontStarveTool/actions)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![React](https://img.shields.io/badge/React-18.3-blue.svg)](https://react.dev/)

---

## 📖 文档

- **[📚 完整文档](https://your-org.github.io/DontStarveTool/)** - 基于 Docusaurus 的文档网站
- **[快速开始](doc/getting-started/001-quickstart.md)** - 5 分钟快速体验
- **[开发指南](doc/dev-guide/)** - 开发者文档
- **[用户指南](doc/user-guide/)** - 功能说明和使用教程
- **[API 参考](doc/reference/api/)** - REST API 和 JSON-RPC 协议

---

## 🚀 快速开始

### 前置条件

- Docker 24+
- Docker Compose v2+
- 4GB+ 内存
- 10GB+ 磁盘空间

### 一键启动

```bash
# 克隆项目
git clone https://github.com/your-org/DontStarveTool.git
cd DontStarveTool/deploy/docker

# 配置环境变量
cp .env.example .env

# 启动所有服务
./start-all.sh

# 访问平台
# Customer 前端：http://localhost
# Admin 管理后台：http://localhost:3000
```

详细步骤请参考 [5 分钟快速体验](doc/getting-started/001-quickstart.md)。

---

## 🎯 核心功能

### 服务器管理
- ✅ 一键部署 DST 服务器
- ✅ 服务器监控和告警
- ✅ 自动备份和回档
- ✅ 模组管理（Steam Workshop）

### 集群管理
- ✅ 多分片支持（主世界 + 洞穴）
- ✅ 集群配置管理
- ✅ 跨服通信

### 用户管理
- ✅ RBAC 权限控制
- ✅ OAuth2 集成
- ✅ MFA 双因素认证
- ✅ 审计日志

### Node Agent
- ✅ Go 语言编写，轻量级
- ✅ WebSocket 长连接
- ✅ JSON-RPC 2.0 协议
- ✅ 远程命令执行

---

## 🏗️ 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│ Layer 1: 外部入口                                            │
│  Browser (React SPA)        Node Agent (Go)                 │
└────────────────┬──────────────────────┬─────────────────────┘
                 │                      │
                 ▼                      ▼
┌─────────────────────────────────────────────────────────────┐
│ Layer 2: API Gateway (Nginx)                                │
│  - 路由 / 静态文件 / WebSocket 升级 / JWT 验证                │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ Layer 3: 服务层                                              │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐        │
│  │core-platform │ │template-svc  │ │server-service │        │
│  │   (8081)     │ │   (8082)     │ │   (8083)     │        │
│  └──────────────┘ └──────────────┘ └──────────────┘        │
│  ┌──────────────┐                                           │
│  │steam-cache-service    │ (8084)                                    │
│  └──────────────┘                                           │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 数据层                                                       │
│  MySQL (3306-3308)  Redis (6379)                            │
└─────────────────────────────────────────────────────────────┘
```

详细架构请参考 [架构概览](doc/getting-started/003-architecture-overview.md)。

---

## 🛠️ 技术栈

### 后端
- **Java 21** + **Spring Boot 3.4**
- **MyBatis-Plus 3.5** - ORM 框架
- **Redisson 3.40** - Redis 客户端
- **Flyway 10.22** - 数据库迁移
- **Knife4j 4.5** - API 文档

### 前端
- **React 18** + **TypeScript 5**
- **Ant Design 5** - UI 组件库
- **Vite 5** - 构建工具

### Node Agent
- **Go 1.22** + **gorilla/websocket**

### 基础设施
- **MySQL 8.0** - 关系数据库
- **Redis 7** - 缓存
- **Docker Compose** - 容器编排
- **Nginx** - API Gateway

---

## 📦 部署

### Docker 部署（推荐）

```bash
cd deploy/docker
docker compose up -d
```

详细配置请参考 [部署指南](doc/dev-guide/deployment/006-docker-guide.md)。

### 生产环境

参考 [生产环境配置](doc/dev-guide/deployment/007-production-config.md)。

---

## 🧪 测试

### 后端测试

```bash
cd src/backend/general-web-backend
./mvnw test
```

### 前端测试

```bash
cd src/frontend/admin
npm run test

cd src/frontend/customer
npm run test
```

---

## 📚 文档结构

```
doc/
├── getting-started/    # 新手入门
│   ├── 001-quickstart.md
│   ├── 002-local-setup.md
│   └── 003-architecture-overview.md
├── dev-guide/          # 开发指南
│   ├── setup/          # 环境搭建
│   ├── guides/         # 开发指南
│   ├── deployment/     # 部署指南
│   └── operations/     # 运维指南
├── user-guide/         # 用户指南
│   ├── features/       # 功能说明
│   └── tutorials/      # 使用教程
├── reference/          # 参考文档
│   ├── api/            # API 文档
│   ├── database/       # 数据库
│   └── configuration/  # 配置参考
├── architecture/       # 架构文档
│   ├── overview/       # 架构概述
│   ├── adr/            # 架构决策
│   └── design/         # 详细设计
└── modules/            # 模块文档
```

---

## 🤝 贡献

我们欢迎各种形式的贡献！

### 如何贡献

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

详细指南请参考 [贡献指南](CONTRIBUTING.md)。

### 开发环境搭建

参考 [本地开发环境搭建](doc/dev-guide/setup/001-local-setup.md)。

---

## 📝 变更日志

查看 [GitHub Releases](https://github.com/your-org/DontStarveTool/releases) 了解最新版本和变更。

---

## 📄 开源协议

本项目采用 [MIT](LICENSE) 协议开源。

---

## 👥 团队

- **Tech Lead**: @your-username
- **Architect**: @architect-username
- **Product Owner**: @product-username

感谢所有 [贡献者](https://github.com/your-org/DontStarveTool/graphs/contributors)！

---

## 🔗 相关链接

- [文档网站](https://your-org.github.io/DontStarveTool/)
- [问题反馈](https://github.com/your-org/DontStarveTool/issues)
- [讨论区](https://github.com/your-org/DontStarveTool/discussions)
- [DST 官方论坛](https://forums.kleientertainment.com/)

---

**最后更新**: 2026-05-22
