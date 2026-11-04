package com.tianjin.wenlu.data.offline

import com.tianjin.wenlu.data.RouteDay
import com.tianjin.wenlu.data.RoutePlanRequest
import com.tianjin.wenlu.data.RoutePlanResult
import com.tianjin.wenlu.data.Scenic
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 多目标智能路线规划（Kotlin 移植）。
 * 与原后端逻辑一致，但通勤时长按相邻两点单段距离计算（修复累计距离口径问题）。
 */
object RoutePlanner {
    private val transportSpeed = mapOf("地铁" to 30, "公交" to 20, "步行" to 4, "自驾" to 35, "打车" to 35)
    private val transportCostPerLeg = mapOf("地铁" to 3, "公交" to 2, "自驾" to 15, "打车" to 25, "步行" to 0)
    private const val mealCostPerPerson = 30
    private const val lunchMinutes = 60
    private const val transferMinutes = 10

    private class Ctx(
        val days: Int,
        val people: Int,
        val budget: Double,
        val transport: String,
        val interests: List<String>,
        val hasElder: Boolean,
        val startPoint: Scenic,
    )

    private fun haversineKm(a: Scenic, b: Scenic): Double {
        val r = 6371.0
        val p1 = Math.toRadians(a.latitude)
        val p2 = Math.toRadians(b.latitude)
        val dp = Math.toRadians(b.latitude - a.latitude)
        val dl = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * r * asin(sqrt(h))
    }

    private fun travelMinutes(distKm: Double, transport: String): Int =
        (distKm / (transportSpeed[transport] ?: 30) * 60 + transferMinutes).toInt()

    private fun dailyTimeLimit(ctx: Ctx): Int = if (ctx.hasElder) 360 else 480

    private fun interestScore(scenic: Scenic, interests: List<String>): Double {
        if (interests.isEmpty()) return 0.5
        val hits = scenic.tags.count { it in interests }
        return 0.4 + 0.6 * minOf(1.0, hits / 2.0)
    }

    private fun heatScore(scenic: Scenic, maxHeat: Int): Double =
        if (maxHeat > 0) scenic.heat.toDouble() / maxHeat else 0.5

    private data class DayStats(val dist: Double, val time: Int, val cost: Double)

    private fun dayStats(ctx: Ctx, seq: List<Scenic>, start: Scenic): DayStats {
        var dist = 0.0
        var t = 0
        var cost = 0.0
        var prev = start
        for (s in seq) {
            val leg = haversineKm(prev, s)
            dist += leg
            t += travelMinutes(leg, ctx.transport) + s.durationMinutes
            cost += s.ticketPrice * ctx.people
            prev = s
        }
        if (seq.isNotEmpty()) {
            val back = haversineKm(prev, start)
            dist += back
            t += travelMinutes(back, ctx.transport)
        }
        t += if (seq.isNotEmpty()) lunchMinutes else 0
        cost += mealCostPerPerson * ctx.people * (if (seq.isNotEmpty()) 2 else 0)
        val legs = maxOf(0, seq.size * 2)
        cost += (transportCostPerLeg[ctx.transport] ?: 3) * legs * ctx.people
        return DayStats(dist, t, cost)
    }

    private fun splitDays(ctx: Ctx, seq: List<Scenic>): List<List<Scenic>> {
        if (seq.isEmpty()) return emptyList()
        val limit = dailyTimeLimit(ctx)
        val days = mutableListOf<List<Scenic>>()
        var current = mutableListOf<Scenic>()
        for (s in seq) {
            val test = current + s
            if (dayStats(ctx, test, ctx.startPoint).time <= limit || current.isEmpty()) {
                current = test.toMutableList()
            } else {
                days.add(current)
                current = mutableListOf(s)
            }
        }
        if (current.isNotEmpty()) days.add(current)
        return days
    }

