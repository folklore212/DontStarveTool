---
name: DST 管理平台文档中心
description: DST 管理平台完整技术文档索引
status: approved
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [index, documentation]
---

# DST 管理平台文档中心

## 📚 文档分类

### 🚀 入门指南

适合新手，快速了解和使用平台。

- [5 分钟快速体验](getting-started/001-quickstart.md) - Docker Compose 一键启动
- [本地开发环境搭建](getting-started/002-local-setup.md) - Java 21 + Node 20 + MySQL + Redis
- [架构概览](getting-started/003-architecture-overview.md) - 三层架构设计详解

### 🛠️ 开发指南

为开发者准备的技术文档。

**环境搭建**:
- [本地开发环境](dev-guide/setup/001-local-setup.md) - IDE+ 数据库 + 调试配置
- [Docker 开发环境](dev-guide/setup/002-docker-setup.md) - 容器化开发

**开发规范**:
- [编码规范](dev-guide/guides/003-coding-standards.md) - Java + TypeScript
- [调试指南](dev-guide/guides/004-debugging.md) - 调试技巧和最佳实践
- [测试指南](dev-guide/guides/005-testing.md) - 单元测试、集成测试、E2E 测试

**部署运维**:
- [Docker 部署](dev-guide/deployment/006-docker-guide.md) - Docker Compose 部署
- [生产环境配置](dev-guide/deployment/007-production-config.md) - 性能优化和安全加固
- [故障排查](dev-guide/operations/008-troubleshooting.md) - 常见问题诊断

### 👤 用户指南

面向服务器管理员的功能说明和教程。

**功能说明**:
- [服务器管理](user-guide/features/001-server-mgmt.md)
- [集群管理](user-guide/features/002-cluster-mgmt.md)
- [创意工坊](user-guide/features/003-workshop.md)
- [仪表盘监控](user-guide/features/004-dashboard.md)
- [地图预览](user-guide/features/005-map-preview.md)
- [文件管理器](user-guide/features/006-file-manager.md)
- [多用户协作](user-guide/features/007-collaboration.md)

**使用教程**:
- [部署第一个服务器](user-guide/tutorials/001-first-server.md)
- [部署向导流程](user-guide/tutorials/002-deploy-server.md)

### 📖 参考文档

技术参考文档。

**API**:
- [REST API 参考](reference/api/001-rest-api.md) - 完整的 REST API 接口
- [JSON-RPC 协议](reference/api/002-json-rpc.md) - Node 通信协议
- [Node 命令集](reference/api/003-node-commands.md) - 50+ 命令参考

**数据库**:
- [数据库 Schema](reference/database/001-schema-reference.md) - ER 图 + 表结构
- [Flyway 迁移](reference/database/002-flyway-migrations.md) - 数据库版本管理

**配置**:
- [应用配置](reference/configuration/001-application-props.md) - 完整配置项说明

### 🏗️ 架构文档

系统架构和设计文档。

**架构概述**:
- [系统架构总览](architecture/overview/001-system-overview.md) - 三层架构设计

**架构决策 (ADR)**:
- [ADR-001](architecture/adr/001-gateway-trust-auth.md) - Gateway 注入 header
- [ADR-002](architecture/adr/002-module-deletions.md) - 删除冗余模块

**详细设计**:
- [服务调用关系](architecture/design/001-service-calls.md)
- [认证流程](architecture/design/002-authentication.md)
- [Node Agent 设计](../modules/node-agent/001-node-agent.md)
- [Workshop 缓存流](../modules/workshop/001-cache-flow.md)

### 🧩 模块文档

按业务模块组织的文档。

- [服务器管理](modules/server-management/) - 服务器管理模块
- [集群管理](modules/cluster/) - 集群管理模块
- [Node Agent](modules/node-agent/) - Node 节点代理
- [创意工坊](modules/workshop/) - Steam Workshop 集成

---

## 📊 文档统计

| 分类 | 文档数 | 状态 |
|------|--------|------|
| 入门指南 | 3 | ✅ 完成 |
| 开发指南 | 8 | ✅ 完成 |
| 用户指南 | 9 | ✅ 完成 |
| 参考文档 | 5 | ✅ 完成 |
| 架构文档 | 6 | ✅ 完成 |
| 模块文档 | 2 | ✅ 完成 |
| **总计** | **33** | **100%** |

---

## 🔗 相关链接

- [领域术语表](../CONTEXT.md) - 游戏和平台概念定义
- [贡献指南](_meta/CONTRIBUTING.md) - 如何贡献文档
- [下一步行动](_meta/NEXT-STEPS.md) - 后续工作计划
- [项目首页](_meta/README-PROJECT.md) - 项目介绍

### 报告和总结

- [重构完成报告](_reports/REFACTOR-COMPLETE.md) - 详细重构报告
- [最终总结](_reports/FINAL-SUMMARY.md) - 项目总结
- [清理总结](_reports/CLEANUP-SUMMARY.md) - 目录清理记录

---

## 🛠️ 工具使用

### 验证文档

```bash
# 验证文档完整性
./tools/docs/validate-docs.sh

# 检查链接
./tools/docs/check-broken-links.sh

# 生成索引
./tools/docs/generate-index.sh
```

### 运行文档网站

```bash
cd _static
npm install
npm start
# 访问 http://localhost:3000
```

---

## 📝 文档规范

所有文档遵循以下规范：

- **Frontmatter**: 包含 name, description, status, owner, created 等字段
- **编号规则**: 目录内唯一，按重要性排序
- **状态标记**: draft | in_review | approved | deprecated
- **审核流程**: 至少 1 人审核，必须包含 Tech Lead

---

**最后更新**: 2026-05-22  
**文档版本**: 2.0  
**维护人**: @TechLead

---

*本文档索引由脚本自动生成 - 查看 [tools/docs/README.md](../tools/docs/README.md) 了解更多工具*
