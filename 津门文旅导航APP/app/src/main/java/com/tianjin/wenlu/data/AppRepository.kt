package com.tianjin.wenlu.data

import com.tianjin.wenlu.data.offline.LocalUser
import com.tianjin.wenlu.data.offline.OfflineData
import com.tianjin.wenlu.data.offline.Prediction
import com.tianjin.wenlu.data.offline.Recommender
import com.tianjin.wenlu.data.offline.RoutePlanner
import com.tianjin.wenlu.data.offline.Sentiment
import com.tianjin.wenlu.data.offline.WeatherService
import com.tianjin.wenlu.data.offline.hashPassword
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * 纯离线数据仓库：景点、评论、用户、路线规划与全部算法均来自内置数据，不请求后端。
 */
object AppRepository {

    // ---------- 用户 ----------
    fun login(body: LoginRequest): LoginResponse {
        val user = OfflineData.users.firstOrNull { it.username == body.username }
            ?: LocalUserStore.findByUsername(body.username)
        if (user == null || user.passwordHash != hashPassword(body.password)) {
            throw IllegalArgumentException("用户名或密码错误")
        }
        val u = User(
            id = user.id, username = user.username, nickname = user.nickname,
            age = user.age, gender = user.gender, city = user.city,
            consumptionLevel = user.consumptionLevel,
        )
        return LoginResponse(token = "offline", user = u)
    }

    fun register(body: RegisterRequest): LoginResponse {
        if (OfflineData.users.any { it.username == body.username } ||
            LocalUserStore.findByUsername(body.username) != null
        ) {
            throw IllegalArgumentException("用户名已存在")
        }
        // 内置用户与已注册用户共同占号：只按内置用户算 max，会让第 2 个起的注册账号都拿到同一个 id
        val reservedMax = OfflineData.users.maxOfOrNull { it.id } ?: 0
        val user = LocalUser(
            id = LocalUserStore.nextId(reservedMax), username = body.username,
            passwordHash = hashPassword(body.password),
            nickname = body.nickname ?: body.username,
            city = "天津", consumptionLevel = "中等",
        )
        val saved = LocalUserStore.add(user)
        return LoginResponse(
            token = "offline",
            user = User(
                saved.id, saved.username, saved.nickname,
                null, null, saved.city, saved.consumptionLevel,
            ),
        )
    }

    // ---------- 景点 ----------
    fun categories(): List<String> =
        OfflineData.scenics.map { it.category }.distinct()

    fun hotScenics(n: Int): List<Scenic> =
        OfflineData.scenics.sortedByDescending { it.heat }.take(n)

    fun scenicDetail(id: Int): Scenic =
        OfflineData.scenicById(id) ?: throw IllegalArgumentException("景点不存在")

    fun scenics(keyword: String = "", category: String = "", sort: String = "heat", limit: Int = 50): List<Scenic> {
        var list = OfflineData.scenics
        if (keyword.isNotBlank()) {
            list = list.filter { keyword in it.name || keyword in it.intro }
        }
        if (category.isNotBlank()) {
            list = list.filter { it.category == category }
        }
        list = when (sort) {
            "score" -> list.sortedWith(compareByDescending<Scenic> { it.score }.thenByDescending { it.heat })
            "price" -> list.sortedBy { it.ticketPrice }
            else -> list.sortedByDescending { it.heat }
        }
        return list.take(limit)
    }

    // ---------- 路线 ----------
    fun planRoute(body: RoutePlanRequest): RoutePlanResult = RoutePlanner.plan(body)

    fun saveRoute(result: RoutePlanResult): SaveRouteResponse {
        val userId = SessionManager.currentUser()?.id
            ?: throw IllegalStateException("请先登录")
        return RouteStore.save(userId, result)
    }

    fun myRoutes(): List<RouteRecord> {
        val userId = SessionManager.currentUser()?.id
            ?: throw IllegalStateException("请先登录")
        return RouteStore.all(userId)
    }

    // ---------- 推荐与画像 ----------
    fun recommend(limit: Int): RecommendResponse =
        Recommender.hybridRecommend(SessionManager.currentUser()?.id, limit)

