---
name: 系统架构总览
description: 系统架构总览
status: approved
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [architecture, system-design]
---

# 系统架构总览

## 全局架构（方案 A：三层结构）

```
Layer 1: 外部入口
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│  浏览器 (React SPA)               Node Agent (Go 守护进程)        │
│  ┌──────────┐ ┌──────────┐       ┌───────┐  ┌───────┐           │
│  │ Customer │ │  Admin   │       │ Node 1│  │ Node 2│  ...      │
│  │  :80     │ │  :3000   │       │ (DST) │  │ (DST) │           │
│  └────┬─────┘ └────┬─────┘       └──┬────┘  └──┬────┘           │
│       │            │                │          │                 │
│       │  HTTP REST │                │  wss://  │                 │
└───────┼────────────┼────────────────┼──────────┼─────────────────┘
        │            │                │          │
        ▼            ▼                ▼          ▼
Layer 2: API Gateway
┌──────────────────────────────────────────────────────────────────┐
│                    ┌─────────────────────┐                       │
│                    │  nginx (:80)         │                      │
│                    │  - 路由 / 静态文件   │                      │
│                    │  - WebSocket 升级    │                      │
│                    │  - JWT 验证预留      │                      │
│                    └───┬─────┬─────┬──────┘                      │
└────────────────────────┼─────┼─────┼─────────────────────────────┘
                         │     │     │
        ┌────────────────┤     │     └──────────────┐
        ▼                ▼     ▼                    ▼
Layer 3: 服务层
┌──────────────────────────────────────────────────────────────────┐
│                                                                    │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐               │
│  │ admin        │ │ customer     │ │ node-gateway │               │
│  │ (nginx:80)   │ │ (nginx:80)   │ │ (Go :8090)   │               │
│  │ 静态文件服务  │ │ 静态文件服务  │ │ WebSocket服务器│              │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘               │
│         │                │                │                        │
│  ┌──────┴──────┐  ┌──────┴──────┐  ┌──────┴──────┐               │
│  │ /api/ 代理  │  │ /api/ 代理  │  │ 路由分发    │               │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘               │
│         │                │                │                        │
│         └────────────────┼────────────────┘                        │
│                          ▼                                         │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │              核心服务层 (Spring Boot / Go)                 │    │
│  │                                                          │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │    │
│  │  │core-platform │  │template-svc  │  │server-service │   │    │
│  │  │   :8081      │  │   :8082      │  │    :8083      │   │    │
│  │  │ user/role     │  │ Template CRUD│  │ Server CRUD  │   │    │
│  │  │ auth/mfa/oauth│  │ WorldGen预设  │  │ Cluster管理  │   │    │
│  │  │ apikey/audit  │  │ Workshop搜索 │  │ SSH/部署编排 │   │    │
│  │  └──────────────┘  └──────────────┘  └──────────────┘   │    │
│  │                                                          │    │
│  │  ┌──────────────┐                                        │    │
│  │  │ steam-cache-service   │  单实例                                 │    │
│  │  │   :8084      │  Workshop缓存 / ModConfig下载           │    │
│  │  └──────────────┘  Steam版本检查                          │    │
│  │                                                          │    │
│  └──────────────────────────────────────────────────────────┘    │
│                          │                                         │
│  ┌───────────────────────┴───────────────────────────────┐       │
│  │              数据层                                     │       │
│  │                                                        │       │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐       │       │
│  │  │ mysql      │  │mysql-      │  │mysql-      │       │       │
│  │  │ :3306      │  │template    │  │server      │       │       │
│  │  │            │  │:3307       │  │:3308       │       │       │
│  │  │auth_system │  │dst_templates│ │dst_servers │       │       │
│  │  └────────────┘  └────────────┘  └────────────┘       │       │
│  │                                                        │       │
│  │  ┌────────────┐                                        │       │
│  │  │ redis :6379│  缓存 / 分布式锁 / JWT 黑名单           │       │
│  │  └────────────┘                                        │       │
│  └───────────────────────────────────────────────────────┘       │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

## Node Agent 通信路径

```
DST 游戏服务器                    平台 (Docker)
┌──────────────┐                ┌─────────────────────────────┐
│              │   wss://       │                             │
│  node-agent  │──────────────▶ │  nginx (:80)                │
│  (Go 守护进程) │   WebSocket    │  │                          │
│              │                │  ├─ /node → proxy to :8090  │
│  ┌──────────┐│                │  │                          │
│  │screen    ││                │  ▼                          │
│  │session   ││   DST 进程     │  node-gateway (:8090)       │
│  │Master    ││                │  ├─ token 验证               │
│  │Caves     ││                │  ├─ JSON-RPC 路由            │
│  └──────────┘│                │  └─ HTTP → server-service   │
│              │                │                             │
└──────────────┘                └─────────────────────────────┘
```

## 服务间调用关系

```
core-platform ◄──── template-service  (CorePlatformClient — 用户信息)
core-platform ◄──── node-gateway       (token 验证 API)

template-service ◄── server-service    (RemoteModSearchProvider — 模组搜索)
template-service ◄── steam-cache-service        (共享 mysql-template DB)

server-service ────► (SSH) ──► DST 游戏服务器 (部署/管理)
node-gateway ──────► (HTTP) ─► server-service (命令转发)
```

## 模块依赖方向（Maven）

```
common (共享库)
    ↓
├── core-platform   (端口 8081, 可横向扩展)
├── template-service (端口 8082, 可横向扩展)
├── server-service   (端口 8083, 可横向扩展)
└── steam-cache-service       (端口 8084, 固定单实例)
```

## 关键技术决策

| 决策 | 选择 | 原因 |
|------|------|------|
| 跨服务认证 | Gateway 注入 `X-User-Id` header | 效率最高，仅验一次 JWT |
| Scheduled 去重 | `@ConditionalOnProperty` + 单实例 worker | 无需 Redis 锁，配置即控制 |
| 元数据管理 | JSON 文件 (`worldgen-metadata.json`) | Java/TS 双端由 JSON 生成，单一数据源 |
| 错误码 | ErrorCode 枚举 (10001-50003) | i18n 自动翻译，全局统一 |
| 权限检查 | 所有权匹配 (authorId/userId) | 每层独立校验，不信任上游 |
| **Node 语言** | **Go** | 单二进制、无依赖、低内存 |
| **通信协议** | **JSON-RPC 2.0 over WebSocket** | 标准化、请求-响应 + 事件推送 |
| **Node 架构** | **独立 node-gateway (Go)** | 长连接管理独立扩缩容 |
| **Node 认证** | **Bootstrap Token (方案 1)** | 规模 <100 台，简单可靠，预留 HMAC 扩展 |
| **MVP 命令集** | **12 个方法** | 进程管理 + 玩家管理 + 系统监控 |

## 部署模式

- 平台：Docker Compose (11 个容器：3 个 MySQL + Redis + 5 个 Java + 1 个 Go + nginx + admin + customer)
- Node Agent：systemd 管理，与 DST 服务同机运行
- 扩展：core-platform / template-service / server-service 可横向扩容至多实例；steam-cache-service 和 node-gateway 固定单实例

## 配置文件

| 配置 | 位置 |
|------|------|
| 服务端口 / DB 连接 | 各模块 `application.yml` |
| Docker 部署配置 | `deploy/docker/.env` |
| Nginx 路由 | `deploy/docker/nginx/nginx.conf` |
| Node Agent 配置 | `/opt/dst-node/config.json` |
| 世界生成元数据 | `worldgen-metadata.json` |
