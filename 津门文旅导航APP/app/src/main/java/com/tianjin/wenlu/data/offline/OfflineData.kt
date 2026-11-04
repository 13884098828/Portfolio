package com.tianjin.wenlu.data.offline

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.tianjin.wenlu.data.InterestWeight
import com.tianjin.wenlu.data.Scenic

/** 内置本地数据（由后端种子数据导出，与 Web 端一致） */
data class LocalComment(
    val id: Int,
    @SerializedName("scenic_id") val scenicId: Int,
    @SerializedName("user_id") val userId: Int?,
    val content: String,
    val sentiment: String,
    val rating: Int,
)

data class LocalUser(
    val id: Int,
    val username: String,
    @SerializedName("password_hash") val passwordHash: String,
    val nickname: String,
    val age: Int? = null,
    val gender: String? = null,
    val city: String? = null,
    @SerializedName("consumption_level") val consumptionLevel: String = "中等",
)

data class LocalProfile(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("travel_type") val travelType: String,
    val interests: List<InterestWeight>,
    @SerializedName("consumption_level") val consumptionLevel: String,
)

data class LocalBehavior(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("scenic_id") val scenicId: Int,
    val action: String,
)

data class LocalDataSet(
    val scenics: List<Scenic>,
    val comments: List<LocalComment>,
    val users: List<LocalUser>,
    val profiles: List<LocalProfile>,
    val behaviors: List<LocalBehavior>,
)

object OfflineData {
    lateinit var scenics: List<Scenic>
        private set
    lateinit var comments: List<LocalComment>
        private set
    lateinit var users: List<LocalUser>
        private set
    lateinit var profiles: List<LocalProfile>
        private set
    lateinit var behaviors: List<LocalBehavior>
        private set

    fun load(context: Context) {
        val json = context.assets.open("local_data.json").bufferedReader().use { it.readText() }
        val set = Gson().fromJson(json, LocalDataSet::class.java)
        scenics = set.scenics.sortedBy { it.id }
        comments = set.comments
        users = set.users
        profiles = set.profiles
        behaviors = set.behaviors
    }

    fun scenicById(id: Int): Scenic? = scenics.firstOrNull { it.id == id }
}

/** 与后端 security.hash_password 一致的 SHA-256 加盐哈希 */
fun hashPassword(password: String): String {
    val input = "tianjin:$password"
    val digest = java.security.MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}
