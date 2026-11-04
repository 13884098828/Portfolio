package com.tianjin.wenlu.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianjin.wenlu.data.AppRepository
import com.tianjin.wenlu.data.RoutePlanRequest
import com.tianjin.wenlu.data.RoutePlanResult
import com.tianjin.wenlu.data.SessionManager
import com.tianjin.wenlu.ui.components.DAY_COLORS
import com.tianjin.wenlu.ui.components.ErrorBox
import com.tianjin.wenlu.ui.components.LoadingBox
import com.tianjin.wenlu.ui.components.ScenicMap
import com.tianjin.wenlu.ui.components.SectionTitle
import com.tianjin.wenlu.ui.components.friendlyError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val interestOptions = listOf(
    "历史文化", "美食", "自然", "亲子", "拍照", "夜景",
    "网红", "建筑", "科教", "游乐", "民俗", "购物",
)
private val transportOptions = listOf("地铁", "公交", "自驾", "打车", "步行")

@Composable
fun RoutePlanScreen(onOpenScenic: (Int) -> Unit) {
    var days by remember { mutableStateOf(2) }
    var people by remember { mutableStateOf(3) }
    var budget by remember { mutableStateOf(1000f) }
    var transport by remember { mutableStateOf("地铁") }
    var interests by remember { mutableStateOf(setOf("历史文化", "美食")) }
    var hasElder by remember { mutableStateOf(false) }
    var useGenetic by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<RoutePlanResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SectionTitle("🎯 行程偏好设置")

        Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = days.toString(),
                        onValueChange = { days = it.toIntOrNull()?.coerceIn(1, 7) ?: days },
                        label = { Text("天数") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = people.toString(),
                        onValueChange = { people = it.toIntOrNull()?.coerceIn(1, 20) ?: people },
                        label = { Text("人数") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text("总预算：¥${budget.toInt()}", fontSize = 13.sp)
                Slider(
                    value = budget,
                    onValueChange = { budget = it },
                    valueRange = 200f..5000f,
                    steps = 47,
                )
                Text("交通方式", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    transportOptions.forEach { t ->
                        FilterChip(
                            selected = transport == t,
                            onClick = { transport = t },
                            label = { Text(t, fontSize = 12.sp) },
                        )
                    }
                }
                Text("兴趣偏好（可多选）", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    interestOptions.take(6).forEach { tag ->
                        FilterChip(
                            selected = tag in interests,
                            onClick = {
                                interests = if (tag in interests) interests - tag else interests + tag
                            },
                            label = { Text(tag, fontSize = 12.sp) },
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    interestOptions.drop(6).forEach { tag ->
                        FilterChip(
                            selected = tag in interests,
                            onClick = {
                                interests = if (tag in interests) interests - tag else interests + tag
                            },
                            label = { Text(tag, fontSize = 12.sp) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasElder, onCheckedChange = { hasElder = it })
                    Text("行程包含老人（节奏放缓）", fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = useGenetic, onCheckedChange = { useGenetic = it })
                    Text("启用遗传算法多目标优化", fontSize = 13.sp)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                Button(
                    onClick = {
                        if (interests.isEmpty()) {
                            error = "请至少选择一个兴趣偏好"
                            return@Button
                        }
                        loading = true
                        error = null
                        saved = false
                        scope.launch {
                            try {
                                result = withContext(Dispatchers.IO) {
                                    AppRepository.planRoute(
                                        RoutePlanRequest(
                                            days = days,
                                            people = people,
                                            budget = budget.toDouble(),
                                            transport = transport,
                                            interests = interests.toList(),
                                            hasElder = hasElder,
                                            useGenetic = useGenetic,
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                error = friendlyError(e)
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                ) {
                    Text(if (loading) "生成中…" else "生成智能路线", fontSize = 15.sp)
                }
            }
        }

        result?.let { r ->
            SectionTitle("路线规划结果")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ResultStat("总时长", "${r.totalDurationHours}h", Modifier.weight(1f))
                    ResultStat("总距离", "${r.totalDistanceKm}km", Modifier.weight(1f))
                    ResultStat("总花费", "¥${r.totalCost}", Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(r.algorithm, fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold)

            // 把 GA 算出来的行程直接画在地图上：每天的景点连成一条线，按天配色，圆点上标出行程序号
            Spacer(Modifier.height(12.dp))
            SectionTitle("路线轨迹图")
            ScenicMap(
                points = r.days.flatMap { it.spots },
                height = 270.dp,
                routeSegments = r.days.map { d -> d.spots.map { it.id } },
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                r.days.forEachIndexed { i, d ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(Modifier.size(10.dp)) { drawCircle(DAY_COLORS[i % DAY_COLORS.size]) }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "第 ${d.day} 天",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            r.notes.forEach { note ->
                Surface(
                    color = if (note.contains("⚠")) Color(0xFFFFF7E6) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 6.dp).fillMaxWidth(),
                ) {
                    Text(note.removePrefix("⚠ "), fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                }
            }

            r.days.forEach { day ->
                Spacer(Modifier.height(14.dp))
                Card {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("第 ${day.day} 天", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                            Text(
                                "${day.durationHours}h · ${day.distanceKm}km · ¥${day.cost}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        day.spots.forEachIndexed { index, s ->
                            Spacer(Modifier.height(8.dp))
                            Row(
                                Modifier.fillMaxWidth().clickable { onOpenScenic(s.id) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Text(
                                        "${index + 1}",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(s.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${if (s.ticketPrice > 0) "¥${s.ticketPrice}" else "免费"} · ${s.durationMinutes}分钟 · ⭐${s.score}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (index < day.spots.size - 1) {
                                    Text("↓", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val res = withContext(Dispatchers.IO) {
                                AppRepository.saveRoute(r)
                            }
                            saved = true
                            error = res.message
                        } catch (e: Exception) {
                            error = friendlyError(e)
                        }
                    }
                },
                enabled = SessionManager.isLoggedIn,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (SessionManager.isLoggedIn) "保存我的路线" else "登录后可保存路线")
            }
            if (saved) {
                Spacer(Modifier.height(6.dp))
                Text("✓ 路线已保存，可在“我的 → 我的路线”查看", fontSize = 12.sp, color = Color(0xFF67C23A))
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ResultStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
    }
}
