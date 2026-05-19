#!/bin/bash
# ============================================================
# 云端部署脚本 — 上传产物到服务器并启动服务
# 用法:
#   SERVER_HOST=106.15.53.246 bash deploy.sh
#   SERVER_HOST=1.2.3.4 SERVER_USER=root bash deploy.sh
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DOCKER_DIR="$SCRIPT_DIR/docker"

SERVER_HOST="${SERVER_HOST:?请设置 SERVER_HOST 环境变量}"
SERVER_USER="${SERVER_USER:-root}"
DEPLOY_PATH="${DEPLOY_PATH:-/opt/auth-system}"
SSH_KEY="${SSH_KEY:-}"
CLEAN="${CLEAN:-false}"        # CLEAN=true 清除旧镜像和容器缓存
CLEAN_VOLUMES="${CLEAN_VOLUMES:-false}"  # 同时清除数据卷（危险！）

if [ -n "$SSH_KEY" ]; then
    SSH_OPTS="-i $SSH_KEY -o StrictHostKeyChecking=no"
    SCP_OPTS="-i $SSH_KEY -o StrictHostKeyChecking=no"
else
    SSH_OPTS="-o StrictHostKeyChecking=no"
    SCP_OPTS="-o StrictHostKeyChecking=no"
fi

echo "=== Deploying to $SERVER_USER@$SERVER_HOST:$DEPLOY_PATH ==="

# 1. 创建远程目录
ssh $SSH_OPTS "$SERVER_USER@$SERVER_HOST" "mkdir -p $DEPLOY_PATH/docker/{mysql,redis,backend,admin,test,customer}"

# 2. 上传 docker 配置（排除 .env 保护敏感信息）
echo "--- Uploading configs ---"
scp $SCP_OPTS -r "$DOCKER_DIR/docker-compose.yml" "$SERVER_USER@$SERVER_HOST:$DEPLOY_PATH/docker/"
scp $SCP_OPTS -r "$DOCKER_DIR/mysql/"* "$SERVER_USER@$SERVER_HOST:$DEPLOY_PATH/docker/mysql/"
scp $SCP_OPTS -r "$DOCKER_DIR/redis/"* "$SERVER_USER@$SERVER_HOST:$DEPLOY_PATH/docker/redis/"
scp $SCP_OPTS -r "$DOCKER_DIR/backend/"* "$SERVER_USER@$SERVER_HOST:$DEPLOY_PATH/docker/backend/"
scp $SCP_OPTS -r "$DOCKER_DIR/admin/"* "$SERVER_USER@$SERVER_HOST:$DEPLOY_PATH/docker/admin/"
scp $SCP_OPTS -r "$DOCKER_DIR/customer/"* "$SERVER_USER@$SERVER_HOST:$DEPLOY_PATH/docker/customer/"
scp $SCP_OPTS -r "$DOCKER_DIR/test/"* "$SERVER_USER@$SERVER_HOST:$DEPLOY_PATH/docker/test/"

# 3. 上传 .env（仅当远程不存在时，避免覆盖用户修改）
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

# 4. Clean old images/containers (if enabled)
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

# 5. 构建镜像并启动
echo "--- Building images on server ---"
ssh $SSH_OPTS "$SERVER_USER@$SERVER_HOST" "cd $DEPLOY_PATH/docker && docker compose build"

echo "--- Starting services ---"
ssh $SSH_OPTS "$SERVER_USER@$SERVER_HOST" "cd $DEPLOY_PATH/docker && docker compose --env-file .env up -d --remove-orphans"

# 5. 等待健康检查
echo "--- Waiting for services ---"
sleep 5
ssh $SSH_OPTS "$SERVER_USER@$SERVER_HOST" "cd $DEPLOY_PATH/docker && docker compose ps"

echo ""
echo "Deploy complete!"
echo "  Customer: http://$SERVER_HOST:${CUSTOMER_PORT:-80}"
echo "  Admin:    http://$SERVER_HOST:${ADMIN_PORT:-3000}"
