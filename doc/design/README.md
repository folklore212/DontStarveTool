# DST 管理平台设计文档

```
doc/design/
├── README.md                                    # 本文档 — 索引
│
├── architecture/
│   ├── 01-system-overview.md                   # 系统架构总览 (三层结构 + Node Agent)
│   ├── 02-node-agent.md                        # Node Agent 设计 (Go 守护进程)
│   ├── 03-service-calls.md                     # 服务调用关系 (REST / DB共享 / Redis)
│   ├── 04-node-auth-flow.md                    # Node 认证流程 (Bootstrap Token + 预留 HMAC)
│   ├── 05-workshop-cache-flow.md               # Workshop 缓存数据流 (主动 + 被动)
│   └── 06-deployment-topology.md               # 部署拓扑 (Docker 容器 / 端口 / 卷)
│
├── adr/
│   ├── 001-gateway-trust-auth.md               # Gateway 注入 X-User-Id header
│   └── 002-module-deletions.md                 # 删除冗余模块
│
├── api/
│   ├── 01-json-rpc-protocol.md                 # JSON-RPC 2.0 通信协议
│   └── 02-node-commands.md                     # Node 命令集参考 (50+ 命令)
│
├── security/
│   └── 01-authentication.md                    # Node Agent 认证流程（四阶段）
│
├── frontend/
│   ├── 01-server-detail.md                     # 服务器详情页
│   ├── 02-deploy-wizard.md                     # 部署向导
│   ├── 03-dashboard.md                         # 仪表盘
│   ├── 04-map-preview.md                       # 地图预览
│   ├── 05-file-manager.md                      # 文件管理器
│   └── 06-collaboration.md                     # 多用户协作
│
└── database/
    └── DB_DESIGN.md                             # 数据库设计文档
```

## 领域术语

参见项目根目录下的 [CONTEXT.md](../../CONTEXT.md)。

## 关键架构决策

| 决策 | ADR | 状态 |
|------|-----|------|
| 跨服务认证 | Gateway 注入 `X-User-Id` header → [ADR-001](adr/001-gateway-trust-auth.md) | 已决定 |
| 模块精简 | 删除 Market/SteamApiService/HealthScoring + 4 个接口 → [ADR-002](adr/002-module-deletions.md) | 已决定 |
| Node 语言 | Go — 单二进制、goroutine 并发、低内存 | 已决定 |
| Node 架构 | 独立 node-gateway (Go) — 长连接管理独立扩缩容 | 已决定 |
| Node 认证 | Bootstrap Token (方案 1) — 规模 <100，预留 HMAC 扩展 | 已决定 |
| 通信协议 | JSON-RPC 2.0 over WebSocket → [协议文档](api/01-json-rpc-protocol.md) | 已决定 |
| MVP 命令集 | 12 个方法 — 进程管理 + 玩家管理 + 系统监控 → [命令集](api/02-node-commands.md) | 已决定 |
| Service 层 | Server 模块从 Controller 直调 Mapper 重构为 ServerService + ClusterService | 已决定 |
| 调度去重 | `@ConditionalOnProperty` 控制 @Scheduled 任务开关 | 已决定 |
| 元数据管理 | `worldgen-metadata.json` 作为 Java/TS 单一数据源 | 已决定 |

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | React 18 + TypeScript + Ant Design 5 + Vite |
| 后端 | Java 21 + Spring Boot 3.4 + MyBatis-Plus 3.5 |
| 缓存 | Redis 7 + Redisson 3.40 |
| 数据库 | MySQL 8.0 + Flyway 迁移 |
| Node | Go 1.22 + gorilla/websocket |
| 部署 | Docker Compose + Nginx |
| 版本控制 | Git (GitHub) |
