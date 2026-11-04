package com.tianjin.wenlu.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tianjin.wenlu.data.offline.LocalUser

/** 离线模式下注册的新账号（演示用，存本地） */
object LocalUserStore {
    private const val PREFS = "wenlu_local_users"
    private const val KEY = "users"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun all(): List<LocalUser> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<LocalUser>>() {}.type
            gson.fromJson<List<LocalUser>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun findByUsername(username: String): LocalUser? =
        all().firstOrNull { it.username == username }

    /**
     * 下一个可用用户 id。
     * [reservedMax] 传内置演示用户的最大 id —— 只按内置用户池算 max（原来的做法），
     * 会让第 2 个及以后注册的账号全部拿到同一个 id，行为数据、画像、路线随之串号。
     */
    fun nextId(reservedMax: Int = 0): Int {
        val localMax = all().maxOfOrNull { it.id } ?: 0
        return maxOf(reservedMax, localMax) + 1
    }

    /** 写入用户；id 已被占用时自动换号，保证 id 唯一 */
    fun add(user: LocalUser): LocalUser {
        val list = all().toMutableList()
        val saved = if (list.any { it.id == user.id }) user.copy(id = nextId()) else user
        list.add(saved)
        prefs.edit().putString(KEY, gson.toJson(list)).apply()
        return saved
    }
}
