---
name: 架构概览
description: 架构概览
status: approved
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [architecture, overview, getting-started]
---

# 架构概览

本文档介绍 DST 管理平台的整体架构设计，帮助你快速理解系统的组织结构和关键组件。

## 架构设计原则

- **分层架构**: 清晰的职责分离
- **微服务化**: 独立部署和扩展
- **事件驱动**: 异步通信和事件处理
- **容器化**: Docker 部署，开箱即用
- **安全性**: 多层安全防护

---

## 三层架构

```
┌──────────────────────────────────────────────────────────────────┐
│ Layer 1: 外部入口                                                 │
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
┌──────────────────────────────────────────────────────────────────┐
│ Layer 2: API Gateway                                             │
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
┌──────────────────────────────────────────────────────────────────┐
│ Layer 3: 服务层                                                   │
│                                                                    │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐               │
│  │ admin        │ │ customer     │ │ node-gateway │               │
│  │ (nginx:80)   │ │ (nginx:80)   │ │ (Go :8090)   │               │
│  │ 静态文件服务  │ │ 静态文件服务  │ │ WebSocket 服务器│              │
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
│  │  │ auth/mfa/oauth│  │ WorldGen 预设  │  │ Cluster 管理  │   │    │
│  │  │ apikey/audit  │  │ Workshop 搜索 │  │ SSH/部署编排 │   │    │
│  │  └──────────────┘  └──────────────┘  └──────────────┘   │    │
│  │                                                          │    │
│  │  ┌──────────────┐                                        │    │
│  │  │ steam-cache-service   │  单实例                                 │    │
│  │  │   :8084      │  Workshop 缓存 / ModConfig 下载           │    │
│  │  └──────────────┘  Steam 版本检查                          │    │
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

### Layer 1: 外部入口

#### Customer 前端
- **技术栈**: React 18 + TypeScript + Ant Design 5
- **端口**: 80 (通过 Nginx)
- **功能**: 用户界面，服务器列表、监控、配置

#### Admin 管理后台
- **技术栈**: React 18 + TypeScript + Ant Design 5
- **端口**: 3000 (开发环境), 80 (生产环境通过 Nginx)
- **功能**: 系统管理、用户管理、模板管理

#### Node Agent
- **技术栈**: Go 1.22 + gorilla/websocket
- **部署**: 与 DST 服务器同机运行
- **功能**: 
  - 通过 WebSocket 连接平台
  - 执行平台下发的命令
  - 上报服务器状态

### Layer 2: API Gateway

#### Nginx Gateway
- **端口**: 80
- **职责**:
  - 静态文件服务（前端 SPA）
  - API 路由转发
  - WebSocket 升级支持
  - JWT 验证（预留）
  - 负载均衡

**路由规则**:
```nginx
# 前端静态文件
location / {
    root /usr/share/nginx/html/admin;
    try_files $uri $uri/ /index.html;
}

# Customer 前端
location /customer {
    root /usr/share/nginx/html/customer;
}

# API 代理
location /api/ {
    proxy_pass http://core-platform:8081;
}

