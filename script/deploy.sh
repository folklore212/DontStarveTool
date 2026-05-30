#!/bin/bash
# ============================================================
# 云端部署脚本 — 上传产物到服务器并启动服务
# 用法:
#   SERVER_HOST=106.15.53.246 bash deploy.sh           # 普通部署
#   SERVER_HOST=106.15.53.246 CLEAN=true bash deploy.sh # 清理旧镜像后部署
#   SERVER_HOST=106.15.53.246 RESET=true bash deploy.sh # 完全重建环境后部署
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DOCKER_DIR="$SCRIPT_DIR/docker"

SERVER_HOST="${SERVER_HOST:?请设置 SERVER_HOST 环境变量}"
SERVER_USER="${SERVER_USER:-root}"
DEPLOY_PATH="${DEPLOY_PATH:-/opt/auth-system}"
SSH_KEY="${SSH_KEY:-}"

# --- 部署模式 ---
CLEAN="${CLEAN:-false}"          # 删除旧镜像和构建缓存
CLEAN_VOLUMES="${CLEAN_VOLUMES:-false}"  # 同时清除数据卷（危险）
RESET="${RESET:-false}"          # 完全重建：停止服务、删除容器/镜像/网络、清理目录
RESET_VOLUMES="${RESET_VOLUMES:-false}"  # RESET 时同时删除数据卷

if [ -n "$SSH_KEY" ]; then
    SSH_OPTS="-i $SSH_KEY -o StrictHostKeyChecking=no"
    SCP_OPTS="-i $SSH_KEY -o StrictHostKeyChecking=no"
else
    SSH_OPTS="-o StrictHostKeyChecking=no"
    SCP_OPTS="-o StrictHostKeyChecking=no"
fi

echo "=== Deploying to $SERVER_USER@$SERVER_HOST:$DEPLOY_PATH ==="
echo "    CLEAN=$CLEAN  RESET=$RESET  CLEAN_VOLUMES=$CLEAN_VOLUMES  RESET_VOLUMES=$RESET_VOLUMES"
echo ""

# ═══════════════════════════════════════════════════════════
# RESET: 完全停止并清理服务器环境
# ═══════════════════════════════════════════════════════════
if [ "$RESET" = "true" ]; then
    echo "=== RESET: 清理服务器现有环境 ==="
    ssh $SSH_OPTS "$SERVER_USER@$SERVER_HOST" "bash -s" << RESETEOF
cd "$DEPLOY_PATH/docker" 2>/dev/null || { echo "  部署目录为空，跳过清理"; exit 0; }

echo "  停止所有服务..."
docker compose down --remove-orphans --timeout 30 2>/dev/null || true

echo "  删除项目容器..."
docker ps -a --filter "name=auth-" --format '{{.Names}}' 2>/dev/null | \
  while read name; do docker rm -f "\$name" 2>/dev/null || true; done

echo "  删除项目镜像..."
docker images --filter "reference=dst-*" --format '{{.Repository}}:{{.Tag}}' 2>/dev/null | \
  while read img; do docker rmi "\$img" 2>/dev/null || true; done

echo "  清理未使用网络..."
docker network prune -f 2>/dev/null || true

echo "  清理构建缓存..."
docker builder prune -f 2>/dev/null || true

if [ "$RESET_VOLUMES" = "true" ]; then
    echo "  WARNING: 删除数据卷..."
    docker compose down -v 2>/dev/null || true
    docker volume ls --filter "name=auth-" -q 2>/dev/null | \
      while read vol; do docker volume rm "\$vol" 2>/dev/null || true; done
fi

echo "  清理部署目录..."
rm -rf "$DEPLOY_PATH/docker/backend" "$DEPLOY_PATH/docker/admin" \
       "$DEPLOY_PATH/docker/customer" "$DEPLOY_PATH/docker/nginx" \
       "$DEPLOY_PATH/docker/test" 2>/dev/null || true

echo "  RESET 完成"
RESETEOF
    echo ""
fi

# ═══════════════════════════════════════════════════════════
# 1. 创建远程目录
# ═══════════════════════════════════════════════════════════
ssh $SSH_OPTS "$SERVER_USER@$SERVER_HOST" "mkdir -p $DEPLOY_PATH/docker/{mysql,redis,backend,admin,customer,test}"

