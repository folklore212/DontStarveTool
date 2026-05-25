#!/bin/bash

# 批量添加 Frontmatter 脚本
# 用途：为缺少 Frontmatter 的文档添加标准头部

set -e

echo "📝 开始添加 Frontmatter..."
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 获取当前日期
CURRENT_DATE=$(date +%Y-%m-%d)

# 从文件名提取信息的函数
extract_info() {
    local file=$1
    local filename=$(basename "$file" .md)

    # 从文件第一行提取标题
    local title=$(head -1 "$file" | sed 's/^# //')

    # 根据目录确定文档类型
    local dir=$(dirname "$file")
    local status="approved"
    local owner="@TechLead"
    local tags=""

    case "$dir" in
        *architecture/adr*)
            tags="[architecture,decision,adr]"
            ;;
        *architecture*)
            tags="[architecture,design]"
            ;;
        *dev-guide*)
            tags="[development,guide]"
            ;;
        *user-guide*)
            tags="[user,guide]"
            ;;
        *reference*)
            tags="[reference]"
            ;;
        *modules*)
            tags="[module,design]"
            ;;
        *getting-started*)
            tags="[getting-started,beginner]"
            ;;
        *_meta*)
            tags="[meta,documentation]"
            ;;
        *_reports*)
            tags="[report,summary]"
            status="approved"
            ;;
        *internal*)
            tags="[internal]"
            ;;
        *)
            tags="[documentation]"
            ;;
    esac

    # 生成 description（从标题截取前 50 字符）
    local description=$(echo "$title" | cut -c1-50)

    echo "---"
    echo "name: $title"
    echo "description: $description"
    echo "status: $status"
    echo "owner: $owner"
    echo "created: $CURRENT_DATE"
    echo "last_updated: $CURRENT_DATE"
    echo "reviewers: []"
    echo "review_cycle: release"
    echo "tags: $tags"
    echo "---"
    echo ""
}

# 处理文件
process_file() {
    local file=$1
    local temp_file=$(mktemp)

    # 检查是否已经有 Frontmatter
    if head -1 "$file" | grep -q "^---"; then
        echo -e "${GREEN}✓${NC} $file (已有 Frontmatter)"
        return
    fi

    # 生成 Frontmatter
    local frontmatter=$(extract_info "$file")

    # 读取原文件内容（跳过可能存在的空行）
    local content=$(cat "$file")

    # 写入新文件
    echo "$frontmatter" > "$temp_file"
    echo "$content" >> "$temp_file"

    # 替换原文件
    mv "$temp_file" "$file"

    echo -e "${GREEN}✓${NC} $file"
}

# 统计
total=0
processed=0

# 处理所有 Markdown 文件（排除 _static 目录）
while IFS= read -r -d '' file; do
    ((total++))
    process_file "$file"
    ((processed++))
done < <(find doc -name "*.md" -type f -not -path "doc/_static/*" -print0)

echo ""
echo "📊 处理完成"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "总文件数： $total"
echo "已处理：   $processed"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${GREEN}✓ Frontmatter 添加完成！${NC}"
