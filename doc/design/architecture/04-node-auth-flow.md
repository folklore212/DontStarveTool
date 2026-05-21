# Node 认证流程

## Bootstrap Token 认证（当前方案 1）

```
DST Server (node-agent)                  平台 (Docker)
═══════════════════════                  ══════════════

 ① 管理员在平台生成 Token
    ┌─────────────────────────────────────────────────────┐
    │ POST /api/v1/servers/{id}/tokens                   │
    │ → {token: "dsn-abcd1234...", prefix: "dsn-abcd1234"}│
    │ Token 仅此时完整返回，之后仅存 SHA-256 哈希           │
    └─────────────────────────────────────────────────────┘

 ② Token 写入 Node 配置
    /opt/dst-node/config.json
    { "token": "dsn-abcd1234...", ... }

 ③ WebSocket 连接 + 验证
    ┌──────────┐         ┌─────┐         ┌─────────────┐
    │node-agent│         │nginx│         │node-gateway │
    └────┬─────┘         └──┬──┘         └──────┬──────┘
         │                  │                    │
         │ wss://platform/node?token=dsn-abcd1234
         │─────────────────▶│                    │
         │                  │  Upgrade WS        │
         │                  │───────────────────▶│
         │                  │                    │
         │                  │            token → GET /internal/nodes/verify
         │                  │                    │───────────────▶│
         │                  │                    │                │ core-platform
         │                  │                    │   {valid:true} │
         │                  │                    │◀───────────────│
         │                  │                    │
         │                  │    101 Switching   │
         │◀─────────────────│◀───────────────────│
         │                  │                    │
         │  {"jsonrpc":"2.0","method":"node.health","id":1}
         │──────────────────────────────────────▶│
         │                  │                    │
         │  {"jsonrpc":"2.0","result":{...},"id":1}
         │◀──────────────────────────────────────│
         │                  │                    │

 ④ Token 吊销
    ┌─────────────────────────────────────────────────────┐
    │ DELETE /api/v1/servers/{id}/tokens/{tid}            │
    │ → status=0, 下次验证返回 {valid:false}               │
    │ node-gateway 定时刷新 token 缓存（60s TTL）           │
    │ 已连接的 Node 不受影响，但重连时会被拒绝               │
    └─────────────────────────────────────────────────────┘
```

## 重连与退避

```
连接断开
    │
    ▼
等待 1s ──▶ 重连 ──▶ 失败 ──▶ 等待 2s ──▶ 重连 ──▶ 失败
                                              │
                                              ▼
                                         等待 4s ──▶ ... ──▶ 等待 60s (保持)
                                              │
                                              ▼
                                         重连成功 ──▶ 恢复通信
```

## 预留的 HMAC 扩展点（方案 2）

JSON-RPC Request 中 `"sig"` 字段当前为 `null`。未来升级路径：

```
① Bootstrap token 验证成功后
② node-gateway 下发 session_key: {"jsonrpc":"2.0","id":0,"result":{"session_key":"..."}}
③ 后续请求携带 HMAC 签名:
   {"jsonrpc":"2.0","method":"dst.start","id":1, "sig":"HmacSHA256(body|timestamp|session_key)"}
④ node-gateway 验证签名, 签名不匹配 → 断开连接
```

**何时启用**：当 Node 数量 >100 且 Token 泄漏风险增大时。
