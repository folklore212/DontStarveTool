---
name: 本地开发环境搭建
description: 本地开发环境搭建
status: approved
owner: @TechLead
created: 2026-05-22
last_updated: 2026-05-22
reviewers: []
review_cycle: release
tags: [setup, development, java, nodejs]
---

# 本地开发环境搭建

本文档详细说明如何在本地搭建完整的 DST 管理平台开发环境。

## 前置条件

### 硬件要求

- **CPU**: 4 核以上（推荐 8 核）
- **内存**: 16GB 以上（推荐 32GB）
- **磁盘**: 50GB 可用空间（SSD 推荐）
- **操作系统**: Linux / macOS / Windows (WSL2)

### 软件要求

| 软件 | 版本 | 必须 | 用途 |
|------|------|------|------|
| Java | 21+ | ✅ | 后端开发 |
| Node.js | 20+ | ✅ | 前端开发 |
| Maven | 3.9+ | ✅ | 后端构建 |
| Git | 2.40+ | ✅ | 版本控制 |
| Docker | 24+ | ⚠️ | 容器化服务（可选，推荐） |
| IDE | 最新版 | ✅ | 开发工具 |

---

## 步骤 1: 安装 Java 21

### Linux (Ubuntu/Debian)

```bash
# 安装 Temurin JDK 21
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor | sudo tee /etc/apt/trusted.gpg.d/adoptium.gpg > /dev/null
echo "deb https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update
sudo apt install temurin-21-jdk

# 验证安装
java -version
# 输出：openjdk version "21.x.x"
```

### macOS

```bash
# 使用 Homebrew 安装
brew install openjdk@21

# 配置 JAVA_HOME（添加到 ~/.zshrc 或 ~/.bashrc）
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH=$JAVA_HOME/bin:$PATH

# 验证安装
java -version
```

### Windows (WSL2)

```bash
# 在 WSL2 中执行 Linux 安装步骤
# 或者下载 Windows 安装包：https://adoptium.net/
```

### 配置 Java 环境变量

```bash
# 添加到 ~/.bashrc 或 ~/.zshrc
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64  # 根据实际路径调整
export PATH=$JAVA_HOME/bin:$PATH

# 使配置生效
source ~/.bashrc
```

---

## 步骤 2: 安装 Node.js 20

### 使用 nvm（推荐）

```bash
# 安装 nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash

# 重启终端或执行
source ~/.bashrc

# 安装 Node.js 20
nvm install 20
nvm use 20
nvm alias default 20

# 验证安装
node -v  # v20.x.x
npm -v   # 10.x.x
```

### 配置 npm 镜像（中国大陆）

```bash
# 使用淘宝镜像
npm config set registry https://registry.npmmirror.com

# 或者临时使用
npm install --registry=https://registry.npmmirror.com
```

---

## 步骤 3: 安装 Maven

```bash
# Linux
sudo apt install maven

# macOS
brew install maven

# 验证安装
mvn -version
```

### 配置 Maven 镜像（中国大陆）

编辑 `~/.m2/settings.xml`:

```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <name>Aliyun Maven</name>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
```

---

## 步骤 4: 安装数据库和缓存

### 方案 A: 使用 Docker（推荐）

```bash
# 启动 MySQL 和 Redis
cd deploy
docker compose up -d mysql redis

# 验证服务
docker compose ps
```

**连接信息**:
```yaml
MySQL:
  host: localhost
  port: 3306
  username: root
  password: change_me  # 来自 .env 文件
  database: auth_system

Redis:
  host: localhost
  port: 6379
  password: change_me_too
```

### 方案 B: 本地安装

#### MySQL 8.0

```bash
# Ubuntu/Debian
sudo apt install mysql-server-8.0

# macOS
brew install mysql@8.0

# 初始化数据库
mysql -u root -p
CREATE DATABASE auth_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE dst_templates CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE dst_servers CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### Redis 7

```bash
# Ubuntu/Debian
sudo apt install redis-server

# macOS
brew install redis

# 启动 Redis
redis-server

# 测试连接
redis-cli ping  # 应返回 PONG
```

---

## 步骤 5: 克隆项目

```bash
# 克隆项目
git clone https://github.com/your-org/DontStarveTool.git
cd DontStarveTool

# 安装 Git LFS（如果有大文件）
git lfs install
```

---

## 步骤 6: 构建后端

```bash
cd src/backend/general-web-backend

# 使用 Maven Wrapper（推荐）
./mvnw clean install -DskipTests

# 或者使用系统 Maven
mvn clean install -DskipTests

