package com.tianjin.wenlu.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

/**
 * 本地会话与配置存储（Token / 用户信息 / 服务器地址）。
 * MVP 使用 SharedPreferences 同步读写，便于请求拦截器直接取用。
 */
object SessionManager {
    private const val PREFS = "wenlu_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER = "user_json"
    private const val KEY_SERVER = "server_url"

    // 当前开发电脑的局域网地址：手机与电脑连同一 WiFi 时可直接访问。
    // 若路由器重新分配 IP，请在 App 的“服务器设置”中改成新的电脑 IP。
    const val DEFAULT_SERVER_URL = "http://172.16.0.138:8000/api/"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_TOKEN, value).apply()
        }

    val isLoggedIn: Boolean
        get() = !token.isNullOrBlank()

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) {
            val normalized = if (value.endsWith("/")) value else "$value/"
            prefs.edit().putString(KEY_SERVER, normalized).apply()
        }

    fun saveLogin(token: String, user: User) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER, gson.toJson(user))
            .apply()
    }

    fun currentUser(): User? {
        val json = prefs.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(json, User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USER).apply()
    }
}
