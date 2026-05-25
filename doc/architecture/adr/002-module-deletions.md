---
name: ADR-002: 删除冗余模块和仪式性代码
description: ADR-002: 删除冗余模块和仪式性代码
status: approved
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [documentation]
---

# ADR-002: 删除冗余模块和仪式性代码

## 状态
已决定（2026-05-18）

## 背景
四轮架构审查中发现多个模块通过了删除测试——删除后复杂度集中在保留模块，不产生新复杂度。

## 决策

### 已删除
| 模块 | 删除测试结果 |
|------|-------------|
| `module/market` | MarketConfig 与 Template 字段 90% 重复。删除后 Marketplace 功能由 Template 的分页筛选实现 |
| `infrastructure/steam/SteamApiService` | 76 行，`validateToken()` 返回 `token.length > 20`（stub），`getDstVersion()` 返回 `"unknown"`（stub）。合并入 `SteamWorkshopCacheService` |
| `infrastructure/monitor/HealthScoringService` | 零调用。`calculateScore`、`getScoreLabel`、`generateSuggestions` 在整个代码库中无任何引用 |
| `ITemplateService` / `IWorldGenPresetService` / `ISteamWorkshopCacheService` / `IModConfigService` | 4 个接口零提供者。Spring CGLIB 无需接口即可代理。所有调用者注入具体实现类 |

### 保留但标记
| 模块 | 状态 |
|------|------|
| `RedisUtil` | 62 行 1:1 直通，但被 6 个模块使用（auth/oauth/role/template），删除影响面大，延期 |
| `deploy_tasks` / `dst_backups` 表 | V8 中已创建但无代码使用，延期至 Node Agent PRD 实施时激活 |

## 后果
- 代码库从 4 个模块精简为 3 个核心模块（auth/server/template）
- 所有跨模块依赖通过具体 Service 类或接口注入，无循环依赖
- Server 实体的 `password` 字段已添加 `@JsonIgnore` 防止 API 泄露
