package com.tianjin.wenlu.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** 离线模式下的"我的路线"本地存储 */
object RouteStore {
    private const val PREFS = "wenlu_routes"
    private const val KEY = "routes"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    /** 读取全部记录（不按用户过滤）。写回时必须用它，否则会把其他账号的路线一起覆盖掉 */
    private fun raw(): List<RouteRecord> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<RouteRecord>>() {}.type
            gson.fromJson<List<RouteRecord>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(userId: Int, result: RoutePlanResult): SaveRouteResponse {
        val list = raw().toMutableList()
        val spots = result.days.flatMap { d ->
            d.spots.mapIndexed { i, s -> RouteSpot(d.day, i, s.id, s.name) }
        }
        // id 在全部记录范围内递增；若按"当前账号的记录"算 max，不同账号会拿到重复 id
        val record = RouteRecord(
            id = (list.maxOfOrNull { it.id } ?: 0) + 1,
            userId = userId,
            title = "${result.totalDurationHours.toInt()}小时 ${result.totalDistanceKm}km 行程",
            days = result.days.size,
            distanceKm = result.totalDistanceKm,
            cost = result.totalCost,
            spots = spots,
            createdAt = java.time.LocalDate.now().toString(),
        )
        list.add(0, record)
        prefs.edit().putString(KEY, gson.toJson(list)).apply()
        return SaveRouteResponse(record.id, "路线已保存")
    }

    /** 只返回该用户自己的路线 */
    fun all(userId: Int): List<RouteRecord> = raw().filter { it.userId == userId }
}
