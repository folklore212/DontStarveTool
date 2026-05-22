# DST 管理平台文档索引

欢迎来到 DST 管理平台的文档中心！本文档索引帮助您快速找到所需文档。

## 📚 文档分类

### 🚀 入门指南 (Getting Started)

适合新手，快速了解和使用平台。

| 文档 | 说明 | 状态 |
|------|------|------|
| [001-quickstart.md](getting-started/001-quickstart.md) | 5 分钟快速体验 | 🟡 编写中 |
| [002-local-setup.md](getting-started/002-local-setup.md) | 本地开发环境搭建 | 🟡 编写中 |
| [003-architecture-overview.md](getting-started/003-architecture-overview.md) | 架构概览 | 🟡 编写中 |

### 🛠️ 开发者指南 (Developer Guide)

为开发者准备的技术文档。

#### 环境搭建
| 文档 | 说明 | 状态 |
|------|------|------|
| [001-local-setup.md](dev-guide/setup/001-local-setup.md) | 本地开发环境 | 🟡 编写中 |
| [002-docker-setup.md](dev-guide/setup/002-docker-setup.md) | Docker 开发环境 | 🟡 编写中 |

#### 开发指南
| 文档 | 说明 | 状态 |
|------|------|------|
| [003-coding-standards.md](dev-guide/guides/003-coding-standards.md) | 编码规范 | 🟡 编写中 |
| [004-debugging.md](dev-guide/guides/004-debugging.md) | 调试指南 | 🟡 编写中 |
| [005-testing.md](dev-guide/guides/005-testing.md) | 测试指南 | 🟡 编写中 |

#### 部署指南
| 文档 | 说明 | 状态 |
|------|------|------|
| [006-docker-guide.md](dev-guide/deployment/006-docker-guide.md) | Docker 部署指南 | ✅ 已迁移 |
| [007-production-config.md](dev-guide/deployment/007-production-config.md) | 生产环境配置 | 🟡 编写中 |

#### 运维指南
| 文档 | 说明 | 状态 |
|------|------|------|
| [008-troubleshooting.md](dev-guide/operations/008-troubleshooting.md) | 故障排查 | 🟡 编写中 |

### 👤 用户指南 (User Guide)

面向服务器管理员的功能说明和教程。

#### 功能说明
| 文档 | 说明 | 状态 |
|------|------|------|
| [001-server-mgmt.md](user-guide/features/001-server-mgmt.md) | 服务器管理 | ✅ 已迁移 |
| [002-cluster-mgmt.md](user-guide/features/002-cluster-mgmt.md) | 集群管理 | 🟡 编写中 |
| [003-workshop.md](user-guide/features/003-workshop.md) | 创意工坊 | 🟡 编写中 |
| [004-dashboard.md](user-guide/features/004-dashboard.md) | 仪表盘 | ✅ 已迁移 |
| [005-map-preview.md](user-guide/features/005-map-preview.md) | 地图预览 | ✅ 已迁移 |
| [006-file-manager.md](user-guide/features/006-file-manager.md) | 文件管理器 | ✅ 已迁移 |
| [007-collaboration.md](user-guide/features/007-collaboration.md) | 多用户协作 | ✅ 已迁移 |

#### 使用教程
| 文档 | 说明 | 状态 |
|------|------|------|
| [001-first-server.md](user-guide/tutorials/001-first-server.md) | 部署第一个服务器 | 🟡 编写中 |
| [002-deploy-server.md](user-guide/tutorials/002-deploy-server.md) | 部署向导流程 | ✅ 已迁移 |

### 📖 参考文档 (Reference)

技术参考文档。

#### API 文档
| 文档 | 说明 | 状态 |
|------|------|------|
| [001-rest-api.md](reference/api/001-rest-api.md) | REST API (OpenAPI) | 🟡 编写中 |
| [002-json-rpc.md](reference/api/002-json-rpc.md) | JSON-RPC 协议 | ✅ 已迁移 |
| [003-node-commands.md](reference/api/003-node-commands.md) | Node 命令集 | ✅ 已迁移 |

