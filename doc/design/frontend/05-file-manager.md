# 文件管理器设计

## 概述

在线查看和编辑 DST 服务器配置文件。Node 提供文件列表/读/写能力。前端使用 Monaco Editor 提供语法高亮。

## 页面结构

```
┌──────────────────────────────────────────────────┐
│  文件管理器 — MyServer1                            │
├──────────────┬───────────────────────────────────┤
│  目录树       │  编辑器                            │
│              │                                   │
│  📁 Master   │  1  [Server]                      │
│   📄 cluster │  2  network_port = 10999          │
│   📄 server  │  3  is_master = true              │
│   📄 modover │  4  max_players = 6               │
│   📄 adminli │  5  game_mode = survival          │
│  📁 Caves    │  6  ...                           │
│   📄 server  │                                   │
│   📄 modover │                                   │
│              │                                   │
│              │  ┌─ 文件信息 ────────────────────┐ │
│              │  │ 路径: /Master/server.ini       │ │
│              │  │ 大小: 256 B                    │ │
│              │  │ 修改: 2026-05-15 10:00         │ │
│              │  └───────────────────────────────┘ │
│              │                     [保存] [重置]  │
└──────────────┴───────────────────────────────────┘
```

## 编辑器能力

- Monaco Editor（VS Code 内核）
- 语言自动检测：`.lua` → Lua 语法高亮，`.ini` → INI 语法高亮，`.txt` → 纯文本
- 自动缩进、括号匹配、查找替换
- 只读模式（对于不应被编辑的文件）
- 未保存修改提示

## 文件白名单

仅允许访问 DST 配置目录内的文件：
- `{cluster_path}/cluster.ini`
- `{cluster_path}/cluster_token.txt`
- `{cluster_path}/{shard}/server.ini`
- `{cluster_path}/{shard}/modoverrides.lua`
- `{cluster_path}/{shard}/leveldataoverride.lua`
- `{cluster_path}/{shard}/adminlist.txt`
- `{cluster_path}/{shard}/blocklist.txt`

禁止访问：`save/`（二进制数据）、系统文件

## 保存后操作

- 修改 `server.ini` → 提示需要重启服务器生效
- 修改 `modoverrides.lua` → 提示需要重启服务器后加载新配置
- 修改 `adminlist.txt` / `blocklist.txt` → 立即生效
- 提供"保存并重启"快捷按钮

## Node 实现

```json
// dst.files.list
{"path": "/{shard}/"}

// dst.files.read
{"path": "/Master/server.ini"}

// dst.files.write
{"path": "/Master/server.ini", "content": "[Server]\nnetwork_port = 10999\n..."}
```
