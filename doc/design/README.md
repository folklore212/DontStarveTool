# DST 管理平台设计文档

```
doc/design/
├── README.md
├── architecture/
│   ├── 01-system-overview.md     # 系统架构总览（含服务拆分方案）
│   └── 02-node-agent.md          # Node Agent 设计
├── adr/
│   ├── 001-gateway-trust-auth.md # Gateway 注入 X-User-Id header
│   └── 002-module-deletions.md   # 删除冗余模块
├── security/
│   └── 01-authentication.md     # Node Agent 认证流程（四阶段）
├── api/
│   ├── 01-json-rpc-protocol.md  # JSON-RPC 2.0 通信协议
│   └── 02-node-commands.md      # Node 命令集参考（50+ 命令）
└── frontend/
    ├── 01-server-detail.md      # 服务器详情页
    ├── 02-deploy-wizard.md      # 部署向导
    ├── 03-dashboard.md          # 仪表盘
    ├── 04-map-preview.md        # 地图预览
    ├── 05-file-manager.md       # 文件管理器
    └── 06-collaboration.md      # 多用户协作
```

## 关键架构决策

| 决策 | ADR |
|------|-----|
| 跨服务认证 | Gateway 注入 `X-User-Id` header → [ADR-001](adr/001-gateway-trust-auth.md) |
| 模块精简 | 删除 Market/SteamApiService/HealthScoring + 4 个仪式接口 → [ADR-002](adr/002-module-deletions.md) |
| Service 层 | Server 模块从 Controller 直调 Mapper 重构为 ServerService + ClusterService |
| 调度去重 | `@ConditionalOnProperty` 控制 @Scheduled 任务开关 |
| 模板元数据 | `worldgen-metadata.json` 作为 Java/TS 单一数据源 |
| 服务解耦 | `ModSearchProvider` 接口隔离 server ↔ template 依赖 |
