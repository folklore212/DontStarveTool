# Workshop 缓存数据流

## 主动刷新（mod-worker）

```
                    ┌─────────────────────────┐
                    │   Steam Web API          │
                    │ IPublishedFileService    │
                    │ /QueryFiles/v1/          │
                    └──────────┬──────────────┘
                               │
        @Scheduled            │ fetchHotMods(500)
        cron: 0 7,37 * * * *  │ 10 pages × 50/page
                               ▼
                    ┌─────────────────────────┐
                    │   mod-worker (:8084)      │
                    │   WorkshopRefreshService │
                    │   - 30min 刷新前500      │
                    │   - 调用 SteamApiClient   │
                    │   - 写入 shared DB        │
                    └──────────┬──────────────┘
                               │
                               │ upsert()
                               ▼
                    ┌─────────────────────────┐
                    │   mysql-template (:3307) │
                    │   steam_workshop_cache  │
                    │   - 500+ mod 元数据      │
                    └──────────┬──────────────┘
                               │
                               │ refreshRedis()
                               ▼
                    ┌─────────────────────────┐
                    │   redis (:6379)          │
                    │   steam:workshop:hot    │
                    │   - Top 50 mods          │
                    │   - TTL: 6h              │
                    └─────────────────────────┘
```

## 被动冷启动（template-service）

```
用户搜索 "crock pot"
         │
         ▼
┌─────────────────────┐
│ SteamWorkshopController│
│ GET /api/v1/workshop/search?keyword=crock+pot
└──────────┬──────────┘
           │
           │ searchCached("crock pot")
           ▼
┌─────────────────────┐
│ SteamWorkshopCacheService│
└──────────┬──────────┘
           │
     ┌─────┴─────┐
     │ 检查 Redis │
     │ steam:workshop:hot
     └─────┬─────┘
           │
    ┌──────┴──────┐
    ▼             ▼
  [命中]       [未命中]
    │             │
    ▼             ▼
返回Top50     检索 DB (500条)
过滤keyword   模糊匹配 title/desc/tags
    │             │
    │        ┌────┴────┐
    │        ▼         ▼
    │     [找到]    [未找到]
    │        │         │
    │        ▼         ▼
    │    返回结果   调用 SteamApiClient
    │        │     searchMods("crock pot")
    │        │         │
    │        │         ▼
    │        │    upsert() → 写入 DB + Redis
    │        │         │
    │        │         ▼
    │        │    返回 Steam 实时结果
    │        │
    └────────┴──────▶ 前端渲染卡片
```

## 读写路径总结

| 路径 | 触发 | 延迟 | 数据新鲜度 |
|------|------|------|-----------|
| Redis 命中 | 用户搜索/浏览 | <1ms | 最多 6h 旧 |
| DB 命中 | Redis 过期或未命中 | ~5ms | 和上次 @Scheduled 刷新一致 |
| Steam API 回源 | 首次搜索长尾关键词 | ~500ms | 实时 |
| @Scheduled 刷新 | 每 30min 自动 | 不面向用户 | 前 500 个保持最新 |
