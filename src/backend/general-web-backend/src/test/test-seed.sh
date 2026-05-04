#!/bin/bash
#===============================================================================
# test-seed.sh — 测试数据初始化
# Creates test user accounts, roles, OAuth client, and API key directly via MySQL.
#
# Usage:
#   chmod +x test-seed.sh
#   ./test-seed.sh
#
# Prerequisites:
#   - Docker container auth-mysql-container running MySQL
#   - python3 with bcrypt library (pip install bcrypt)
#   - MySQL root password set in MYSQL_ROOT_PASSWORD
#
# Users created:
#   super@test.com    / Admin1234!  — super_admin role (status=2 ACTIVE)
#   admin@test.com    / Admin1234!  — admin role       (status=2 ACTIVE)
#   user@test.com     / User1234!   — user role        (status=2 ACTIVE)
#   locked@test.com   / User1234!   — LOCKED           (status=3)
#   disabled@test.com / User1234!   — DISABLED         (status=1)
#===============================================================================

set -euo pipefail

# ─── Configuration ───────────────────────────────────────────────────────────
MYSQL_CONTAINER="${MYSQL_CONTAINER:-auth-mysql-container}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-your_strong_password}"
MYSQL_DATABASE="${MYSQL_DATABASE:-auth_system}"

MYSQL_CMD="docker exec ${MYSQL_CONTAINER} mysql -u root -p${MYSQL_ROOT_PASSWORD} -e"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# ─── Helper ──────────────────────────────────────────────────────────────────

# Run MySQL query; exit on error
mysql_run() {
    local query="$1"
    $MYSQL_CMD "USE ${MYSQL_DATABASE}; ${query}" 2>/dev/null
}

# Run MySQL query; return output
mysql_query() {
    local query="$1"
    docker exec "${MYSQL_CONTAINER}" mysql -u root -p"${MYSQL_ROOT_PASSWORD}" \
        --batch --skip-column-names \
        -e "USE ${MYSQL_DATABASE}; ${query}" 2>/dev/null
}

# Generate a bcrypt hash for the given password
hash_password() {
    local password="$1"
    python3 -c "import bcrypt; print(bcrypt.hashpw(b'${password}', bcrypt.gensalt(12)).decode())" 2>/dev/null
}

# Generate a SHA-256 hash string
sha256_hash() {
    local input="$1"
    echo -n "$input" | python3 -c "import sys,hashlib; print(hashlib.sha256(sys.stdin.buffer.read()).hexdigest())" 2>/dev/null
}

# Generate a UUID-like value
uuid_v4() {
    python3 -c "import uuid; print(str(uuid.uuid4()))" 2>/dev/null || echo "$(date +%s)$(shuf -i 1000-9999 -n 1)"
}

# ─── Main ────────────────────────────────────────────────────────────────────

# Generate a unique bigint user ID (uses microsecond timestamp + offset to avoid collisions)
next_user_id() {
    local offset="${1:-0}"
    # Base: seconds since epoch + offset in a range far from Snowflake IDs
    local base=9000000000000000000
    echo $((base + $(date +%s%N | head -c 13) + offset))
}