    private fun fitness(ctx: Ctx, seq: List<Scenic>, maxHeat: Int): Double {
        val days = splitDays(ctx, seq)
        if (days.isEmpty()) return -1e9
        if (days.size > ctx.days) return -1e9 + (days.size - ctx.days) * 1000.0
        val perDayBudget = ctx.budget / ctx.days
        var score = 0.0
        for (day in days) {
            val stats = dayStats(ctx, day, ctx.startPoint)
            val interest = day.sumOf { interestScore(it, ctx.interests) } / day.size
            val heat = day.sumOf { heatScore(it, maxHeat) } / day.size
            score += 2.0 * interest + 1.0 * heat
            score -= 0.15 * stats.dist
            score -= 0.02 * maxOf(0, stats.time - dailyTimeLimit(ctx))
            score -= 0.005 * maxOf(0.0, stats.cost - perDayBudget)
        }
        score += 0.08 * seq.size
        return score
    }

    private fun greedyPlan(ctx: Ctx): List<Scenic> {
        val mh = OfflineData.scenics.maxOf { it.heat }
        val remaining = OfflineData.scenics.filter { it.id != ctx.startPoint.id }.toMutableList()
        val chosen = mutableListOf<Scenic>()
        val limit = dailyTimeLimit(ctx)
        val dayBudget = ctx.budget / ctx.days

        while (remaining.isNotEmpty()) {
            val prev = chosen.lastOrNull() ?: ctx.startPoint
            val curCount = splitDays(ctx, chosen).size

            fun tryPick(allowNewDay: Boolean): Scenic? {
                var best: Scenic? = null
                var bestScore = -1e9
                for (s in remaining) {
                    val candDays = splitDays(ctx, chosen + s)
                    if (candDays.size > ctx.days) continue
                    if (!allowNewDay && candDays.size > curCount) continue
                    val stats = dayStats(ctx, candDays.last(), ctx.startPoint)
                    if (stats.time > limit + 30) continue
                    if (stats.cost > dayBudget * 1.5) continue
                    val dist = haversineKm(prev, s)
                    val score = 2.2 * interestScore(s, ctx.interests) + 1.0 * heatScore(s, mh) - 0.05 * dist
                    if (score > bestScore) {
                        best = s
                        bestScore = score
                    }
                }
                return best
            }

            val pick = tryPick(allowNewDay = false) ?: if (curCount < ctx.days) tryPick(allowNewDay = true) else null
            if (pick == null) break
            chosen.add(pick)
            remaining.remove(pick)
        }
        return chosen
    }

    private fun geneticOptimize(ctx: Ctx, seedSeq: List<Scenic>, pop: Int = 30, gens: Int = 60): List<Scenic> {
        val random = Random(7)
        val maxHeat = OfflineData.scenics.maxOf { it.heat }
        val pool = mutableListOf<List<Scenic>>(seedSeq)
        repeat(pop - 1) {
            pool.add(seedSeq.shuffled(random))
        }

        fun score(seq: List<Scenic>) = fitness(ctx, seq, maxHeat)

        var best = pool.maxByOrNull { score(it) } ?: seedSeq
        repeat(gens) {
            val newPool = mutableListOf<List<Scenic>>()
            repeat(pop) {
                val a = pool.random(random)
                val b = pool.random(random)
                newPool.add(if (score(a) > score(b)) a else b)
            }
            pool.clear()
            pool.addAll(newPool)

            val offspring = mutableListOf<List<Scenic>>()
            var i = 0
            while (i < pop - 1) {
                val p1 = pool[i]
                val p2 = pool[i + 1]
                var child: List<Scenic> = if (p1.size >= 2) {
                    val cut1 = random.nextInt(p1.size)
                    val cut2 = random.nextInt(cut1, p1.size)
                    val seg = p1.subList(cut1, cut2)
                    val rest = p2.filterNot { it in seg }
                    rest.subList(0, minOf(cut1, rest.size)) + seg + rest.subList(minOf(cut1, rest.size), rest.size)
                } else {
                    p1
                }
                if (child.size >= 3 && random.nextDouble() < 0.3) {
                    val i1 = random.nextInt(child.size)
                    val i2 = random.nextInt(child.size)
                    val from = minOf(i1, i2)
                    val to = maxOf(i1, i2)
                    child = child.subList(0, from) + child.subList(from, to).reversed() + child.subList(to, child.size)
                }
                if (random.nextDouble() < 0.25) {
                    val unused = OfflineData.scenics.filter { it !in child && it.id != ctx.startPoint.id }
                    if (unused.isNotEmpty()) {
                        val cand = child.toMutableList()
                        cand.add(random.nextInt(cand.size + 1), unused.random(random))
                        if (splitDays(ctx, cand).size <= ctx.days) child = cand
                    }
                }
                offspring.add(child)
                i += 2
            }
            pool.addAll(offspring)
            pool.sortByDescending { score(it) }
            while (pool.size > pop) pool.removeAt(pool.size - 1)
            val curBest = pool.maxByOrNull { score(it) }
            if (curBest != null && score(curBest) > score(best)) best = curBest
        }
        return best
    }