    fun profile(): Profile {
        val userId = SessionManager.currentUser()?.id
            ?: throw IllegalStateException("请先登录")
        return Recommender.profileOf(userId)
    }

    // ---------- 分析 ----------
    fun predict(scenicId: Int, days: Int): PredictResult {
        val scenic = scenicDetail(scenicId)
        val (weather, source) = WeatherService.current(scenic.latitude, scenic.longitude)
        return PredictResult(scenic.id, scenic.name, Prediction.forecast(scenic, days, weather, source))
    }

    // ---------- 天气 ----------

    /** 当前生效的天气与来源。不联网，可在主线程调用。 */
    fun weatherState(): WeatherState {
        val (weather, source) = WeatherService.current()
        return WeatherState(weather, source, WeatherService.manualOrNull())
    }

    /** 手动指定天气；传 null 恢复自动（实时 → 缓存 → 默认晴）。 */
    fun setWeatherManual(weather: String?) {
        WeatherService.setManual(weather)
    }

    /**
     * 联网刷新实时天气。**内部会发 HTTP 请求，必须在 IO 线程调用。**
     * 先刷新市区，再看该景点是否落在另一个天气分桶（蓟州山区），是则额外拉一份。
     * @return true 表示至少一次拉取成功；false 时界面继续用降级结果即可，不要报错
     */
    fun refreshWeather(scenicId: Int? = null): Boolean {
        var ok = WeatherService.refresh(WeatherService.CITY_LAT, WeatherService.CITY_LNG)
        val s = scenicId?.let { OfflineData.scenicById(it) }
        if (s != null && !WeatherService.sameBucket(s.latitude, s.longitude, WeatherService.CITY_LAT, WeatherService.CITY_LNG)) {
            ok = WeatherService.refresh(s.latitude, s.longitude) || ok
        }
        return ok
    }

    fun comments(scenicId: Int): CommentAnalysis {
        val scenic = scenicDetail(scenicId)
        val list = OfflineData.comments.filter { it.scenicId == scenicId }
        return Sentiment.analyze(scenic, list)
    }

    fun safety(): SafetyResponse {
        val warnings = OfflineData.scenics.mapNotNull { s ->
            val (weather, source) = WeatherService.current(s.latitude, s.longitude)
            Prediction.forecastOne(s, weather, source)?.takeIf { it.crowdLevel in setOf("高", "爆满") }?.let { fc ->
                val occTxt = if (fc.capacity > 0) "，承载量占用 ${(fc.occupancy * 100).toInt()}%" else ""
                SafetyWarning(
                    scenicId = s.id, scenicName = s.name, date = fc.date,
                    predictNum = fc.predictNum, crowdLevel = fc.crowdLevel,
                    suggestion = "${s.name} 当前客流预计偏多（${fc.crowdLevel}$occTxt），建议错峰或选择周边替代景点",
                )
            }
        }
        return SafetyResponse(count = warnings.size, warnings = warnings)
    }

    /** 客流数字统一按"万人"保留一位小数，避免出现 23.899万人 这类原始小数 */
    private fun wan(n: Int): String = String.format(Locale.ROOT, "%.1f万人", n / 10000.0)

    /** 环比：负数自带负号（原来写成 "环比 +${abs(growth)}%"，下滑时会显示 +-3%） */
    private fun signedPercent(v: Int): String = if (v >= 0) "+$v%" else "$v%"

    fun dashboard(): Dashboard {
        val todayTotal = OfflineData.scenics.sumOf { Prediction.history7(it).last().second }
        val tomorrow = OfflineData.scenics.sumOf { s ->
            val (weather, source) = WeatherService.current(s.latitude, s.longitude)
            Prediction.forecastOne(s, weather, source)?.predictNum ?: 0
        }
        val hot = OfflineData.scenics.maxByOrNull { it.heat }
        val sat = Sentiment.overall()["satisfaction"] ?: 0
        val growth = if (todayTotal > 0) ((tomorrow - todayTotal) * 100.0 / todayTotal).toInt() else 0
        return Dashboard(
            cards = listOf(
                MetricCard("今日游客", wan(todayTotal), "环比 ${signedPercent(growth)}"),
                MetricCard("热门景区", hot?.name ?: "-", "热度 ${hot?.heat ?: 0}"),
                MetricCard("游客满意度", "$sat%", "基于游客评论分析"),
                MetricCard("明日客流预测", wan(tomorrow), "较今日 ${signedPercent(growth)}"),
            )
        )
    }

