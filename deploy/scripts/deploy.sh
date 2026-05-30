#!/bin/bash
# ============================================================
# 服务端部署脚本 — CI 通过 SSH 调用或运维手动执行
# 用法: bash deploy.sh
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEPLOY_DIR="$(dirname "$SCRIPT_DIR")"

cd "$DEPLOY_DIR"

echo "=== Pulling latest images ==="
docker compose pull

echo "=== Restarting services ==="
docker compose up -d --force-recreate

echo "=== Service status ==="
docker compose ps

echo ""
echo "Deploy complete."
