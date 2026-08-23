package com.aiastia.mealplanner

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ---------- 菜谱页 ----------

@Composable
fun PlanScreen(
    plan: List<DayPlan>?,
    selected: Set<String>,
    generating: Boolean,
    days: Int,
    people: Int,
    pref: String,
    onDays: (Int) -> Unit,
    onPeople: (Int) -> Unit,
    onPref: (String) -> Unit,
    onGenerate: () -> Unit,
    onToggle: (String) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("生成菜单", style = MaterialTheme.typography.titleMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("天数", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..7).forEach { d -> DayChip(d, d == days) { onDays(d) } }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("人数", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = { if (people > 1) onPeople(people - 1) },
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) { Text("−") }
                        Text(
                            "$people 人",
                            Modifier.padding(horizontal = 10.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedButton(
                            onClick = { if (people < 8) onPeople(people + 1) },
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) { Text("＋") }
                    }
                    OutlinedTextField(
                        value = pref,
                        onValueChange = onPref,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("口味偏好 / 忌口（可选）") },
                        placeholder = { Text("比如：不吃辣、少油、多海鲜") },
                        singleLine = true
                    )
                    Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth(), enabled = !generating) {
                        if (generating) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("生成中…")
                        } else {
                            Text("✨ 生成菜单")
                        }
                    }
                    if (!Store.aiReady) {
                        Text(
                            "提示：在「设置」页配置 AI 后可智能生成，目前将使用内置菜单随机搭配",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        if (plan == null) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "先点上面按钮生成一份菜单，勾选想做的菜，\n然后去「采购」页看清单 👉",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            itemsIndexed(plan) { d, day ->
                Column {
                    Text(
                        day.label,
                        Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    day.meals.forEachIndexed { m, meal ->
                        Text(
                            meal.type,
                            Modifier.padding(top = 8.dp, bottom = 2.dp),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        meal.dishes.forEachIndexed { i, dish ->
                            DishRow(dishKey(d, m, i), dish, selected, onToggle)
                        }
                    }
                    HorizontalDivider(Modifier.padding(top = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun DishRow(key: String, dish: Dish, selected: Set<String>, onToggle: (String) -> Unit) {
    val checked = key in selected
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle(key) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle(key) })
        Column(Modifier.padding(start = 4.dp)) {
            Text(dish.name, fontWeight = FontWeight.Medium)
            if (dish.ingredients.isNotEmpty()) {
                Text(
                    dish.ingredients.joinToString("、"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DayChip(n: Int, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                if (on) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$n",
            color = if (on) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------- 采购清单页 ----------

@Composable
fun ShoppingScreen(
    plan: List<DayPlan>?,
    selected: Set<String>,
    checked: Set<String>,
    onToggle: (String) -> Unit
) {
    val items = remember(plan, selected) {
        val dishes = plan.orEmpty().flatMapIndexed { d, day ->
            day.meals.flatMapIndexed { m, meal ->
                meal.dishes.filterIndexed { i, _ -> dishKey(d, m, i) in selected }
            }
        }
        mergeIngredients(dishes)
    }
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current

    fun asText(): String = buildString {
        appendLine("🛒 采购清单（共 ${items.size} 项）")
        items.forEach { appendLine((if (it in checked) "☑ " else "☐ ") + it) }
    }

    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "还没有采购清单\n\n去「菜谱」页生成菜单，\n勾选想做的菜后自动汇总",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            Column {
                Text("共 ${items.size} 项，已买 ${checked.size} 项", style = MaterialTheme.typography.titleMedium)
                Row(
                    Modifier.padding(top = 6.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { clipboard.setText(AnnotatedString(asText())) },
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) { Text("复制") }
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, asText())
                            }
                            ctx.startActivity(Intent.createChooser(intent, "分享采购清单"))
                        },
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) { Text("分享") }
                    TextButton(onClick = { items.forEach { if (it in checked) onToggle(it) } }) {
                        Text("清空已买")
                    }
                }
                HorizontalDivider()
            }
        }
        items(items) { name ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggle(name) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = name in checked, onCheckedChange = { onToggle(name) })
                Text(name, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

// ---------- 设置页 ----------

@Composable
fun SettingsScreen(onMsg: (String) -> Unit) {
    var url by remember { mutableStateOf(Store.baseUrl) }
    var key by remember { mutableStateOf(Store.apiKey) }
    var model by remember { mutableStateOf(Store.model) }
    var models by remember { mutableStateOf<List<String>?>(null) }
    var loadingModels by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("AI 设置", style = MaterialTheme.typography.headlineSmall)
        Text(
            "App 已内置默认配置（下面预填的地址/密钥/模型），开箱即用；也可改成自己的，改乱了点「恢复默认」即可。密钥只保存在手机本地。",
            style = MaterialTheme.typography.bodySmall
        )
        Text("快速填充（点一下自动填地址和模型名）：", style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = {
            url = "https://open.bigmodel.cn/api/paas/v4"; model = "glm-4-flash"
        }) { Text("智谱（glm-4-flash 免费）") }
        OutlinedButton(onClick = {
            url = "https://api.deepseek.com"; model = "deepseek-chat"
        }) { Text("DeepSeek") }
        OutlinedButton(onClick = {
            url = "http://192.168.1.100:11434/v1"; model = "qwen3.8-27b-uncensored"
        }) { Text("本地 Ollama（家里电脑）") }

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("接口地址 Base URL") },
            placeholder = { Text("https://open.bigmodel.cn/api/paas/v4") },
            singleLine = true
        )
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API Key（本地 Ollama 可留空）") },
            singleLine = true
        )
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("模型名") },
            placeholder = { Text("glm-4-flash") },
            singleLine = true
        )
        OutlinedButton(
            onClick = {
                scope.launch {
                    loadingModels = true
                    try {
                        val list = Ai.fetchModels(url, key)
                        if (list.isEmpty()) onMsg("接口没有返回任何模型")
                        else models = list
                    } catch (e: Exception) {
                        onMsg("获取模型列表失败：${e.message}")
                    } finally {
                        loadingModels = false
                    }
                }
            },
            enabled = url.isNotBlank() && !loadingModels,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loadingModels) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(6.dp))
                Text("获取中…")
            } else {
                Text("🔄 获取在线模型")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    Store.baseUrl = url; Store.apiKey = key; Store.model = model
                    onMsg("已保存 ✅")
                },
                modifier = Modifier.weight(1f)
            ) { Text("保存") }
            OutlinedButton(
                onClick = {
                    Store.resetAi()
                    url = Store.baseUrl; key = Store.apiKey; model = Store.model
                    onMsg("已恢复默认配置 ✅")
                },
                modifier = Modifier.weight(1f)
            ) { Text("恢复默认") }
        }
        models?.let { list ->
            AlertDialog(
                onDismissRequest = { models = null },
                confirmButton = {
                    TextButton(onClick = { models = null }) { Text("关闭") }
                },
                title = { Text("在线模型（${list.size} 个）") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        list.forEach { m ->
                            Text(
                                m,
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        model = m
                                        models = null
                                    }
                                    .padding(vertical = 10.dp),
                                fontWeight = if (m == model) FontWeight.Bold else null,
                                color = if (m == model) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }

        Text(
            "· 智谱：open.bigmodel.cn 注册后创建 API Key，glm-4-flash 免费\n" +
                "· DeepSeek：platform.deepseek.com 充值很便宜\n" +
                "· 本地 Ollama：手机和电脑连同一个 Wi-Fi，地址里 IP 改成电脑的 IP；" +
                "电脑上需执行 OLLAMA_HOST=0.0.0.0 ollama serve，模型名以 ollama list 为准",
            style = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
