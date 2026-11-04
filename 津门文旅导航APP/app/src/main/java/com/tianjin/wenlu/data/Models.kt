package com.tianjin.wenlu.data

import com.google.gson.annotations.SerializedName

// ---------- 用户与认证 ----------
data class User(
    val id: Int,
    val username: String,
    val nickname: String,
    val age: Int? = null,
    val gender: String? = null,
    val city: String? = null,
    @SerializedName("consumption_level") val consumptionLevel: String = "中等",
)

data class LoginRequest(
    val username: String,
    val password: String,
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val nickname: String? = null,
    val city: String? = null,
)

data class LoginResponse(
    val token: String,
    val user: User,
)

// ---------- 景点 ----------
data class Scenic(
    val id: Int,
    val name: String,
    val category: String,
    val longitude: Double,
    val latitude: Double,
    val score: Double,
    val heat: Int,
    /**
     * 日承载量（人）。客流等级按「预测客流 / 承载量」的占用率判定，
     * 而不是拿绝对客流数字卡死阈值——否则每个景点的档位常年恒定，不随时间变化。
     */
    val capacity: Int = 0,
    @SerializedName("ticket_price") val ticketPrice: Double,
    val address: String = "",
    val intro: String = "",
    val tags: List<String> = emptyList(),
    @SerializedName("duration_minutes") val durationMinutes: Int = 120,
    @SerializedName("open_time") val openTime: String = "",
)

data class FavoriteItem(
    @SerializedName("scenic_id") val scenicId: Int,
    val name: String,
    val category: String,
    val longitude: Double = 0.0,
    val latitude: Double = 0.0,
    val score: Double = 0.0,
    val heat: Int = 0,
    @SerializedName("ticket_price") val ticketPrice: Double = 0.0,
    val address: String = "",
    val intro: String = "",
    val tags: List<String> = emptyList(),
    @SerializedName("duration_minutes") val durationMinutes: Int = 120,
    @SerializedName("open_time") val openTime: String = "",
) {
    fun toScenic() = Scenic(
        id = scenicId, name = name, category = category,
        longitude = longitude, latitude = latitude, score = score, heat = heat,
        ticketPrice = ticketPrice, address = address, intro = intro, tags = tags,
        durationMinutes = durationMinutes, openTime = openTime,
    )
}

data class FavoriteActionResponse(
    val message: String = "",
    @SerializedName("scenic_id") val scenicId: Int? = null,
)

data class MessageResponse(
    val message: String = "",
)

data class HealthResponse(
    val status: String = "",
    val service: String = "",
)

// ---------- 路线规划 ----------
data class RoutePlanRequest(
    val days: Int,
    val people: Int,
    val budget: Double,
    val transport: String,
    val interests: List<String>,
    @SerializedName("has_elder") val hasElder: Boolean = false,
    @SerializedName("hotel_near") val hotelNear: String = "",
    @SerializedName("start_scenic") val startScenic: String? = null,
    @SerializedName("use_genetic") val useGenetic: Boolean = true,
)

data class RouteDay(
    val day: Int,
    val spots: List<Scenic>,
    @SerializedName("distance_km") val distanceKm: Double,
    @SerializedName("duration_hours") val durationHours: Double,
    val cost: Double,
)

data class RoutePlanResult(
    val days: List<RouteDay>,
    @SerializedName("total_distance_km") val totalDistanceKm: Double,
    @SerializedName("total_cost") val totalCost: Double,
    @SerializedName("total_duration_hours") val totalDurationHours: Double,
    val algorithm: String,
    val notes: List<String> = emptyList(),
)

data class SaveRouteResponse(
    val id: Int,
    val message: String,
)

data class RouteSpot(
    val day: Int,
    val order: Int,
    @SerializedName("scenic_id") val scenicId: Int,
    val name: String,
)

data class RouteRecord(
    val id: Int,
    /** 归属用户。历史数据无此字段时反序列化为 0（不归属任何账号），不会串号 */
    @SerializedName("user_id") val userId: Int = 0,
    val title: String,
    val days: Int,
    @SerializedName("distance_km") val distanceKm: Double,
    val cost: Double,
    val spots: List<RouteSpot> = emptyList(),
    @SerializedName("created_at") val createdAt: String = "",
)

