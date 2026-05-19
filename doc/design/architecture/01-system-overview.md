# 系统架构总览

## 当前架构

```
┌──────────────────────────────────────────────────┐
│                  管理平台 (Spring Boot + React)    │
│                                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────┐ │
│  │  Admin   │ │ Customer │ │ Backend  │ │ Test │ │
│  │  :3000   │ │   :80    │ │  :8080   │ │      │ │
│  └──────────┘ └──────────┘ └──────────┘ └──────┘ │
│                                                  │
│  ┌─────────────────────────────────────────────┐ │
│  │  Infrastructure                             │ │
│  │  MySQL :3306  Redis :6379                   │ │
│  └─────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────┘
```

## 服务拆分架构（已设计，待实施）

```
                    ┌──────────────────────┐
                    │  API Gateway (Nginx)  │
                    │  JWT 验证 + 注入       │
                    │  X-User-Id header    │
                    └────────┬─────────────┘
                             │
            ┌────────────────┼────────────────────┐
            ▼                ▼                     ▼
  ┌──────────────────┐ ┌──────────────┐ ┌──────────────────┐
  │ core-platform    │ │template-svc  │ │  mod-worker      │
  │ auth/user/role   │ │Template CRUD │ │  (单实例)         │
  │ mfa/oauth/apikey │ │WorldGen预设   │ │  Workshop缓存    │
  │ audit            │ │Marketplace  │ │  ModConfig下载   │
  │ 可横向扩展        │ │可横向扩展    │ │  @Scheduled任务   │
  └──────────────────┘ └──────────────┘ └──────────────────┘
            │
            ▼
  ┌──────────────────┐
  │ server-service   │
  │ Server CRUD      │
  │ Cluster管理       │
  │ SSH/部署编排      │
  │ 可横向扩展        │
  └──────────────────┘
```

**拆分条件控制**：
- `dst.scheduled.workshop-cache.enabled=false` — 在多实例部署时关闭重复的 Scheduled 任务
- `GatewayTrustFilter` — 读取 Gateway 注入的 `X-User-Id` header（仅信任内网 IP）
- `ModSearchProvider` — server-service 通过接口调用模组搜索，未来可替换为 REST client

## 模块依赖方向

```
infrastructure (SSH, Redis, Security)
    ↓
module/template (模板, 世界生成, Workshop缓存, Mod配置)
    ↓
module/server (服务器, 集群管理)
    ↓
module/auth → module/user → module/role
```

**关键约束**：
- 跨模块依赖仅通过接口（`ModSearchProvider` 解耦 server ↔ template）
- `@Scheduled` 任务通过 `@ConditionalOnProperty` 控制，避免多实例重复
- Server 密码字段 `@JsonIgnore` 防止 API 响应泄露

## 关键技术决策

| 决策 | 选择 | 原因 |
|------|------|------|
| 跨服务认证 | Gateway 注入 `X-User-Id` header | 效率最高，仅验一次 JWT |
| Scheduled 去重 | `@ConditionalOnProperty` + 单实例 worker | 无需 Redis 锁，配置即控制 |
| 模块解耦 | Provider 接口 + 本地实现 | 预留 REST client 替换点 |
| 元数据管理 | JSON 文件 (`worldgen-metadata.json`) | Java/TS 双端由 JSON 生成，单一数据源 |
| 错误码 | ErrorCode 枚举 (10001-50003) | i18n 自动翻译，全局统一 |
| 权限检查 | 所有权匹配 (authorId/userId) | 每层独立校验，不信任上游 |
| Node 语言 | Go | 单二进制、无依赖、低内存 |
| 通信协议 | JSON-RPC 2.0 over WebSocket | 标准化、请求-响应 + 事件推送 |

## 已删除模块

| 模块 | 原因 | 替换 |
|------|------|------|
| `module/market` | MarketConfig 与 Template 90% 重复 | Marketplace 直接用 Template 系统 |
| `infrastructure/steam/SteamApiService` | 76 行浅层直通 + 2 个 stub | 合并入 SteamWorkshopCacheService |
| `infrastructure/monitor/HealthScoringService` | 零调用死代码 | 已删除 |
| 4 个 `I*Service` 接口 | 仪式性噪音，零提供者 | 已删除 |

## 部署模式

- 平台：Docker Compose（mysql + redis + backend + admin + customer）
- Node Agent：systemd 管理，与 DST 服务同机运行
- 扩展：template-service / server-service 可横向扩容至多实例