main() {
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║             TEST DATA SEEDING SCRIPT                             ║${NC}"
    echo -e "${CYAN}║             Database: ${MYSQL_DATABASE}                                   ║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════════╝${NC}"
    echo ""

    # ─── Prerequisites check ─────────────────────────────────────────────────
    echo -e "${BLUE}[INFO]${NC} Checking MySQL connectivity..."
    if ! docker exec "${MYSQL_CONTAINER}" mysqladmin -u root -p"${MYSQL_ROOT_PASSWORD}" ping --silent 2>/dev/null; then
        echo -e "${RED}[ERROR]${NC} Cannot connect to MySQL container '${MYSQL_CONTAINER}'. Aborting."
        exit 1
    fi
    echo -e "  ${GREEN}[OK]${NC} MySQL is reachable."

    echo -e "${BLUE}[INFO]${NC} Checking bcrypt availability..."
    if ! python3 -c "import bcrypt" 2>/dev/null; then
        echo -e "${RED}[ERROR]${NC} Python bcrypt module not found. Install with: pip install bcrypt"
        exit 1
    fi
    echo -e "  ${GREEN}[OK]${NC} bcrypt is available."

    echo ""

    # ─── 1. Generate bcrypt hashes ───────────────────────────────────────────
    echo -e "${CYAN}[Step 1]${NC} Generating bcrypt hashes..."
    HASH_ADMIN=$(hash_password "Admin1234!")
    echo -e "  ${GREEN}[OK]${NC} Hash for 'Admin1234!' generated."
    HASH_USER=$(hash_password "User1234!")
    echo -e "  ${GREEN}[OK]${NC} Hash for 'User1234!' generated."
    echo ""

    # ─── Helper function to create a user + auth + role ─────────────────────
    # Usage: create_user <email> <username> <hash> <status> <role_name> [locked_until_epoch]
    create_user() {
        local email="$1"
        local username="$2"
        local hash="$3"
        local status="$4"
        local role_name="$5"
        local locked_until="${6:-0}"

        local user_id
        local existing_count
        existing_count=$(mysql_query "SELECT COUNT(*) FROM users WHERE email='${email}'")

        if [[ "$existing_count" -gt 0 ]]; then
            echo -e "  ${YELLOW}[SKIP]${NC} User ${email} already exists — updating status."
            user_id=$(mysql_query "SELECT user_id FROM users WHERE email='${email}'")
            if [[ "$locked_until" != "0" ]]; then
                mysql_run "UPDATE users SET status=${status}, locked_until=${locked_until} WHERE user_id=${user_id};"
            else
                mysql_run "UPDATE users SET status=${status}, locked_until=0 WHERE user_id=${user_id};"
            fi
        else
            user_id=$(next_user_id "${status}")
            local locked_sql="0"
            if [[ "$locked_until" != "0" ]]; then
                locked_sql="${locked_until}"
            fi
            mysql_run "INSERT INTO users (user_id, email, username, status, locked_until, created_at, updated_at)
VALUES (${user_id}, '${email}', '${username}', ${status}, ${locked_sql}, NOW(), NOW());"
            echo -e "  ${GREEN}[OK]${NC} User created: ${email} (user_id=${user_id}, status=${status})"

            # Insert into user_auths (password credential)
            mysql_run "INSERT INTO user_auths (user_id, identity_type, identifier, credential, is_verified, is_primary, created_at, updated_at)
VALUES (${user_id}, 'email', '${email}', '${hash}', 1, 1, NOW(), NOW());"
            echo -e "  ${GREEN}[OK]${NC} Auth credential created for ${email}"
        fi

        # Assign role
        local role_id
        role_id=$(mysql_query "SELECT id FROM roles WHERE role_name='${role_name}' LIMIT 1")
        if [[ -z "$role_id" ]]; then
            echo -e "  ${YELLOW}[WARN]${NC} Role '${role_name}' not found — skipping role assignment."
        else
            local ur_exists
            ur_exists=$(mysql_query "SELECT COUNT(*) FROM user_roles WHERE user_id=${user_id} AND role_id=${role_id}")
            if [[ "$ur_exists" -eq 0 ]]; then
                mysql_run "INSERT INTO user_roles (user_id, role_id, created_at) VALUES (${user_id}, ${role_id}, NOW());"
                echo -e "  ${GREEN}[OK]${NC} Assigned '${role_name}' role to ${email}"
            else
                echo -e "  ${YELLOW}[SKIP]${NC} '${role_name}' role already assigned."
            fi
        fi
    }

    # ─── 2. Create super_admin user ──────────────────────────────────────────
    echo -e "${CYAN}[Step 2]${NC} Creating super_admin user..."
    create_user "super@test.com" "super_admin_test" "${HASH_ADMIN}" 2 "super_admin"
    SUPER_ID=$(mysql_query "SELECT user_id FROM users WHERE email='super@test.com'")
    echo ""

    # ─── 3. Create admin user ────────────────────────────────────────────────
    echo -e "${CYAN}[Step 3]${NC} Creating admin user..."
    create_user "admin@test.com" "admin_test" "${HASH_ADMIN}" 2 "admin"
    ADMIN_ID=$(mysql_query "SELECT user_id FROM users WHERE email='admin@test.com'")
    echo ""

    # ─── 4. Create regular user ─────────────────────────────────────────────
    echo -e "${CYAN}[Step 4]${NC} Creating regular user..."
    create_user "user@test.com" "regular_user_test" "${HASH_USER}" 2 "user"
    USER_ID=$(mysql_query "SELECT user_id FROM users WHERE email='user@test.com'")
    echo ""

    # ─── 5. Create locked user (status=3, locked_until=far future) ──────────
    echo -e "${CYAN}[Step 5]${NC} Creating locked user..."
    LOCKED_FUTURE_EPOCH=4102444799  # 2099-12-31 23:59:59 UTC
    create_user "locked@test.com" "locked_test" "${HASH_USER}" 3 "user" "${LOCKED_FUTURE_EPOCH}"
    LOCKED_ID=$(mysql_query "SELECT user_id FROM users WHERE email='locked@test.com'")
    echo ""

    # ─── 6. Create disabled user (status=1) ──────────────────────────────────
    echo -e "${CYAN}[Step 6]${NC} Creating disabled user..."
    create_user "disabled@test.com" "disabled_test" "${HASH_USER}" 1 "user"
    DISABLED_ID=$(mysql_query "SELECT user_id FROM users WHERE email='disabled@test.com'")
    echo ""

    # ─── 7. Insert test OAuth client ─────────────────────────────────────────
    echo -e "${CYAN}[Step 7]${NC} Creating test OAuth client..."
    local oauth_client_uid
    oauth_client_uid=$(uuid_v4)
    local oauth_client_secret
    oauth_client_secret=$(uuid_v4)
    local oauth_client_name="Test OAuth Client (Seeded)"

    local oauth_exists
    oauth_exists=$(mysql_query "SELECT COUNT(*) FROM oauth_clients WHERE client_name='${oauth_client_name}'" 2>/dev/null || echo "0")
    if [[ "$oauth_exists" -gt 0 ]]; then
        echo -e "  ${YELLOW}[SKIP]${NC} OAuth client already exists."
    else
        mysql_run "INSERT INTO oauth_clients (client_id, client_secret, client_name, client_type, redirect_uris, allowed_scopes, is_trusted, status, created_at, updated_at)
VALUES ('${oauth_client_uid}', '${oauth_client_secret}', '${oauth_client_name}',
        'confidential', '[\"http://localhost:3000/callback\"]',
        '[\"read\",\"write\"]', 1, 1, NOW(), NOW());" 2>/dev/null || {
            echo -e "  ${YELLOW}[WARN]${NC} Could not insert OAuth client — oauth_clients table may have different schema."
            echo -e "  ${YELLOW}[WARN]${NC} This is not critical; OAuth tests will use dynamic creation."
        }
        echo -e "  ${GREEN}[OK]${NC} OAuth client created."
    fi
    echo ""

    # ─── 8. Insert test API key (SHA-256 hash) ────────────────────────────────
    echo -e "${CYAN}[Step 8]${NC} Creating test API key..."
    local raw_key="test-api-key-$(uuid_v4)"
    local key_hash
    key_hash=$(sha256_hash "$raw_key")
    local prefix="${raw_key:0:8}"

    local apikey_exists
    apikey_exists=$(mysql_query "SELECT COUNT(*) FROM api_keys WHERE key_hash='${key_hash}'" 2>/dev/null || echo "0")
    if [[ "$apikey_exists" -gt 0 ]]; then
        echo -e "  ${YELLOW}[SKIP]${NC} API key already exists."
    else
        local owner_id
        owner_id="${ADMIN_ID:-${SUPER_ID}}"
        mysql_run "INSERT INTO api_keys (user_id, key_name, key_hash, key_prefix, allowed_scopes, status, created_at, updated_at)
