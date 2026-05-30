#!/bin/bash
# ============================================================
# 回滚脚本 — 将服务回滚到指定 Git SHA 版本的镜像
# 用法: bash rollback.sh <sha> [service]
#   sha     — 目标 Git commit SHA (短格式, 7位)
#   service — 可选, 指定回滚的服务名; 默认 all 回滚全部
# 示例:
#   bash rollback.sh a1b2c3d              # 回滚全部服务
#   bash rollback.sh a1b2c3d core-platform # 仅回滚 core-platform
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEPLOY_DIR="$(dirname "$SCRIPT_DIR")"

SHA="${1:-}"
SERVICE="${2:-all}"

if [ -z "$SHA" ]; then
  echo "Usage: bash rollback.sh <sha> [service]"
  echo "Example: bash rollback.sh a1b2c3d"
  exit 1
fi

cd "$DEPLOY_DIR"

SERVICES=(core-platform template-service server-service steam-cache-service node-gateway admin customer)
ACR_REGISTRY="${ACR_REGISTRY:-}"
ACR_NAMESPACE="${ACR_NAMESPACE:-}"

rollback_one() {
  local svc="$1"
  local image="${ACR_REGISTRY}/${ACR_NAMESPACE}/dst-${svc}:${SHA}"
  echo "Pulling ${svc}:${SHA} ..."
  docker pull "$image"
  echo "Restarting ${svc} ..."
  docker compose up -d --force-recreate "$svc"
}

if [ "$SERVICE" = "all" ]; then
  for svc in "${SERVICES[@]}"; do
    rollback_one "$svc"
  done
else
  rollback_one "$SERVICE"
fi

echo ""
echo "=== Service status ==="
docker compose ps

echo ""
echo "Rollback to ${SHA} complete. Running health check..."
bash "$SCRIPT_DIR/health-check.sh"
