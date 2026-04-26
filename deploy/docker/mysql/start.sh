#!/bin/bash
set -e

# 可通过环境变量覆盖默认值
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-your_strong_password}"
MYSQL_PORT="${MYSQL_PORT:-3306}"

docker build -t dst-mysql:v1 .
docker rm -f auth-mysql-container 2>/dev/null || true

docker run -d \
    --name auth-mysql-container \
    --restart unless-stopped \
    -p "${MYSQL_PORT}:3306" \
    -e "MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}" \
    --security-opt seccomp:unconfined \
    dst-mysql:v1

echo "MySQL starting (port ${MYSQL_PORT})..."
until docker exec auth-mysql-container mysqladmin ping -h 127.0.0.1 -u root -p"${MYSQL_ROOT_PASSWORD}" --silent 2>/dev/null; do
    sleep 2
done
echo "MySQL is ready."
