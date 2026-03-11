#!/bin/bash

echo "=========================================="
echo "小程序图表问题修复脚本"
echo "=========================================="
echo ""

# 检查是否在正确的目录
if [ ! -f "package.json" ]; then
    echo "❌ 错误：请在 miniprogram 目录下运行此脚本"
    exit 1
fi

echo "✅ 当前目录正确"
echo ""

# 提示关闭微信开发者工具
echo "⚠️  请先关闭微信开发者工具，然后按回车继续..."
read -p ""

echo "🔧 开始修复..."
echo ""

# 尝试删除编译输出目录
echo "1. 清理编译缓存..."
if [ -d "dist/dev/mp-weixin" ]; then
    # 尝试删除，如果失败则提示手动删除
    rm -rf dist/dev/mp-weixin 2>/dev/null
    if [ $? -eq 0 ]; then
        echo "   ✅ 缓存清理成功"
    else
        echo "   ⚠️  自动清理失败，请手动删除 dist/dev/mp-weixin 目录"
        echo "   提示：可以在 Finder 中找到该目录并删除"
        read -p "   删除完成后按回车继续..."
    fi
else
    echo "   ℹ️  缓存目录不存在，跳过"
fi
echo ""

# 重新编译
echo "2. 重新编译项目..."
npm run dev:mp-weixin &
COMPILE_PID=$!

echo "   ⏳ 编译中，请等待..."
sleep 15

# 检查编译是否成功
if [ -f "dist/dev/mp-weixin/app.json" ]; then
    echo "   ✅ 编译成功"
else
    echo "   ⚠️  编译可能未完成，请等待编译完成"
fi
echo ""

echo "=========================================="
echo "✅ 修复步骤完成！"
echo "=========================================="
echo ""
echo "接下来请执行以下步骤："
echo ""
echo "1. 打开微信开发者工具"
echo "2. 点击菜单：工具 -> 清缓存 -> 清除全部缓存"
echo "3. 重新编译项目（如果需要）"
echo "4. 查看控制台日志，确认图表是否正常显示"
echo ""
echo "如果图表仍然无法显示，请查看控制台的错误信息"
echo "并参考 图表问题修复说明.md 文档"
echo ""