#### 数据库
| 文档 | 说明 | 状态 |
|------|------|------|
| [001-schema-reference.md](reference/database/001-schema-reference.md) | 数据库 Schema | ✅ 已迁移 |
| [002-flyway-migrations.md](reference/database/002-flyway-migrations.md) | Flyway 迁移 | 🟡 编写中 |

#### 配置参考
| 文档 | 说明 | 状态 |
|------|------|------|
| [001-application-props.md](reference/configuration/001-application-props.md) | 应用配置项 | 🟡 编写中 |

### 🏗️ 架构文档 (Architecture)

系统架构和设计文档。

#### 架构概述
| 文档 | 说明 | 状态 |
|------|------|------|
| [001-system-overview.md](architecture/overview/001-system-overview.md) | 系统架构总览 | ✅ 已迁移 |

#### 架构决策记录 (ADR)
| 文档 | 说明 | 状态 |
|------|------|------|
| [001-gateway-trust-auth.md](architecture/adr/001-gateway-trust-auth.md) | Gateway 注入 header | ✅ 已迁移 |
| [002-module-deletions.md](architecture/adr/002-module-deletions.md) | 删除冗余模块 | ✅ 已迁移 |

#### 详细设计
| 文档 | 说明 | 状态 |
|------|------|------|
| [001-node-agent.md](modules/node-agent/001-node-agent.md) | Node Agent 设计 | ✅ 已迁移 |
| [002-service-calls.md](architecture/design/001-service-calls.md) | 服务调用关系 | ✅ 已迁移 |
| [003-authentication.md](architecture/design/003-authentication.md) | 认证流程 | ✅ 已迁移 |
| [004-workshop-cache-flow.md](modules/workshop/001-cache-flow.md) | Workshop 缓存流 | ✅ 已迁移 |
| [005-deployment-topology.md](dev-guide/deployment/006-docker-guide.md) | 部署拓扑 | ✅ 已迁移 |

### 🧩 模块文档 (Modules)

按业务模块组织的文档。

| 文档 | 说明 | 状态 |
|------|------|------|
| [001-server-detail.md](modules/server-management/001-server-detail.md) | 服务器详情 | 🟡 待迁移 |
| [002-cluster-mgmt.md](modules/cluster/002-cluster-mgmt.md) | 集群管理 | 🟡 待迁移 |
| [003-node-agent.md](modules/node-agent/003-node-agent.md) | Node Agent | 🟡 待迁移 |
| [004-workshop.md](modules/workshop/004-workshop.md) | 创意工坊 | 🟡 待迁移 |

### 🎨 前端文档 (Frontend)

前端组件和 UI 文档。

| 文档 | 说明 | 状态 |
|------|------|------|
| [001-component-guide.md](frontend/001-component-guide.md) | 组件指南 | 🟡 编写中 |

---

## 📊 文档状态说明

| 图标 | 状态 | 说明 |
|------|------|------|
| ✅ | 已完成 | 文档已完成并审核通过 |
| 🟡 | 编写中 | 文档正在编写或需要更新 |
| 🔴 | 已废弃 | 文档已过时，仅供参考 |

## 🔗 相关链接

- [领域术语表](../CONTEXT.md)
- [贡献指南](../CONTRIBUTING.md)
- [文档模板](internal/templates/)

## 📝 文档审核

所有文档在提交前需要经过审核。审核流程请参考 [PR 审核检查表](internal/templates/pr-checklist.md)。

**审核人**:
- 架构文档：@TechLead @Architect
- 开发文档：@TechLead @BackendLead
- 用户文档：@ProductOwner @TechLead
- API 文档：@BackendLead

## 📅 复审计划

文档会与项目 Release 周期同步复审，确保内容准确性。

下次复审日期：**与下个 Release 同步**

---

**最后更新**: 2026-05-22  
**文档版本**: 2.0 (重构版)
