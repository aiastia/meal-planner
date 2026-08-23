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

    // 已有菜单但没有任何勾选时，默认全选
    LaunchedEffect(plan) {
        if (plan != null && selected.isEmpty()) {
            selected = allDishKeys(plan!!).toSet()
            Store.selected = selected
        }
    }

    fun generate() {
        if (generating) return
        scope.launch {
            generating = true
            try {
                var result: List<DayPlan>? = null
                if (Store.aiReady) {
                    try {
                        result = Ai.generatePlan(Store.baseUrl, Store.apiKey, Store.model, days, people, pref)
                    } catch (e: Exception) {
                        snackbar.showSnackbar("AI 生成失败：${e.message}。已改用内置菜单。")
                    }
                }
                val p = result ?: LocalDishes.randomPlan(days, people)
                plan = p
                Store.plan = p
                selected = allDishKeys(p).toSet()
                Store.selected = selected
                checked = emptySet()
                Store.checked = checked
                if (result != null) snackbar.showSnackbar("已生成 $days 天菜单 🎉")
            } finally {
                generating = false
            }
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
                    plan, selected, generating, days, people, pref,
                    onDays = { days = it; Store.days = it },
                    onPeople = { people = it; Store.people = it },
                    onPref = { pref = it; Store.pref = it },
                    onGenerate = ::generate, onToggle = ::toggleSel
                )
                1 -> ShoppingScreen(plan, selected, checked, onToggle = ::toggleChecked)
                2 -> SettingsScreen { msg -> scope.launch { snackbar.showSnackbar(msg) } }
            }
        }
    }
}
