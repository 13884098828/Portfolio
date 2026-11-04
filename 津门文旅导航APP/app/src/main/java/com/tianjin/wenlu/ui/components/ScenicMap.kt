package com.tianjin.wenlu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianjin.wenlu.data.Scenic
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * 离线景点分布图。
 *
 * 为什么不用高德/百度/腾讯地图 SDK：
 *  1. 三家都要注册开发者账号 + 实名认证 + 申请 Key + 绑定签名，属于外部依赖；
 *  2. 它们的地图瓦片走网络，本 App 的演示环境（`DEFAULT_SERVER_URL` 指向局域网）
 *     现场很可能没有公网——断网时高德就是一张空白格子，反而砸场子；
 *  3. 本项目 30 个景点已经带真实 `longitude` / `latitude`，等距圆柱投影自绘
 *     完全够表达「景点在哪、彼此多远、路线怎么走」，而且**零依赖、零 Key、永远可用**。
 *
 * 因此这里画的是「基于真实经纬度的示意图」：位置、相对距离、方位都是真的，
 * 只是底图没有街道。界面上如实标注为「离线示意」，不假装是电子地图。
 */

/** 景点类别配色——列表页、详情页、地图共用一套 */
val CATEGORY_COLORS: Map<String, Color> = mapOf(
    "城市地标" to Color(0xFF2F6FED),
    "历史文化" to Color(0xFFC9822B),
    "美食街区" to Color(0xFFE4572E),
    "自然风光" to Color(0xFF2E9E5B),
    "人文校园" to Color(0xFF6B7A8F),
    "主题乐园" to Color(0xFF8E5BD9),
    "科普场馆" to Color(0xFF17A2B8),
    "网红打卡" to Color(0xFFE2508B),
)

fun categoryColor(category: String): Color = CATEGORY_COLORS[category] ?: Color(0xFF5B6B7C)

/** 多日路线的每日配色 */
val DAY_COLORS: List<Color> = listOf(
    Color(0xFF1E88E5), Color(0xFFE53935), Color(0xFF43A047),
    Color(0xFFFB8C00), Color(0xFF8E24AA), Color(0xFF00897B),
)

private val MAP_BG = Color(0xFFE9F0F7)
private val GRID_COLOR = Color(0x33607D8B)
private val LABEL_BG = Color(0xF2FFFFFF)

// ---- 等距圆柱投影：以天津中部为锚点，经度按锚点纬度做余弦压缩，保证形状不变形 ----
private const val ANCHOR_LAT = 39.60
private const val ANCHOR_LNG = 117.20
private val COS_ANCHOR = cos(Math.toRadians(ANCHOR_LAT)).toFloat()
private const val KM_PER_DEG = 111.32f

/** 比例尺可选的「整数公里」档位 */
private val SCALE_STEPS = listOf(0.1f, 0.2f, 0.5f, 1f, 2f, 5f, 10f, 20f, 50f)

/**
 * 手势回调需要读到「当前」的缩放与视图中心。它们不能作为 pointerInput 的 key
 * （否则每帧重启手势识别，拖动会被打断），也不需要触发重组，
 * 所以用一个普通可变对象传递，而不是 Compose 的快照状态。
 */
private class MapViewRef {
    var scale: Float = 1f
    var center: Offset = Offset.Zero
}

private fun projX(lng: Double): Float = ((lng - ANCHOR_LNG) * COS_ANCHOR).toFloat()
private fun projY(lat: Double): Float = (ANCHOR_LAT - lat).toFloat()

private data class BBox(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float) {
    val w: Float get() = (maxX - minX).coerceAtLeast(1e-3f)
    val h: Float get() = (maxY - minY).coerceAtLeast(1e-3f)
    val cx: Float get() = (minX + maxX) / 2f
    val cy: Float get() = (minY + maxY) / 2f
}

private fun bboxOf(pts: Collection<Offset>): BBox {
    var mnx = Float.MAX_VALUE
    var mxx = -Float.MAX_VALUE
    var mny = Float.MAX_VALUE
    var mxy = -Float.MAX_VALUE
    pts.forEach {
        mnx = minOf(mnx, it.x); mxx = maxOf(mxx, it.x)
        mny = minOf(mny, it.y); mxy = maxOf(mxy, it.y)
    }
    return BBox(mnx, mxx, mny, mxy)
}

private enum class MapScope(val label: String) {
    CITY("市区"), ALL("全域")
}

/**
 * @param points        要打点的景点（带真实经纬度）
 * @param selectedId    当前高亮点
 * @param routeSegments 多日路线：外层是「天」，内层是按顺序访问的景点 id——会连成折线并标序号
 * @param onSelect      点中圆点的回调；传 null 则地图只读
 */
