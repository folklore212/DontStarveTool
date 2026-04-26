#!/bin/bash
set -e

cd "$(dirname "$0")"

# 读取 .env 配置
set -a; source .env; set +a

docker compose up -d --build --force-recreate --remove-orphans

echo "Waiting for services..."

check_mysql() {
    until docker compose exec -T mysql mysqladmin ping \
        -h 127.0.0.1 -u root -p"${MYSQL_ROOT_PASSWORD}" --silent 2>/dev/null; do
        sleep 2
    done
    echo "  MySQL ready."
}

check_redis() {
    until docker compose exec -T redis redis-cli \
        -a "${REDIS_PASSWORD}" --no-auth-warning PING 2>/dev/null; do
        sleep 1
    done
    echo "  Redis ready."
}

check_mysql &
check_redis &
wait

echo "All services running."
docker compose ps
