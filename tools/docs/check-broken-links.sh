#!/bin/bash

# 链接检查脚本
# 用途：检查 Markdown 文档中的死链

set -e

echo "🔗 检查文档链接..."
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

total_files=0
total_links=0
broken_links=0
external_links=0

# 临时文件存储结果
temp_file=$(mktemp)

# 遍历所有 Markdown 文件
while IFS= read -r -d '' file; do
    ((total_files++))
    file_has_issues=false

    # 提取所有 Markdown 链接
    links=$(grep -oE '\[([^\]]+)\]\(([^\)]+)\)' "$file" 2>/dev/null | grep -oE '\(([^\)]+)\)' | tr -d '()' || true)

    for link in $links; do
        ((total_links++))

        # 跳过外部链接
        if [[ $link == http* ]]; then
            ((external_links++))
            continue
        fi

        # 跳过邮件链接
        if [[ $link == mailto:* ]]; then
            continue
        fi

        # 跳过锚点链接
        if [[ $link == \#* ]]; then
            continue
        fi

        # 解析链接路径
        link_path=$(echo "$link" | cut -d'#' -f1)
        anchor=$(echo "$link" | grep -o '#.*' || true)

        # 如果是相对路径，转换为绝对路径
        if [[ $link_path == /* ]]; then
            check_path="doc${link_path}"
        else
            dir=$(dirname "$file")
            check_path="$dir/$link_path"
        fi

        # 检查文件是否存在
        if [ -n "$link_path" ]; then
            if [ ! -f "$check_path" ] && [ ! -d "$check_path" ]; then
                if [ "$file_has_issues" = false ]; then
                    echo -e "${RED}✗${NC} $file"
                    file_has_issues=true
                fi
                echo "    └─ broken: $link"
                echo "       check_path: $check_path"
                ((broken_links++))
                echo "$file|$link|$check_path" >> "$temp_file"
            fi
        fi
    done

    if [ "$file_has_issues" = false ]; then
        echo -e "${GREEN}✓${NC} $file"
    fi

done < <(find doc -name "*.md" -type f -print0)

# 输出统计
echo ""
echo "📊 链接检查统计"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "检查文件数： $total_files"
echo "总链接数：   $total_links"
echo "外部链接：   $external_links"
echo "内部链接：   $((total_links - external_links))"
echo "死链数量：   ${RED}$broken_links${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 如果有死链，输出详细列表
if [ $broken_links -gt 0 ]; then
    echo ""
    echo "📋 死链详情:"
    echo ""
    cat "$temp_file" | while IFS='|' read -r file link check_path; do
        echo "文件：$file"
        echo "链接：$link"
        echo "检查路径：$check_path"
        echo ""
    done

    echo -e "${RED}✗ 发现 $broken_links 个死链，请修复${NC}"
    rm -f "$temp_file"
    exit 1
else
    echo -e "${GREEN}✓ 所有链接都有效！${NC}"
    rm -f "$temp_file"
    exit 0
fi
