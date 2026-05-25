---
name: 应用配置参考
description: 应用配置参考
status: approved
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [configuration, properties, reference]
---

# 应用配置参考

本文档提供所有配置项的详细说明和最佳实践。

---

## 配置文件位置

```
src/backend/general-web-backend/
├── core-platform/src/main/resources/
│   ├── application.yml           # 主配置文件
│   ├── application-dev.yml       # 开发环境
│   ├── application-prod.yml      # 生产环境
│   └── application-test.yml      # 测试环境
├── template-service/src/main/resources/
│   └── application.yml
└── server-service/src/main/resources/
    └── application.yml
```

---

## 核心配置

### 1. 服务器配置

```yaml
# application.yml
server:
  port: 8081                      # 服务端口
  servlet:
    context-path: /               # 上下文路径
  compression:
    enabled: true                 # 启用 Gzip 压缩
    min-response-size: 1024       # 最小压缩大小
    mime-types: text/html,text/xml,text/plain,application/json
  
  # Tomcat 配置
  tomcat:
    max-threads: 200              # 最大线程数
    min-spare-threads: 10         # 最小空闲线程
    connection-timeout: 20000     # 连接超时 (ms)
    keep-alive-timeout: 60000     # 保持连接超时
    accept-count: 100             # 等待队列长度
```

**最佳实践**:
- 开发环境：`max-threads: 50`
- 生产环境：`max-threads: 200-500`
- 高并发场景：增加 `accept-count`

---

### 2. 数据源配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:auth_system}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:change_me}
    driver-class-name: com.mysql.cj.jdbc.Driver
    
    # HikariCP 连接池配置
    hikari:
      pool-name: HikariPool-1
      minimum-idle: 5             # 最小空闲连接
      maximum-pool-size: 20       # 最大连接数
      idle-timeout: 300000        # 空闲超时 (5 分钟)
      connection-timeout: 30000   # 连接超时 (30 秒)
      max-lifetime: 1800000       # 最大生命周期 (30 分钟)
      connection-test-query: SELECT 1
```

**环境差异**:

| 配置项 | 开发环境 | 生产环境 |
|--------|----------|----------|
| minimum-idle | 2 | 10 |
| maximum-pool-size | 10 | 50 |
| connection-timeout | 30000 | 10000 |

---

### 3. JPA/MyBatis 配置

```yaml
spring:
  # MyBatis-Plus 配置
  mybatis-plus:
    mapper-locations: classpath*:/mapper/**/*.xml
    type-aliases-package: com.iccuu.general_web_backend.**.entity
    configuration:
      map-underscore-to-camel-case: true    # 驼峰转换
      cache-enabled: true                   # 启用缓存
      lazy-loading-enabled: false           # 延迟加载
      aggressive-lazy-loading: false        # 积极延迟加载
      log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
    
    # 分页插件
    global-config:
      db-config:
        id-type: auto                       # 主键策略
        logic-delete-field: deleted         # 逻辑删除字段
        logic-delete-value: 1
        logic-not-delete-value: 0
```

---

### 4. Redis 配置

```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:change_me_too}
    database: 0
    timeout: 5000ms
    
    # Lettuce 连接池
    lettuce:
      pool:
        max-active: 8           # 最大连接数
        max-idle: 8             # 最大空闲连接
        min-idle: 0             # 最小空闲连接
        max-wait: -1ms          # 阻塞等待时间
```

**Redis Key 命名规范**:
```yaml
# 推荐格式
user:{id}                     # 用户对象
user:{id}:session             # 用户会话
server:{id}:status            # 服务器状态
template:{id}:cache           # 模板缓存
auth:token:{token}            # JWT 令牌黑名单
```

---

### 5. JWT 配置

```yaml
jwt:
  secret: ${JWT_SECRET:your-secret-key-change-in-production}
  header: Authorization
  prefix: Bearer 
  access-token-ttl: 900           # 访问令牌有效期 (15 分钟)
  refresh-token-ttl: 604800       # 刷新令牌有效期 (7 天)
  
  # 黑名单配置
  blacklist:
    enabled: true
    redis-key-prefix: auth:token:
```

**安全建议**:
- 生产环境必须修改 `jwt.secret`
- 使用强密码（至少 32 字符）
- 定期轮换密钥

---

### 6. 安全配置

```yaml
spring:
  security:
    # OAuth2 配置
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
    
    # MFA 配置
    mfa:
      enabled: true
      issuer: DST Management Platform
      totp:
        digits: 6
        period: 30
```

---

### 7. 日志配置

```yaml
logging:
  level:
    root: INFO
    com.iccuu.general_web_backend: DEBUG
    org.springframework.web: DEBUG
    org.mybatis: DEBUG
    org.hibernate: WARN
  
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  
  file:
    name: logs/app.log
    max-size: 10MB
    max-history: 30
    total-size-cap: 1GB
