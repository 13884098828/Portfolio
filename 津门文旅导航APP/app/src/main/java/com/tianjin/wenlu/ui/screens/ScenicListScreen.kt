package com.tianjin.wenlu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianjin.wenlu.data.AppRepository
import com.tianjin.wenlu.data.Scenic
import com.tianjin.wenlu.data.ServerMonitor
import com.tianjin.wenlu.ui.components.ErrorBox
import com.tianjin.wenlu.ui.components.LoadingBox
import com.tianjin.wenlu.ui.components.LocalImageStore
import com.tianjin.wenlu.ui.components.ScenicCover
import com.tianjin.wenlu.ui.components.ScenicMap
import com.tianjin.wenlu.ui.components.ScenicMapLegend
import com.tianjin.wenlu.ui.components.TagsRow
import com.tianjin.wenlu.ui.components.friendlyError
import com.tianjin.wenlu.ui.components.PriceText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val sortOptions = listOf(
    "heat" to "按热度",
    "score" to "按评分",
    "price" to "按门票",
)

@Composable
fun ScenicListScreen(onOpenScenic: (Int) -> Unit) {
    var keyword by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var sort by remember { mutableStateOf("heat") }
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    var scenics by remember { mutableStateOf<List<Scenic>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    val retryTick by ServerMonitor.retryTick.collectAsState()
    val scope = rememberCoroutineScope()

    fun load() {
        loading = true
        error = null
        scope.launch(Dispatchers.IO) {
            try {
                if (categories.isEmpty()) {
                    categories = runCatching { AppRepository.categories() }.getOrNull() ?: emptyList()
                }
                scenics = AppRepository.scenics(
                    keyword = keyword.trim(),
                    category = category.orEmpty(),
                    sort = sort,
                    limit = 50,
                )
            } catch (e: Exception) {
                error = friendlyError(e)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(retryTick) { load() }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            label = { Text("搜索景点 / 简介") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = category == null,
                onClick = { category = null },
                label = { Text("全部") },
            )
            categories.take(6).forEach { c ->
                FilterChip(
                    selected = category == c,
                    onClick = { category = c },
                    label = { Text(c) },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("共 ${scenics.size} 个景点", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("排序", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    sortOptions.first { it.first == sort }.second,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { sortMenuOpen = true }
                        .padding(8.dp),
                )
                DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                    sortOptions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { sort = value; sortMenuOpen = false },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        when {
            loading && scenics.isEmpty() -> LoadingBox("正在加载景点…")
            error != null && scenics.isEmpty() -> ErrorBox(error!!, onRetry = { load() })
            scenics.isEmpty() -> Text(
                "没有找到匹配的景点",
                modifier = Modifier.padding(32.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "scenic-map") {
                    ScenicDistributionCard(scenics, onOpenScenic)
                }
                items(scenics, key = { it.id }) { s ->
                    ScenicCard(s, onClick = { onOpenScenic(s.id) })
                }
            }
        }
    }
}

/**
 * 景点分布图卡片 —— 挂在列表最上面。
 * 用真实的 latitude / longitude 做墨卡托投影后自绘，不依赖任何地图 SDK、不需要网络，
 * 所以演示现场断网也照样显示。点击圆点先在图上选中，再点「查看详情」进详情页。
 */
@Composable
private fun ScenicDistributionCard(scenics: List<Scenic>, onOpen: (Int) -> Unit) {
    val ctx = LocalContext.current
    var selected by remember { mutableStateOf<Scenic?>(null) }
    val withPhoto = remember(scenics) { scenics.count { LocalImageStore.has(ctx, it.id) } }

    Column {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("景点分布图", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
                if (withPhoto > 0) "已收录 $withPhoto 张实拍图" else "点击圆点查看详情",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ScenicMap(
            points = scenics,
            height = 250.dp,
            selectedId = selected?.id,
            onSelect = { selected = it },
        )
        Spacer(Modifier.height(6.dp))

        // 选中后在图下方弹一条信息条：先选中、再决定要不要进详情，避免误触跳走
        selected?.let { s ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(s.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${s.category} · 🔥 ${s.heat} · ${PriceText(s.ticketPrice)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { onOpen(s.id) }) { Text("查看详情", fontSize = 13.sp) }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        ScenicMapLegend(scenics.map { it.category }.distinct())
    }
}

@Composable
fun ScenicCard(scenic: Scenic, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val hasPhoto = remember(scenic.id) { LocalImageStore.has(ctx, scenic.id) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column {
            // 封面：有实拍图就渲染实拍图，没有则用按 id 生成的渐变占位封面，
            // 两者尺寸一致，素材陆续到齐的过程中列表不会跳动。
            Box(Modifier.fillMaxWidth().height(104.dp)) {
                ScenicCover(
                    scenicId = scenic.id,
                    label = scenic.name,
                    subLabel = scenic.category,
                    modifier = Modifier.matchParentSize(),
                )
                Box(
                    Modifier.matchParentSize().background(
                        Brush.verticalGradient(
                            0.30f to Color.Transparent,
                            1f to Color(0xC4000000),
                        )
                    )
                )
                // 如实标注：这张是实拍图还是程序生成的占位图
                Text(
                    if (hasPhoto) "实拍图" else "示意封面",
                    fontSize = 9.sp,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color(0x73000000), RoundedCornerShape(50))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                )
                Row(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        scenic.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White,
                    )
                    Text(scenic.category, fontSize = 11.sp, color = Color(0xCCFFFFFF))
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(scenic.intro, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Spacer(Modifier.height(8.dp))
                TagsRow(scenic.tags)
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🔥 ${scenic.heat}", fontSize = 12.sp)
                    Text("⭐ ${scenic.score}", fontSize = 12.sp)
                    Text(
                        PriceText(scenic.ticketPrice),
                        fontSize = 12.sp,
                        color = if (scenic.ticketPrice > 0) MaterialTheme.colorScheme.tertiary else ColorGreen,
                    )
                }
            }
        }
    }
}

private val ColorGreen = androidx.compose.ui.graphics.Color(0xFF67C23A)
