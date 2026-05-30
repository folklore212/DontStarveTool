#!/bin/bash
# ============================================================
# 健康检查脚本 — 验证所有服务是否正常
# 用法: bash health-check.sh
# 退出码: 0 = 全部健康, 1 = 存在异常
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEPLOY_DIR="$(dirname "$SCRIPT_DIR")"

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'
PASS=0
FAIL=0

check() {
  local name="$1"
  local method="$2"
  local target="$3"
  printf "  %-35s " "$name"
  if $method "$target" > /dev/null 2>&1; then
    echo -e "${GREEN}PASS${NC}"
    PASS=$((PASS + 1))
  else
    echo -e "${RED}FAIL${NC}"
    FAIL=$((FAIL + 1))
  fi
}

echo "=== Health Check ==="

# Infrastructure
check "mysql (3306)" docker compose exec -T mysql mysqladmin ping -h 127.0.0.1 -u root --password="\$MYSQL_ROOT_PASSWORD" --silent
check "redis (6379)" docker compose exec -T redis redis-cli -a "\$REDIS_PASSWORD" --no-auth-warning PING

# Java backends
check "core-platform (8081)" curl -sf http://localhost:8081/actuator/health
check "template-service (8082)" curl -sf http://localhost:8082/actuator/health
check "server-service (8083)" curl -sf http://localhost:8083/actuator/health
check "steam-cache-service (8084)" curl -sf http://localhost:8084/actuator/health

# Go gateway
check "node-gateway (8090)" curl -sf http://localhost:8090/health

# Frontends
check "admin (3000)" curl -sf http://localhost:3000/
check "customer (80)" curl -sf http://localhost:80/

# API gateway
check "nginx gateway (80)" curl -sf http://localhost:80/

echo ""
echo "Result: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] && echo -e "${GREEN}ALL HEALTHY${NC}" && exit 0
echo -e "${RED}SOME SERVICES UNHEALTHY${NC}"
exit 1
