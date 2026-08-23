package com.aiastia.mealplanner

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 一次 HTTP 请求的记录（排查用，内存保存、重启清空、最多 30 条） */
data class HttpLogEntry(
    val time: String,
    val title: String,
    val request: String,
    val response: String,
    val ok: Boolean
)

object HttpLog {
    val entries = mutableStateListOf<HttpLogEntry>() // 最新在前
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private const val MAX = 30

    fun add(title: String, request: String, response: String, ok: Boolean) {
        entries.add(0, HttpLogEntry(fmt.format(Date()), title, request, response, ok))
        while (entries.size > MAX) entries.removeAt(entries.size - 1)
    }

    /** 把日志文本里的 key 打码，方便安全分享 */
    fun maskKey(s: String, key: String): String =
        if (key.length < 8) s else s.replace(key, "***")
}
