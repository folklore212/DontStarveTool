# Docker 容器新增规范

## 目录约定

每个服务一个子目录，完全自包含，可独立启动：

```
deploy/docker/<service>/
├── dockerfile      # 构建镜像（ENV 设置默认值）
├── start.sh        # 独立启动脚本（bash ${VAR:-default} 覆盖默认值）
└── <config files>  # 服务所需配置文件
```

## dockerfile 规范

```dockerfile
FROM <base_image>

# 默认配置（docker compose 可覆盖）
ENV <SERVICE>_PASSWORD=<default_password>
ENV <SERVICE>_PORT=<default_port>

# ... 其他 ENV 默认值 ...

COPY <config> /path/in/container/

EXPOSE <port>
```

## start.sh 规范

```bash
#!/bin/bash
set -e

# 环境变量覆盖默认值（${VAR:-default} 模式）
SERVICE_VAR="${SERVICE_VAR:-default_value}"

docker build -t dst-<service>:v1 .
docker rm -f auth-<service>-container 2>/dev/null || true

docker run -d \
    --name auth-<service>-container \
    --restart unless-stopped \
    -p "${SERVICE_PORT}:<container_port>" \
    -e "SERVICE_VAR=${SERVICE_VAR}" \
    dst-<service>:v1

echo "<Service> starting..."
# 等待就绪
until <health_check_command>; do
    sleep <interval>
done
echo "<Service> is ready."
```

## 注册到 docker compose

在 `docker-compose.yml` 的 `services:` 下新增：

```yaml
  <service>:
    build:
      context: ./<service>
      dockerfile: dockerfile
    image: dst-<service>:v1
    container_name: auth-<service>-container
    restart: unless-stopped
    ports:
      - "${SERVICE_PORT}:<container_port>"
    environment:
      SERVICE_VAR: ${SERVICE_VAR}
    volumes:
      - <service>-data:/path/in/container
    healthcheck:
      test: ["CMD-SHELL", "<health_check_command>"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - auth-net
```

同时在 `volumes:` 下声明数据卷，在 `.env` 中追加可配置项。

## 命名约定

| 项 | 格式 | 示例 |
|----|------|------|
| 目录名 | `<service>` | `mysql`, `redis` |
| 镜像名 | `dst-<service>:v1` | `dst-mysql:v1` |
| 容器名 | `auth-<service>-container` | `auth-mysql-container` |
| 环境变量前缀 | `<SERVICE>_` | `MYSQL_`, `REDIS_` |
| 数据卷 | `<service>-data` | `mysql-data` |

## 就绪检测

每种服务提供不同的就绪检测方式：
- **MySQL**: `mysqladmin ping -h 127.0.0.1 -u root -p"$PASSWORD" --silent`
- **Redis**: `redis-cli -a "$PASSWORD" --no-auth-warning PING | grep -q PONG`
- **PostgreSQL**: `pg_isready -U postgres`
- **通用 TCP**: `timeout 1 bash -c "cat < /dev/null > /dev/tcp/127.0.0.1/$PORT"`

## 注意事项

1. 部分镜像在特定内核需要 `security_opt: seccomp:unconfined`（如 MySQL 8.0 在旧内核）
2. 环境变量命名避免与 Docker/系统内置变量冲突
3. 生产密码不要硬编码，通过 `.env` 或环境变量注入
4. 数据卷统一在 `docker-compose.yml` 中声明，不在 dockerfile 中定义 VOLUME
