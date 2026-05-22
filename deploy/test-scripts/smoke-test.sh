#!/bin/bash
# API Smoke Test — run after deploy to verify all services respond.
# Usage: bash smoke-test.sh [HOST]
#        HOST defaults to localhost; set HOST=106.15.53.246 for remote.

set -euo pipefail
HOST="${1:-localhost}"
PASS=0; FAIL=0

check() {
    local name="$1"; local method="$2"; local url="$3"; local expected="${4:-200}"
    local code=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$url" --connect-timeout 5)
    if [ "$code" = "$expected" ]; then
        echo "  PASS $name ($url → $code)"
        PASS=$((PASS+1))
    else
        echo "  FAIL $name ($url → $code, expected $expected)"
        FAIL=$((FAIL+1))
    fi
}

echo "=== Core Platform (:8081) ==="
check "captcha-config"     GET  "http://$HOST:8081/api/v1/auth/captcha-config"
check "login rejects"      POST "http://$HOST:8081/api/v1/auth/login"          400
check "register rejects"   POST "http://$HOST:8081/api/v1/auth/register"       400

echo "=== Template Service (:8082) ==="
check "browse templates"   GET  "http://$HOST:8082/api/v1/templates"
check "workshop hot"       GET  "http://$HOST:8082/api/v1/workshop/hot"
check "workshop search"    GET  "http://$HOST:8082/api/v1/workshop/search?keyword=boss"

echo "=== Server Service (:8083) ==="
check "list servers"       GET  "http://$HOST:8083/api/v1/servers"
check "non-existent 404"   GET  "http://$HOST:8083/api/v1/servers/99999"       404

echo "=== Admin (:3000) ==="
check "admin login page"   GET  "http://$HOST:3000/login"

echo "=== Customer Frontend (nginx :80) ==="
check "customer index"     GET  "http://$HOST:80/"
check "customer login"     GET  "http://$HOST:80/login"

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] && echo "ALL SMOKE TESTS PASSED" || echo "SOME SMOKE TESTS FAILED"
exit $FAIL