```

**环境差异**:

| 环境 | Root Level | 应用 Level |
|------|------------|------------|
| 开发 | INFO | DEBUG |
| 测试 | INFO | DEBUG |
| 生产 | WARN | INFO |

---

### 8. Actuator 配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,loggers
      base-path: /actuator
  
  endpoint:
    health:
      show-details: when_authorized
      probes:
        enabled: true
    
    info:
      enabled: true
    
    loggers:
      enabled: true
  
  info:
    env:
      enabled: true
    java:
      enabled: true
    os:
      enabled: true
```

---

## 环境特定配置

### 开发环境 (application-dev.yml)

```yaml
spring:
  config:
    activate:
      on-profile: dev
  
  # H2 内存数据库（可选）
  datasource:
    url: jdbc:h2:mem:testdb
  
  # 关闭 Flyway 自动迁移（可选）
  flyway:
    enabled: false
  
  # 启用 DevTools
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true

# 启用 Swagger/Knife4j
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
```

### 生产环境 (application-prod.yml)

```yaml
spring:
  config:
    activate:
      on-profile: prod
  
  # 关闭 DevTools
  devtools:
    restart:
      enabled: false
  
  # 启用 Flyway
  flyway:
    enabled: true
    validate-on-migrate: true
  
  # 连接池优化
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10

# 关闭 Swagger
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false

# 安全加固
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_PASSWORD}
```

### 测试环境 (application-test.yml)

```yaml
spring:
  config:
    activate:
      on-profile: test
  
  # H2 测试数据库
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
  
  # 禁用 Flyway
  flyway:
    enabled: false
  
  # 禁用 Redis
  redis:
    host: localhost
    port: 0

# 测试配置
testing:
  enabled: true
  mock-external-services: true
```

---

## 自定义配置

### Node Agent 配置

```yaml
node:
  gateway:
    url: ws://localhost:8090/node
    token: ${NODE_TOKEN:change_me}
    reconnect:
      enabled: true
      max-attempts: 10
      interval: 5000
    heartbeat:
      enabled: true
      interval: 30000
```

### Workshop 缓存配置

```yaml
workshop:
  cache:
    enabled: true
    redis-key-prefix: workshop:
    ttl: 86400                  # 24 小时
    max-size: 1000              # 最大缓存数量
  
  download:
    timeout: 300                # 下载超时 (秒)
    max-retry: 3
    concurrent-limit: 5         # 并发下载限制
```

### 定时任务配置

```yaml
tasks:
  scheduler:
    enabled: true
    thread-pool-size: 5
  
  # 清理过期令牌
  cleanup-tokens:
    cron: "0 0 * * * *"         # 每小时执行
    enabled: true
  
  # 同步 Steam 数据
  sync-steam:
    cron: "0 0 */6 * * *"       # 每 6 小时执行
    enabled: true
  
  # 备份数据库
  backup-db:
    cron: "0 2 * * *"           # 每天凌晨 2 点
    enabled: false              # 生产环境启用
```

---

## 配置优先级

Spring Boot 配置加载优先级（从高到低）：

1. 命令行参数
2. SPRING_APPLICATION_JSON 中的属性
3. ServletConfig 初始化参数
4. Context 初始化参数
5. JNDI 属性
6. Java 系统属性（System.getProperties()）
7. 操作系统环境变量
8. RandomValuePropertySource
9. jar 包外部的 application-{profile}.yml
10. jar 包内部的 application-{profile}.yml
11. jar 包外部的 application.yml
12. jar 包内部的 application.yml
13. @PropertySource 注解
14. 默认属性

---

## 配置验证

### 使用 Spring Boot ConfigData 验证

```java
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Validated
public class JwtProperties {
    
    @NotNull
    private String secret;
    
    @Min(60)
    @Max(86400)
    private Integer accessTokenTtl;
    
    // getters and setters
}
```

### 配置测试

```java
@SpringBootTest
@ActiveProfiles("test")
class ConfigurationTest {
    
    @Autowired
    private Environment env;
    
    @Test
    void testDatabaseConfiguration() {
        assertThat(env.getProperty("spring.datasource.url"))
            .contains("jdbc:h2:mem:testdb");
    }
    
    @Test
    void testJwtConfiguration() {
        assertThat(env.getProperty("jwt.access-token-ttl"))
            .isEqualTo("900");
    }
}
```

---

## 下一步

- 📖 [部署指南](../dev-guide/deployment/006-docker-guide.md)
- 📖 [生产环境配置](../dev-guide/deployment/007-production-config.md)
- 📖 [数据库 Schema](../reference/database/001-schema-reference.md)

---

**参考链接**:
- [Spring Boot 配置](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [HikariCP 配置](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [MyBatis-Plus 配置](https://baomidou.com/pages/56bac0/)

**最后更新**: 2026-05-22
