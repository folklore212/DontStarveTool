# ADR-001: 跨服务认证采用 Gateway 注入 X-User-Id header

## 状态
已决定（2026-05-18）

## 背景
平台计划从单体拆分为 4 个独立服务。当前 `SecurityUtil.getCurrentUserId()` 依赖 Spring Security 上下文，仅在同一个 JVM 内工作。拆分为独立进程后，每个服务无法直接读取用户身份。

## 决策
采用 **API Gateway 注入 header** 模式（方案 A）：

1. API Gateway（Nginx/Kong）对所有外部请求验证 JWT
2. Gateway 将 `X-User-Id` header 注入下游请求
3. 内部服务通过 `GatewayTrustFilter` 读取 header 并注入 Spring Security 上下文
4. 仅信任来自内网 IP（10.x/172.x/192.168.x/127.0.0.1）的请求
5. 该 Filter 通过 `gateway.trust.enabled=true` 开关控制

## 拒绝的方案
- **方案 B（各自验 JWT）**：每个服务验签增加约 1ms 延迟，需要分发 JWT 公钥和黑名单。小规模时开销大于收益。
- **方案 C（mTLS 信任）**：适合大规模微服务，4 个服务的规模不需要引入 CA 管理。

## 后果
- 内部服务必须确保端口不对公网暴露（Docker Compose 网络隔离）
- `gateway.trust.enabled` 仅在拆分后启用，当前单体模式不影响
- 若 Gateway 被攻破，攻击者可伪造 header，但内网隔离降低了风险
