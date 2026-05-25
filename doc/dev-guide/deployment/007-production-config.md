---
name: 生产环境配置
description: 生产环境配置
status: draft
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [deployment, production, performance, optimization]
---

# 生产环境配置

本文档介绍生产环境的部署配置、性能优化和安全加固。

---

## 硬件要求

### 小型部署（< 100 用户）

| 组件 | 配置 | 数量 |
|------|------|------|
| CPU | 4 核 | 1 |
| 内存 | 8GB | 1 |
| 磁盘 | 100GB SSD | 1 |
| 带宽 | 10Mbps | 1 |

### 中型部署（100-500 用户）

| 组件 | 配置 | 数量 |
|------|------|------|
| CPU | 8 核 | 2 |
| 内存 | 16GB | 2 |
| 磁盘 | 200GB SSD | 1 |
| 带宽 | 50Mbps | 1 |

### 大型部署（> 500 用户）

| 组件 | 配置 | 数量 |
|------|------|------|
| CPU | 16 核 | 4+ |
| 内存 | 32GB | 4+ |
| 磁盘 | 500GB SSD | 1 |
| 带宽 | 100Mbps+ | 1 |

---

## Docker 生产配置

### docker-compose.prod.yml

```yaml
version: '3.8'

services:
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.prod.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    depends_on:
      - core-platform
      - template-service
      - server-service
    restart: always
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 256M

  core-platform:
    image: your-org/core-platform:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC
    depends_on:
      - mysql
      - redis
    restart: always
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 1536M

  template-service:
    image: your-org/template-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JAVA_OPTS=-Xms384m -Xmx768m
    restart: always
    deploy:
      resources:
        limits:
          cpus: '1.5'
          memory: 1024M

  server-service:
    image: your-org/server-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JAVA_OPTS=-Xms384m -Xmx768m
    restart: always
    deploy:
      resources:
        limits:
          cpus: '1.5'
          memory: 1024M

  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
      - ./mysql/my.prod.cnf:/etc/mysql/my.cnf:ro
    restart: always
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 2048M

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis-data:/data
    restart: always
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 512M

volumes:
  mysql-data:
  redis-data:
```

---

## JVM 优化

### 生产环境 JVM 参数

```bash
# 堆内存配置
-Xms512m                    # 初始堆大小
-Xmx1024m                   # 最大堆大小

# GC 配置（G1 GC）
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+ParallelRefProcEnabled

# 元空间
-XX:MetaspaceSize=128m
-XX:MaxMetaspaceSize=256m

# 日志
-Xlog:gc*:file=/var/log/app/gc.log:time,uptime:filecount=5,filesize=10M

# 错误处理
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/app/heapdump.hprof
-XX:ErrorFile=/var/log/app/hs_err_pid%p.log

# 性能优化
-XX:+UseStringDeduplication
-XX:+UnlockDiagnosticVMOptions
-XX:+LogVMOutput
```

### 不同服务的 JVM 配置

```yaml
# core-platform (高并发)
JAVA_OPTS: >
  -Xms768m -Xmx1536m
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=150
  -XX:ConcGCThreads=2

# template-service (中等负载)
JAVA_OPTS: >
  -Xms384m -Xmx768m
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200

# server-service (IO 密集)
JAVA_OPTS: >
  -Xms512m -Xmx1024m
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -Djava.net.preferIPv4Stack=true
```

---

## 数据库优化

### MySQL 生产配置

```ini
# my.prod.cnf
[mysqld]
# 基础配置
server-id = 1
port = 3306
basedir = /var/lib/mysql
datadir = /var/lib/mysql
socket = /var/run/mysqld/mysqld.sock

# 内存配置
innodb_buffer_pool_size = 1G
innodb_log_file_size = 256M
innodb_log_buffer_size = 16M
query_cache_size = 64M
query_cache_type = 1

# 连接配置
max_connections = 200
max_connect_errors = 1000
wait_timeout = 28800
interactive_timeout = 28800

# InnoDB 配置
innodb_flush_log_at_trx_commit = 1
innodb_flush_method = O_DIRECT
innodb_file_per_table = 1
innodb_autoinc_lock_mode = 2

# 日志配置
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2
log_queries_not_using_indexes = 1

# 字符集
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci
```

### 索引优化

```sql
-- 添加常用查询索引
ALTER TABLE users ADD INDEX idx_email (email);
ALTER TABLE servers ADD INDEX idx_status (status);
ALTER TABLE servers ADD INDEX idx_user_id (user_id);
ALTER TABLE audit_logs ADD INDEX idx_created_date (created_date);

-- 查看慢查询
SELECT * FROM mysql.slow_log;

-- 分析查询
EXPLAIN SELECT * FROM servers WHERE status = 'running';
```

---

## Redis 优化

### 生产配置

```conf
# redis.prod.conf
# 内存管理
maxmemory 512mb
maxmemory-policy allkeys-lru

# 持久化
save 900 1
save 300 10
save 60 10000

# RDB + AOF
appendonly yes
appendfsync everysec

# 网络
bind 127.0.0.1
port 6379
timeout 300
tcp-keepalive 60

# 安全
requirepass your-strong-password
rename-command FLUSHDB ""
rename-command FLUSHALL ""
rename-command DEBUG ""
```

