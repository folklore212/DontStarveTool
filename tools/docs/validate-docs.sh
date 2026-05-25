#!/bin/bash

# 文档验证脚本
# 用途：检查文档的完整性和规范性

set -e

echo "🔍 开始验证文档..."
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 统计
total_docs=0
valid_docs=0
errors=0

# 检查文档目录结构
echo "📁 检查目录结构..."
required_dirs=(
    "doc/getting-started"
    "doc/dev-guide/setup"
    "doc/dev-guide/guides"
    "doc/dev-guide/deployment"
    "doc/user-guide/features"
    "doc/user-guide/tutorials"
    "doc/reference/api"
    "doc/reference/database"
    "doc/architecture/overview"
    "doc/architecture/adr"
    "doc/architecture/design"
    "doc/modules"
    "doc/internal/templates"
    "doc/internal/reviews"
    "doc/_meta"
    "doc/_reports"
    "doc/_static"
)

for dir in "${required_dirs[@]}"; do
    if [ -d "$dir" ]; then
        echo -e "${GREEN}✓${NC} $dir"
    else
        echo -e "${RED}✗${NC} $dir (缺失)"
        ((errors++))
    fi
done

echo ""
echo "📄 检查文档文件..."

# 检查所有 Markdown 文件（排除 _static 目录）
doc_files=$(find doc -name "*.md" -type f -not -path "doc/_static/*")

for file in $doc_files; do
    ((total_docs++))

    has_frontmatter=false
    has_name=false
    has_description=false
    has_status=false
    has_owner=false

    # 检查是否以 --- 开头
    if head -1 "$file" | grep -q "^---"; then
        has_frontmatter=true

        # 检查必要字段
        grep -q "^name:" "$file" && has_name=true
        grep -q "^description:" "$file" && has_description=true
        grep -q "^status:" "$file" && has_status=true
        grep -q "^owner:" "$file" && has_owner=true
    fi

    # 输出结果
    if [ "$has_frontmatter" = true ] && [ "$has_name" = true ] && \
       [ "$has_description" = true ] && [ "$has_status" = true ] && \
       [ "$has_owner" = true ]; then
        echo -e "${GREEN}✓${NC} $file"
        ((valid_docs++))
    else
        echo -e "${RED}✗${NC} $file"
        echo "  缺少字段:"
        [ "$has_frontmatter" = false ] && echo "    - Frontmatter header (---)"
        [ "$has_name" = false ] && echo "    - name"
        [ "$has_description" = false ] && echo "    - description"
        [ "$has_status" = false ] && echo "    - status"
        [ "$has_owner" = false ] && echo "    - owner"
        ((errors++))
    fi
done

echo ""
echo "📊 验证结果"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "总文档数：   $total_docs"
echo "有效文档：   ${GREEN}$valid_docs${NC}"
echo "错误数：     ${RED}$errors${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 输出最终状态
if [ $errors -eq 0 ]; then
    echo -e "${GREEN}✓ 文档验证通过！${NC}"
    exit 0
else
    echo -e "${RED}✗ 文档验证失败，发现 $errors 个错误${NC}"
    exit 1
fi
