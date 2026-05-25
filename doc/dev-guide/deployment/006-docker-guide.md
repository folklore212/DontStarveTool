---
name: 部署拓扑
description: 部署拓扑
status: approved
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [documentation]
---

# 部署拓扑

## Docker Compose 容器全景

```
Network: docker_auth-net (bridge, internal)
══════════════════════════════════════════════════════════════════════

┌──────────────────────────────┐  ┌──────────────────────────────┐
│ nginx (api-gateway)         │  │ admin (auth-admin-container)  │
│ dst-nginx:v1                 │  │ dst-admin:v1                  │
│ 0.0.0.0:80 → :80            │  │ 0.0.0.0:3000 → :80            │
│ volumes:                     │  │ - 独立 nginx + dist              │
│   ./customer/dist:/html:ro  │  └──────────────────────────────┘
│   ./nginx/nginx.conf          │
└──────────┬───────────────────┘  ┌──────────────────────────────┐
           │                      │ customer (auth-customer-      │
           │ /api/* 路由分发       │         container)            │
           │                      │ dst-customer:v1               │
    ┌──────┼──────────────────────┤ 0.0.0.0:8085 → :80            │
    │      │                      │ - 独立 nginx + dist              │
    ▼      ▼           ▼         └──────────────────────────────┘
┌──────────┐  ┌──────────┐  ┌──────────┐
│core-     │  │template- │  │server-   │
│platform  │  │service   │  │service   │
│:8081     │  │:8082     │  │:8083     │
└────┬─────┘  └────┬─────┘  └────┬─────┘
     │             │             │
     │    ┌────────┼─────────────┘
     │    │        │
     ▼    ▼        ▼
┌──────────┐  ┌──────────┐  ┌──────────┐
│mysql     │  │mysql-    │  │mysql-    │
│:3306     │  │template  │  │server    │
│          │  │:3307     │  │:3308     │
│auth_     │  │dst_      │  │dst_      │
│system    │  │templates │  │servers   │
└──────────┘  └──────────┘  └──────────┘

     ┌──────────────────────┐
     │ steam-cache-service (:8084)   │  固定单实例
     │ - @Scheduled 刷新     │
     │ - 连接 mysql-template │
     └──────────────────────┘

     ┌──────────────────────┐
     │ node-gateway (:8090) │  固定单实例
     │ Go WebSocket 服务器   │
     │ - token 验证          │
     │ - JSON-RPC 路由       │
     │ 127.0.0.1:8090→:8090 │
     └──────────────────────┘

┌──────────────────────┐  ┌──────────────────────┐
│ redis (:6379)        │  │ test (auth-test-      │
│ dst-redis:v1          │  │      container)       │
│ 127.0.0.1:6379→:6379 │  │ dst-test:v1            │
│ mem: 256m cpu: 0.5   │  │ - 集成测试, 按需构建   │
└──────────────────────┘  └──────────────────────┘
```

## 端口映射

| 容器 | 内部端口 | 宿主机端口 | 绑定 | 用途 |
|------|---------|-----------|------|------|
| nginx | 80 | 80 | 0.0.0.0 | 公网入口 |
| admin | 80 | 3000 | 0.0.0.0 | 管理后台 |
| customer | 80 | 8085 | 0.0.0.0 | 用户前台（dev） |
| core-platform | 8081 | 8081 | 0.0.0.0 | core API |
| template-service | 8082 | 8082 | 0.0.0.0 | 模板 API |
| server-service | 8083 | 8083 | 0.0.0.0 | 服务器 API |
| steam-cache-service | 8084 | — | 无映射 | 仅内网 |
| node-gateway | 8090 | 8090 | 127.0.0.1 | nginx 代理 |
| mysql | 3306 | 3306 | 127.0.0.1 | 本地调试 |
| mysql-template | 3306 | 3307 | 127.0.0.1 | 本地调试 |
| mysql-server | 3306 | 3308 | 127.0.0.1 | 本地调试 |
| redis | 6379 | 6379 | 127.0.0.1 | 本地调试 |

## 数据卷

| 卷名 | 挂载到 | 持久化 |
|------|--------|--------|
| mysql-data | mysql:/var/lib/mysql | 是 |
| mysql-template-data | mysql-template:/var/lib/mysql | 是 |
| mysql-server-data | mysql-server:/var/lib/mysql | 是 |
| redis-data | redis:/data | 是 |
| nginx 配置 | nginx:/etc/nginx/nginx.conf | 构建时 |
| customer dist | nginx:/usr/share/nginx/html | 构建时 |

## 资源限制

| 容器 | CPU | 内存 | 可扩展 |
|------|-----|------|--------|
| core-platform | 1.5 | 768m | ✅ |
| template-service | 1.0 | 512m | ✅ |
| server-service | 1.0 | 512m | ✅ |
| steam-cache-service | 0.5 | 384m | ❌ 单实例 |
| node-gateway | — | ~50m | ❌ 当前单实例 |
| redis | 0.5 | 256m | ❌ |

## Node Agent 部署

```
DST 游戏服务器 (非 Docker)
══════════════════════════════

/opt/dst-node/
├── dst-node              # Go 二进制 (~8MB)
├── config.json           # 配置文件
├── cert.pem / key.pem    # 未来 mTLS 证书 (暂不使用)
└── logs/
    └── node-agent.log

systemd 管理:
  /etc/systemd/system/dst-node.service
  User=steam
  Restart=always
  RestartSec=10

网络: wss://platform-ip/node?token=dsn-xxx
无需公网 IP，主动出站连接
```
