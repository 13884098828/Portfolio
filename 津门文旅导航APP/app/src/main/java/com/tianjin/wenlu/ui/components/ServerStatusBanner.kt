package com.tianjin.wenlu.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianjin.wenlu.data.ServerMonitor
import com.tianjin.wenlu.data.ServerStatus
import kotlinx.coroutines.delay

@Composable
fun ServerStatusBanner(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val status by ServerMonitor.status.collectAsState()
    val retryTick by ServerMonitor.retryTick.collectAsState()
    var showRecovered by remember { mutableStateOf(false) }

    LaunchedEffect(retryTick) {
        if (retryTick > 0) {
            showRecovered = true
            delay(2_500)
            showRecovered = false
        }
    }

    if (status != ServerStatus.UNREACHABLE && !showRecovered) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (showRecovered) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (showRecovered) "连接已恢复，正在刷新…" else "无法连接服务器",
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize = 13.sp,
                )
                if (!showRecovered) {
                    Text(
                        "请检查后端服务与服务器设置",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!showRecovered) {
                TextButton(onClick = onRetry) { Text("重试", fontSize = 12.sp) }
                TextButton(onClick = onOpenSettings) { Text("服务器设置", fontSize = 12.sp) }
            }
        }
    }
}
