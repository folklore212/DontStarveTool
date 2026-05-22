---
sidebar_position: 0
---

# DST 管理平台文档

欢迎使用 **DST 管理平台** - 一个专业的 Don't Starve Together 服务器管理平台。

## 🚀 快速开始

如果你是第一次使用本平台，建议从以下文档开始：

- [5 分钟快速体验](getting-started/001-quickstart) - Docker Compose 一键启动
- [本地开发环境搭建](getting-started/002-local-setup) - Java 21 + Node 20 + MySQL + Redis
- [架构概览](getting-started/003-architecture-overview) - 了解系统架构

## 📚 文档分类

### 入门指南
快速上手，从零开始搭建和体验平台。

### 开发指南
为开发者准备的环境搭建、编码规范、调试测试等文档。

### 用户指南
面向服务器管理员的功能说明和使用教程。

### 参考文档
API 接口、数据库 schema、配置项等技术参考。

### 架构文档
系统架构设计、架构决策记录 (ADR)、详细设计文档。

## 🎯 平台特性

- **三层架构设计** - 外部入口 → API Gateway → 服务层
- **Node Agent** - Go 语言编写的轻量级守护进程
- **JSON-RPC 2.0** - 标准化的通信协议
- **Docker 部署** - 一键部署，开箱即用
- **多用户协作** - 完整的 RBAC 权限管理

## 📊 技术栈

| 层 | 技术 |
|----|------|
| 前端 | React 18 + TypeScript + Ant Design 5 + Vite |
| 后端 | Java 21 + Spring Boot 3.4 + MyBatis-Plus 3.5 |
| 缓存 | Redis 7 + Redisson 3.40 |
| 数据库 | MySQL 8.0 + Flyway 迁移 |
| Node Agent | Go 1.22 + gorilla/websocket |
| 部署 | Docker Compose + Nginx |

## 🔗 相关链接

- [GitHub 仓库](https://github.com/your-org/DontStarveTool)
- [领域术语表](../CONTEXT.md)
- [贡献指南](../CONTRIBUTING.md)

## 📝 需要帮助？

如果您在使用或开发过程中遇到问题：

1. 查看 [故障排查指南](dev-guide/operations/008-troubleshooting)
2. 在 GitHub 提交 [Issue](https://github.com/your-org/DontStarveTool/issues)
3. 查看已有的 [讨论](https://github.com/your-org/DontStarveTool/discussions)

---

**文档版本**: 1.0.0  
**最后更新**: 2026-05-22
