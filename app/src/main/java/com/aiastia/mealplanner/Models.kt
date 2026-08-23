package com.aiastia.mealplanner

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class Dish(val name: String, val ingredients: List<String>)
data class Meal(val type: String, val dishes: List<Dish>)
data class DayPlan(val label: String, val meals: List<Meal>)

/** 家庭成员及其单独的口味偏好/忌口 */
data class Member(val name: String, val pref: String)

/** 从 AI 或本地存储的 JSON 解析出菜单，格式不合法返回 null */
fun dayPlansFromJson(s: String): List<DayPlan>? = try {
    val days = JSONObject(s).getJSONArray("days")
    (0 until days.length()).map { d ->
        val day = days.getJSONObject(d)
        val meals = day.optJSONArray("meals") ?: JSONArray()
        DayPlan(
            label = day.optString("label", "第${d + 1}天"),
            meals = (0 until meals.length()).map { m ->
                val meal = meals.getJSONObject(m)
                val dishes = meal.optJSONArray("dishes") ?: JSONArray()
                Meal(
                    type = meal.optString("type", ""),
                    dishes = (0 until dishes.length()).map { i ->
                        val dish = dishes.getJSONObject(i)
                        Dish(
                            name = dish.optString("name", "未命名"),
                            ingredients = (dish.optJSONArray("ingredients") ?: JSONArray())
                                .let { arr -> (0 until arr.length()).map { arr.optString(it) } }
                        )
                    }
                )
            }
        )
    }.ifEmpty { null }
} catch (e: Exception) {
    null
}

/** 选中状态的 key：第几天-第几餐-第几道菜 */
fun dishKey(d: Int, m: Int, i: Int) = "$d-$m-$i"

fun allDishKeys(plan: List<DayPlan>): List<String> =
    plan.flatMapIndexed { d, day ->
        day.meals.flatMapIndexed { m, meal ->
            meal.dishes.mapIndexed { i, _ -> dishKey(d, m, i) }
        }
    }

/** 把勾选菜品的食材合并去重：同食材同单位自动加总数量 */
fun mergeIngredients(dishes: List<Dish>): List<String> {
    val qty = LinkedHashMap<Pair<String, String>, Double>()
    val plain = LinkedHashSet<String>()
    val pattern = Regex("^(.+?)[\\s:：]*([0-9]+(?:\\.[0-9]+)?)\\s*([a-zA-Z\\u4e00-\\u9fa5]*)$")
    for (dish in dishes) {
        for (raw in dish.ingredients) {
            val s = raw.trim()
            if (s.isEmpty()) continue
            val m = pattern.find(s)
            if (m != null) {
                val name = m.groupValues[1].trim()
                if (name.isNotEmpty()) {
                    val key = name to m.groupValues[3].trim()
                    qty[key] = (qty[key] ?: 0.0) + m.groupValues[2].toDouble()
                    continue
                }
            }
            plain.add(s)
        }
    }
    fun fmt(n: Double) = if (n == n.toLong().toDouble()) n.toLong().toString() else n.toString()
    return qty.map { (k, v) -> "${k.first} ${fmt(v)}${k.second}" } + plain.toList()
}

/** 本地持久化：菜单、勾选状态、设置，全部存在手机里。
 *  AI 三项配置采用「构建时注入默认值 + 用户可覆盖」：未单独设置时使用内置默认值。 */
object Store {
    private lateinit var appCtx: Context
    private lateinit var prefs: SharedPreferences

    fun init(ctx: Context) {
        appCtx = ctx.applicationContext
        prefs = appCtx.getSharedPreferences("store", Context.MODE_PRIVATE)
    }

    private fun putStringSet(key: String, set: Set<String>) =
        prefs.edit().putStringSet(key, HashSet(set)).apply()

    /** 恢复到构建时注入的默认 AI 配置 */
    fun resetAi() {
        prefs.edit().remove("baseUrl").remove("apiKey").remove("model").apply()
    }

    /** AI 是否已可用：地址和模型已配置；https 接口还要求有 key（本地 Ollama 可无 key） */
    val aiReady: Boolean
        get() = baseUrl.isNotBlank() && model.isNotBlank() &&
            (!baseUrl.startsWith("https") || apiKey.isNotBlank())

    var plan: List<DayPlan>?
        get() = prefs.getString("plan", null)?.let { if (it.isEmpty()) null else dayPlansFromJson(it) }
        set(v) {
            val json = v?.let { p ->
                JSONObject().put("days", JSONArray(p.map { day ->
                    JSONObject()
                        .put("label", day.label)
                        .put("meals", JSONArray(day.meals.map { meal ->
                            JSONObject()
                                .put("type", meal.type)
                                .put("dishes", JSONArray(meal.dishes.map { d ->
                                    JSONObject()
                                        .put("name", d.name)
                                        .put("ingredients", JSONArray(d.ingredients))
                                }))
                        }))
                })).toString()
            } ?: ""
            prefs.edit().putString("plan", json).apply()
        }

    var selected: Set<String>
        get() = HashSet(prefs.getStringSet("selected", emptySet()) ?: emptySet())
        set(v) = putStringSet("selected", v)

    var checked: Set<String>
        get() = HashSet(prefs.getStringSet("checked", emptySet()) ?: emptySet())
        set(v) = putStringSet("checked", v)

    var baseUrl: String
        get() = prefs.getString("baseUrl", null) ?: appCtx.getString(R.string.default_base_url)
        set(v) = prefs.edit().putString("baseUrl", v.trim()).apply()

    var apiKey: String
        get() = prefs.getString("apiKey", null) ?: appCtx.getString(R.string.default_api_key)
        set(v) = prefs.edit().putString("apiKey", v.trim()).apply()

    var model: String
        get() = prefs.getString("model", null) ?: appCtx.getString(R.string.default_model)
        set(v) = prefs.edit().putString("model", v.trim()).apply()

    var days: Int
        get() = prefs.getInt("days", 3)
        set(v) = prefs.edit().putInt("days", v).apply()

    var people: Int
        get() = prefs.getInt("people", 2)
        set(v) = prefs.edit().putInt("people", v).apply()

    var pref: String
        get() = prefs.getString("pref", "") ?: ""
        set(v) = prefs.edit().putString("pref", v).apply()

    var members: List<Member>
        get() {
            val s = prefs.getString("members", null) ?: return emptyList()
            return try {
                val arr = JSONArray(s)
                (0 until arr.length()).map {
                    Member(arr.getJSONObject(it).optString("name"), arr.getJSONObject(it).optString("pref"))
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
        set(v) {
            val arr = JSONArray(v.map { JSONObject().put("name", it.name).put("pref", it.pref) })
            prefs.edit().putString("members", arr.toString()).apply()
        }
}
