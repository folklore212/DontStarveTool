#!/bin/bash
# ============================================================
# 本地构建脚本 — 编译所有产物到 deploy/docker/ 目录
# 前置条件: JDK 21, Maven, Node.js 18+
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DOCKER_DIR="$SCRIPT_DIR/docker"

echo "=== 1/3 构建 Backend JAR ==="
cd "$PROJECT_ROOT/src/backend/general-web-backend"
./mvnw package -DskipTests -q
cp target/*.jar "$DOCKER_DIR/backend/app.jar"
echo "  -> $DOCKER_DIR/backend/app.jar"

echo "=== 2/3 构建 Admin 前端 ==="
cd "$PROJECT_ROOT/src/admin"
npm install --registry=https://registry.npmmirror.com --silent
npm run build
rm -rf "$DOCKER_DIR/admin/dist"
cp -r dist "$DOCKER_DIR/admin/dist"
echo "  -> $DOCKER_DIR/admin/dist/"

echo "=== 3/3 构建 Customer 前端 ==="
cd "$PROJECT_ROOT/src/customer"
npm install --registry=https://registry.npmmirror.com --silent
npm run build
rm -rf "$DOCKER_DIR/customer/dist"
cp -r dist "$DOCKER_DIR/customer/dist"
echo "  -> $DOCKER_DIR/customer/dist/"

echo ""
echo "Build complete. Artifacts ready in $DOCKER_DIR"
