package com.tianjin.wenlu.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ---------- 用户 ----------
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): LoginResponse

    @GET("users/me")
    suspend fun me(): User

    @GET("users/me/favorites")
    suspend fun myFavorites(): List<FavoriteItem>

    @POST("users/me/favorites/{scenicId}")
    suspend fun addFavorite(@Path("scenicId") scenicId: Int): FavoriteActionResponse

    @DELETE("users/me/favorites/{scenicId}")
    suspend fun removeFavorite(@Path("scenicId") scenicId: Int): FavoriteActionResponse

    // ---------- 景点 ----------
    @GET("scenics")
    suspend fun scenics(
        @Query("keyword") keyword: String = "",
        @Query("category") category: String = "",
        @Query("sort") sort: String = "heat",
        @Query("limit") limit: Int = 50,
    ): List<Scenic>

    @GET("scenics/hot")
    suspend fun hotScenics(@Query("n") n: Int = 8): List<Scenic>

    @GET("scenics/categories")
    suspend fun categories(): List<String>

    @GET("scenics/{id}")
    suspend fun scenicDetail(@Path("id") id: Int): Scenic

    // ---------- 路线 ----------
    @POST("route/plan")
    suspend fun planRoute(@Body body: RoutePlanRequest): RoutePlanResult

    @POST("route/save")
    suspend fun saveRoute(@Body body: RoutePlanResult): SaveRouteResponse

    @GET("route/my")
    suspend fun myRoutes(): List<RouteRecord>

    @DELETE("route/{routeId}")
    suspend fun deleteRoute(@Path("routeId") routeId: Int): MessageResponse

    // ---------- 推荐与画像 ----------
    @GET("recommend")
    suspend fun recommend(@Query("limit") limit: Int = 8): RecommendResponse

    @GET("profile/me")
    suspend fun profile(): Profile

    // ---------- 分析 ----------
    @GET("analysis/predict")
    suspend fun predict(@Query("scenic_id") scenicId: Int, @Query("days") days: Int = 3): PredictResult

    @GET("analysis/comments")
    suspend fun comments(@Query("scenic_id") scenicId: Int): CommentAnalysis

    @GET("analysis/safety")
    suspend fun safety(): SafetyResponse

    @GET("analysis/dashboard")
    suspend fun dashboard(): Dashboard

    // ---------- 预算 ----------
    @POST("budget/estimate")
    suspend fun budget(@Body body: BudgetRequest): BudgetResult

    // ---------- 行为 ----------
    @POST("behavior")
    suspend fun behavior(@Body body: BehaviorRequest): BehaviorResponse

    // ---------- 系统 ----------
    @GET("health")
    suspend fun health(): HealthResponse
}
