---
name: DST 管理平台文档索引
description: DST 管理平台文档索引
status: approved
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [documentation]
---

# DST 管理平台文档索引

> 本文档索引由脚本自动生成，请勿手动编辑

欢迎使用 **DST 管理平台** 的文档中心！

## 📊 文档统计

| 分类 | 文档数量 |
|------|----------|
| 入门指南 | 3 篇 |
| 开发指南 | 8 篇 |
| 用户指南 | 7 篇 |
| 参考文档 | 5 篇 |
| 架构文档 | 5 篇 |
| 模块文档 | 2 篇 |
| **总计** | **34 篇** |

## 📚 文档分类

### 🚀 入门指南

适合新手，快速了解和使用平台。

| 文档 | 说明 |
|------|------|
| [5 分钟快速体验](getting-started/001-quickstart.md) | 使用 Docker Compose 一键启动 DST 管理平台 |
| [本地开发环境搭建](getting-started/002-local-setup.md) | Java 21 + Node 20 + MySQL + Redis 完整开发环境配置 |
| [架构概览](getting-started/003-architecture-overview.md) | DST 管理平台三层架构设计详解 |

### 🛠️ 开发指南

为开发者准备的技术文档。

#### 环境搭建

| 文档 | 说明 |
|------|------|
| [本地开发环境搭建](dev-guide/setup/001-local-setup.md) | 完整的本地开发环境配置指南（IDE+ 数据库 + 调试） |
| [Docker 开发环境](dev-guide/setup/002-docker-setup.md) | 使用 Docker 进行容器化开发 |

#### 开发指南

| 文档 | 说明 |
|------|------|
| [编码规范](dev-guide/guides/003-coding-standards.md) | Java + TypeScript 编码规范和最佳实践 |
| [调试指南](dev-guide/guides/004-debugging.md) | 后端和前端的调试技巧和最佳实践 |
| [测试指南](dev-guide/guides/005-testing.md) | 单元测试、集成测试和 E2E 测试最佳实践 |

#### 部署指南

| 文档 | 说明 |
|------|------|
| [](dev-guide/deployment/006-docker-guide.md) |  |
| [生产环境配置](dev-guide/deployment/007-production-config.md) | 生产环境部署配置和性能优化指南 |

### 👤 用户指南

面向服务器管理员的功能说明和教程。

#### 功能说明

| 文档 | 说明 |
|------|------|
| [](user-guide/features/001-server-mgmt.md) |  |
| [](user-guide/features/004-dashboard.md) |  |
| [](user-guide/features/005-map-preview.md) |  |
| [](user-guide/features/006-file-manager.md) |  |
| [](user-guide/features/007-collaboration.md) |  |

#### 使用教程

| 文档 | 说明 |
|------|------|
| [部署第一个服务器](user-guide/tutorials/001-first-server.md) | 从零开始部署第一个 DST 服务器的完整教程 |
| [](user-guide/tutorials/002-deploy-server.md) |  |

### 📖 参考文档

技术参考文档。

#### API 文档

| 文档 | 说明 |
|------|------|
| [REST API 参考](reference/api/001-rest-api.md) | 完整的 REST API 接口文档 |
| [](reference/api/002-json-rpc.md) |  |
| [](reference/api/003-node-commands.md) |  |

#### 数据库

| 文档 | 说明 |
|------|------|
| [](reference/database/001-schema-reference.md) |  |

### 🏗️ 架构文档

系统架构和设计文档。

#### 架构概述

| 文档 | 说明 |
|------|------|
| [](architecture/overview/001-system-overview.md) |  |

#### 架构决策记录 (ADR)

| 文档 | 说明 |
|------|------|
| [](architecture/adr/001-gateway-trust-auth.md) |  |
| [](architecture/adr/002-module-deletions.md) |  |

#### 详细设计

| 文档 | 说明 |
|------|------|
| [](architecture/design/001-service-calls.md) |  |
| [](architecture/design/002-authentication.md) |  |

### 🧩 模块文档

按业务模块组织的文档。

| 文档 | 说明 |
|------|------|
| [](modules/node-agent/001-node-agent.md) |  |
| [](modules/workshop/001-cache-flow.md) |  |

---

## 🔗 相关链接

- [领域术语表](../CONTEXT.md)
- [贡献指南](../CONTRIBUTING.md)
- [文档模板](internal/templates/)
- [重构完成报告](REFACTOR-COMPLETE.md)
- [下一步行动](NEXT-STEPS.md)

## 📅 更新信息

- **最后更新**: 2026-05-22
- **文档总数**: 34 篇
- **生成工具**: tools/docs/generate-index.sh

---

*本文档索引由脚本自动生成 - *
