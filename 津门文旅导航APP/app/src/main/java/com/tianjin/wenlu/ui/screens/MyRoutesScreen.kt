package com.tianjin.wenlu.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianjin.wenlu.data.AppRepository
import com.tianjin.wenlu.data.RouteRecord
import com.tianjin.wenlu.data.ServerMonitor
import com.tianjin.wenlu.ui.components.ErrorBox
import com.tianjin.wenlu.ui.components.LoadingBox
import com.tianjin.wenlu.ui.components.friendlyError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRoutesScreen(onBack: () -> Unit) {
    var routes by remember { mutableStateOf<List<RouteRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var expandedId by remember { mutableStateOf<Int?>(null) }
    val retryTick by ServerMonitor.retryTick.collectAsState()
    val scope = rememberCoroutineScope()

    fun load() {
        loading = true
        error = null
        scope.launch(Dispatchers.IO) {
            try {
                routes = AppRepository.myRoutes()
            } catch (e: Exception) {
                error = friendlyError(e)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(retryTick) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的路线") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading && routes.isEmpty() -> LoadingBox()
            error != null && routes.isEmpty() -> ErrorBox(error!!, onRetry = { load() })
            routes.isEmpty() -> Text(
                "暂无保存的路线，去“路线”页生成并保存吧",
                modifier = Modifier.padding(32.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(routes, key = { it.id }) { route ->
                    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(route.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                Text("${route.days} 天", fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${route.distanceKm}km · ¥${route.cost} · ${route.createdAt.take(10)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (expandedId == route.id) "收起行程 ▲" else "展开每日行程 ▼",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        expandedId = if (expandedId == route.id) null else route.id
                                    },
                            )
                            if (expandedId == route.id) {
                                route.spots.groupBy { it.day }.forEach { (day, spots) ->
                                    Text("第 $day 天", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                                    spots.sortedBy { it.order }.forEach { spot ->
                                        Text("  ${spot.order + 1}. ${spot.name}", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