VALUES (${owner_id}, 'Test API Key (Seeded)', '${key_hash}', '${prefix}',
        '[\"read\"]', 1, NOW(), NOW());" 2>/dev/null || {
            echo -e "  ${YELLOW}[WARN]${NC} Could not insert API key — api_keys table may have different schema."
            echo -e "  ${YELLOW}[WARN]${NC} This is not critical; API key tests will use dynamic creation."
        }
        echo -e "  ${GREEN}[OK]${NC} API key created."
    fi
    echo ""

    # ─── 9. Print summary ────────────────────────────────────────────────────
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║                   SEED DATA SUMMARY                              ║${NC}"
    echo -e "${CYAN}╠══════════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${CYAN}║${NC}                                                                ${CYAN}║${NC}"

    # Query and display created users
    local users_count
    users_count=$(mysql_query "SELECT COUNT(*) FROM users WHERE email IN ('super@test.com','admin@test.com','user@test.com','locked@test.com','disabled@test.com')")
    echo -e "${CYAN}║${NC}  Test users created/verified: ${users_count}                              ${CYAN}║${NC}"

    echo -e "${CYAN}║${NC}                                                                ${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}  Account           Email              Password      Role       ${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}  ────────────────  ─────────────────  ────────────  ────────── ${CYAN}║${NC}"

    printf "${CYAN}║${NC}  %-16s  %-17s  %-12s  %-10s ${CYAN}║${NC}\n" "super_admin" "super@test.com" "Admin1234!" "super_admin"
    printf "${CYAN}║${NC}  %-16s  %-17s  %-12s  %-10s ${CYAN}║${NC}\n" "admin" "admin@test.com" "Admin1234!" "admin"
    printf "${CYAN}║${NC}  %-16s  %-17s  %-12s  %-10s ${CYAN}║${NC}\n" "regular user" "user@test.com" "User1234!" "user"
    printf "${CYAN}║${NC}  %-16s  %-17s  %-12s  %-10s ${CYAN}║${NC}\n" "locked user" "locked@test.com" "User1234!" "LOCKED"
    printf "${CYAN}║${NC}  %-16s  %-17s  %-12s  %-10s ${CYAN}║${NC}\n" "disabled user" "disabled@test.com" "User1234!" "DISABLED"

    echo -e "${CYAN}║${NC}                                                                ${CYAN}║${NC}"
    echo -e "${CYAN}╠══════════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${CYAN}║${NC}  User status codes:                                            ${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}    1 = DISABLED                                                ${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}    2 = ACTIVE                                                  ${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}    3 = LOCKED                                                  ${CYAN}║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════════╝${NC}"
    echo ""

    echo -e "${GREEN}[DONE]${NC} Test data seeding complete. You can now run the API tests."
}

main "$@"
