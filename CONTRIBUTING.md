# 贡献指南

感谢您对本项目的关注！本指南将帮助您了解如何参与项目开发。

## 📋 目录

- [开发环境搭建](#开发环境搭建)
- [提交流程](#提交流程)
- [代码规范](#代码规范)
- [测试要求](#测试要求)
- [文档要求](#文档要求)
- [审核流程](#审核流程)
- [社区行为准则](#社区行为准则)

---

## 开发环境搭建

### 基础要求

- **Java**: 21+ (推荐使用 [Temurin](https://adoptium.net/))
- **Node.js**: 20+ (推荐使用 [nvm](https://github.com/nvm-sh/nvm))
- **Maven**: 3.9+
- **Git**: 2.40+
- **Docker**: 24+ (可选，用于容器化开发)

### 快速开始

```bash
# 1. Fork 项目并克隆到本地
git clone https://github.com/YOUR_USERNAME/DontStarveTool.git
cd DontStarveTool

# 2. 后端环境
cd src/backend/general-web-backend
./mvnw clean install -DskipTests

# 3. 前端环境
cd ../../frontend/admin
npm install
cd ../customer
npm install

# 4. 启动开发服务
# 参考文档：doc/dev-guide/setup/001-local-setup.md
```

### 详细文档

- [本地开发环境搭建](doc/dev-guide/setup/001-local-setup.md)
- [Docker 开发环境](doc/dev-guide/setup/002-docker-setup.md)

---

## 提交流程

### 1. 创建功能分支

```bash
# 确保基于最新的 master 分支
git checkout master
git pull origin master

# 创建功能分支（命名规范：feature/描述-issue编号）
git checkout -b feature/add-new-feature-123
```

### 2. 进行开发

- 编写代码
- 编写测试
- 更新文档

### 3. 提交更改

本项目遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范。

```bash
# 提交格式：<type>(<scope>): <description>
git add .
git commit -m "feat(server): add server restart API endpoint"
```

**提交类型**:
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式（不影响代码逻辑）
- `refactor`: 重构
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建/工具/配置

**示例**:
```bash
git commit -m "fix(auth): resolve JWT token expiration issue"
git commit -m "docs(api): update REST API documentation"
git commit -m "refactor(core): simplify user service logic"
```

### 4. 推送到分支

```bash
git push origin feature/add-new-feature-123
```

### 5. 创建 Pull Request

1. 访问 GitHub 项目页面
2. 点击 "Compare & pull request"
3. 填写 PR 描述（使用提供的模板）
4. 选择审核人
5. 提交 PR

---

## 代码规范

### Java 代码规范

- 遵循 [阿里巴巴 Java 开发手册](https://github.com/alibaba/p3c)
- 使用 Checkstyle 进行代码风格检查
- 命名规范：
  - 类名：大驼峰 `UserService`
  - 方法/变量：小驼峰 `getUserById`
  - 常量：全大写 `MAX_RETRY_COUNT`
  - 包名：全小写 `com.iccuu.general_web_backend`

**IDE 配置**:
- [IntelliJ IDEA 配置](tools/idea/JavaCodeStyle.xml)
- [Eclipse 配置](tools/eclipse/java-formatter.xml)

### TypeScript 代码规范

- 遵循 ESLint 配置
- 使用 Prettier 格式化代码
- 命名规范：
  - 组件：大驼峰 `ServerDetail.tsx`
  - 函数/变量：小驼峰 `fetchServerList`
  - 类型：大驼峰 `ServerDTO`
  - 常量：全大写 `API_BASE_URL`

### 代码审查要点

- [ ] 代码是否简洁易读
- [ ] 是否有适当的注释
- [ ] 是否遵循了设计模式
- [ ] 是否有性能问题
- [ ] 是否有安全隐患

---

## 测试要求

### 后端测试

```bash
# 运行所有测试
./mvnw test

# 运行特定模块测试
./mvnw test -pl core-platform

# 生成测试覆盖率报告
./mvnw test jacoco:report
```

**要求**:
- 单元测试覆盖率 > 80%
- 关键业务逻辑必须有测试
- 集成测试必须通过

### 前端测试

```bash
# Admin 前端
cd src/frontend/admin
npm run test

# Customer 前端（包含 Vitest）
cd src/frontend/customer
npm run test
```

**要求**:
- 核心组件必须有测试
- 关键功能必须有 E2E 测试
- 测试必须通过

### 测试类型

- **单元测试**: 测试单个函数/方法
- **集成测试**: 测试模块间交互
- **E2E 测试**: 测试完整用户流程

---

## 文档要求

### 何时需要更新文档

- ✅ 新增功能/接口
- ✅ API 变更
- ✅ 配置项变更
- ✅ 架构调整
- ✅ Bug 修复（影响用户行为）

### 文档类型

1. **代码注释**: 公共方法、复杂逻辑
2. **API 文档**: OpenAPI 注解（Controller 层）
3. **设计文档**: 架构变更需提交 ADR
4. **用户文档**: 功能使用说明

### 文档规范

- 使用项目统一的文档模板
- 添加 Frontmatter 元数据
- 使用中文编写（技术术语可保留英文）
- 提供代码示例和截图（如适用）

**示例**:
```markdown
---
name: 用户认证流程
description: 用户登录和 Token 刷新流程
status: approved
owner: @username
created: 2026-05-22
---

# 用户认证流程

## 概述

本文档说明用户登录和 Token 刷新的完整流程...
```

---

## 审核流程

### PR 审核要求

- **最少审核人数**: 1 人
- **必须审核人**: Tech Lead（对于重要变更）
- **审核通过条件**: 所有 CI 检查通过 + 审核人批准

### 审核检查表

审核人会检查以下内容：

**代码质量**:
- [ ] 代码是否遵循规范
- [ ] 是否有潜在的 Bug
- [ ] 是否有性能问题
- [ ] 是否有安全隐患

**测试**:
- [ ] 是否添加了必要的测试
- [ ] 测试是否充分
- [ ] 所有测试是否通过

**文档**:
- [ ] 是否更新了相关文档
- [ ] 文档是否准确
- [ ] 文档格式是否规范

### 审核反馈

- 审核人会在 PR 中留下评论
- 作者需要根据反馈进行修改
- 修改完成后重新请求审核

### 合并策略

- 使用 **Squash and Merge**（压缩提交）
- 合并前确保 CI 全部通过
- 合并后删除功能分支

---

## 社区行为准则

### 我们的承诺

为了营造一个开放和友好的环境，我们承诺：

- 使用友好和包容的语言
- 尊重不同的观点和经验
- 优雅地接受建设性批评
- 关注对社区最有利的事情
- 对其他社区成员表示同理心

### 不可接受的行为

- 使用性化的语言或图像
- 人身攻击或侮辱性评论
- 公开或私下骚扰
- 未经许可发布他人信息
- 其他不道德或不专业的行为

### 执行

如发现不可接受的行为，请通过以下方式举报：

- Email: [项目邮箱]
- GitHub Issue（私密）

---

## 常见问题

### Q: 我可以认领 Issue 吗？

A: 当然！在 Issue 下留言说明你想认领，维护者会分配给你。

### Q: 我的 PR 多久会被审核？

A: 通常在 2-3 个工作日内。如果超过一周未回复，可以 @ 相关审核人。

### Q: 如何成为维护者？

A: 持续贡献高质量的代码和文档，积极参与社区讨论，现有维护者会邀请你加入。

---

## 致谢

感谢所有为本项目做出贡献的开发者！

<a href="https://github.com/your-org/DontStarveTool/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=your-org/DontStarveTool" />
</a>

---

**最后更新**: 2026-05-22
