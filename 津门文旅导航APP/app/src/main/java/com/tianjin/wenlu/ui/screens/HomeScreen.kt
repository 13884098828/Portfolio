package com.tianjin.wenlu.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianjin.wenlu.data.AppRepository
import com.tianjin.wenlu.data.Dashboard
import com.tianjin.wenlu.data.WeatherState
import com.tianjin.wenlu.data.Scenic
import com.tianjin.wenlu.data.ServerMonitor
import com.tianjin.wenlu.ui.components.ErrorBox
import com.tianjin.wenlu.ui.components.LoadingBox
import com.tianjin.wenlu.ui.components.MetricCard
import com.tianjin.wenlu.ui.components.ScenicCover
import com.tianjin.wenlu.ui.components.SectionTitle
import com.tianjin.wenlu.ui.components.TagsRow
import com.tianjin.wenlu.ui.components.friendlyError
import com.tianjin.wenlu.ui.components.PriceText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onOpenScenic: (Int) -> Unit,
    onOpenBudget: () -> Unit,
    onOpenRoute: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var dashboard by remember { mutableStateOf<Dashboard?>(null) }
    var hotScenics by remember { mutableStateOf<List<Scenic>>(emptyList()) }
    var warnings by remember { mutableStateOf<List<com.tianjin.wenlu.data.SafetyWarning>>(emptyList()) }
    var weather by remember { mutableStateOf<WeatherState?>(null) }
    val retryTick by ServerMonitor.retryTick.collectAsState()
    val scope = rememberCoroutineScope()

    fun load() {
        loading = true
        error = null
        scope.launch(Dispatchers.IO) {
            try {
                dashboard = runCatching { AppRepository.dashboard() }.getOrNull()
                hotScenics = runCatching { AppRepository.hotScenics(8) }.getOrNull() ?: emptyList()
                warnings = runCatching { AppRepository.safety().warnings }.getOrNull() ?: emptyList()
                weather = runCatching { AppRepository.weatherState() }.getOrNull()
            } catch (e: Exception) {
                error = friendlyError(e)
            } finally {
                loading = false
            }

            // 联网刷新实时天气放最后、不阻塞上面渲染：断网时只是等一个 connectTimeout，首页此时已能显示。
            // 拿到新天气后重算一次驾驶舱——客流预测数字本身就乘了天气系数。
            val ok = runCatching { AppRepository.refreshWeather() }.getOrDefault(false)
            if (ok) {
                dashboard = runCatching { AppRepository.dashboard() }.getOrNull()
                warnings = runCatching { AppRepository.safety().warnings }.getOrNull() ?: emptyList()
                weather = runCatching { AppRepository.weatherState() }.getOrNull()
            }
        }
    }

    LaunchedEffect(retryTick) { load() }

    when {
        loading && dashboard == null -> LoadingBox("正在加载首页数据…")
        error != null && dashboard == null && hotScenics.isEmpty() -> ErrorBox(error!!, onRetry = { load() })
        else -> Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text("津门文旅", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("智能规划行程 · 数据分析辅助决策", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))

            // 指标卡
            val cards = dashboard?.cards.orEmpty()
            if (cards.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    cards.take(2).forEach { c ->
                        MetricCard(c.label, c.value, c.trend, Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    cards.drop(2).take(2).forEach { c ->
                        MetricCard(c.label, c.value, c.trend, Modifier.weight(1f))
                    }
                }
            }

            // 天气是客流预测的输入之一，必须让用户看见是哪个值在乘系数
            weather?.let { w ->
                val tip = when (w.source) {
                    "实时" -> "实时预报"
                    "缓存" -> "本地缓存"
                    "手动" -> "手动指定"
                    else -> "默认，未联网"
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "客流预测按「${w.weather}」（$tip）估算，可在景点详情页切换天气",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 快捷入口
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickEntry("智能路线规划", Icons.Filled.Map, onClick = onOpenRoute, Modifier.weight(1f))
                QuickEntry("费用预算", Icons.Filled.AccountBalanceWallet, onClick = onOpenBudget, Modifier.weight(1f))
            }

            // 安全预警
            if (warnings.isNotEmpty()) {
                SectionTitle("安全预警")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("未来客流偏高景区 ${warnings.size} 个", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        warnings.take(3).forEach { w ->
                            Spacer(Modifier.height(8.dp))
                            Text("⚠ ${w.scenicName}（${w.crowdLevel}）：建议错峰或选择替代景点", fontSize = 12.sp)
                        }
                    }
                }
            }

            // 热门景点
            SectionTitle("当前热门景点")
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                hotScenics.forEach { s ->
                    HotScenicCard(s, onClick = { onOpenScenic(s.id) })
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuickEntry(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            Text(title, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun HotScenicCard(scenic: Scenic, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(180.dp).clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            // 卡片顶部一张封面：有实拍图用实拍图，没有就是按 id 生成的占位封面
            ScenicCover(
                scenicId = scenic.id,
                label = scenic.name,
                subLabel = scenic.category,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(scenic.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.width(110.dp))
                Text(scenic.category, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            TagsRow(scenic.tags)
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🔥 ${scenic.heat}", fontSize = 11.sp)
                Text("⭐ ${scenic.score}", fontSize = 11.sp)
                Text(PriceText(scenic.ticketPrice), fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}