@Composable
fun ScenicMap(
    points: List<Scenic>,
    modifier: Modifier = Modifier,
    height: Dp = 300.dp,
    selectedId: Int? = null,
    routeSegments: List<List<Int>> = emptyList(),
    onSelect: ((Scenic) -> Unit)? = null,
) {
    if (points.isEmpty()) return

    val xy = remember(points) {
        points.associate { it.id to Offset(projX(it.longitude), projY(it.latitude)) }
    }
    val allBox = remember(xy) { bboxOf(xy.values) }
    // 「市区」= 天津主城（经度 < 117.30）；其余的 滨海 4 个 + 杨柳青 2 个 + 蓟州 3 个 才需要拉远看
    val cityBox = remember(xy) {
        val sub = xy.filterKeys { id -> points.first { it.id == id }.longitude < 117.30 }.values
        if (sub.size >= 3) bboxOf(sub) else allBox
    }
    val hasCity = cityBox.w < allBox.w * 0.9f

    var scope by remember { mutableStateOf(if (hasCity) MapScope.CITY else MapScope.ALL) }
    val bounds = if (scope == MapScope.CITY) cityBox else allBox

    var zoom by remember { mutableStateOf(1f) }
    var center by remember { mutableStateOf(Offset(bounds.cx, bounds.cy)) }
    LaunchedEffect(scope, bounds) {
        zoom = 1f
        center = Offset(bounds.cx, bounds.cy)
    }

    // 手势回调里要读最新的 s / center，但不能把它们作为 key——否则每帧都会重启手势识别，
    // 拖到一半手势就被取消。用一个普通对象持有最新值（不走快照状态，避免重组回环）。
    val view = remember { MapViewRef() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp)),
        color = MAP_BG,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0x1F000000)),
        shadowElevation = 1.dp,
    ) {
        BoxWithConstraints {
            val density = LocalDensity.current
            val wPx = with(density) { maxWidth.toPx() }
            val hPx = with(density) { maxHeight.toPx() }
            val padPx = with(density) { 22.dp.toPx() }
            val fit = minOf((wPx - 2 * padPx) / bounds.w, (hPx - 2 * padPx) / bounds.h)
            val s = (fit * zoom).coerceAtLeast(1e-4f)
            view.scale = s
            view.center = center

            val dotBase = with(density) { 5.5.dp.toPx() }
            val hitRadius = with(density) { 26.dp.toPx() }

            // 比例尺的「公里数」在这里算定，文字标签和画线共用同一个值
            val kmPerPx = KM_PER_DEG / s
            val niceKm = SCALE_STEPS.firstOrNull { it / kmPerPx in 46f..108f } ?: 20f
            val barPx = niceKm / kmPerPx

            // 屏幕坐标（用于文字标签定位）
            fun screenOf(w: Offset): Offset = Offset(
                wPx / 2f + (w.x - center.x) * s,
                hPx / 2f + (w.y - center.y) * s,
            )

            val visible = points.mapNotNull { p ->
                xy[p.id]?.let { w -> p to screenOf(w) }
            }.filter { (_, o) -> o.x > -30f && o.x < wPx + 30f && o.y > -30f && o.y < hPx + 30f }

            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    // 地图嵌在可滚动的列表里，所以手势要「让路」：
                    // 未放大时单指拖动不消费事件 → 正常滚动列表；
                    // 双指捏合始终生效；放大之后单指拖动才用来平移地图。
                    .pointerInput(points) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                val z = event.calculateZoom()
                                val pan = event.calculatePan()
                                if (z != 1f) {
                                    zoom = (zoom * z).coerceIn(0.75f, 9f)
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                } else if (zoom > 1.02f && (pan.x != 0f || pan.y != 0f)) {
                                    val cur = view.scale
                                    center = Offset(
                                        center.x - pan.x / cur,
                                        center.y - pan.y / cur,
                                    )
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                    .pointerInput(points) {
                        if (onSelect == null) return@pointerInput
                        detectTapGestures { pos ->
                            val curS = view.scale
                            val cur = view.center
                            val wx = (pos.x - wPx / 2f) / curS + cur.x
                            val wy = (pos.y - hPx / 2f) / curS + cur.y
                            val hit = points.minByOrNull {
                                val w = xy[it.id] ?: return@minByOrNull Float.MAX_VALUE
                                val dx = (w.x - wx) * curS
                                val dy = (w.y - wy) * curS
                                dx * dx + dy * dy
                            }
                            if (hit != null) {
                                val w = xy.getValue(hit.id)
                                val dx = (w.x - wx) * curS
                                val dy = (w.y - wy) * curS
                                if (dx * dx + dy * dy <= hitRadius * hitRadius) onSelect(hit)
                            }
                        }
                    }
            ) {
                drawRect(MAP_BG)
                withTransform({
                    translate(wPx / 2f - center.x * s, hPx / 2f - center.y * s)
                    scale(s, s, pivot = Offset.Zero)
                }) {
                    val halfW = wPx / (2f * s)
                    val halfH = hPx / (2f * s)
                    val l = center.x - halfW
                    val r = center.x + halfW
                    val t = center.y - halfH
                    val b = center.y + halfH
                    val hair = 1f / s

                    // 经纬网：尺度越大网越细，让"放大"这件事有反馈
                    val step = when {
                        s > 260f -> 0.01f
                        s > 90f -> 0.02f
                        s > 30f -> 0.05f
                        s > 9f -> 0.1f
                        else -> 0.25f
                    }
                    var gx = floor(l / step) * step
                    while (gx <= r) {
                        drawLine(GRID_COLOR, Offset(gx, t), Offset(gx, b), strokeWidth = hair)
                        gx += step
                    }
                    var gy = floor(t / step) * step
                    while (gy <= b) {
                        drawLine(GRID_COLOR, Offset(l, gy), Offset(r, gy), strokeWidth = hair)
                        gy += step
                    }

                    // 高亮点的距离环：1 / 2 / 5 km。
                    // 半径 = 公里数 / 111.32（1° 纬度约 111.32km），在这个尺度上够用；
                    // 半径超过画面短边 45% 的环不画，免得市区视图被 5km 的大圈糊住。
                    selectedId?.let { sid ->
                        xy[sid]?.let { w ->
                            val dash = PathEffect.dashPathEffect(floatArrayOf(18f / s, 15f / s), 0f)
                            val maxR = minOf(wPx, hPx) * 0.45f
                            listOf(1f, 2f, 5f).forEach { km ->
                                val rWorld = km / KM_PER_DEG
                                if (rWorld * s <= maxR) {
                                    drawCircle(
                                        color = Color(0x662F6FED),
                                        radius = rWorld,
                                        center = w,
                                        style = Stroke(width = 1.5f / s, pathEffect = dash),
                                    )
                                }
                            }
                        }
                    }

                    // 路线折线（按天分色）
                    routeSegments.forEachIndexed { dayIdx, ids ->
                        val col = DAY_COLORS[dayIdx % DAY_COLORS.size]
                        val pts = ids.mapNotNull { xy[it] }
                        for (i in 0 until pts.size - 1) {
                            drawLine(col, pts[i], pts[i + 1], strokeWidth = 3.5f / s)
                        }
                    }

                    // 景点圆点：大小随热度，颜色随类别
                    points.forEach { p ->
                        val w = xy[p.id] ?: return@forEach
                        val frac = ((p.heat - 11000f) / 39000f).coerceIn(0f, 1f)
                        val r = dotBase * (0.62f + 0.85f * frac) / s
                        val col = categoryColor(p.category)
                        if (p.id == selectedId) {
                            drawCircle(Color(0x552F6FED), r * 2.6f, w)
                            drawCircle(col, r * 1.5f, w)
                            drawCircle(Color.White, r * 1.5f, w, style = Stroke(2.5f / s))
                        } else {
                            drawCircle(Color(0x33FFFFFF), r * 1.7f, w)
                            drawCircle(col, r, w)
                            drawCircle(Color.White, r, w, style = Stroke(1.5f / s))
                        }
                    }
                }

                // ---- 以下都在屏幕坐标里画，不受缩放影响 ----
                // 比例尺（长度由组件体算好传入，和上面的文字标签同值）
                val by = hPx - 14f
                val bx = 14f
                drawLine(Color(0xCC37474F), Offset(bx, by), Offset(bx + barPx, by), strokeWidth = 2f)
                drawLine(Color(0xCC37474F), Offset(bx, by - 5f), Offset(bx, by + 5f), strokeWidth = 2f)
                drawLine(Color(0xCC37474F), Offset(bx + barPx, by - 5f), Offset(bx + barPx, by + 5f), strokeWidth = 2f)
            }

            // ---- 文字标签（Compose 文本叠在 Canvas 上，位置用同一套投影算） ----
            val labelled: List<Pair<Scenic, Offset>> = when {
                zoom >= 2.2f -> visible
                else -> {
                    val hot = visible.sortedByDescending { it.first.heat }.take(3)
                    val sel = visible.filter { it.first.id == selectedId }
                    (sel + hot).distinctBy { it.first.id }
                }
            }
            // 标签避让：先把路线序号方块占住，标签撞上就往下让，让不开干脆不画。
            // 市区点位本来就密（五大道一带几个点相距不到 1km），不避让的话标签会糊在
            // 序号方块上——「意式风情区」被「③」劈成两半就是这么来的。
            val labelFs = 10.sp
            val charPx = with(density) { labelFs.toPx() }
            val taken = mutableListOf<Rect>()
            if (routeSegments.isNotEmpty()) {
                val bs = with(density) { 17.dp.toPx() }
                routeSegments.flatten().forEach { sid ->
                    xy[sid]?.let { w ->
                        val o = screenOf(w)
                        if (o.x >= -20f && o.x <= wPx + 20f && o.y >= -20f && o.y <= hPx + 20f) {
                            taken += Rect(o.x - bs / 2f, o.y - bs / 2f, o.x + bs / 2f, o.y + bs / 2f)
                        }
                    }
                }
            }
            labelled.forEach { (p, o) ->
                val tw = p.name.length * charPx + 8f
                val th = charPx + 4f
                var lx = o.x + 8f
                var ly = o.y - 9f
                repeat(3) {
                    if (taken.any { it.overlaps(Rect(lx, ly, lx + tw, ly + th)) }) ly += th + 2f
                }
                val box = Rect(lx, ly, lx + tw, ly + th)
                if (taken.any { it.overlaps(box) }) return@forEach
                taken += box
                Text(
                    text = p.name,
                    fontSize = labelFs,
                    color = Color(0xFF1B2A38),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                lx.roundToInt().coerceIn(2, (wPx - tw).roundToInt().coerceAtLeast(2)),
                                ly.roundToInt().coerceIn(2, (hPx - th).roundToInt().coerceAtLeast(2)),
                            )
                        }
                        .background(LABEL_BG, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }

            // 路线序号
            if (routeSegments.isNotEmpty()) {
                val badge = with(density) { 17.dp.toPx() }
                routeSegments.forEachIndexed { dayIdx, ids ->
                    val col = DAY_COLORS[dayIdx % DAY_COLORS.size]
                    ids.forEachIndexed { i, sid ->
                        val w = xy[sid] ?: return@forEachIndexed
                        val o = screenOf(w)
                        if (o.x < -20f || o.x > wPx + 20f || o.y < -20f || o.y > hPx + 20f) return@forEachIndexed
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = col,
                            border = BorderStroke(1.dp, Color.White),
                            modifier = Modifier
                                .size(17.dp)
                                .offset {
                                    IntOffset((o.x - badge / 2f).roundToInt(), (o.y - badge / 2f).roundToInt())
                                },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "${i + 1}",
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }

            // 范围切换 + 离线说明
            Row(
                Modifier.align(Alignment.TopStart).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (hasCity) {
                    ScopeChip(MapScope.CITY.label, scope == MapScope.CITY) { scope = MapScope.CITY }
                    ScopeChip(MapScope.ALL.label, scope == MapScope.ALL) { scope = MapScope.ALL }
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xE6FFFFFF),
                // 提示必须待在顶部那一行（和「市区/全域」同一带）：
                // 地图只有 300dp 高，底部那条带子上挤着比例尺、最南端的点位圆点、
                // 景点标签和路线序号，气泡放这儿一定会压住它们。
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 10.dp, top = 8.dp),
            ) {
                Text(
                    // 全域视图下市区 22 个景点会挤成一个点——这是真实地理（62×133km 的范围里市区只有 16×8km），
                    // 与其假装不挤，不如直接告诉用户该点哪里。文案压短，免得在 300dp 高的地图上横贯整条顶边。
                    if (scope == MapScope.ALL && zoom <= 1.05f) {
                        "点位密集 · 建议切「市区」"
                    } else {
                        "离线示意 · 拖动缩放"
                    },
                    fontSize = 9.sp,
                    color = Color(0xFF546E7A),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
            Text(
                text = "${niceKm.let { if (it >= 1f) "${it.toInt()}" else it.toString() }} km",
                fontSize = 9.sp,
                color = Color(0xFF37474F),
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun ScopeChip(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (active) Color(0xFF2F6FED) else Color(0xE6FFFFFF),
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) Color.White else Color(0xFF37474F),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        )
    }
}

/** 地图下方的类别图例，只显示当前确实出现的类别 */
@Composable
fun ScenicMapLegend(categories: List<String>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        categories.forEach { c ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(categoryColor(c), RoundedCornerShape(50))
                )
                Text(
                    text = " $c",
                    fontSize = 10.sp,
                    color = Color(0xFF546E7A),
                )
            }
        }
    }
}