# Node WebSocket
location /node {
    proxy_pass http://node-gateway:8090;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```

### Layer 3: 服务层

#### 核心服务（Spring Boot）

| 服务 | 端口 | 职责 | 扩展性 |
|------|------|------|--------|
| **core-platform** | 8081 | 用户认证、授权、OAuth2、MFA、API Key、审计日志 | 可横向扩展 |
| **template-service** | 8082 | DST 服务器模板、世界生成预设、Workshop 搜索 | 可横向扩展 |
| **server-service** | 8083 | 服务器 CRUD、集群管理、SSH 部署、备份管理 | 可横向扩展 |
| **steam-cache-service** | 8084 | Workshop 缓存、Mod 配置下载、Steam 版本检查 | 单实例 |

#### Node Gateway（Go）
- **端口**: 8090
- **职责**:
  - WebSocket 连接管理
  - Node Agent 认证
  - JSON-RPC 请求路由
  - 命令转发到 server-service

#### 数据层

| 数据库 | 端口 | 用途 | 数据 |
|--------|------|------|------|
| **mysql** (auth_system) | 3306 | 认证授权 | 用户、角色、权限、OAuth、审计日志 |
| **mysql-template** (dst_templates) | 3307 | 模板数据 | 服务器模板、世界预设、Mod 缓存 |
| **mysql-server** (dst_servers) | 3308 | 服务器数据 | 服务器实例、集群、备份、部署任务 |
| **redis** | 6379 | 缓存/锁 | 会话缓存、JWT 黑名单、分布式锁 |

---

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

**通信流程**:
1. Node Agent 启动，读取 `config.json` 中的平台地址和 token
2. 通过 WebSocket 连接到 `wss://platform-address/node`
3. Node Gateway 验证 token
4. 验证通过后建立长连接
5. 平台通过 JSON-RPC 下发命令
6. Node Agent 执行命令并返回结果

---

## 服务间调用关系

```
core-platform ◄──── template-service  (CorePlatformClient — 用户信息)
core-platform ◄──── node-gateway       (token 验证 API)

template-service ◄── server-service    (RemoteModSearchProvider — 模组搜索)
template-service ◄── steam-cache-service        (共享 mysql-template DB)

server-service ────► (SSH) ──► DST 游戏服务器 (部署/管理)
node-gateway ──────► (HTTP) ─► server-service (命令转发)
```

### 调用方式

| 调用类型 | 方式 | 说明 |
|----------|------|------|
| 服务间调用 | REST API | Spring RestTemplate / WebClient |
| 数据库共享 | 直接查询 | 只读查询，避免写冲突 |
| 缓存共享 | Redis | 分布式缓存、锁 |
| 事件通知 | Redis Pub/Sub | 异步事件通知 |

---

## 模块依赖关系（Maven）

```
common (共享库)
    ↓
├── core-platform   (端口 8081, 可横向扩展)
├── template-service (端口 8082, 可横向扩展)
├── server-service   (端口 8083, 可横向扩展)
└── steam-cache-service       (端口 8084, 固定单实例)
```

**依赖方向**:
- `common`: 工具类、常量、异常处理、通用配置
- `core-platform`: 基础服务，其他服务都依赖它
- `template-service`: 依赖 core-platform
- `server-service`: 依赖 core-platform 和 template-service
- `steam-cache-service`: 独立服务，无依赖

---

## 关键技术决策

| 决策 | 选择 | 原因 |
|------|------|------|
| **跨服务认证** | Gateway 注入 `X-User-Id` header | 效率最高，仅验一次 JWT |
| **Scheduled 去重** | `@ConditionalOnProperty` + 单实例 worker | 无需 Redis 锁，配置即控制 |
| **元数据管理** | JSON 文件 (`worldgen-metadata.json`) | Java/TS 双端由 JSON 生成，单一数据源 |
| **错误码** | ErrorCode 枚举 (10001-50003) | i18n 自动翻译，全局统一 |
| **权限检查** | 所有权匹配 (authorId/userId) | 每层独立校验，不信任上游 |
| **Node 语言** | **Go** | 单二进制、无依赖、低内存 |
| **通信协议** | **JSON-RPC 2.0 over WebSocket** | 标准化、请求 - 响应 + 事件推送 |
| **Node 架构** | **独立 node-gateway (Go)** | 长连接管理独立扩缩容 |
| **Node 认证** | **Bootstrap Token (方案 1)** | 规模 <100 台，简单可靠，预留 HMAC 扩展 |
| **MVP 命令集** | **12 个方法** | 进程管理 + 玩家管理 + 系统监控 |

---

## 部署模式

### Docker Compose 部署

**容器列表** (11 个):
1. `api-gateway` - Nginx Gateway
2. `auth-admin-container` - Admin 前端
3. `auth-customer-container` - Customer 前端
4. `core-platform-container` - 核心平台服务
5. `template-service-container` - 模板服务
6. `server-service-container` - 服务器服务
7. `steam-cache-service-container` - Steam Cache Service
8. `node-gateway-container` - Node Gateway
9. `auth-mysql-container` - 认证数据库
10. `template-mysql-container` - 模板数据库
11. `server-mysql-container` - 服务器数据库
12. `auth-redis-container` - Redis 缓存

### Node Agent 部署

- **方式**: systemd 服务
- **位置**: 与 DST 服务器同机
- **配置**: `/opt/dst-node/config.json`
- **启动**: `systemctl start dst-node-agent`

---

## 扩展性

### 横向扩展

| 服务 | 扩展方式 | 说明 |
|------|----------|------|
| core-platform | 多实例 + 负载均衡 | 通过 Nginx 负载均衡 |
| template-service | 多实例 + 负载均衡 | 无状态服务 |
| server-service | 多实例 + 负载均衡 | 无状态服务 |

### 固定单实例

| 服务 | 原因 |
|------|------|
| steam-cache-service | Workshop 缓存需要全局一致性 |
| node-gateway | WebSocket 连接管理，状态复杂 |

---

## 下一步

- 📖 [Node Agent 设计](../modules/node-agent/001-node-agent.md) - 详细了解 Node Agent
- 📖 [服务调用关系](../architecture/design/001-service-calls.md) - 服务间调用详解
- 📖 [部署拓扑](../dev-guide/deployment/006-docker-guide.md) - 部署配置详解
- 📖 [数据库设计](../reference/database/001-schema-reference.md) - 数据模型详解

---

**相关文档**:
- [ADR-001: Gateway 注入 header](../architecture/adr/001-gateway-trust-auth.md)
- [JSON-RPC 协议](../reference/api/002-json-rpc.md)
- [Node 命令集](../reference/api/003-node-commands.md)

**最后更新**: 2026-05-22
