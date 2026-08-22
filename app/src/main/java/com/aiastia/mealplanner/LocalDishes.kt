package com.aiastia.mealplanner

/** 内置家常菜库：不配置 AI 也能随机出菜单，App 开箱即用 */
object LocalDishes {

    private val breakfast = listOf(
        Dish("小米粥", listOf("小米 100g", "红枣 3颗")),
        Dish("鸡蛋饼", listOf("鸡蛋 2个", "面粉 100g", "小葱 1根")),
        Dish("牛奶麦片", listOf("牛奶 250ml", "燕麦片 50g")),
        Dish("皮蛋瘦肉粥", listOf("大米 80g", "皮蛋 1个", "猪瘦肉 100g")),
        Dish("豆浆配包子", listOf("豆浆 300ml", "包子 4个")),
        Dish("三明治", listOf("吐司 3片", "鸡蛋 1个", "生菜 100g", "火腿 2片")),
        Dish("煮玉米配牛奶", listOf("玉米 1根", "牛奶 250ml")),
        Dish("蔬菜鸡蛋面", listOf("面条 100g", "鸡蛋 1个", "青菜 100g"))
    )

    private val mains = listOf(
        Dish("西红柿炒鸡蛋", listOf("西红柿 2个", "鸡蛋 3个", "小葱 1根")),
        Dish("青椒肉丝", listOf("猪里脊 200g", "青椒 2个")),
        Dish("红烧排骨", listOf("排骨 500g", "冰糖 20g", "生抽 15ml")),
        Dish("麻婆豆腐", listOf("豆腐 400g", "猪肉末 100g", "豆瓣酱 1勺")),
        Dish("清蒸鲈鱼", listOf("鲈鱼 1条", "姜 1块", "小葱 2根")),
        Dish("土豆烧牛腩", listOf("牛腩 400g", "土豆 2个")),
        Dish("蒜蓉西兰花", listOf("西兰花 1颗", "大蒜 3瓣")),
        Dish("酸辣土豆丝", listOf("土豆 2个", "干辣椒 3个")),
        Dish("可乐鸡翅", listOf("鸡翅 8个", "可乐 330ml")),
        Dish("白灼菜心", listOf("菜心 300g", "大蒜 2瓣")),
        Dish("宫保鸡丁", listOf("鸡胸肉 300g", "花生 50g", "黄瓜 1根")),
        Dish("番茄炖牛腩", listOf("牛腩 300g", "西红柿 2个")),
        Dish("蛋炒饭", listOf("米饭 2碗", "鸡蛋 2个", "火腿 50g")),
        Dish("香菇青菜", listOf("青菜 300g", "香菇 5朵")),
        Dish("红烧带鱼", listOf("带鱼 400g", "姜 1块")),
        Dish("回锅肉", listOf("五花肉 300g", "青椒 1个", "蒜苗 100g")),
        Dish("黄瓜炒虾仁", listOf("虾仁 200g", "黄瓜 1根")),
        Dish("韭菜炒蛋", listOf("韭菜 200g", "鸡蛋 3个"))
    )

    private val soups = listOf(
        Dish("紫菜蛋花汤", listOf("紫菜 10g", "鸡蛋 1个")),
        Dish("冬瓜排骨汤", listOf("排骨 300g", "冬瓜 400g")),
        Dish("西红柿鸡蛋汤", listOf("西红柿 1个", "鸡蛋 1个")),
        Dish("萝卜排骨汤", listOf("排骨 300g", "白萝卜 1根"))
    )

    fun randomPlan(days: Int, @Suppress("UNUSED_PARAMETER") people: Int): List<DayPlan> {
        val pool = mains.shuffled()
        var idx = 0
        fun next(): Dish = pool[(idx++) % pool.size]
        return (1..days).map { d ->
            DayPlan(
                label = "第${d}天",
                meals = listOf(
                    Meal("早餐", listOf(breakfast.random())),
                    Meal("午餐", listOf(next(), next(), soups.random())),
                    Meal("晚餐", listOf(next(), next()))
                )
            )
        }
    }
}
