package com.tianjin.wenlu.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 本地收藏存储（阶段一实现）。后端收藏列表接口随阶段二 P-06 上线后可切换为服务端数据。
 *
 * 收藏按账号分开存：一个用户一个 SharedPreferences key。
 * 原来所有账号共用同一份 "favorites"，换个账号登录就会看到上一个账号的收藏。
 */
object FavoritesStore {
    private const val PREFS = "wenlu_favorites"
    private const val KEY_FAVORITES = "favorites"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    /** 每个账号一个 key，天然隔离 */
    private fun keyOf(userId: Int) = "${KEY_FAVORITES}_$userId"

    fun all(userId: Int): List<Scenic> {
        val json = prefs.getString(keyOf(userId), null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Scenic>>() {}.type
            gson.fromJson<List<Scenic>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun contains(userId: Int, scenicId: Int): Boolean = all(userId).any { it.id == scenicId }

    /** 收藏/取消收藏，返回操作后的收藏状态 */
    fun toggle(userId: Int, scenic: Scenic): Boolean {
        val list = all(userId).toMutableList()
        val exists = list.any { it.id == scenic.id }
        if (exists) {
            list.removeAll { it.id == scenic.id }
        } else {
            list.add(0, scenic)
        }
        prefs.edit().putString(keyOf(userId), gson.toJson(list)).apply()
        return !exists
    }

    fun remove(userId: Int, scenicId: Int) {
        val list = all(userId).filterNot { it.id == scenicId }
        prefs.edit().putString(keyOf(userId), gson.toJson(list)).apply()
    }
}
