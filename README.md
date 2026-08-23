# 买菜小助手 🥬

AI 帮你规划几天三餐菜单 → 勾选想做的菜 → 自动汇总采购清单（同食材自动合并数量）。

- AI 流式生成（SSE），整份菜单一次请求边生成边接收，不会被网关空闲超时掐断
- 支持家庭成员每人单独设偏好（忌口/过敏一并照顾）
- 设置页「🔌 测试连接」一键验证接口和 Key，「🔄 获取在线模型」在线选模型，「🧪 请求日志」可查看每次请求/响应（key 自动打码，重启清空）
- 采购清单可勾选「已买」、一键复制、分享
- 所有数据只存在手机本地

## 云端打包（不需要在本机装任何开发工具）

每次推送到 main 分支，GitHub Actions 会自动编译出**固定签名**的 APK（签名密钥存在仓库 Secrets，每次构建签名一致，更新直接覆盖安装）：

1. 仓库页面点 **Actions** 标签可看编译进度（首次约 5-10 分钟）
2. 编译成功后到 **Releases** 页面下载 `app-release.apk`
3. 手机打开安装，提示「未知来源」时选择允许

维护者签名密钥（KEYSTORE_BASE64 / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD 四个 Secrets）已配置；本地留有备份 `release.keystore` + `signing-info.txt`（均已 gitignore，勿外传，丢失后更新需卸载重装）。

## AI 配置（App 内「设置」页）

App 内置默认接口地址和模型名（**不含 key**，key 由用户在设置页自己填写）；设置页可改成自己的，或点「恢复默认」还原。模型名不用手打——点「🔄 获取在线模型」自动拉取接口的模型列表点选。

**维护者：如何改默认配置**

- 默认接口地址/默认模型：改 `app/src/main/res/values/defaults.xml` 后推送，自动出新 APK
- 不内置任何 API Key（源码和 APK 中都没有密钥）

也支持手动改用其他服务：

| 服务 | Base URL | 模型名 | Key |
| --- | --- | --- | --- |
| 智谱（免费模型） | `https://open.bigmodel.cn/api/paas/v4` | `glm-4-flash` | open.bigmodel.cn 创建 |
| DeepSeek | `https://api.deepseek.com` | `deepseek-chat` | platform.deepseek.com 创建 |
| 本地 Ollama | `http://<电脑IP>:11434/v1` | 见 `ollama list` | 留空 |

本地 Ollama 注意：手机与电脑需同一 Wi-Fi；电脑需 `OLLAMA_HOST=0.0.0.0 ollama serve` 启动服务。

## 技术栈

Kotlin + Jetpack Compose (Material 3)，HttpURLConnection + org.json，菜单生成走 SSE 流式接口，minSdk 26。
