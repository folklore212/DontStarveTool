---
name: Node Agent 设计
description: Node Agent 设计
status: approved
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [documentation]
---

# Node Agent 设计

## 概述

Node Agent 是部署到每台 DST 服务器的轻量 Go 守护进程。它通过 WebSocket 长连接主动连接平台，使用 JSON-RPC 2.0 协议接收命令和推送数据。

## 核心设计原则

1. **单二进制部署**：无运行时依赖，跨平台编译（Linux/Windows/macOS）
2. **主动出站连接**：Node 发起 WebSocket 连接，解决 NAT/防火墙问题
3. **最小权限**：仅操作 DST 相关目录，不访问系统敏感区域
4. **优雅降级**：平台不可用时继续本地操作，重连后批量同步

## 配置

```json
// /opt/dst-node/config.json
{
  "platform_url": "wss://platform.example.com:8443/node",
  "platform_tls_hash": "sha256:AAAA...",
  "dst_install_path": "/home/steam/steamapps/DST",
  "cluster_base_path": "/home/steam/.klei/DoNotStarveTogether",
  "log_level": "info",
  "metrics_interval_sec": 30,
  "heartbeat_interval_sec": 30
}
```

## 生命周期

```
启动 → 检查本地证书 → 有证书 → mTLS 连接 → 正常通信
                    → 无证书 → stdin 读 token → TLS 连接 → 注册 → 获取证书 → 断开 → mTLS 重连

证书 < 30 天到期 → 自动续期 (renew_cert)
证书过期 → 退回到注册流程（需 bootstrap token）
bootstrap token 过期 → 退出，等待管理员重新部署
```

## 连接管理

- 心跳间隔：30 秒发送 ping，60 秒无 pong 视为断开
- 重连退避：1s → 2s → 4s → 8s → 16s → 30s → 60s（保持）
- 离线缓冲：断开期间持续采集指标，重连后批量上报
- 平台故障：节点继续本地运行 DST，不中断游戏服务

## 文件系统权限

| 路径 | 权限 | 用途 |
|------|------|------|
| `/opt/dst-node/` | rw- | 二进制 + 配置文件 + 证书 |
| `steamapps/DST/mods/` | rw- | 模组安装/读取 modinfo.lua |
| `.klei/DoNotStarveTogether/*/` | rw- | 存档/配置/日志 |
| `dst_backups/` | rw- | 备份文件创建/读取 |

禁止访问：`/etc/`、`/home/其他用户/`、系统二进制目录

## 系统集成

```ini
# /etc/systemd/system/dst-node.service
[Unit]
Description=DST Node Agent
After=network.target

[Service]
Type=simple
User=steam
WorkingDirectory=/opt/dst-node
ExecStart=/opt/dst-node/dst-node
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

## 升级策略

- 平台推送新版本通知（node.update.available 事件）
- 管理员在平台触发更新：Node 下载新二进制 → 验证 checksum → 优雅关闭 → 替换 → 重启
- 滚动更新：逐台更新，避免同时中断
