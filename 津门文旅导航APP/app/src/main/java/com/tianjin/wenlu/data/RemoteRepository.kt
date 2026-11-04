package com.tianjin.wenlu.data

import com.tianjin.wenlu.ApiClient
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * 联网模式数据仓库：所有方法走后端 FastAPI。
 * 界面层仍保持同步调用风格，网络请求统一在 IO 线程执行。
 */
object RemoteRepository {

    private fun <T> request(block: suspend () -> T): T {
        return try {
            val result = runBlocking(Dispatchers.IO) { block() }
            ServerMonitor.onRequestSuccess()
            result
        } catch (e: IOException) {
            ServerMonitor.onRequestFailure(e)
            throw e
        } catch (e: Exception) {
            ServerMonitor.onRequestSuccess()
            throw e
        }
    }

    // ---------- 用户 ----------
    fun login(body: LoginRequest): LoginResponse = request { ApiClient.api.login(body) }

    fun register(body: RegisterRequest): LoginResponse = request { ApiClient.api.register(body) }

    // ---------- 景点 ----------
    fun categories(): List<String> = request { ApiClient.api.categories() }

    fun hotScenics(n: Int): List<Scenic> = request { ApiClient.api.hotScenics(n) }

    fun scenicDetail(id: Int): Scenic = request { ApiClient.api.scenicDetail(id) }

    fun scenics(keyword: String, category: String, sort: String, limit: Int): List<Scenic> =
        request { ApiClient.api.scenics(keyword, category, sort, limit) }

    // ---------- 路线 ----------
    fun planRoute(body: RoutePlanRequest): RoutePlanResult =
        request { ApiClient.api.planRoute(body) }

    fun saveRoute(result: RoutePlanResult): SaveRouteResponse =
        request { ApiClient.api.saveRoute(result) }

    fun myRoutes(): List<RouteRecord> = request { ApiClient.api.myRoutes() }

    fun deleteRoute(routeId: Int): MessageResponse =
        request { ApiClient.api.deleteRoute(routeId) }

    // ---------- 推荐与画像 ----------
    fun recommend(limit: Int): RecommendResponse = request { ApiClient.api.recommend(limit) }

    fun profile(): Profile = request { ApiClient.api.profile() }

    // ---------- 分析 ----------
    fun predict(scenicId: Int, days: Int): PredictResult =
        request { ApiClient.api.predict(scenicId, days) }

    fun comments(scenicId: Int): CommentAnalysis =
        request { ApiClient.api.comments(scenicId) }

    fun safety(): SafetyResponse = request { ApiClient.api.safety() }

    fun dashboard(): Dashboard = request { ApiClient.api.dashboard() }

    // ---------- 预算 ----------
    fun budget(body: BudgetRequest): BudgetResult = request { ApiClient.api.budget(body) }

    // ---------- 收藏与行为 ----------
    fun favorites(): List<FavoriteItem> = request { ApiClient.api.myFavorites() }

    fun addFavorite(scenicId: Int): FavoriteActionResponse =
        request { ApiClient.api.addFavorite(scenicId) }

    fun removeFavorite(scenicId: Int): FavoriteActionResponse =
        request { ApiClient.api.removeFavorite(scenicId) }

    fun behavior(body: BehaviorRequest): BehaviorResponse =
        request { ApiClient.api.behavior(body) }

    // ---------- 系统 ----------
    fun health(): HealthResponse = request { ApiClient.api.health() }
}
