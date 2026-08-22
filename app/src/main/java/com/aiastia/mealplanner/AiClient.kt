package com.aiastia.mealplanner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

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
            if (pref.isNotBlank()) append("口味偏好/忌口：$pref。")
            append("严格按此 JSON 格式输出：")
            append("{\"days\":[{\"label\":\"第1天\",\"meals\":[{\"type\":\"早餐\",\"dishes\":[{\"name\":\"菜名\",\"ingredients\":[\"食材 用量\"]}]}]}]}")
        }
        val content = chat(baseUrl, apiKey, model, prompt)
        val i = content.indexOf('{')
        val j = content.lastIndexOf('}')
        if (i < 0 || j <= i) throw IOException("AI 返回的内容里找不到菜单，请重试")
        dayPlansFromAi(content.substring(i, j + 1))
            ?: throw IOException("AI 返回的菜单格式不对，请重试或换个模型")
    }

    private fun dayPlansFromAi(json: String): List<DayPlan>? = try {
        dayPlansFromJson(json)
    } catch (e: Exception) {
        null
    }

    private fun chat(baseUrl: String, apiKey: String, model: String, userPrompt: String): String {
        val url = URL(baseUrl.trimEnd('/') + "/chat/completions")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 180000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer $apiKey")
        }
        val body = JSONObject().apply {
            put("model", model)
            put("temperature", 0.7)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", SYSTEM))
                put(JSONObject().put("role", "user").put("content", userPrompt))
            })
        }
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val text = try {
            (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
        } finally {
            conn.disconnect()
        }
        if (code !in 200..299) throw IOException("接口返回 HTTP $code：${text.take(200)}")
        return try {
            JSONObject(text).getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
        } catch (e: Exception) {
            throw IOException("接口返回内容无法解析：${text.take(200)}")
        }
    }
}
