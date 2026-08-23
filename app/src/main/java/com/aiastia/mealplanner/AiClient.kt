package com.aiastia.mealplanner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * 调用 OpenAI 兼容接口（/chat/completions）生成菜单。
 * 智谱、DeepSeek、本地 Ollama 等都兼容这个格式，在设置里填对应地址即可。
 */
object Ai {

    private const val SYSTEM =
        "你是专业的家庭菜单规划师。必须只输出一个 JSON 对象，不要输出任何解释文字、markdown 或代码块标记。"

    suspend fun generatePlan(
        baseUrl: String,
        apiKey: String,
        model: String,
        days: Int,
        people: Int,
        pref: String
    ): List<DayPlan> = withContext(Dispatchers.IO) {
        val prompt = buildString {
            append("请规划 ${days} 天的家庭三餐菜单（${people} 人份），全部为中式家常菜，食材在普通超市或菜市场容易买到。")
            append("每天早餐 1-2 道、午餐 2-3 道、晚餐 2-3 道，荤素搭配。")
            if (pref.isNotBlank()) append("成员与家庭偏好（逐行「称呼：偏好」，必须全部满足，过敏食材严格避开）：\n$pref\n")
            append("严格按此 JSON 格式输出：")
            append("{\"days\":[{\"label\":\"第1天\",\"meals\":[{\"type\":\"早餐\",\"dishes\":[{\"name\":\"菜名\",\"ingredients\":[\"食材 用量\"]}]}]}]}")
        }
        val content = chat(baseUrl, apiKey, model, prompt)
        val i = content.indexOf('{')
        val j = content.lastIndexOf('}')
        val parsed = if (i < 0 || j <= i) null else dayPlansFromAi(content.substring(i, j + 1))
        if (parsed == null) {
            HttpLog.add(
                "解析菜单 JSON",
                "解析 AI 返回的 ${content.length} 字内容",
                "❌ 没有找到合法的菜单 JSON\n\n内容开头：${HttpLog.maskKey(content, apiKey).take(300)}…",
                false
            )
            throw IOException("AI 返回的菜单格式不对，请重试或换个模型")
        }
        parsed
    }

    private fun dayPlansFromAi(json: String): List<DayPlan>? = try {
        dayPlansFromJson(json)
    } catch (e: Exception) {
        null
    }

    /** 拉取接口的在线模型列表（OpenAI 兼容的 /models 端点），用于设置页一键选模型 */
    suspend fun fetchModels(baseUrl: String, apiKey: String): List<String> = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()
        val url = baseUrl.trimEnd('/') + "/models"
        val reqDesc = "GET $url\n携带 Key: ${apiKey.isNotBlank()}"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 30000
            if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer $apiKey")
        }
        val code = conn.responseCode
        val text = try {
            (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
        } finally {
            conn.disconnect()
        }
        val dur = "${System.currentTimeMillis() - t0}ms"
        if (code !in 200..299) {
            HttpLog.add("获取模型列表", reqDesc, "❌ HTTP $code · $dur\n${HttpLog.maskKey(text, apiKey).take(300)}", false)
            throw IOException("HTTP $code：${text.take(200)}")
        }
        val ids = ArrayList<String>()
        val trimmed = text.trim()
        if (trimmed.startsWith("[")) {
            val arr = JSONArray(trimmed)
            for (i in 0 until arr.length()) {
                ids.add(arr.optJSONObject(i)?.optString("id") ?: arr.optString(i))
            }
        } else {
            val data = JSONObject(trimmed).optJSONArray("data") ?: JSONArray()
            for (i in 0 until data.length()) {
                ids.add(data.getJSONObject(i).optString("id"))
            }
        }
        val result = ids.filter { it.isNotBlank() }.distinct().sorted()
        HttpLog.add(
            "获取模型列表", reqDesc,
            "✅ HTTP 200 · $dur · ${result.size} 个模型" +
                (if (result.isNotEmpty()) "：\n${result.take(10).joinToString("、")}${if (result.size > 10) " 等" else ""}" else ""),
            true
        )
        result
    }

    /** SSE 流式调用：边生成边收数据，长回答不会被网关空闲超时掐断 */
    private fun chat(baseUrl: String, apiKey: String, model: String, userPrompt: String): String {
        val url = URL(baseUrl.trimEnd('/') + "/chat/completions")
        val t0 = System.currentTimeMillis()
        val reqDesc = "POST $url\n模型: $model（SSE 流式）\n携带 Key: ${apiKey.isNotBlank()}\n\n提示词：\n$userPrompt"
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 120000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "text/event-stream")
            if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer $apiKey")
        }
        val body = JSONObject().apply {
            put("model", model)
            put("temperature", 0.7)
            put("stream", true)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", SYSTEM))
                put(JSONObject().put("role", "user").put("content", userPrompt))
            })
        }
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        if (code !in 200..299) {
            val err = try {
                (conn.errorStream ?: conn.inputStream)?.bufferedReader()?.use { it.readText() } ?: ""
            } catch (e: Exception) {
                ""
            } finally {
                conn.disconnect()
            }
            HttpLog.add(
                "生成菜单 · $model", reqDesc,
                "❌ HTTP $code · ${System.currentTimeMillis() - t0}ms\n${HttpLog.maskKey(err, apiKey).take(300)}",
                false
            )
            throw IOException("接口返回 HTTP $code：${err.take(200)}")
        }
        val sb = StringBuilder()
        var chunks = 0
        try {
            conn.inputStream.bufferedReader().useLines { lines ->
                for (raw in lines) {
                    val line = raw.trim()
                    if (!line.startsWith("data:")) continue
                    val payload = line.removePrefix("data:").trim()
                    if (payload.isEmpty()) continue
                    if (payload == "[DONE]") break
                    chunks++
                    try {
                        val delta = JSONObject(payload)
                            .optJSONArray("choices")?.optJSONObject(0)
                            ?.optJSONObject("delta")?.optString("content")
                        if (!delta.isNullOrEmpty()) sb.append(delta)
                    } catch (e: Exception) {
                        // 跳过无法解析的心跳/杂行
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        val dur = String.format(Locale.US, "%.1f", (System.currentTimeMillis() - t0) / 1000.0)
        if (sb.isBlank()) {
            HttpLog.add("生成菜单 · $model", reqDesc, "❌ HTTP 200 但没有收到任何内容 · 耗时 ${dur}s（$chunks 个流式块）", false)
            throw IOException("AI 没有返回内容（流为空），请重试或换个模型")
        }
        HttpLog.add(
            "生成菜单 · $model", reqDesc,
            "✅ HTTP 200 · 耗时 ${dur}s · $chunks 个流式块、共 ${sb.length} 字\n\n内容开头：\n${HttpLog.maskKey(sb.toString(), apiKey).take(300)}…",
            true
        )
        return sb.toString()
    }
}
