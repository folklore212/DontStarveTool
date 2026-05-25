#!/bin/bash

# 文档索引生成脚本
# 用途：自动生成 doc/README.md 索引文件

set -e

OUTPUT_FILE="doc/README.md"

echo "📝 生成文档索引..."

# 开始写入
cat > "$OUTPUT_FILE" << 'EOF'
# DST 管理平台文档索引

> 本文档索引由脚本自动生成，请勿手动编辑

欢迎使用 **DST 管理平台** 的文档中心！

## 📊 文档统计

EOF

# 统计各分类文档数
getting_started_count=$(find doc/getting-started -name "*.md" 2>/dev/null | wc -l)
dev_guide_count=$(find doc/dev-guide -name "*.md" 2>/dev/null | wc -l)
user_guide_count=$(find doc/user-guide -name "*.md" 2>/dev/null | wc -l)
reference_count=$(find doc/reference -name "*.md" 2>/dev/null | wc -l)
architecture_count=$(find doc/architecture -name "*.md" 2>/dev/null | wc -l)
modules_count=$(find doc/modules -name "*.md" 2>/dev/null | wc -l)
total_count=$(find doc -name "*.md" -type f | wc -l)

cat >> "$OUTPUT_FILE" << EOF
| 分类 | 文档数量 |
|------|----------|
| 入门指南 | $getting_started_count 篇 |
| 开发指南 | $dev_guide_count 篇 |
| 用户指南 | $user_guide_count 篇 |
| 参考文档 | $reference_count 篇 |
| 架构文档 | $architecture_count 篇 |
| 模块文档 | $modules_count 篇 |
| **总计** | **$total_count 篇** |

## 📚 文档分类

EOF

# 生成入门指南
cat >> "$OUTPUT_FILE" << 'EOF'
### 🚀 入门指南

适合新手，快速了解和使用平台。

| 文档 | 说明 |
|------|------|
EOF

