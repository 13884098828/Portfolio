package com.tianjin.wenlu.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianjin.wenlu.data.AppRepository
import com.tianjin.wenlu.data.CommentAnalysis
import com.tianjin.wenlu.data.PredictResult
import com.tianjin.wenlu.data.Scenic
import com.tianjin.wenlu.data.SessionManager
import com.tianjin.wenlu.data.ServerMonitor
import com.tianjin.wenlu.data.WeatherState
import com.tianjin.wenlu.data.offline.WeatherService
import com.tianjin.wenlu.ui.components.ErrorBox
import com.tianjin.wenlu.ui.components.KeyValueRow
import com.tianjin.wenlu.ui.components.LoadingBox
import com.tianjin.wenlu.ui.components.ScenicCover
import com.tianjin.wenlu.ui.components.ScenicMap
import com.tianjin.wenlu.ui.components.ScenicMapLegend
import com.tianjin.wenlu.ui.components.SectionTitle
import com.tianjin.wenlu.ui.components.TagsRow
import com.tianjin.wenlu.ui.components.friendlyError
import com.tianjin.wenlu.data.BehaviorRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenicDetailScreen(scenicId: Int, onBack: () -> Unit) {
    var detail by remember { mutableStateOf<Scenic?>(null) }
    var allScenics by remember { mutableStateOf<List<Scenic>>(emptyList()) }
    var predict by remember { mutableStateOf<PredictResult?>(null) }
    var comments by remember { mutableStateOf<CommentAnalysis?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isFavorite by remember { mutableStateOf(false) }
    var weather by remember { mutableStateOf<WeatherState?>(null) }
    var weatherPicker by remember { mutableStateOf(false) }
    val retryTick by ServerMonitor.retryTick.collectAsState()
    val scope = rememberCoroutineScope()

    fun load() {
        loading = true
        error = null
        scope.launch(Dispatchers.IO) {
            try {
                detail = AppRepository.scenicDetail(scenicId)
                allScenics = AppRepository.scenics(limit = 100)
                // 先用手上已有的天气（实时缓存 / 默认"晴"）出一版预测，界面立刻可用
                predict = runCatching { AppRepository.predict(scenicId, 3) }.getOrNull()
                weather = runCatching { AppRepository.weatherState() }.getOrNull()
                comments = runCatching { AppRepository.comments(scenicId) }.getOrNull()
                if (SessionManager.isLoggedIn) {
                    isFavorite = runCatching { AppRepository.isFavorite(scenicId) }.getOrDefault(false)
                }
                if (SessionManager.isLoggedIn) {
                    runCatching { AppRepository.behavior(BehaviorRequest(scenicId, "view")) }
                }
            } catch (e: Exception) {
                error = friendlyError(e)
            } finally {
                loading = false
            }

            // 联网刷新实时天气必须放在最后，且不能阻塞上面的渲染：
            // 断网时最坏只是等一个 connectTimeout，页面此时已经能看了；拿到新天气再把预测替换一次。
            val ok = runCatching { AppRepository.refreshWeather(scenicId) }.getOrDefault(false)
            if (ok) {
                predict = runCatching { AppRepository.predict(scenicId, 3) }.getOrNull()
                weather = runCatching { AppRepository.weatherState() }.getOrNull()
            }
        }
    }

    LaunchedEffect(scenicId, retryTick) { load() }

    fun applyWeather(w: String?) {
        AppRepository.setWeatherManual(w)
        weatherPicker = false
        scope.launch(Dispatchers.IO) {
            predict = runCatching { AppRepository.predict(scenicId, 3) }.getOrNull()
            weather = runCatching { AppRepository.weatherState() }.getOrNull()
        }
    }

    if (weatherPicker) {
        WeatherPickerDialog(
            current = weather,
            onPick = { applyWeather(it) },
            onDismiss = { weatherPicker = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.name ?: "景点详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val s = detail ?: return@IconButton
                            scope.launch(Dispatchers.IO) {
                                runCatching {
                                    if (isFavorite) {
                                        AppRepository.removeFavorite(s.id)
                                    } else {
                                        AppRepository.addFavorite(s.id)
                                    }
                                    isFavorite = !isFavorite
                                }
                            }
                        },
                    ) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "取消收藏" else "收藏",
                            tint = if (isFavorite) Color(0xFFE6A23C) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading && detail == null -> LoadingBox("正在加载景点详情…")
            error != null && detail == null -> ErrorBox(error!!, onRetry = { load() })
            detail != null -> Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                val s = detail!!
                // 详情页头图：有实拍图就渲染实拍图，没有则用按 id 生成的占位封面
                ScenicCover(
                    scenicId = s.id,
                    label = s.name,
                    subLabel = s.category,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.height(14.dp))
                Text(s.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text("📍 ${s.address}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))

                Card {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        KeyValueRow("分类", s.category)
                        KeyValueRow("评分", "${s.score} 分")
                        KeyValueRow("热度", "${s.heat}")
                        KeyValueRow("门票", if (s.ticketPrice > 0) "¥${s.ticketPrice}" else "免费")
                        KeyValueRow("开放时间", s.openTime)
                        KeyValueRow("建议时长", "${s.durationMinutes} 分钟")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(s.intro, fontSize = 13.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(8.dp))
                TagsRow(s.tags)

                SectionTitle("景点位置分布")
                // 换成统一的离线地图组件：真实经纬度投影 + 类别配色 + 高亮当前景点
                // （选中点带 1/2/5 km 距离环，用的是和路线规划同一套公里换算口径）
                ScenicMap(
                    points = allScenics,
                    height = 240.dp,
                    selectedId = s.id,
                )
                Spacer(Modifier.height(6.dp))
                ScenicMapLegend(allScenics.map { it.category }.distinct())

                SectionTitle("未来 3 天客流预测")
                WeatherChip(weather) { weatherPicker = true }
                Spacer(Modifier.height(8.dp))
                predict?.forecast?.let { fc ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fc.forEach { f ->
                            ForecastCard(f.date, f.weekday, f.predictNum, f.crowdLevel, f.occupancy, Modifier.weight(1f))
                        }
                    }
                } ?: Text("暂无预测数据", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                SectionTitle("游客评价智能分析")
                val c = comments
                if (c != null && c.total > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${c.satisfaction}%", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("总体满意度（${c.total} 条评论）", fontSize = 12.sp)
                            LinearProgressIndicator(
                                progress = { c.satisfaction / 100f },
                                modifier = Modifier.fillMaxWidth(0.6f),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        c.positiveKeywords.forEach { k ->
                            Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(6.dp)) {
                                Text("👍 $k", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                        c.negativeKeywords.forEach { k ->
                            Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(6.dp)) {
                                Text("👎 $k", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    c.sampleComments.take(5).forEach { sample ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))) {
                            Column(Modifier.padding(10.dp)) {
                                Text("★ ${sample.rating} · ${sample.sentiment}", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                                Text(sample.content, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                } else {
                    Text("暂无评论", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ForecastCard(
    date: String,
    weekday: String,
    predictNum: Int,
    crowdLevel: String,
    occupancy: Double,
    modifier: Modifier = Modifier,
) {
    val color = when (crowdLevel) {
        "低" -> Color(0xFF67C23A)
        "中" -> Color(0xFFE6A23C)
        "高" -> Color(0xFFF5785F)
        else -> Color(0xFFF56C6C)   // 爆满
    }
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // 原来写死 removePrefix("2026-").removePrefix("2025-")，跨年就得改代码
            Text(date.substringAfter("-"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(weekday, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text("${(predictNum / 10000.0).toString().take(3)}万", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(crowdLevel, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
            if (occupancy > 0) {
                Text(
                    "承载 ${(occupancy * 100).toInt()}%",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 预测区块顶部的天气标签：点一下就能换天气，立刻看到预测值变化 */
@Composable
private fun WeatherChip(w: WeatherState?, onClick: () -> Unit) {
    val weather = w?.weather ?: "晴"
    val tip = when (w?.source) {
        "实时" -> "实时预报"
        "缓存" -> "本地缓存"
        "手动" -> "手动指定"
        else -> "默认（未联网）"
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("天气因子：$weather", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("（$tip）", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("点击切换 ▾", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun WeatherPickerDialog(
    current: WeatherState?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择天气", fontSize = 16.sp) },
        text = {
            Column {
                Text(
                    "天气会直接乘进客流预测（晴 ×1.1 → 大雨 ×0.55），换一档三天预测值和客流等级立刻变化。",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                WeatherOptionRow("自动（实时预报 → 本地缓存 → 默认晴）", current?.source != "手动") { onPick(null) }
                WeatherService.OPTIONS.forEach { option ->
                    val selected = current != null && current.source == "手动" && current.weather == option
                    WeatherOptionRow(option, selected) { onPick(option) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
private fun WeatherOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Text(
            (if (selected) "✓ " else "    ") + label,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        )
    }
}
