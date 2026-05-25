---
name: 多用户协作设计
description: 多用户协作设计
status: approved
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [documentation]
---

# 多用户协作设计

## 概述

服主可以将服务器分享给其他平台用户共同管理，设置不同的权限级别。

## 数据模型

```sql
CREATE TABLE server_collaborators (
    id BIGINT PRIMARY KEY,
    server_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,  -- reader / operator / admin
    invited_by BIGINT NOT NULL,
    created_at DATETIME,
    UNIQUE KEY uk_server_user (server_id, user_id)
);
```

## 权限级别

| 操作 | reader | operator | admin |
|------|--------|----------|-------|
| 查看服务器状态 | ✅ | ✅ | ✅ |
| 查看日志/控制台 | ✅ | ✅ | ✅ |
| 查看玩家列表 | ✅ | ✅ | ✅ |
| 发送控制台命令 | ❌ | ✅ | ✅ |
| 踢出/封禁玩家 | ❌ | ✅ | ✅ |
| 管理模组 | ❌ | ✅ | ✅ |
| 创建/恢复备份 | ❌ | ✅ | ✅ |
| 修改服务器配置 | ❌ | ❌ | ✅ |
| 删除服务器 | ❌ | ❌ | ❌ (仅所有者) |
| 管理协作者 | ❌ | ❌ | ❌ (仅所有者) |

## 页面交互

### 服务器设置 → 协作标签页

```
┌─ 协作者 ─────────────────────────────────────────┐
│                                                  │
│  [邀请用户]                                       │
│                                                  │
│  用户         │ 权限      │ 邀请者   │ 操作       │
│  user1@qq.com │ 管理员    │ 我      │ [改权限][移除]│
│  user2@qq.com │ 操作员    │ 我      │ [改权限][移除]│
│  user3@qq.com │ 只读      │ 我      │ [改权限][移除]│
└──────────────────────────────────────────────────┘
```

### 邀请流程

1. 服主输入被邀请用户的邮箱/用户名
2. 选择权限级别
3. 发送邀请
4. 被邀请用户收到通知（平台内通知 + 邮件）
5. 被邀请用户接受 → 服务器出现在其仪表盘
6. 被邀请用户在其仪表盘看到共享服务器（带"共享"标识）

### 协作者视角

- 仪表盘服务器列表中，共享服务器显示👥图标 + 权限级别
- 点击进入后，根据权限级别显示/隐藏操作按钮
- 只读用户：所有操作按钮置灰 + tooltip "需要操作员权限"
- 操作员用户：配置修改类按钮隐藏

## API 端点

复用 ServerController 已有的三个端点（目前是 stub）：

- `GET /api/v1/servers/{id}/collaborators` → 列表
- `POST /api/v1/servers/{id}/collaborators` → 添加（body: {userId, role}）
- `DELETE /api/v1/servers/{id}/collaborators/{userId}` → 移除
- `PUT /api/v1/servers/{id}/collaborators/{userId}` → 修改权限 (新增)