    // ---------- 预算 ----------
    fun budget(body: BudgetRequest): BudgetResult {
        val transportDaily = mapOf("地铁" to 8, "公交" to 10, "自驾" to 60, "打车" to 70, "步行" to 0)
        val foodDaily = mapOf("经济" to 75, "舒适" to 120, "高档" to 220)
        val hotelNight = mapOf("经济" to 120, "舒适" to 260, "高档" to 480)

        // 门票按内置景点数据的真实票价均值估算（每天 3 个收费景点）。
        // 原来用 ticketDaily[body.hotelLevel]，门票被住宿档次决定，且完全没用到 Scenic.ticketPrice。
        val avgTicket = OfflineData.scenics.map { it.ticketPrice }.average()
        val spotsPerDay = 3

        val transportCost = (transportDaily[body.transport] ?: 8) * body.people * body.days
        val ticketCost = (avgTicket * spotsPerDay * body.people * body.days).roundToInt()
        val foodCost = (foodDaily[body.hotelLevel] ?: 120) * body.people * body.days
        val rooms = ceil(body.people / 2.0).toInt()
        val hotelCost = (hotelNight[body.hotelLevel] ?: 260) * rooms * body.days
        val total = transportCost + ticketCost + foodCost + hotelCost
        val within = total <= body.budget

        val tips = mutableListOf<String>()
        if (within) {
            tips.add("预估总花费 $total 元，在预算 ${body.budget.toInt()} 元内，余 ${(body.budget - total).toInt()} 元")
            tips.add("餐饮按大众点评水平估算，实际可灵活调整")
        } else {
            tips.add("当前预估总花费 $total 元已超出预算 ${body.budget.toInt()} 元")
            // 已是经济型时不再给"降为经济型"的建议（原来会算出"可节省约 0 元"）
            val hotelSave = (hotelNight[body.hotelLevel] ?: 260) - 120
            if (hotelSave > 0) {
                tips.add("建议将住宿降为经济型，可节省约 $hotelSave 元/间/晚")
            }
            tips.add("减少收费景点数量或选择免费景点，可节省门票约 $ticketCost 元")
        }
        tips.add("使用地铁+步行可进一步控制交通成本")

        return BudgetResult(
            transportCost = transportCost.toDouble(), ticketCost = ticketCost.toDouble(),
            foodCost = foodCost.toDouble(), hotelCost = hotelCost.toDouble(),
            totalCost = total.toDouble(), withinBudget = within, tips = tips,
        )
    }

    // ---------- 行为与收藏 ----------
    private fun currentUserId(): Int? = SessionManager.currentUser()?.id

    fun behavior(body: BehaviorRequest) {
        val userId = currentUserId() ?: return
        BehaviorStore.record(userId, body.scenicId, body.action)
    }

    fun favorites(): List<Scenic> {
        val userId = currentUserId() ?: return emptyList()
        return FavoritesStore.all(userId)
    }

    fun isFavorite(scenicId: Int): Boolean {
        val userId = currentUserId() ?: return false
        return FavoritesStore.contains(userId, scenicId)
    }

    fun addFavorite(scenicId: Int) {
        val userId = currentUserId() ?: throw IllegalStateException("请先登录")
        val scenic = OfflineData.scenicById(scenicId)
            ?: throw IllegalArgumentException("景点不存在")
        if (!FavoritesStore.contains(userId, scenicId)) {
            FavoritesStore.toggle(userId, scenic)
        }
    }

    fun removeFavorite(scenicId: Int) {
        val userId = currentUserId() ?: return
        FavoritesStore.remove(userId, scenicId)
    }
}