// ---------- 推荐与画像 ----------
data class RecommendItem(
    @SerializedName("scenic_id") val scenicId: Int,
    val name: String,
    val category: String,
    val tags: List<String> = emptyList(),
    val score: Double,
    val heat: Int,
    @SerializedName("ticket_price") val ticketPrice: Double,
    val intro: String = "",
    val reason: String,
    @SerializedName("final_score") val finalScore: Double = 0.0,
)

data class RecommendResponse(
    @SerializedName("user_id") val userId: Int?,
    val mode: String,
    val items: List<RecommendItem>,
)

data class InterestWeight(
    val tag: String,
    val weight: Double,
)

data class Profile(
    @SerializedName("travel_type") val travelType: String,
    val interests: List<InterestWeight> = emptyList(),
    @SerializedName("consumption_level") val consumptionLevel: String,
    val summary: String = "",
    val similarity: Double = 0.0,
)

// ---------- 驾驶舱与安全 ----------
data class MetricCard(
    val label: String,
    val value: String,
    val trend: String = "",
)

data class Dashboard(
    val cards: List<MetricCard> = emptyList(),
)

data class SafetyWarning(
    @SerializedName("scenic_id") val scenicId: Int,
    @SerializedName("scenic_name") val scenicName: String,
    val date: String = "",
    @SerializedName("predict_num") val predictNum: Int,
    @SerializedName("crowd_level") val crowdLevel: String,
    val suggestion: String,
)

data class SafetyResponse(
    val count: Int,
    val warnings: List<SafetyWarning> = emptyList(),
)

// ---------- 客流预测 ----------
data class ForecastItem(
    val date: String,
    val weekday: String,
    @SerializedName("predict_num") val predictNum: Int,
    @SerializedName("crowd_level") val crowdLevel: String,
    /** 该景点日承载量；0 表示数据缺失，此时退回绝对阈值判定 */
    val capacity: Int = 0,
    /** 占用率 = 预测客流 / 承载量 */
    val occupancy: Double = 0.0,
    /** 本次预测实际采用的天气 */
    val weather: String = "晴",
    /** 天气来源：实时 / 缓存 / 手动 / 默认 */
    val weatherSource: String = "默认",
)

data class PredictResult(
    @SerializedName("scenic_id") val scenicId: Int,
    @SerializedName("scenic_name") val scenicName: String,
    val forecast: List<ForecastItem> = emptyList(),
)

/** 当前生效的天气与来源，供界面标注「按 XX 天气估算」 */
data class WeatherState(
    val weather: String,
    val source: String,
    val manual: String? = null,
)

// ---------- 评论分析 ----------
data class CommentSample(
    val content: String,
    val sentiment: String,
    val rating: Int,
)

data class CommentAnalysis(
    @SerializedName("scenic_id") val scenicId: Int,
    @SerializedName("scenic_name") val scenicName: String,
    val total: Int,
    val positive: Int,
    val neutral: Int,
    val negative: Int,
    val satisfaction: Int,
    @SerializedName("positive_keywords") val positiveKeywords: List<String> = emptyList(),
    @SerializedName("negative_keywords") val negativeKeywords: List<String> = emptyList(),
    @SerializedName("sample_comments") val sampleComments: List<CommentSample> = emptyList(),
)

// ---------- 预算 ----------
data class BudgetRequest(
    val days: Int,
    val people: Int,
    val budget: Double,
    val transport: String,
    @SerializedName("hotel_level") val hotelLevel: String,
)

data class BudgetResult(
    @SerializedName("transport_cost") val transportCost: Double,
    @SerializedName("ticket_cost") val ticketCost: Double,
    @SerializedName("food_cost") val foodCost: Double,
    @SerializedName("hotel_cost") val hotelCost: Double,
    @SerializedName("total_cost") val totalCost: Double,
    @SerializedName("within_budget") val withinBudget: Boolean,
    val tips: List<String> = emptyList(),
)

// ---------- 行为 ----------
data class BehaviorRequest(
    @SerializedName("scenic_id") val scenicId: Int,
    val action: String,
)

data class BehaviorResponse(
    val ok: Boolean,
)