    fun plan(request: RoutePlanRequest): RoutePlanResult {
        val start = OfflineData.scenicById(1) ?: OfflineData.scenics.first()
        val ctx = Ctx(
            days = request.days,
            people = request.people,
            budget = request.budget,
            transport = request.transport,
            interests = request.interests,
            hasElder = request.hasElder,
            startPoint = start,
        )
        val seedSeq = greedyPlan(ctx)
        val seq = if (request.useGenetic && seedSeq.size >= 2) geneticOptimize(ctx, seedSeq) else seedSeq
        var daysPlan = splitDays(ctx, seq)
        while (daysPlan.size > ctx.days) daysPlan = daysPlan.dropLast(1)

        val maxHeat = OfflineData.scenics.maxOf { it.heat }
        val resultDays = mutableListOf<RouteDay>()
        var totalDist = 0.0
        var totalCost = 0.0
        var totalHours = 0.0
        val notes = mutableListOf<String>()

        daysPlan.forEachIndexed { index, day ->
            val stats = dayStats(ctx, day, ctx.startPoint)
            totalDist += stats.dist
            totalCost += stats.cost
            totalHours += stats.time / 60.0
            resultDays.add(
                RouteDay(
                    day = index + 1,
                    spots = day,
                    distanceKm = (stats.dist * 10).toInt() / 10.0,
                    durationHours = (stats.time / 60.0 * 10).toInt() / 10.0,
                    cost = stats.cost.toInt().toDouble(),
                )
            )
            day.forEach { s ->
                // 路线提示也必须用同一个天气，否则路线页和详情页会对同一景点给出不同客流等级
                val (weather, source) = WeatherService.current(s.latitude, s.longitude)
                val fc = Prediction.forecastOne(s, weather, source)
                if (fc != null && fc.crowdLevel in setOf("高", "爆满")) {
                    val occTxt = if (fc.capacity > 0) "，承载量占用 ${(fc.occupancy * 100).toInt()}%" else ""
                    notes.add("⚠ ${s.name} 未来客流预计偏多（${fc.crowdLevel}$occTxt），建议错峰或调整路线")
                }
            }
        }

        if (ctx.hasElder) notes.add("行程已考虑老人出行，每日游玩控制在 6 小时内，节奏较缓")
        if (ctx.transport == "地铁") notes.add("全程按地铁通勤估算，天津地铁可覆盖大部分核心景点")

        return RoutePlanResult(
            days = resultDays,
            totalDistanceKm = (totalDist * 10).toInt() / 10.0,
            totalCost = totalCost.toInt().toDouble(),
            totalDurationHours = (totalHours * 10).toInt() / 10.0,
            algorithm = if (request.useGenetic) "遗传算法(多目标优化) + 贪心构造" else "贪心算法",
            notes = notes,
        )
    }
}
