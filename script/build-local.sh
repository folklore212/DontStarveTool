#!/bin/bash
# ============================================================
# 本地构建脚本 — 编译所有产物到 script/build/ 目录
# 前置条件: JDK 21, Maven, Node.js 18+
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BUILD_DIR="$SCRIPT_DIR/build"
CLEAN="${CLEAN:-false}"

if [ "$CLEAN" = "true" ]; then
    echo "=== 0/3 清理构建缓存 ==="
    cd "$PROJECT_ROOT/src/backend/general-web-backend" && ./mvnw clean -q 2>/dev/null || true
    cd "$PROJECT_ROOT/src/frontend/admin" && rm -rf dist node_modules/.vite 2>/dev/null || true
    cd "$PROJECT_ROOT/src/frontend/customer" && rm -rf dist node_modules/.vite 2>/dev/null || true
    rm -rf "$BUILD_DIR"
    echo "  构建缓存已清除"
fi

mkdir -p "$BUILD_DIR/backend/core-platform" "$BUILD_DIR/backend/template-service" \
         "$BUILD_DIR/backend/server-service" "$BUILD_DIR/backend/steam-cache-service" \
         "$BUILD_DIR/frontend/admin" "$BUILD_DIR/frontend/customer"

echo "=== 1/3 构建 Backend JARs (5 modules) ==="
cd "$PROJECT_ROOT/src/backend/general-web-backend"
./mvnw package -DskipTests -q

cp core-platform/target/*.jar "$BUILD_DIR/backend/core-platform/app.jar" 2>/dev/null
cp template-service/target/*.jar "$BUILD_DIR/backend/template-service/app.jar" 2>/dev/null
cp server-service/target/*.jar "$BUILD_DIR/backend/server-service/app.jar" 2>/dev/null
cp steam-cache-service/target/*.jar "$BUILD_DIR/backend/steam-cache-service/app.jar" 2>/dev/null

echo "  -> core-platform: $(ls -lh $BUILD_DIR/backend/core-platform/app.jar 2>/dev/null | awk '{print $5}')"
echo "  -> template-service: $(ls -lh $BUILD_DIR/backend/template-service/app.jar 2>/dev/null | awk '{print $5}')"
echo "  -> server-service: $(ls -lh $BUILD_DIR/backend/server-service/app.jar 2>/dev/null | awk '{print $5}')"
echo "  -> steam-cache-service: $(ls -lh $BUILD_DIR/backend/steam-cache-service/app.jar 2>/dev/null | awk '{print $5}')"

echo "=== 2/3 构建 Admin 前端 ==="
cd "$PROJECT_ROOT/src/frontend/admin"
npm install --registry=https://registry.npmmirror.com --silent
npm run build
rm -rf "$BUILD_DIR/frontend/admin/dist"
cp -r dist "$BUILD_DIR/frontend/admin/dist"
echo "  -> $BUILD_DIR/frontend/admin/dist/"

echo "=== 3/3 构建 Customer 前端 ==="
cd "$PROJECT_ROOT/src/frontend/customer"
npm install --registry=https://registry.npmmirror.com --silent
npm run build
rm -rf "$BUILD_DIR/frontend/customer/dist"
cp -r dist "$BUILD_DIR/frontend/customer/dist"
echo "  -> $BUILD_DIR/frontend/customer/dist/"

echo ""
echo "Build complete. Artifacts ready in $BUILD_DIR"
