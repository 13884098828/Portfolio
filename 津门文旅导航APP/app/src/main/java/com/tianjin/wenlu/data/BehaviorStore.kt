package com.tianjin.wenlu.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tianjin.wenlu.data.offline.LocalBehavior
import com.tianjin.wenlu.data.offline.OfflineData

/**
 * 本地行为记录：离线模式下记录浏览/收藏/搜索等行为，供画像与推荐使用。
 */
object BehaviorStore {
    private const val PREFS = "wenlu_behavior"
    private const val KEY = "behaviors"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun record(userId: Int, scenicId: Int, action: String) {
        val list = local().toMutableList()
        list.add(LocalBehavior(userId, scenicId, action))
        prefs.edit().putString(KEY, gson.toJson(list)).apply()
    }

    fun local(): List<LocalBehavior> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<LocalBehavior>>() {}.type
            gson.fromJson<List<LocalBehavior>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 内置种子行为 + 本地新行为（按用户过滤） */
    fun allFor(userId: Int): List<LocalBehavior> =
        OfflineData.behaviors.filter { it.userId == userId } + local().filter { it.userId == userId }
}
