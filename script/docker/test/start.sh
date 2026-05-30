#!/bin/bash
set -e

MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-your_strong_password}"
REDIS_PASSWORD="${REDIS_PASSWORD:-your_strong_redis_password}"

docker build \
    --build-arg HTTP_PROXY="${HTTP_PROXY:-http://proxyhk.zte.com.cn:80}" \
    --build-arg HTTPS_PROXY="${HTTPS_PROXY:-http://proxyhk.zte.com.cn:80}" \
    --build-arg NO_PROXY="${NO_PROXY:-localhost,127.0.0.1,*.zte.com.cn,*.zte.intra,10.0.0.0/8}" \
    -t dst-test:v1 .
echo "Running database tests..."
docker run --rm \
    --network docker_auth-net \
    -e "MYSQL_HOST=auth-mysql-container" \
    -e "MYSQL_PASSWORD=${MYSQL_ROOT_PASSWORD}" \
    -e "REDIS_HOST=auth-redis-container" \
    -e "REDIS_PASSWORD=${REDIS_PASSWORD}" \
    dst-test:v1
