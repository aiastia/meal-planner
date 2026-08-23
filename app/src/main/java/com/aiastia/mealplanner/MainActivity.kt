package com.aiastia.mealplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(applicationContext)
        setContent { App() }
    }
}

@Composable
fun App() {
    var tab by remember { mutableStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var plan by remember { mutableStateOf(Store.plan) }
    var selected by remember { mutableStateOf(Store.selected) }
    var checked by remember { mutableStateOf(Store.checked) }
    var generating by remember { mutableStateOf(false) }
    var days by remember { mutableStateOf(Store.days) }
    var people by remember { mutableStateOf(Store.people) }
    var pref by remember { mutableStateOf(Store.pref) }
    var members by remember { mutableStateOf(Store.members) }

    // 已有菜单但没有任何勾选时，默认全选
    LaunchedEffect(plan) {
        if (plan != null && selected.isEmpty()) {
            selected = allDishKeys(plan!!).toSet()
            Store.selected = selected
        }
    }

    fun generate() {
        if (generating) return
        // 有家庭成员时：人数按成员数算，每人偏好逐行并入提示词
        val effPeople = if (members.isEmpty()) people else members.size
        val prefAll = buildString {
            members.forEach { m -> if (m.name.isNotBlank()) appendLine("${m.name}：${m.pref}") }
            if (pref.isNotBlank()) appendLine("全家：$pref")
        }.trim()
        scope.launch {
            generating = true
            var msg: String? = null
            try {
                if (!Store.aiReady) {
                    msg = "还没配置 AI：去「设置」页填入你的 API Key（地址已预填）"
                } else {
                    try {
                        val p = Ai.generatePlan(Store.baseUrl, Store.apiKey, Store.model, days, effPeople, prefAll)
                        plan = p
                        Store.plan = p
                        selected = allDishKeys(p).toSet()
                        Store.selected = selected
                        checked = emptySet()
                        Store.checked = checked
                        msg = "已生成 $days 天菜单 🎉"
                    } catch (e: Exception) {
                        msg = "生成失败：${e.message}"
                    }
                }
            } finally {
                generating = false
            }
            // 提示条独立弹出，不阻塞上面的状态复位
            msg?.let { scope.launch { snackbar.showSnackbar(it) } }
        }
    }

    fun toggleSel(key: String) {
        selected = if (key in selected) selected - key else selected + key
        Store.selected = selected
    }

    fun toggleChecked(name: String) {
        checked = if (name in checked) checked - name else checked + name
        Store.checked = checked
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) }, label = { Text("菜谱") })
                NavigationBarItem(tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) }, label = { Text("采购") })
                NavigationBarItem(tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) }, label = { Text("设置") })
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> PlanScreen(
                    plan, selected, generating, days, people, pref, members,
                    onDays = { days = it; Store.days = it },
                    onPeople = { people = it; Store.people = it },
                    onPref = { pref = it; Store.pref = it },
                    onMembers = { members = it; Store.members = it },
                    onGenerate = ::generate, onToggle = ::toggleSel
                )
                1 -> ShoppingScreen(plan, selected, checked, onToggle = ::toggleChecked)
                2 -> SettingsScreen { msg -> scope.launch { snackbar.showSnackbar(msg) } }
            }
        }
    }
}
