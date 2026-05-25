---
name: Node Agent 认证流程
description: Node Agent 认证流程
status: approved
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [documentation]
---

# Node Agent 认证流程

## 四阶段认证

### 阶段 0：预置信任（编译时）

平台 TLS 公钥 SHA256 Hash 硬编码进 Node 二进制。Node 在 TLS 握手时验证服务端证书 Hash，拒绝不匹配的连接。
即使攻击者控制 DNS 或网络，也无法伪造平台身份。

### 阶段 1：引导注册（SSH 安全通道）

1. 管理员在平台点击"纳管服务器"
2. 平台生成 bootstrap token（JWT, 24h 过期, 5 次使用上限）
3. 平台 SSH 到目标服务器，SCP 上传 Node 二进制，写入配置文件
4. 平台通过 SSH stdin 将 token 传入 Node 进程（不落盘）
5. Node 从 stdin 读取 token，存入内存

Bootstrap token JWT payload：
```json
{
  "sub": "bootstrap",
  "server_id": 123,
  "iat": 1700000000,
  "exp": 1700086400,
  "jti": "unique-token-id",
  "max_uses": 5
}
```

### 阶段 2：mTLS 证书签发

1. Node 生成临时 ECDSA P-256 密钥对
2. Node 创建 CSR（包含 server_id 和 hostname）
3. Node 建立 wss:// 连接，TLS 握手时验证平台证书 Hash
4. Node 发送 `register` 消息（携带 token + CSR）
5. 平台验证 token（签名、过期时间、使用次数、未撤销）
6. 平台用 CA 签发客户端证书（CN=node_xxx, SAN=server:123, 365 天有效期）
7. 平台返回证书 + CA 链
8. Node 保存证书到磁盘（cert.pem 0644, key.pem 0600, ca.pem 0644）

### 阶段 3：持久 mTLS 连接

1. Node 断开当前连接
2. Node 建立新的 wss:// 连接，出示客户端证书
3. 平台验证：证书有效、CA 签名正确、未被 CRL 撤销、CN 匹配 node_id
4. 连接建立，开始正常 JSON-RPC 通信
5. 每条消息携带 timestamp + nonce（5 秒窗口防重放）

## 证书生命周期

| 事件 | 时间 | 操作 |
|------|------|------|
| 签发 | 注册时 | 365 天有效期 |
| 续期 | 到期前 30 天 | Node 发送 renew_cert → 平台签发新证书 |
| 过期 | 到期后 | Node 退回阶段 2（需新的 bootstrap token） |
| 撤销 | 管理员操作 | 加入 CRL → 下次心跳时断开连接 |

## 平台 CA 管理

平台首次启动时自动生成 CA 密钥对（或通过配置指定）：
- `ca-cert.pem`：用于签发 Node 客户端证书
- `ca-key.pem`：仅平台 Gateway 可读（0600 权限）
- `crl.pem`：证书吊销列表，定期更新

## 安全场景防护

| 场景 | 措施 |
|------|------|
| Bootstrap token 泄露 | 5 次使用上限, 24h 过期, stdin 传入不落盘 |
| 中间人攻击 (MITM) | Node 硬编码平台 TLS Hash, 非匹配证书拒绝连接 |
| 证书文件被盗 | 私钥仅本机可读 (0600), 盗走后需同时窃取 Node 才能使用 |
| 证书丢失/磁盘故障 | Node 进入恢复模式, 等待管理员重新 SSH 部署 |
| 节点被攻陷 | 管理员撤销节点 → CRL → 连接断开, DST 进程不受影响 |
| 平台私钥泄露 | 重新签发 CA → 所有 Node 需重新部署（灾难恢复） |
| 重放攻击 | 每条消息携带 timestamp + nonce, 5 秒窗口 |

## 恢复流程

当 Node 无法正常连接时（证书过期 + 无 bootstrap token）：

1. Node 退出并记录日志（exit code 2）
2. systemd 检测到退出，按 RestartSec 重启
3. 重启后仍然失败 → systemd 记录到 journald
4. 管理员收到平台告警（该服务器 Node 离线超过 N 分钟）
5. 管理员 SSH 到服务器，检查日志，重新生成 token 并部署
