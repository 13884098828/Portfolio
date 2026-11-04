package com.tianjin.wenlu.data.offline

import com.tianjin.wenlu.data.BehaviorStore
import com.tianjin.wenlu.data.InterestWeight
import com.tianjin.wenlu.data.Profile
import com.tianjin.wenlu.data.RecommendItem
import com.tianjin.wenlu.data.RecommendResponse
import kotlin.math.sqrt

/**
 * 游客画像 + 混合推荐（Kotlin 移植）：内容匹配 + 协同过滤 + 热度。
 */
object Recommender {
    val travelTypes = mapOf(
        "文化探索型" to listOf("历史", "建筑", "文化", "民俗", "古建"),
        "亲子旅游型" to listOf("亲子", "科教", "游乐", "自然"),
        "年轻潮玩型" to listOf("刺激", "网红", "游乐", "夜景"),
        "摄影打卡型" to listOf("拍照", "夜景", "网红", "建筑"),
        "休闲度假型" to listOf("放松", "自然", "休闲", "夜景"),
    )
    private val actionWeight = mapOf("search" to 1.0, "view" to 1.5, "favorite" to 3.0, "route" to 2.0, "comment" to 2.5)

    fun userInterestVector(userId: Int): Map<String, Double> {
        val vec = mutableMapOf<String, Double>()
        OfflineData.profiles.firstOrNull { it.userId == userId }?.interests?.forEach {
            vec[it.tag] = (vec[it.tag] ?: 0.0) + it.weight
        }
        BehaviorStore.allFor(userId).forEach { beh ->
            val w = actionWeight[beh.action] ?: 1.0
            OfflineData.scenicById(beh.scenicId)?.tags?.forEach { tag ->
                vec[tag] = (vec[tag] ?: 0.0) + w * 0.3
            }
        }
        return vec
    }

    fun classifyTravelType(vec: Map<String, Double>): Pair<String, Double> {
        if (vec.isEmpty()) return "休闲度假型" to 0.0
        var bestType = "休闲度假型"
        var bestSim = -1.0
        travelTypes.forEach { (name, tags) ->
            val tv = tags.associateWith { 1.0 }
            val num = (vec.keys + tv.keys).sumOf { vec[it]?.times(tv[it] ?: 0.0) ?: 0.0 }
            val d1 = sqrt(vec.values.sumOf { it * it }).takeIf { it > 0 } ?: 1.0
            val d2 = sqrt(tv.values.sumOf { it * it }).takeIf { it > 0 } ?: 1.0
            val sim = num / (d1 * d2)
            if (sim > bestSim) {
                bestSim = sim
                bestType = name
            }
        }
        return bestType to bestSim
    }

    private fun similarUsers(userId: Int, top: Int = 5): List<Int> {
        val mine = BehaviorStore.allFor(userId).map { it.scenicId }.toSet()
        if (mine.isEmpty()) return emptyList()
        val scores = mutableMapOf<Int, Double>()
        OfflineData.users.filter { it.id != userId }.forEach { other ->
            val theirs = BehaviorStore.allFor(other.id).map { it.scenicId }.toSet()
            val inter = (mine intersect theirs).size
            val union = (mine union theirs).size
            if (union > 0) scores[other.id] = inter.toDouble() / union
        }
        return scores.entries.sortedByDescending { it.value }.take(top).map { it.key }
    }

    fun hybridRecommend(userId: Int?, limit: Int): RecommendResponse {
        val scenics = OfflineData.scenics
        val maxHeat = scenics.maxOf { it.heat }.coerceAtLeast(1)
        val vec = if (userId != null) userInterestVector(userId) else emptyMap()
        val simIds = if (userId != null) similarUsers(userId) else emptyList()
        val mine = if (userId != null) BehaviorStore.allFor(userId).map { it.scenicId }.toSet() else emptySet()

        val collab = mutableMapOf<Int, Double>()
        simIds.forEach { uid ->
            BehaviorStore.allFor(uid).forEach { b ->
                if (b.scenicId !in mine) {
                    collab[b.scenicId] = (collab[b.scenicId] ?: 0.0) + (actionWeight[b.action] ?: 1.0)
                }
            }
        }

        val items = scenics.map { s ->
            val content = if (vec.isEmpty()) 0.0 else {
                val hits = s.tags.sumOf { vec[it] ?: 0.0 }
                hits / (vec.values.sum().takeIf { it > 0 } ?: 1.0)
            }
            val collabScore = minOf(1.0, (collab[s.id] ?: 0.0) / 4.0)
            val hot = s.heat.toDouble() / maxHeat
            val finalScore = 0.5 * content + 0.3 * collabScore + 0.2 * hot
            val reason = buildString {
                if (content > 0.15) append("符合你的兴趣（${s.tags.take(2).joinToString("/")}）")
                if (collabScore > 0.15) append(if (isNotEmpty()) "、" else "").append("兴趣相近的游客也在关注")
                if (hot > 0.6) append(if (isNotEmpty()) "、" else "").append("当前热门景点")
                if (isEmpty()) append("综合评分较高")
            }
            RecommendItem(
                scenicId = s.id, name = s.name, category = s.category, tags = s.tags,
                score = s.score, heat = s.heat, ticketPrice = s.ticketPrice, intro = s.intro,
                reason = reason, finalScore = finalScore,
            )
        }.sortedByDescending { it.finalScore }.take(limit)

        return RecommendResponse(
            userId = userId,
            mode = if (userId != null) "混合推荐" else "热度推荐",
            items = items,
        )
    }

    fun profileOf(userId: Int): Profile {
        val vec = userInterestVector(userId)
        val (type, sim) = classifyTravelType(vec)
        val profile = OfflineData.profiles.firstOrNull { it.userId == userId }
        val interests = vec.entries.sortedByDescending { it.value }.take(6)
            .map { InterestWeight(it.key, (it.value * 100).toInt() / 100.0) }
        val summaries = mapOf(
            "文化探索型" to "偏好历史文化与建筑景观，推荐深度游览类路线",
            "亲子旅游型" to "适合亲子出行，关注科普场馆与主题乐园",
            "年轻潮玩型" to "追求新鲜刺激，热衷网红打卡与娱乐项目",
            "摄影打卡型" to "注重取景与出片，优先推荐地标与网红机位",
            "休闲度假型" to "偏好轻松慢游，适合自然风光与休闲街区",
        )
        return Profile(
            travelType = type,
            interests = interests,
            consumptionLevel = profile?.consumptionLevel ?: "中等",
            summary = summaries[type] ?: "",
            similarity = (sim * 100).toInt() / 100.0,
        )
    }

    fun distribution(): List<Map<String, Any>> {
        val counts = travelTypes.keys.associateWith { 0 }.toMutableMap()
        val userIds = (OfflineData.behaviors.map { it.userId } + OfflineData.profiles.map { it.userId }).toSet()
        userIds.forEach { uid ->
            val vec = userInterestVector(uid)
            val type = if (vec.isNotEmpty()) classifyTravelType(vec).first else "休闲度假型"
            counts[type] = (counts[type] ?: 0) + 1
        }
        val total = maxOf(counts.values.sum(), 1)
        return counts.map { (type, count) ->
            mapOf("travel_type" to type, "count" to count, "percent" to (count * 100.0 / total).toInt())
        }
    }
}
