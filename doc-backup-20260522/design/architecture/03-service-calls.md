# 服务调用关系

## REST 调用

```
┌────────────────────┐         ┌────────────────────┐
│   template-service │────────▶│   core-platform    │
│   CorePlatformClient│ GET    │ InternalController  │
│                    │ /api/v1/internal/users/{id}│
└────────────────────┘         └────────────────────┘

┌────────────────────┐         ┌────────────────────┐
│   server-service   │────────▶│  template-service  │
│RemoteModSearchProvider│GET   │ SteamWorkshopController│
│                    │/api/v1/workshop/search?kw=│
└────────────────────┘         └────────────────────┘

┌────────────────────┐         ┌────────────────────┐
│   node-gateway     │────────▶│   core-platform    │
│   (Go)             │ GET     │ InternalController  │
│                    │/api/v1/internal/nodes/verify│
└────────────────────┘         └────────────────────┘

┌────────────────────┐         ┌────────────────────┐
│   node-gateway     │────────▶│   server-service   │
│   (Go)             │ POST    │ NodeForwardController│
│                    │/api/v1/internal/nodes/forward│
└────────────────────┘         └────────────────────┘
```

## 数据库共享

```
┌──────────────┐     ┌──────────────┐
│core-platform │     │mod-worker    │
│              │     │              │
│ auth_system  │     │ dst_templates│──┐
│   (独占)      │     │   (与 template共享)││
└──────────────┘     └──────────────┘  │
                                   │
┌──────────────┐                    │
│template-svc  │                    │
│              │                    │
│ dst_templates│────────────────────┘
│   (主 owner) │
└──────────────┘

┌──────────────┐
│server-service│
│              │
│ dst_servers  │
│   (独占)      │
└──────────────┘
```

## Redis 共享

```
┌──────────┐
│  redis   │  所有服务共享
│  :6379   │
└──────────┘
     │
     ├── steam:workshop:hot    (Workshop 热门缓存)
     ├── jwt:blacklist         (JWT 黑名单)
     ├── rate_limit:*          (限流计数器)
     ├── snowflake:worker:*    (Snowflake worker 注册)
     └── session:*             (WebSocket 会话)
```

## 调用方向约束

```
                    ┌──────────────┐
                    │ core-platform │   ← 上游，不调用其他服务
                    └──────┬───────┘
                           ▲
              ┌────────────┼────────────┐
              │            │            │
     ┌────────┴───┐ ┌─────┴─────┐ ┌───┴──────────┐
     │template-svc │ │node-gateway│ │server-service │  ← 中间层
     └────────────┘ └───────────┘ └───────────────┘
              ▲                          │
              │                          ▼
     ┌────────┴───┐              ┌──────────────┐
     │ mod-worker  │              │  DST Server  │   ← 末梢
     └────────────┘              └──────────────┘
```

**规则**：
- 上游（core-platform）不主动调用下游服务
- 下游通过 REST Client 调用上游
- mod-worker 和 template-service 共享 `dst_templates` 通过 DB，非 REST
- node-gateway 仅调用 core-platform 和 server-service，不访问 DB
