#!/bin/bash
# 买菜小助手 · 推送到 GitHub 向导
# 只需要粘贴两个东西：仓库地址 + 令牌。令牌只在推送瞬间使用，不保存到磁盘。
set -e
cd "$(dirname "$0")"

echo "══════════════════════════════════════════"
echo "  买菜小助手 · 推送到 GitHub 向导"
echo "══════════════════════════════════════════"
echo

read -p "① 粘贴你的仓库地址（例如 https://github.com/你的用户名/meal-planner ）: " REPO
REPO=$(echo "$REPO" | xargs)
while [ -z "$REPO" ]; do
  read -p "地址不能为空，请重新粘贴: " REPO
  REPO=$(echo "$REPO" | xargs)
done

echo
read -s -p "② 粘贴你的令牌（ghp_ 开头；输入时屏幕不显示，粘贴后直接回车）: " TOKEN
echo
TOKEN=$(echo "$TOKEN" | xargs)
while [ -z "$TOKEN" ]; do
  read -s -p "令牌不能为空，请重新粘贴: " TOKEN
  echo
  TOKEN=$(echo "$TOKEN" | xargs)
done

echo
echo "正在推送（首次约几秒到几十秒）……"
echo

# 令牌写入一个临时 askpass 脚本，推送完立刻删除，全程不落盘保存
ASKPASS="$(mktemp)"
printf '#!/bin/sh\ncase "$1" in\n  Username*) echo "%s";;\n  Password*) echo "";;\nesac\n' "$TOKEN" > "$ASKPASS"
chmod +x "$ASKPASS"

git remote remove origin 2>/dev/null || true
git remote add origin "$REPO"

if GIT_ASKPASS="$ASKPASS" git push -u origin main; then
  echo
  echo "✅ 推送成功！接下来："
  echo "   1. 打开 ${REPO}/actions —— 等出现绿色 ✓（首次约 5-10 分钟）"
  echo "   2. 打开 ${REPO}/releases —— 下载 app-debug.apk 发到手机安装"
  echo "   3. 手机提示「未知来源」时选择允许"
  echo "   如果编译失败（红色 ✗），点进那次运行把报错复制给 ZCode 帮你修。"
else
  echo
  echo "❌ 推送失败，常见原因："
  echo "   · 令牌创建时没勾选 repo 权限"
  echo "   · 仓库地址粘贴错了"
  echo "   · 建仓库时不小心勾了 README（仓库不为空）——建议删掉重建一个空仓库"
  echo "   把上面的报错复制给 ZCode，我帮你解决。"
fi

rm -f "$ASKPASS"