### 缓存策略

```java
@Configuration
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        
        // 使用 JSON 序列化
        Jackson2JsonRedisSerializer serializer = new Jackson2JsonRedisSerializer(Object.class);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        return template;
    }
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new Jackson2JsonRedisSerializer(Object.class)
                )
            );
            
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}
```

---

## Nginx 优化

### 生产配置

```nginx
# nginx.prod.conf
worker_processes auto;
worker_rlimit_nofile 65535;

events {
    worker_connections 4096;
    use epoll;
    multi_accept on;
}

http {
    # 基础优化
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;
    
    # Gzip 压缩
    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml application/json application/javascript;
    
    # 限流
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
    limit_conn_zone $binary_remote_addr zone=conn:10m;
    
    # 上游服务器
    upstream core-platform {
        least_conn;
        server core-platform:8081 max_fails=3 fail_timeout=30s;
    }
    
    upstream template-service {
        least_conn;
        server template-service:8082 max_fails=3 fail_timeout=30s;
    }
    
    upstream server-service {
        least_conn;
        server server-service:8083 max_fails=3 fail_timeout=30s;
    }
    
    server {
        listen 80;
        server_name your-domain.com;
        
        # 安全头
        add_header X-Frame-Options "SAMEORIGIN" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-XSS-Protection "1; mode=block" always;
        
        # 静态文件
        location / {
            root /usr/share/nginx/html/admin;
            try_files $uri $uri/ /index.html;
            expires 1h;
            add_header Cache-Control "public, no-transform";
        }
        
        # API 代理
        location /api/ {
            limit_req zone=api burst=20 nodelay;
            limit_conn conn 10;
            
            proxy_pass http://core-platform;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            
            proxy_connect_timeout 60s;
            proxy_send_timeout 60s;
            proxy_read_timeout 60s;
        }
        
        # WebSocket
        location /node {
            proxy_pass http://node-gateway:8090;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_set_header Host $host;
            proxy_read_timeout 86400;
        }
    }
}
```

---

## 监控配置

### Spring Boot Actuator

```yaml
# application-prod.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when_authorized
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
```

### Prometheus 配置

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'core-platform'
    static_configs:
      - targets: ['core-platform:8081']
    metrics_path: '/actuator/prometheus'
    
  - job_name: 'template-service'
    static_configs:
      - targets: ['template-service:8082']
    metrics_path: '/actuator/prometheus'
    
  - job_name: 'server-service'
    static_configs:
      - targets: ['server-service:8083']
    metrics_path: '/actuator/prometheus'
```

---

## 安全加固

### 1. 网络安全

```bash
# 防火墙配置（Ubuntu）
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 22/tcp  # SSH
sudo ufw enable
```

### 2. 应用安全

```yaml
# application-prod.yml
spring:
  security:
    headers:
      content-security-policy: "default-src 'self'"
      xss-protection: "1; mode=block"
      cache-control: true
      content-type-options: true
      frame-options: "DENY"
      hsts: "max-age=31536000 ; includeSubDomains"
```

### 3. 数据库安全

```sql
-- 创建专用用户
CREATE USER 'app_user'@'%' IDENTIFIED BY 'strong-password';
GRANT SELECT, INSERT, UPDATE, DELETE ON auth_system.* TO 'app_user'@'%';
FLUSH PRIVILEGES;

-- 禁用远程 root 访问
UPDATE mysql.user SET Host='localhost' WHERE User='root';
FLUSH PRIVILEGES;
```

---

## 备份策略

### 数据库备份

```bash
#!/bin/bash
# backup.sh

BACKUP_DIR="/backup/mysql"
DATE=$(date +%Y%m%d_%H%M%S)

# 全量备份
mysqldump -u root -p \
  --single-transaction \
  --routines \
  --triggers \
  auth_system > ${BACKUP_DIR}/auth_system_${DATE}.sql

mysqldump -u root -p \
  --single-transaction \
  --routines \
  --triggers \
  dst_templates > ${BACKUP_DIR}/dst_templates_${DATE}.sql

mysqldump -u root -p \
  --single-transaction \
  --routines \
  --triggers \
  dst_servers > ${BACKUP_DIR}/dst_servers_${DATE}.sql

# 压缩
gzip ${BACKUP_DIR}/*.sql

# 清理 7 天前的备份
find ${BACKUP_DIR} -name "*.sql.gz" -mtime +7 -delete
```

### 定时任务

```bash
# crontab -e
# 每天凌晨 2 点备份
0 2 * * * /opt/scripts/backup.sh

# 每周日凌晨 3 点清理日志
0 3 * * 0 find /var/log -name "*.log" -mtime +30 -delete
```

---

## 下一步

- 📖 [故障排查](../operations/008-troubleshooting.md)
- 📖 [监控告警](../../user-guide/features/004-dashboard.md)
- 📖 [备份恢复](../../user-guide/features/006-file-manager.md)

---

**参考链接**:
- [Spring Boot 生产准备](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready.html)
- [MySQL 性能优化](https://dev.mysql.com/doc/refman/8.0/en/optimization.html)
- [Nginx 最佳实践](https://www.nginx.com/resources/admin-guide/nginx-beginner-guide/)

**最后更新**: 2026-05-22
