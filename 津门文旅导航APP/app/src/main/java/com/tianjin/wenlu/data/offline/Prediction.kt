package com.tianjin.wenlu.data.offline

import com.tianjin.wenlu.data.ForecastItem
import com.tianjin.wenlu.data.Scenic
import java.time.LocalDate
import kotlin.random.Random

/**
 * 景区客流预测（Kotlin 移植）：星期系数 × 节假日 × 天气规则模型。
 *
 * 客流等级按「预测客流 / 该景点日承载量」的占用率判定。
 * 原来用的是固定绝对阈值（>30000 爆满 / >16000 高 / >8000 中），而预测值上限只有约 1.8 万，
 * 于是"爆满"几乎永不出现；更要命的是每个景点的档位全年恒定——等于给景点发了一张终身标签，
 * 等级不再随时间变化。改成占用率后：工作日 0 个爆满 / 周末 5 个 / 国庆 10 个，随日期真实起伏。
 */
object Prediction {
    private const val weekendFactor = 1.25
    private const val holidayFactor = 1.5
    private val weatherFactor = mapOf(
        "晴" to 1.1, "多云" to 1.0, "阴" to 0.95,
        "小雨" to 0.85, "中雨" to 0.7, "大雨" to 0.55,
    )

    /** 占用率分档线 */
    private const val OVER_CROWDED = 1.00   // 预测客流达到承载量 → 爆满
    private const val HIGH = 0.75
    private const val MEDIUM = 0.45

    /**
     * 法定节假日休假区间（公历闭区间）。春节 / 端午 / 中秋属农历节日，已换算为对应年份的公历：
     * 2026 春节 02-17（除夕 02-16）、端午 06-19、中秋 09-25；2027 春节 02-06（除夕 02-05）。
     * 按完整 yyyy-MM-dd 比较 —— 原来只比 "MM-dd"，跨年后 2026-01-01 这类日期会被 2025 年的表误命中。
     * 每年放假通知发布后更新一次即可。
     */
    private val holidayRanges = listOf(
        "2026-01-01" to "2026-01-03",   // 元旦
        "2026-02-16" to "2026-02-22",   // 春节（除夕 ~ 初六）
        "2026-04-04" to "2026-04-06",   // 清明
        "2026-05-01" to "2026-05-05",   // 劳动节
        "2026-06-19" to "2026-06-21",   // 端午
        "2026-09-25" to "2026-09-27",   // 中秋
        "2026-10-01" to "2026-10-07",   // 国庆
        "2027-01-01" to "2027-01-03",   // 元旦
        "2027-02-05" to "2027-02-11",   // 春节（除夕 ~ 初六）
    )

    /** 是否处于法定节假日 */
    private fun isHoliday(d: LocalDate): Boolean {
        val s = d.toString()
        return holidayRanges.any { (start, end) -> s >= start && s <= end }
    }

    private val weekdayCn = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    /**
     * 占用率 → 客流等级。
     * capacity <= 0 表示老数据缺承载量字段，此时退回原来的绝对阈值，不会崩。
     */
    fun levelOf(pred: Int, capacity: Int): String {
        if (capacity <= 0) {
            return when {
                pred > 16000 -> "高"
                pred > 8000 -> "中"
                else -> "低"
            }
        }
        val occ = pred.toDouble() / capacity
        return when {
            occ > OVER_CROWDED -> "爆满"
            occ > HIGH -> "高"
            occ > MEDIUM -> "中"
            else -> "低"
        }
    }

    fun forecast(
        scenic: Scenic,
        days: Int,
        weather: String = "晴",
        weatherSource: String = "默认",
    ): List<ForecastItem> {
        val base = scenic.heat / 30.0 * 10
        // 随机种子与 days 解耦：forecast(s, 1) 与 forecast(s, 3) 的第 1 天取到同一个样本，
        // 首页"明日客流"与详情页"未来 3 天"的第 1 天不会再给出两个互相矛盾的数字。
        val random = Random(scenic.id * 1000 + 7)
        val today = LocalDate.now()
        val capacity = scenic.capacity
        return (1..days).map { i ->
            val d = today.plusDays(i.toLong())
            var factor = 1.0
            if (d.dayOfWeek.value >= 6) factor *= weekendFactor
            if (isHoliday(d)) factor *= holidayFactor
            factor *= weatherFactor[weather] ?: 1.0
            val pred = (base * factor * random.nextDouble(0.92, 1.08)).toInt()
            ForecastItem(
                date = d.toString(),
                weekday = weekdayCn[d.dayOfWeek.value - 1],
                predictNum = pred,
                crowdLevel = levelOf(pred, capacity),
                capacity = capacity,
                occupancy = if (capacity > 0) pred.toDouble() / capacity else 0.0,
                weather = weather,
                weatherSource = weatherSource,
            )
        }
    }

    /** 单日预测（安全预警 / 驾驶舱用） */
    fun forecastOne(
        scenic: Scenic,
        weather: String = "晴",
        weatherSource: String = "默认",
    ): ForecastItem? = forecast(scenic, 1, weather, weatherSource).firstOrNull()

    /** 近 7 天历史客流（驾驶舱趋势用，按星期波动规则生成） */
    fun history7(scenic: Scenic): List<Pair<LocalDate, Int>> {
        val base = scenic.heat / 30.0 * 10
        val random = Random(scenic.id * 1000 + 42)
        val today = LocalDate.now()
        return (6 downTo 0).map { i ->
            val d = today.minusDays(i.toLong())
            val weekend = if (d.dayOfWeek.value >= 6) 1.3 else 1.0
            val visitors = (base * weekend * random.nextDouble(0.85, 1.15)).toInt()
            d to visitors
        }
    }
}
