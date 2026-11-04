package com.tianjin.wenlu.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianjin.wenlu.ui.components.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于本应用") },
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
            SectionTitle("离线模式")
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Text(
                    "本应用为纯离线运行：景点、评论、用户与全部算法（路线规划、客流预测、情感分析、画像推荐、费用预算、安全预警）数据均内置在 APP 中，无需联网，也无需启动后端服务。",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(14.dp),
                )
            }

            SectionTitle("演示账号")
            Text("· demo / 123456（游客，文化探索型画像）", fontSize = 13.sp)
            Text("· admin / admin123（管理员）", fontSize = 13.sp)
            Text("· mom / youth / photo / relax，密码均为 123456", fontSize = 13.sp)

            SectionTitle("功能范围")
            Text("智能路线规划（遗传算法）、游客画像、混合推荐、客流预测、评价情感分析、费用预算、热度排行、安全预警均已内置，与 Web 端演示数据一致。", fontSize = 13.sp)

            SectionTitle("说明")
            Text("· 收藏、保存的路线、注册的新账号仅存储在本机，清除应用数据会丢失；", fontSize = 13.sp)
            Text("· 内置演示数据包含 30 个景点 / 12 条评论 / 6 个账号。", fontSize = 13.sp)
            Spacer(Modifier.height(24.dp))
        }
    }
}
