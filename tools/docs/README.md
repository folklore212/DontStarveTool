# 文档工具集

本目录包含用于文档维护和管理的工具脚本。

---

## 📦 工具列表

### 1. validate-docs.sh - 文档验证

**用途**: 检查文档的完整性和规范性

**功能**:
- ✅ 检查目录结构是否完整
- ✅ 检查 Frontmatter 是否完整（name, description, status, owner）
- ✅ 检查内部链接是否有效
- ✅ 统计文档数量和状态

**使用方法**:
```bash
cd /media/vdc/WorkSpace/Personal/DontStarveTool
./tools/docs/validate-docs.sh
```

**输出示例**:
```
🔍 开始验证文档...

📁 检查目录结构...
✓ doc/getting-started
✓ doc/dev-guide/setup
✓ doc/dev-guide/guides
...

📄 检查文档文件...
✓ doc/getting-started/001-quickstart.md
✓ doc/getting-started/002-local-setup.md
...

🔗 检查链接...

📊 验证结果
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
总文档数：   36
有效文档：   36
错误数：     0
警告数：     0
 broken links: 0
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ 文档验证通过！
```

---

### 2. generate-index.sh - 索引生成

**用途**: 自动生成 doc/README.md 索引文件

**功能**:
- 📝 扫描所有文档目录
- 📝 提取 Frontmatter 信息（name, description）
- 📝 生成 Markdown 表格
- 📝 添加统计信息

**使用方法**:
```bash
cd /media/vdc/WorkSpace/Personal/DontStarveTool
./tools/docs/generate-index.sh
```

**输出示例**:
```
📝 生成文档索引...
✅ 文档索引生成完成：doc/README.md
📊 共收录 36 篇文档
```

**建议**: 每次添加新文档后运行此脚本。

---

### 3. check-broken-links.sh - 链接检查

**用途**: 检查 Markdown 文档中的死链

**功能**:
- 🔗 提取所有 Markdown 链接
- 🔗 检查内部链接是否有效
- 🔗 跳过外部链接和锚点链接
- 🔗 输出详细的死链报告

**使用方法**:
```bash
cd /media/vdc/WorkSpace/Personal/DontStarveTool
./tools/docs/check-broken-links.sh
```

**输出示例**:
```
🔗 检查文档链接...

✓ doc/getting-started/001-quickstart.md
✓ doc/getting-started/002-local-setup.md
✗ doc/user-guide/features/001-server-mgmt.md
    └─ broken: ../../dev-guide/missing.md
       check_path: doc/user-guide/dev-guide/missing.md
...

📊 链接检查统计
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
检查文件数： 36
总链接数：   250
外部链接：   50
内部链接：   200
死链数量：   2
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📋 死链详情:
...
✗ 发现 2 个死链，请修复
```

---

## 🔧 组合使用

### 文档发布前检查

```bash
# 1. 验证文档完整性
./tools/docs/validate-docs.sh

# 2. 检查链接
./tools/docs/check-broken-links.sh

# 3. 重新生成索引
./tools/docs/generate-index.sh

# 4. 提交更改
git add doc/
git commit -m "docs: update documentation"
git push
```

### CI/CD 集成

这些脚本可以集成到 GitHub Actions 中：

```yaml
- name: Validate Documentation
  run: ./tools/docs/validate-docs.sh

- name: Check Broken Links
  run: ./tools/docs/check-broken-links.sh
```

---

## 📝 最佳实践

### 1. 定期运行验证脚本

建议每周运行一次验证脚本，确保文档质量：

```bash
# 添加到 crontab
0 9 * * 1 /path/to/validate-docs.sh >> /var/log/docs-validation.log 2>&1
```

### 2. Git Hooks 集成

可以将验证脚本添加到 pre-commit hook：

```bash
# .githooks/pre-commit
#!/bin/bash
./tools/docs/validate-docs.sh || exit 1
./tools/docs/check-broken-links.sh || exit 1
```

### 3. 自动生成索引

每次提交文档后自动生成索引：

```bash
# .githooks/post-commit
#!/bin/bash
if git diff --name-only HEAD | grep -q "^doc/"; then
    ./tools/docs/generate-index.sh
    git add doc/README.md
fi
```

---

## 🛠️ 扩展工具

### 计划添加的工具

- [ ] `migrate-docs.sh` - 批量迁移文档
- [ ] `update-frontmatter.sh` - 批量更新 Frontmatter
- [ ] `generate-toc.sh` - 生成目录导航
- [ ] `check-spelling.sh` - 拼写检查
- [ ] `stats-report.sh` - 生成统计报告

### 贡献工具

欢迎贡献新的工具脚本！

1. 在 `tools/docs/` 目录创建脚本
2. 添加使用说明到本文件
3. 提交 PR

---

## 📞 问题反馈

如发现问题或有改进建议，请通过 GitHub Issue 反馈。

**维护人**: @TechLead  
**最后更新**: 2026-05-22
