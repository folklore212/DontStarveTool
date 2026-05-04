#!/bin/bash
# ============================================================
# 一键启动所有服务（在服务器上运行）
# 前置条件: 已运行 build-local.sh 并执行过 deploy.sh
# ============================================================
set -e

cd "$(dirname "$0")"

set -a; source .env; set +a

docker compose up -d --build --remove-orphans

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

check_backend() {
    until curl -sf http://localhost:8080/actuator/health/liveness 2>/dev/null; do
        sleep 2
    done
    echo "  Backend ready."
}

check_mysql &
check_redis &
check_backend &
wait

echo "All services running."
docker compose ps
echo ""
echo "  Customer: http://localhost:${CUSTOMER_PORT:-80}"
echo "  Admin:    http://localhost:${ADMIN_PORT:-3000}"