# ═══════════════════════════════════════════════════════════
# 2. 上传 Docker 配置
# ═══════════════════════════════════════════════════════════
echo "--- Uploading configs ---"
scp $SCP_OPTS -r "$DOCKER_DIR/docker-compose.yml" "$SERVER_USER@$SERVER_HOST:$DEPLOY_PATH/docker/"
scp $SCP_OPTS -r "$DOCKER_DIR/mysql/"* "$SERVER_USER@$SERVER_HOST:$DEPLOY_PATH/docker/mysql/"
scp $SCP_OPTS -r "$DOCKER_DIR/redis/"* "$SERVER_USER@$SERVER_HOST:$DEPLOY_PATH/docker/redis/"
scp $SCP_OPTS -r "$DOCKER_DIR/backend/"* "$SERVER_USER@$SERVER_HOST:$DEPLOY_PATH/docker/backend/"
scp $SCP_OPTS -r "$DOCKER_DIR/admin/"* "$SERVER_USER@$SERVER_HOST:$DEPLOY_PATH/docker/admin/"
scp $SCP_OPTS -r "$DOCKER_DIR/customer/"* "$SERVER_USER@$SERVER_HOST:$DEPLOY_PATH/docker/customer/"
scp $SCP_OPTS -r "$DOCKER_DIR/test/"* "$SERVER_USER@$SERVER_HOST:$DEPLOY_PATH/docker/test/"

# ═══════════════════════════════════════════════════════════
# 3. 上传 .env（仅当远程不存在时）
# ═══════════════════════════════════════════════════════════
if ! ssh $SSH_OPTS "$SERVER_USER@$SERVER_HOST" "test -f $DEPLOY_PATH/docker/.env"; then
    if [ -f "$DOCKER_DIR/.env" ]; then
        scp $SCP_OPTS "$DOCKER_DIR/.env" "$SERVER_USER@$SERVER_HOST:$DEPLOY_PATH/docker/.env"
        echo "--- .env uploaded (new) ---"
    else
        echo "WARNING: No .env file found locally. Create one from .env.example on the server."
    fi
else
    echo "--- .env already exists on server, skipping ---"
fi

# ═══════════════════════════════════════════════════════════
# 4. CLEAN: 清理旧镜像/容器（常规清理，保留卷）
# ═══════════════════════════════════════════════════════════
if [ "$CLEAN" = "true" ]; then
    echo "--- Cleaning old images and containers ---"
    ssh $SSH_OPTS "$SERVER_USER@$SERVER_HOST" "cd $DEPLOY_PATH/docker && docker compose down --remove-orphans"
    ssh $SSH_OPTS "$SERVER_USER@$SERVER_HOST" "docker image prune -f"
    ssh $SSH_OPTS "$SERVER_USER@$SERVER_HOST" "docker builder prune -f"
    if [ "$CLEAN_VOLUMES" = "true" ]; then
        echo "--- WARNING: Removing volumes ---"
        ssh $SSH_OPTS "$SERVER_USER@$SERVER_HOST" "cd $DEPLOY_PATH/docker && docker compose down -v"
    fi
fi

# ═══════════════════════════════════════════════════════════
# 5. 构建镜像并启动
# ═══════════════════════════════════════════════════════════
echo "--- Building images on server ---"
ssh $SSH_OPTS "$SERVER_USER@$SERVER_HOST" "cd $DEPLOY_PATH/docker && docker compose build"

echo "--- Starting services ---"
ssh $SSH_OPTS "$SERVER_USER@$SERVER_HOST" "cd $DEPLOY_PATH/docker && docker compose --env-file .env up -d --remove-orphans"

echo "--- Waiting for services ---"
sleep 5
ssh $SSH_OPTS "$SERVER_USER@$SERVER_HOST" "cd $DEPLOY_PATH/docker && docker compose ps"

echo ""
echo "Deploy complete!"
echo "  Customer: http://$SERVER_HOST:${CUSTOMER_PORT:-80}"
echo "  Admin:    http://$SERVER_HOST:${ADMIN_PORT:-3000}"
