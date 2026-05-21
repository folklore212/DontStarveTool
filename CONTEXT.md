# DST 管理平台 — 领域术语表

## 游戏概念

### DST Server
一台物理机或虚拟机，安装了 SteamCMD + Don't Starve Together 专用服务器。一台 DST Server 上可以运行多个 Cluster。

### Cluster
一组 DST 世界实例的集合。最简配置为 1 个 Master shard + 1 个 Caves shard（地面 + 洞穴）。大型服务器可包含多组 Master + Caves 对，实现多个世界共存。

### Shard
Cluster 中的单个 DST 进程实例。分为 Master（地面世界）和 Caves（洞穴世界）两种类型，各自是独立的 `dontstarve_dedicated_server_nullrenderer` 进程，通过 `screen` session 管理。

### Workshop Mod
Steam 创意工坊发布的 DST 模组，由 Workshop ID 唯一标识。客户端模组（`client_only_mod`）仅需玩家订阅，服务端模组需在服务器 `dedicated_server_mods_setup.lua` 中配置。

## 平台概念

### Template
服务器配置模板，包含 game_mode、max_players、category、描述、封面图等元数据。可绑定多个 WorldGen Preset 和 Modpack。状态为 draft（草稿）、published（已发布，可被他人 fork）、archived（归档）。

### WorldGen Preset
世界生成参数预设，包含 world_size、branching、loop_mode、season_start、day_mode、四季长度、resource_variety 等固定字段，以及 150+ 项通过 `extra_settings` JSON 存储的高级设置。分为 surface（地面）和 caves（洞穴）两种类型，各自拥有完全独立的一套参数值。

### Modpack
模组集合模板，包含 Workshop ID 列表和每个模组的配置项（`modinfo.lua` 中 `configuration_options` 的覆盖值）。与 Template 绑定使用。

### Marketplace
已发布 Template 的公开浏览页面。本质上是 Template 的 `status=published` 筛选视图，不独立存储数据。

### Node Agent
部署在每台 DST Server 上的 Go 守护进程，通过 WebSocket 长连接主动连接平台。负责接收 JSON-RPC 2.0 命令并执行 DST 进程管理、玩家管理、模组管理、系统监控等操作。

### Node Gateway
平台侧的 Go WebSocket 服务端。接收 Node Agent 的 WebSocket 连接，验证 Bootstrap Token，将 JSON-RPC 请求路由到对应的后端 Service。

### Bootstrap Token
管理员在平台为 DST Server 生成的认证令牌（格式 `dsn-xxxxxxxx`）。Node Agent 首次连接时携带此 token，Node Gateway 验证通过后建立信任。Token 可随时吊销。

## 平台组件

| 组件 | 技术 | 端口 | 职责 |
|------|------|------|------|
| nginx | C | 80 | API Gateway — 路由/WebSocket 代理/静态文件 |
| core-platform | Java/Spring | 8081 | 认证/用户/角色/MFA/OAuth/APIKey/审计 |
| template-service | Java/Spring | 8082 | Template CRUD / WorldGen Preset / Workshop 搜索 |
| server-service | Java/Spring | 8083 | Server CRUD / Cluster 管理 / SSH 部署编排 |
| mod-worker | Java/Spring | 8084 | Steam Workshop 定时缓存 / Mod 配置下载 |
| node-gateway | Go | 8090 | WebSocket 服务端 / JSON-RPC 路由 |
| node-agent | Go | — | DST Server 守护进程 / JSON-RPC 客户端 |

## 数据库

| 数据库 | 存储内容 |
|--------|---------|
| auth_system | users, roles, permissions, user_auths, mfa, oauth_clients, api_keys, audit_logs, login_logs, node_tokens |
| dst_templates | templates, world_gen_presets, template_world_gen_bindings, steam_workshop_cache, mod_config_cache |
| dst_servers | servers, dst_clusters |
