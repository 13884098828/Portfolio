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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianjin.wenlu.data.AppRepository
import com.tianjin.wenlu.data.Scenic
import com.tianjin.wenlu.data.ServerMonitor
import com.tianjin.wenlu.ui.components.ErrorBox
import com.tianjin.wenlu.ui.components.LoadingBox
import com.tianjin.wenlu.ui.components.ScenicCover
import com.tianjin.wenlu.ui.components.friendlyError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(onBack: () -> Unit) {
    var favorites by remember { mutableStateOf<List<Scenic>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val retryTick by ServerMonitor.retryTick.collectAsState()
    val scope = rememberCoroutineScope()

    fun load() {
        loading = true
        error = null
        scope.launch(Dispatchers.IO) {
            try {
                favorites = AppRepository.favorites()
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
                title = { Text("我的收藏") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading && favorites.isEmpty() -> LoadingBox()
            error != null && favorites.isEmpty() -> ErrorBox(error!!, onRetry = { load() })
            favorites.isEmpty() -> Text(
                "暂无收藏，去景点详情页点 ♥ 收藏吧",
                modifier = Modifier.padding(32.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(favorites, key = { it.id }) { scenic ->
                    FavoriteScenicCard(
                        scenic = scenic,
                        onRemove = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    AppRepository.removeFavorite(scenic.id)
                                    favorites = favorites.filterNot { it.id == scenic.id }
                                } catch (e: Exception) {
                                    error = friendlyError(e)
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteScenicCard(scenic: Scenic, onRemove: () -> Unit) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            ScenicCover(
                scenicId = scenic.id,
                label = scenic.name,
                subLabel = scenic.category,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(
                        scenic.name,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        scenic.category + " · " + (if (scenic.ticketPrice > 0) "¥${scenic.ticketPrice}" else "免费"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "取消收藏",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(8.dp).clickable(onClick = onRemove),
                )
            }
        }
    }
}