# 验证构建
ls -la */target/*.jar
```

### 常见问题

**Q: 构建速度慢**

```bash
# 使用 Maven 缓存和并行构建
./mvnw clean install -DskipTests -T 1C -B
```

**Q: 依赖下载失败**

```bash
# 清理 Maven 缓存
rm -rf ~/.m2/repository/com/iccuu

# 重新构建
./mvnw clean install -U -DskipTests
```

---

## 步骤 7: 构建前端

```bash
# Admin 前端
cd ../../frontend/admin
npm install
npm run build

# Customer 前端
cd ../customer
npm install
npm run build
```

### 常见问题

**Q: npm install 失败**

```bash
# 清理 npm 缓存
npm cache clean --force

# 删除 node_modules 重新安装
rm -rf node_modules package-lock.json
npm install

# 使用国内镜像
npm install --registry=https://registry.npmmirror.com
```

---

## 步骤 8: 配置 IDE

### IntelliJ IDEA（推荐）

1. **打开项目**: File → Open → 选择 `src/backend/general-web-backend`

2. **配置 JDK**: File → Project Structure → SDKs → 添加 JDK 21

3. **配置 Maven**: File → Settings → Build → Maven
   - Maven home directory: 使用 Bundled (Maven Wrapper)
   - User settings file: `~/.m2/settings.xml`

4. **安装插件**:
   - Lombok
   - MyBatisX
   - Spring Boot Initializr
   - GitToolBox

5. **运行配置**: Run → Edit Configurations
   - 添加 Spring Boot 配置
   - Main class: `com.iccuu.general_web_backend.CorePlatformApplication`
   - Environment variables: 参考 `.env.example`

### VS Code（前端开发）

1. **安装插件**:
   - ESLint
   - Prettier
   - Volar (Vue) 或 ES7+ React/Redux/React-Native hooks
   - GitLens

2. **配置设置**: `.vscode/settings.json`
```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.tabSize": 2,
  "typescript.tsdk": "node_modules/typescript/lib"
}
```

---

## 步骤 9: 运行开发服务

### 后端服务

```bash
# 启动核心平台服务
cd src/backend/general-web-backend/core-platform
./mvnw spring-boot:run

# 启动模板服务
cd ../template-service
./mvnw spring-boot:run

# 启动服务器服务
cd ../server-service
./mvnw spring-boot:run
```

### 前端服务

```bash
# Admin 前端（开发模式）
cd src/frontend/admin
npm run dev

# Customer 前端
cd src/frontend/customer
npm run dev
```

### 访问服务

| 服务 | URL | 说明 |
|------|-----|------|
| Customer 前端 | http://localhost:5173 | Vite 开发服务器 |
| Admin 前端 | http://localhost:3000 | Vite 开发服务器 |
| Core Platform API | http://localhost:8081 | 核心 API |
| Template Service | http://localhost:8082 | 模板服务 |
| Server Service | http://localhost:8083 | 服务器服务 |
| Knife4j API 文档 | http://localhost:8081/doc.html | API 文档（开发环境） |

---

## 步骤 10: 初始化数据库

```bash
# 方式 1: 自动初始化（Flyway）
# 后端服务启动时会自动执行 Flyway 迁移脚本

# 方式 2: 手动初始化
cd deploy/config/mysql
mysql -u root -p < init.sql
```

---

## 验证安装

### 后端验证

```bash
# 检查服务健康状态
curl http://localhost:8081/actuator/health
# 返回：{"status":"UP"}

# 测试 API
curl http://localhost:8081/api/v1/auth/login
```

### 前端验证

```bash
# 检查前端页面
curl http://localhost:5173
# 应返回 HTML 内容
```

### 数据库验证

```bash
# 连接 MySQL
mysql -u root -p

# 检查表
USE auth_system;
SHOW TABLES;
# 应看到 17+ 张表
```

---

## 下一步

- 🏃 [5 分钟快速体验](001-quickstart.md) - Docker 快速启动
- 📖 [架构概览](003-architecture-overview.md) - 了解系统架构
- 🛠️ [编码规范](../dev-guide/guides/003-coding-standards.md) - 开发规范
- 🧪 [测试指南](../dev-guide/guides/005-testing.md) - 编写测试

---

## 故障排查

### 常见问题汇总

| 问题 | 可能原因 | 解决方案 |
|------|----------|----------|
| Java 版本不对 | 安装了多个 JDK | `update-alternatives --config java` |
| 端口冲突 | 服务已运行 | `lsof -i :8081` 查找并停止 |
| 数据库连接失败 | MySQL 未启动 | `docker compose ps` 检查 |
| 前端构建失败 | Node 版本不对 | `nvm use 20` |
| Maven 下载慢 | 网络问题 | 配置阿里云镜像 |

### 日志查看

```bash
# 后端日志
tail -f src/backend/general-web-backend/core-platform/logs/app.log

# Docker 日志
docker compose logs -f core-platform

# 前端日志
# 在开发终端查看 npm run dev 输出
```

---

**参考链接**:
- [Java 21 官方下载](https://adoptium.net/)
- [Node.js 官方下载](https://nodejs.org/)
- [Maven 官方下载](https://maven.apache.org/)
- [Docker 官方文档](https://docs.docker.com/)

**最后更新**: 2026-05-22
