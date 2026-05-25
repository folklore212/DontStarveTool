---
name: 本地开发环境搭建
description: 本地开发环境搭建
status: approved
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [setup, development, ide, database]
---

# 本地开发环境搭建

> 本文档是 [Getting Started - 002](../../getting-started/002-local-setup.md) 的详细版本，包含 IDE 配置、数据库初始化、调试配置等详细内容。

## 前置条件

请参考 [002-local-setup.md](../../getting-started/002-local-setup.md) 完成基础环境安装。

---

## IDE 配置详解

### IntelliJ IDEA 配置

#### 1. 导入项目

```
File → Open → 选择 src/backend/general-web-backend
```

#### 2. 配置 JDK

```
File → Project Structure → SDKs → Add JDK → 选择 JDK 21 安装路径
```

**验证配置**:
```bash
# 在 IDEA 终端执行
echo $JAVA_HOME
java -version
```

#### 3. 配置 Maven

```
File → Settings → Build, Execution, Deployment → Build Tools → Maven
```

**关键配置**:
- **Maven home directory**: `/usr/share/maven` (或使用 Bundled)
- **User settings file**: `~/.m2/settings.xml`
- **Local repository**: `~/.m2/repository`

#### 4. 配置 Lombok

```
File → Settings → Plugins → 搜索 Lombok → Enable
```

**启用注解处理**:
```
File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors
✓ Enable annotation processing
```

#### 5. 代码风格配置

导入项目根目录的 `.idea/codeStyles/` 配置：

```
File → Settings → Editor → Code Style → Java → 
  Scheme: Project → Apply
```

#### 6. 数据库连接配置

安装 **Database Navigator** 插件：

```
File → Settings → Plugins → 搜索 Database Navigator → Install
```

**添加 MySQL 连接**:
```
View → Tool Windows → Database → + → Data Source → MySQL
- Host: localhost
- Port: 3306
- User: root
- Password: change_me
- Database: auth_system
```

#### 7. 运行配置

**启动 Core Platform**:
```
Run → Edit Configurations → + → Spring Boot
- Name: core-platform
- Main class: com.iccuu.general_web_backend.core.CorePlatformApplication
- Environment variables: SPRING_PROFILES_ACTIVE=dev
```

**启动多个服务**:
```
Run → Edit Configurations → + → Compound
- Name: All Services
- Add: core-platform, template-service, server-service
```

---

## 数据库初始化

### Flyway 自动迁移

后端服务启动时会自动执行 Flyway 迁移：

```
src/backend/general-web-backend/
├── core-platform/src/main/resources/db/migration/
│   ├── V1__init_schema.sql
│   ├── V2__seed_data.sql
│   └── ...
```

**验证迁移**:
```bash
# 连接 MySQL
mysql -u root -p

# 检查迁移记录
USE auth_system;
SELECT * FROM flyway_schema_history;
```

### 手动初始化（可选）

如果自动迁移失败，可以手动执行：

```bash
cd deploy/docker/mysql
mysql -u root -p < init.sql
```

---

## 调试配置

### 远程调试配置

**1. 修改启动参数**:

编辑 `application-dev.yml`:
```yaml
debug:
  port: 5005
  suspend: false
```

**2. IDEA 配置**:

```
Run → Edit Configurations → + → Remote JVM Debug
- Name: Debug Core Platform
- Host: localhost
- Port: 5005
```

**3. 启动调试**:

```bash
# 使用 debug 模式启动
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

### 断点调试技巧

**常用断点位置**:
1. Controller 入口方法
2. Service 层业务逻辑
3. Mapper 层 SQL 执行

**条件断点**:
```java
// 右键断点 → Condition
userId != null && userId > 100
```

**异常断点**:
```
Run → View Breakpoints → Java Exception Breakpoints
+ 添加：com.iccuu.general_web_backend.exception.BusinessException
```

---

## 环境变量配置

### 开发环境变量

创建 `.env.development`:

```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_USERNAME=root
DB_PASSWORD=change_me
DB_NAME=auth_system

# Redis 配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=change_me_too

# JWT 配置
JWT_SECRET=your-development-secret-key
JWT_ACCESS_TTL=900
JWT_REFRESH_TTL=604800

# 日志配置
LOG_LEVEL=DEBUG
LOG_FILE=logs/app.log
```

### 加载环境变量

**方式 1: IDEA 运行配置**
```
Run → Edit Configurations → Environment variables
→ 选择 .env.development 文件
```

**方式 2: 命令行**
```bash
export $(cat .env.development | xargs)
./mvnw spring-boot:run
```

---

## 常用开发命令

### 后端命令

```bash
# 清理构建
./mvnw clean

# 编译项目
./mvnw compile

# 运行测试
./mvnw test

# 打包
./mvnw package -DskipTests

# 运行服务
./mvnw spring-boot:run -pl core-platform

# 查看依赖树
./mvnw dependency:tree

# 检查代码风格
./mvnw checkstyle:check

# 生成测试覆盖率报告
./mvnw test jacoco:report
```

### 前端命令

```bash
# 安装依赖
npm install

# 开发模式
npm run dev

# 构建生产版本
npm run build

# 运行测试
npm run test

# 代码检查
npm run lint

# 格式化代码
npm run format
```

---

## Git 配置

### Git Hooks

项目使用 pre-commit hook 进行代码检查：

```bash
# 安装 hooks
git config core.hooksPath .githooks

# 测试 hook
git commit -m "test: test commit"
```

### 提交规范

遵循 Conventional Commits:

```bash
# 格式：<type>(<scope>): <description>
git commit -m "feat(server): add server restart API"
git commit -m "fix(auth): resolve JWT expiration issue"
git commit -m "docs(api): update REST API documentation"
```

**Type 说明**:
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式
- `refactor`: 重构
- `test`: 测试
- `chore`: 构建/工具

---

## 常见问题

### Q1: IDEA 无法识别 Maven 项目

**解决方案**:
```bash
# 重新导入 Maven 项目
File → Invalidate Caches / Restart
# 或者
rm -rf .idea *.iml
# 重新打开项目
```

### Q2: 数据库连接超时

**检查清单**:
```bash
# 1. MySQL 是否运行
docker compose ps mysql

# 2. 端口是否被占用
lsof -i :3306

# 3. 防火墙是否阻止
sudo ufw status
```

### Q3: Lombok 注解不生效

**解决方案**:
```
File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors
✓ Enable annotation processing
```

### Q4: 前端热更新不工作

**解决方案**:
```bash
# 删除 node_modules 重新安装
rm -rf node_modules package-lock.json
npm install
npm run dev
```

---

## 下一步

- 📖 [Docker 开发环境](002-docker-setup.md) - 容器化开发
- 📖 [编码规范](../guides/003-coding-standards.md) - Java/TS 规范
- 📖 [调试指南](../guides/004-debugging.md) - 调试技巧
- 📖 [测试指南](../guides/005-testing.md) - 单元测试/集成测试

---

**参考链接**:
- [IntelliJ IDEA 官方文档](https://www.jetbrains.com/idea/documentation/)
- [Spring Boot 开发指南](https://spring.io/projects/spring-boot)
- [Maven 官方文档](https://maven.apache.org/guides/)

**最后更新**: 2026-05-22
