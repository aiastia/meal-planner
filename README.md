# 买菜小助手 🥬

AI 帮你规划几天三餐菜单 → 勾选想做的菜 → 自动汇总采购清单（同食材自动合并数量）。

- 不配置 AI 也能用：内置家常菜库随机搭配
- 配置 AI 后按人数、口味偏好智能生成
- 采购清单可勾选「已买」、一键复制、分享
- 所有数据只存在手机本地

## 云端打包（不需要在本机装任何开发工具）

每次推送到 main 分支，GitHub Actions 会自动编译出 APK：

1. 仓库页面点 **Actions** 标签可看编译进度（首次约 5-10 分钟）
2. 编译成功后到 **Releases** 页面下载 `app-debug.apk`
3. 手机打开安装，提示「未知来源」时选择允许

## AI 配置（App 内「设置」页）

| 服务 | Base URL | 模型名 | Key |
| --- | --- | --- | --- |
| 智谱（免费模型） | `https://open.bigmodel.cn/api/paas/v4` | `glm-4-flash` | open.bigmodel.cn 创建 |
| DeepSeek | `https://api.deepseek.com` | `deepseek-chat` | platform.deepseek.com 创建 |
| 本地 Ollama | `http://<电脑IP>:11434/v1` | 见 `ollama list` | 留空 |

本地 Ollama 注意：手机与电脑需同一 Wi-Fi；电脑需 `OLLAMA_HOST=0.0.0.0 ollama serve` 启动服务。

## 技术栈

Kotlin + Jetpack Compose (Material 3)，无第三方网络库（ HttpURLConnection + org.json ），minSdk 26。
