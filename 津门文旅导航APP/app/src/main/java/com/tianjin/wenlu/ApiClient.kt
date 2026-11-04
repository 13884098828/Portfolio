package com.tianjin.wenlu

import com.tianjin.wenlu.data.ApiService
import com.tianjin.wenlu.data.SessionManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit 单例。服务器地址可在设置中修改（默认指向开发电脑局域网 172.16.0.138:8000）。
 */
object ApiClient {
    var baseUrl: String = SessionManager.serverUrl
        set(value) {
            field = if (value.endsWith("/")) value else "$value/"
            _retrofit = null
        }

    @Volatile
    private var _retrofit: Retrofit? = null

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val token = SessionManager.token
                val request = if (token.isNullOrBlank()) {
                    chain.request()
                } else {
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                }
                chain.proceed(request)
            }
            .build()
    }

    val retrofit: Retrofit
        get() {
            _retrofit?.let { return it }
            return synchronized(this) {
                _retrofit ?: Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .also { _retrofit = it }
            }
        }

    val api: ApiService
        get() = retrofit.create(ApiService::class.java)
}
