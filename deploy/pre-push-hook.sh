#!/bin/bash
# Pre-push hook — auto compile + test before pushing to GitHub.
# Install: bash deploy/install-hooks.sh
set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; NC='\033[0m'
PASS=0; FAIL=0
ROOT="$(git rev-parse --show-toplevel)"

run_step() {
    local name="$1"; shift
    printf "  %-45s " "$name"
    if "$@" > /tmp/hook-out.txt 2>&1; then
        echo -e "${GREEN}PASS${NC}"; PASS=$((PASS+1))
    else
        echo -e "${RED}FAIL${NC}"; FAIL=$((FAIL+1))
        cat /tmp/hook-out.txt | tail -5
    fi
}

echo "=== Pre-push Check ==="

# 1. Java compile
run_step "Maven compile (all modules)" \
    bash -c "cd '$ROOT/src/backend/general-web-backend' && ./mvnw compile -q"

# 2. Java unit tests (skip integration)
run_step "Java unit tests" \
    bash -c "cd '$ROOT/src/backend/general-web-backend' && ./mvnw test -Dtest='*Test,*Tests,!*IntegrationTest,!*SmokeTest' -DfailIfNoTests=false -q 2>&1 | grep -q 'BUILD SUCCESS'"

# 3. TypeScript
run_step "Admin tsc" \
    bash -c "cd '$ROOT/src/frontend/admin' && npx tsc --noEmit"
run_step "Customer tsc" \
    bash -c "cd '$ROOT/src/frontend/customer' && npx tsc --noEmit"

# 4. Go compile + test
if [ -f "$ROOT/src/node/go.mod" ]; then
    run_step "Go compile + test" \
        bash -c "export PATH=\$PATH:/usr/local/go/bin && cd '$ROOT/src/node' && go build ./... && go test ./..."
fi

# 5. .env vs .env.example key consistency
run_step ".env key check" bash -c "
    cd '$ROOT/deploy/docker'
    env_keys=\$(grep -oP '^[A-Z_]+(?==)' .env | sort)
    ex_keys=\$(grep -oP '^[A-Z_]+(?==)' .env.example | sort)
    diff <(echo \"\$env_keys\") <(echo \"\$ex_keys\") > /tmp/env-diff.txt 2>&1 || {
        echo 'KEYS IN .env BUT MISSING FROM .env.example:'
        grep '^<' /tmp/env-diff.txt | sed 's/^< //'
        /bin/false
    }
"

echo ""
echo "Result: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] && echo -e "${GREEN}PUSH ALLOWED${NC}" && exit 0
echo -e "${RED}PUSH BLOCKED — fix failures above${NC}"
exit 1