for file in doc/getting-started/*.md; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        name=$(grep "^name:" "$file" | cut -d':' -f2- | xargs)
        desc=$(grep "^description:" "$file" | cut -d':' -f2- | xargs)
        echo "| [$name](getting-started/$filename) | $desc |" >> "$OUTPUT_FILE"
    fi
done

# 生成开发指南
cat >> "$OUTPUT_FILE" << 'EOF'

### 🛠️ 开发指南

为开发者准备的技术文档。

#### 环境搭建

| 文档 | 说明 |
|------|------|
EOF

for file in doc/dev-guide/setup/*.md; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        name=$(grep "^name:" "$file" | cut -d':' -f2- | xargs)
        desc=$(grep "^description:" "$file" | cut -d':' -f2- | xargs)
        echo "| [$name](dev-guide/setup/$filename) | $desc |" >> "$OUTPUT_FILE"
    fi
done

cat >> "$OUTPUT_FILE" << 'EOF'

#### 开发指南

| 文档 | 说明 |
|------|------|
EOF

for file in doc/dev-guide/guides/*.md; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        name=$(grep "^name:" "$file" | cut -d':' -f2- | xargs)
        desc=$(grep "^description:" "$file" | cut -d':' -f2- | xargs)
        echo "| [$name](dev-guide/guides/$filename) | $desc |" >> "$OUTPUT_FILE"
    fi
done

cat >> "$OUTPUT_FILE" << 'EOF'

#### 部署指南

| 文档 | 说明 |
|------|------|
EOF

for file in doc/dev-guide/deployment/*.md; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        name=$(grep "^name:" "$file" | cut -d':' -f2- | xargs)
        desc=$(grep "^description:" "$file" | cut -d':' -f2- | xargs)
        echo "| [$name](dev-guide/deployment/$filename) | $desc |" >> "$OUTPUT_FILE"
    fi
done

# 生成用户指南
cat >> "$OUTPUT_FILE" << 'EOF'

### 👤 用户指南

面向服务器管理员的功能说明和教程。

#### 功能说明

| 文档 | 说明 |
|------|------|
EOF

for file in doc/user-guide/features/*.md; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        name=$(grep "^name:" "$file" | cut -d':' -f2- | xargs)
        desc=$(grep "^description:" "$file" | cut -d':' -f2- | xargs)
        echo "| [$name](user-guide/features/$filename) | $desc |" >> "$OUTPUT_FILE"
    fi
done

cat >> "$OUTPUT_FILE" << 'EOF'

#### 使用教程

| 文档 | 说明 |
|------|------|
EOF

for file in doc/user-guide/tutorials/*.md; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        name=$(grep "^name:" "$file" | cut -d':' -f2- | xargs)
        desc=$(grep "^description:" "$file" | cut -d':' -f2- | xargs)
        echo "| [$name](user-guide/tutorials/$filename) | $desc |" >> "$OUTPUT_FILE"
    fi
done

# 生成参考文档
cat >> "$OUTPUT_FILE" << 'EOF'

### 📖 参考文档

技术参考文档。

#### API 文档

| 文档 | 说明 |
|------|------|
EOF

for file in doc/reference/api/*.md; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        name=$(grep "^name:" "$file" | cut -d':' -f2- | xargs)
        desc=$(grep "^description:" "$file" | cut -d':' -f2- | xargs)
        echo "| [$name](reference/api/$filename) | $desc |" >> "$OUTPUT_FILE"
    fi
done

cat >> "$OUTPUT_FILE" << 'EOF'

#### 数据库

| 文档 | 说明 |
|------|------|
EOF

for file in doc/reference/database/*.md; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        name=$(grep "^name:" "$file" | cut -d':' -f2- | xargs)
        desc=$(grep "^description:" "$file" | cut -d':' -f2- | xargs)
        echo "| [$name](reference/database/$filename) | $desc |" >> "$OUTPUT_FILE"
    fi
done

# 生成架构文档
cat >> "$OUTPUT_FILE" << 'EOF'

### 🏗️ 架构文档

系统架构和设计文档。

#### 架构概述

| 文档 | 说明 |
|------|------|
EOF

for file in doc/architecture/overview/*.md; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        name=$(grep "^name:" "$file" | cut -d':' -f2- | xargs)
        desc=$(grep "^description:" "$file" | cut -d':' -f2- | xargs)
        echo "| [$name](architecture/overview/$filename) | $desc |" >> "$OUTPUT_FILE"
    fi
done

cat >> "$OUTPUT_FILE" << 'EOF'

#### 架构决策记录 (ADR)

| 文档 | 说明 |
|------|------|
EOF

for file in doc/architecture/adr/*.md; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        name=$(grep "^name:" "$file" | cut -d':' -f2- | xargs)
        desc=$(grep "^description:" "$file" | cut -d':' -f2- | xargs)
        echo "| [$name](architecture/adr/$filename) | $desc |" >> "$OUTPUT_FILE"
    fi
done

cat >> "$OUTPUT_FILE" << 'EOF'

#### 详细设计

| 文档 | 说明 |
|------|------|
EOF

for file in doc/architecture/design/*.md; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        name=$(grep "^name:" "$file" | cut -d':' -f2- | xargs)
        desc=$(grep "^description:" "$file" | cut -d':' -f2- | xargs)
        echo "| [$name](architecture/design/$filename) | $desc |" >> "$OUTPUT_FILE"
    fi
done

# 生成模块文档
cat >> "$OUTPUT_FILE" << 'EOF'

### 🧩 模块文档

按业务模块组织的文档。

| 文档 | 说明 |
|------|------|
EOF

find doc/modules -name "*.md" -type f | sort | while read file; do
    filename=$(basename "$file")
    name=$(grep "^name:" "$file" | cut -d':' -f2- | xargs)
    desc=$(grep "^description:" "$file" | cut -d':' -f2- | xargs)
    dir=$(basename $(dirname "$file"))
    echo "| [$name](modules/$dir/$filename) | $desc |" >> "$OUTPUT_FILE"
done

# 添加页脚
cat >> "$OUTPUT_FILE" << EOF

---

## 🔗 相关链接

- [领域术语表](../CONTEXT.md)
- [贡献指南](../CONTRIBUTING.md)
- [文档模板](internal/templates/)
- [重构完成报告](REFACTOR-COMPLETE.md)
- [下一步行动](NEXT-STEPS.md)

## 📅 更新信息

- **最后更新**: $(date +%Y-%m-%d)
- **文档总数**: $total_count 篇
- **生成工具**: tools/docs/generate-index.sh

---

*本文档索引由脚本自动生成 - $(date +%Y-%m-%d %H:%M)*
EOF

echo -e "✅ 文档索引生成完成：$OUTPUT_FILE"
echo "📊 共收录 $total_count 篇文档"
