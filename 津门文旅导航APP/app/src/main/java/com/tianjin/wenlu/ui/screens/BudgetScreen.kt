package com.tianjin.wenlu.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianjin.wenlu.data.AppRepository
import com.tianjin.wenlu.data.BudgetRequest
import com.tianjin.wenlu.data.BudgetResult
import com.tianjin.wenlu.ui.components.SectionTitle
import com.tianjin.wenlu.ui.components.friendlyError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val hotelLevels = listOf("经济", "舒适", "高档")
private val transportOptions = listOf("地铁", "公交", "自驾", "打车")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(onBack: () -> Unit) {
    var days by remember { mutableStateOf(3) }
    var people by remember { mutableStateOf(2) }
    var budget by remember { mutableStateOf(1500) }
    var transport by remember { mutableStateOf("地铁") }
    var hotelLevel by remember { mutableStateOf("舒适") }
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<BudgetResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("费用预算") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
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
                    OutlinedTextField(
                        value = budget.toString(),
                        onValueChange = { budget = it.toIntOrNull()?.coerceIn(100, 100000) ?: budget },
                        label = { Text("预算上限（元）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
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
                    Text("住宿档次", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        hotelLevels.forEach { level ->
                            FilterChip(
                                selected = hotelLevel == level,
                                onClick = { hotelLevel = level },
                                label = { Text(level, fontSize = 12.sp) },
                            )
                        }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    Button(
                        onClick = {
                            loading = true
                            error = null
                            scope.launch {
                                try {
                                    result = withContext(Dispatchers.IO) {
                                        AppRepository.budget(
                                            BudgetRequest(days, people, budget.toDouble(), transport, hotelLevel)
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
                        Text(if (loading) "估算中…" else "开始估算", fontSize = 15.sp)
                    }
                }
            }

            result?.let { r ->
                SectionTitle("费用构成（$days 天 $people 人）")
                Card {
                    Column(Modifier.padding(14.dp)) {
                        val percent = (r.totalCost / budget * 100).toInt().coerceAtMost(100)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Text("总花费 ¥${r.totalCost} / 预算 ¥$budget", fontWeight = FontWeight.Bold)
                            Text(
                                if (r.withinBudget) "预算内 ✓" else "超出预算 ✗",
                                color = if (r.withinBudget) Color(0xFF67C23A) else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { percent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (r.withinBudget) Color(0xFF67C23A) else MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FeeItem("交通", r.transportCost, Modifier.weight(1f))
                            FeeItem("门票", r.ticketCost, Modifier.weight(1f))
                            FeeItem("餐饮", r.foodCost, Modifier.weight(1f))
                            FeeItem("住宿", r.hotelCost, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("智能建议", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        r.tips.forEach { tip ->
                            Text("· $tip", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FeeItem(label: String, value: Double, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("¥$value", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
