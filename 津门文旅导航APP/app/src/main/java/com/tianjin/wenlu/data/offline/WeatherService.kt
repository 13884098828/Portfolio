package com.tianjin.wenlu.data.offline

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 实时天气预报。数据源：Open-Meteo（免费、无需注册、无需 API Key、非商业无调用限额）。
 *
 * 三级降级链，保证「纯离线」这个卖点不被破坏：
 *   1. 手动覆盖：用户在界面选定的天气，优先级最高（比赛现场没公网也能演示）
 *   2. 在线拉取成功：按 (坐标分桶, 日期) 缓存进 SharedPreferences
 *   3. 拉取失败：读缓存；连缓存都没有 → 回落「晴」
 *
 * 坐标按 0.5°（约 55km）分桶：市区 30 个景点落进同一个桶，保证首页驾驶舱与详情页
 * 拿到的是同一个天气（否则又是一处"两个页面数字打架"）；蓟州山区（盘山 / 黄崖关 / 梨木台）
 * 纬度 40° 上下，自成另一桶，山区天气不会污染市区。
 */
object WeatherService {
    private const val PREFS = "wenlu_weather"
    private const val ENDPOINT = "https://api.open-meteo.com/v1/forecast"
    private const val KEY_LAST_SYNC = "last_sync"

    /** 天津市中心，驾驶舱这类聚合视图用 */
    const val CITY_LAT = 39.13
    const val CITY_LNG = 117.20

    /** 界面手动可选的天气，与 Prediction.weatherFactor 的键保持一致 */
    val OPTIONS = listOf("晴", "多云", "阴", "小雨", "中雨", "大雨")

    @Volatile private var appContext: Context? = null
    @Volatile private var manual: String? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** 手动指定天气；传 null 恢复自动（实时 → 缓存 → 默认晴）。 */
    fun setManual(weather: String?) {
        manual = weather
    }

    fun manualOrNull(): String? = manual

    /** WMO 天气码 → 本项目天气因子使用的中文名 */
    private fun codeToWeather(code: Int): String = when (code) {
        0, 1 -> "晴"
        2 -> "多云"
        3, 45, 48 -> "阴"
        51, 53, 55, 56, 57, 61 -> "小雨"
        63, 66, 67, 80, 81, 71, 73, 75, 77, 85, 86 -> "中雨"
        65, 82, 95, 96, 99 -> "大雨"
        else -> "晴"
    }

    private fun prefs() = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun bucket(lat: Double, lng: Double): String = String.format(
        Locale.ROOT, "%.1f,%.1f",
        (lat * 2).roundToInt() / 2.0,
        (lng * 2).roundToInt() / 2.0,
    )

    private fun key(lat: Double, lng: Double, date: LocalDate) = "${bucket(lat, lng)}|$date"

    /** 两个坐标是否落在同一个天气分桶（用于避免重复拉取） */
    fun sameBucket(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Boolean =
        bucket(lat1, lng1) == bucket(lat2, lng2)

    /**
     * 取「当前生效」的天气，**不联网**，可以在主线程调用。
     * @return 天气名 to 来源（手动 / 实时 / 缓存 / 默认）
     */
    fun current(
        lat: Double = CITY_LAT,
        lng: Double = CITY_LNG,
        date: LocalDate = LocalDate.now().plusDays(1),
    ): Pair<String, String> {
        manual?.let { return it to "手动" }
        val p = prefs() ?: return "晴" to "默认"
        val b = bucket(lat, lng)
        val exact = p.getString("$b|$date", null)
        // 该日期没有缓存时（例如预报窗口刚够不到第 3 天），在同一个区域里取最接近的已缓存日期，
        // 而不是直接跳回"晴"——否则同一排预测卡片会出现前两天下雨、第三天晴天的自相矛盾。
        val hit = exact ?: (0L..6L).asSequence()
            .flatMap { d -> sequenceOf(date.plusDays(d), date.minusDays(d)) }
            .mapNotNull { p.getString("$b|$it", null) }
            .firstOrNull()
            ?: return "晴" to "默认"
        val fresh = exact != null && p.getString(KEY_LAST_SYNC, null) == LocalDate.now().toString()
        return hit to if (fresh) "实时" else "缓存"
    }

    /** 上一次成功拉取的时间（yyyy-MM-dd），供界面显示数据新鲜度 */
    fun lastSync(): String? = prefs()?.getString(KEY_LAST_SYNC, null)

    /**
     * 拉取未来 3 天预报并写入缓存。**会联网，必须在 IO 线程调用**（主线程调用会抛 NetworkOnMainThreadException）。
     * @return true 表示拉取并写入成功；false 表示网络/解析失败（调用方应继续用 current() 的降级结果）
     */
    fun refresh(lat: Double, lng: Double): Boolean {
        val ctx = appContext ?: return false
        return try {
            val url = URL(
                "$ENDPOINT?latitude=" + String.format(Locale.ROOT, "%.4f", lat) +
                    "&longitude=" + String.format(Locale.ROOT, "%.4f", lng) +
                    "&daily=weather_code&timezone=Asia%2FShanghai&forecast_days=5"
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
            }
            val text = try {
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }

            val daily = JSONObject(text).getJSONObject("daily")
            val times = daily.getJSONArray("time")
            val codes = daily.getJSONArray("weather_code")
            val ed = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            for (i in 0 until minOf(times.length(), codes.length())) {
                val d = LocalDate.parse(times.getString(i))
                ed.putString(key(lat, lng, d), codeToWeather(codes.getInt(i)))
            }
            ed.putString(KEY_LAST_SYNC, LocalDate.now().toString())
            // 这里用 commit() 而非 apply()：本函数已在 IO 线程，同步落盘才能保证
            // 拉取成功后界面立刻读到新值，不会出现"刷新了但数字没变"。
            ed.commit()
            true
        } catch (e: Exception) {
            false
        }
    }
}
