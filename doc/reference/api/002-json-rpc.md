# JSON-RPC 2.0 通信协议

## 协议基础

- 传输层：WebSocket (wss://)
- 序列化：JSON
- 协议版本：JSON-RPC 2.0
- 编码：UTF-8

## 消息格式

### 请求（平台 → Node）

```json
{
  "jsonrpc": "2.0",
  "id": "req_001",
  "method": "dst.players.list",
  "params": {}
}
```

### 响应（Node → 平台）

```json
{
  "jsonrpc": "2.0",
  "id": "req_001",
  "result": {
    "players": [
      {"name": "Wilson", "steam_id": "76561198000000001", "character": "wilson", "play_time_min": 120}
    ]
  }
}
```

### 错误响应

```json
{
  "jsonrpc": "2.0",
  "id": "req_001",
  "error": {
    "code": -32601,
    "message": "Method not found",
    "data": "dst.players.listX is not a valid method"
  }
}
```

### 事件推送（Node → 平台，无需响应）

```json
{
  "jsonrpc": "2.0",
  "method": "event.player.join",
  "params": {
    "name": "Willow",
    "steam_id": "76561198000000002",
    "character": "willow",
    "timestamp": 1700000000
  }
}
```

### 任务进度推送（Node → 平台）

```json
{
  "jsonrpc": "2.0",
  "method": "event.task.progress",
  "params": {
    "task_id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "running",
    "progress": 45,
    "step": "Downloading DST server files...",
    "bytes_downloaded": 45000000,
    "total_bytes": 100000000
  }
}
```

### 系统事件推送

```json
{
  "jsonrpc": "2.0",
  "method": "event.system",
  "params": {
    "type": "dst.crash",
    "message": "DST Master process exited with code 1",
    "timestamp": 1700000000
  }
}
```

## 错误码

| 错误码 | 含义 | 说明 |
|--------|------|------|
| -32700 | Parse error | JSON 解析失败 |
| -32600 | Invalid request | 不是有效的 JSON-RPC 2.0 请求 |
| -32601 | Method not found | 方法不存在 |
| -32602 | Invalid params | 参数类型/数量错误 |
| -32603 | Internal error | Node 内部错误 |
| -32000 | DST not running | DST 进程未运行 |
| -32001 | Permission denied | 文件/目录权限不足 |
| -32002 | SteamCMD error | SteamCMD 下载/更新失败 |
| -32003 | Disk full | 磁盘空间不足 |

## 连接管理

### 认证消息（连接建立后第一条消息）

```json
// Node → Platform
{
  "jsonrpc": "2.0",
  "id": "auth_1",
  "method": "connect",
  "params": {
    "node_id": "n_x1y2z3",
    "version": "1.0.0",
    "hostname": "dst-server-01",
    "capabilities": ["dst.process", "dst.mod", "dst.backup", "node.metrics"]
  }
}
```

### 心跳

```json
// Platform → Node（每 30s）
{"jsonrpc": "2.0", "method": "ping", "params": {}}

// Node → Platform（响应）
{"jsonrpc": "2.0", "method": "pong", "params": {"timestamp": 1700000000}}
```

## 并发与顺序

- 请求可以并发发送（通过不同的 id 标识）
- 响应顺序不保证与请求顺序一致
- 事件推送可以在任意时刻发送（无需请求）
- 同一个 id 不应被复用（使用 UUID 或递增序列）

## 日志订阅

日志是特殊的请求-流式响应模式：

```
1. Platform → Node: dst.logs.subscribe {shard: "Master"}
2. Node → Platform: event.log {shard: "Master", line: "[00:00:01] Loading world..."}
3. Node → Platform: event.log {shard: "Master", line: "[00:00:05] World loaded."}
4. ...（持续推送，直到取消订阅）
5. Platform → Node: dst.logs.unsubscribe {shard: "Master"}
```
