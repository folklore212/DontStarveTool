# CLAUDE.md — DST Management Platform

## Build & Test
```bash
cd src/backend/general-web-backend && ./mvnw compile -q
./mvnw test -Dtest='*Test,!*IntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false
cd src/frontend/customer && npm install --legacy-peer-deps && npx tsc --noEmit
cd src/node && go build ./... && go test ./...
```

## Pre-push
```bash
bash deploy/install-hooks.sh  # one-time
```

## Architecture
- 6 services: core-platform (:8081), template-service (:8082), server-service (:8083), steam-cache-service (:8084), node-gateway (:8090), nginx (:80)
- 3 databases: auth_system, dst_templates, dst_servers
- Java 21 + Spring Boot 3.4 + MyBatis-Plus 3.5
- React 18 + TypeScript + Ant Design 5 + Vite
- Go 1.22 + gorilla/websocket

## Test Naming
- `*Test.java` → unit tests (Mockito)
- `*IntegrationTest.java` → integration (Testcontainers)
- Excluded from CI: `!*IntegrationTest`

## Key Docs
- `CONTEXT.md` — domain glossary
- `doc/design/architecture/` — system overview
