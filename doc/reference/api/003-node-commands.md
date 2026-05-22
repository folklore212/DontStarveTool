# Node 命令集参考

## DST 进程管理

### dst.start
启动 DST 服务器。
```json
// 请求
{"method": "dst.start", "params": {"cluster_name": "MyWorld", "shards": ["Master", "Caves"]}}

// 响应
{"result": {"master": {"pid": 12345, "screen": "MyWorld_Master"}, "caves": {"pid": 12346, "screen": "MyWorld_Caves"}}}
```

### dst.stop
停止 DST 服务器。
```json
// 请求
{"method": "dst.stop", "params": {"cluster_name": "MyWorld", "force": false}}

// 响应
{"result": {"stopped": ["Master", "Caves"]}}
```

### dst.restart
重启 DST 服务器。
```json
// 请求
{"method": "dst.restart", "params": {"cluster_name": "MyWorld"}}
```

### dst.status
获取服务器运行状态。
```json
// 请求
{"method": "dst.status", "params": {"cluster_name": "MyWorld"}}

// 响应
{"result": {"master": {"running": true, "pid": 12345, "uptime_sec": 3600, "player_count": 3, "day": 45, "season": "autumn"}, "caves": {"running": true, "pid": 12346, "uptime_sec": 3590}}}
```

### dst.console.send
发送控制台命令到 DST。
```json
// 请求
{"method": "dst.console.send", "params": {"cluster_name": "MyWorld", "shard": "Master", "command": "c_announce(\"Server restart in 5 minutes\")"}}

// 响应
{"result": {"sent": true}}
```

### dst.version.check
检查 DST 服务器更新。
```json
// 响应
{"result": {"current": "562114", "latest": "563210", "update_available": true}}
```

## 日志

### dst.logs.subscribe
订阅 DST 日志流。取消订阅用 `dst.logs.unsubscribe`。
```json
// 请求
{"method": "dst.logs.subscribe", "params": {"cluster_name": "MyWorld", "shard": "Master", "tail_lines": 50}}

// 无直接响应，后续通过 event.log 推送日志行
```

## 玩家管理

### dst.players.list
在线玩家列表。
```json
// 响应
{"result": {"players": [{"name": "Wilson", "steam_id": "76561198000000001", "character": "wilson", "play_time_min": 120, "ping_ms": 45}]}}
```

### dst.players.kick
踢出玩家。
```json
// 请求
{"method": "dst.players.kick", "params": {"cluster_name": "MyWorld", "shard": "Master", "steam_id": "76561198000000001", "reason": "Griefing"}}
```

### dst.players.ban / dst.players.unban
封禁/解封玩家。
```json
// 请求
{"method": "dst.players.ban", "params": {"cluster_name": "MyWorld", "steam_id": "76561198000000001", "reason": "Repeated griefing"}}
```

### dst.adminlist.*
管理员列表管理（get / add / remove）。
### dst.blocklist.*
黑名单管理（get / add / remove）。

## 模组管理

### dst.mods.list
已安装模组列表。
```json
// 响应
{"result": {"mods": [{"workshop_id": "378160973", "name": "Global Positions", "enabled": true, "configured": true}]}}
```

### dst.mods.install
从 Workshop 安装模组。
```json
// 请求
{"method": "dst.mods.install", "params": {"cluster_name": "MyWorld", "workshop_id": "378160973"}}
// 完成后推送 event.task.completed
```

### dst.mods.remove
移除模组。
### dst.mods.config.get / dst.mods.config.set
读取/设置 modoverrides.lua 中的配置项。

## 存档管理

### dst.backup.create
创建存档备份。
```json
// 请求
{"method": "dst.backup.create", "params": {"cluster_name": "MyWorld", "name": "pre-update-backup", "note": "Before DST update 562114"}}

// 响应（异步，完成后平台收到备份文件）
{"result": {"task_id": "uuid", "status": "pending"}}
```

### dst.backup.list
备份列表。
```json
// 响应
{"result": {"backups": [{"id": 1, "name": "pre-update-backup", "size_mb": 45, "created_at": "2026-05-15 10:00:00"}]}}
```

### dst.backup.restore
恢复备份。
```json
// 请求
{"method": "dst.backup.restore", "params": {"cluster_name": "MyWorld", "backup_id": 1}}
```

### dst.saves.list
存档列表（游戏内保存点）。
### dst.saves.rollback
回滚到指定存档。

## 系统监控

### node.metrics
系统资源指标。
```json
// 响应
{"result": {"cpu": {"percent": 45}, "memory": {"used_gb": 2.1, "total_gb": 8}, "disk": {"used_gb": 15, "total_gb": 100, "percent": 15}, "network": {"rx_mbps": 0.5, "tx_mbps": 0.3}, "dst_process": {"cpu_percent": 30, "mem_mb": 800}}}
```

### node.health
自身健康检查。
```json
// 响应
{"result": {"status": "healthy", "uptime_sec": 86400, "version": "1.0.0", "go_version": "go1.22"}}
```

## 文件管理

### dst.files.list
列出目录内容。
### dst.files.read
读取文件内容。
### dst.files.write
写入文件内容（限制 DST 配置目录范围内）。

## 地图

### dst.map.tiles
获取地图瓦片数据。
### dst.map.metadata
获取地图元数据（大小、生物群落分布）。
