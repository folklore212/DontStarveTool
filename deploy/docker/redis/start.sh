#!/bin/bash
set -e

# 可通过环境变量覆盖默认值
REDIS_PASSWORD="${REDIS_PASSWORD:-your_strong_redis_password}"
REDIS_PORT="${REDIS_PORT:-6379}"

docker build -t dst-redis:v1 .
docker rm -f auth-redis-container 2>/dev/null || true

docker run -d \
    --name auth-redis-container \
    --restart unless-stopped \
    -p "${REDIS_PORT}:6379" \
    -e "REDIS_PASSWORD=${REDIS_PASSWORD}" \
    dst-redis:v1

echo "Redis starting (port ${REDIS_PORT})..."
until docker exec auth-redis-container redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning PING 2>/dev/null; do
    sleep 1
done
echo "Redis is ready."
