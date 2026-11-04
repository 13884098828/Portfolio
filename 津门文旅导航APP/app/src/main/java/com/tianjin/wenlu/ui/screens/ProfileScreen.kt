package com.tianjin.wenlu.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianjin.wenlu.data.AppRepository
import com.tianjin.wenlu.data.Profile
import com.tianjin.wenlu.data.SessionManager
import com.tianjin.wenlu.data.ServerMonitor
import com.tianjin.wenlu.ui.components.SectionTitle
import com.tianjin.wenlu.ui.components.friendlyError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onOpenSettings: () -> Unit,
    onOpenMyRoutes: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenBudget: () -> Unit,
    onLogout: () -> Unit,
) {
    var profile by remember { mutableStateOf<Profile?>(null) }
    var profileError by remember { mutableStateOf<String?>(null) }
    val retryTick by ServerMonitor.retryTick.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(retryTick) {
        if (SessionManager.isLoggedIn) {
            scope.launch(Dispatchers.IO) {
                profileError = null
                try {
                    profile = AppRepository.profile()
                } catch (e: Exception) {
                    profileError = friendlyError(e)
                }
            }
        }
    }

    val user = SessionManager.currentUser()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (user == null) {
            Card {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("未登录", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("登录后可使用画像、收藏与路线保存功能", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(52.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(user.nickname.take(1), color = MaterialTheme.colorScheme.onSecondary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.size(14.dp))
                    Column {
                        Text(user.nickname, color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "@${user.username} · 天津 · ${user.consumptionLevel}消费",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            profile?.let { p ->
                SectionTitle("我的游客画像")
                Card {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("类型：${p.travelType}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("相似度 ${(p.similarity * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(p.summary, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            p.interests.take(6).forEach { interest ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                                ) {
                                    Text(
                                        "${interest.tag} ${(interest.weight * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            profileError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
        }

        SectionTitle("功能入口")
        Card {
            Column {
                MenuItem("我的路线", Icons.Filled.Map, onClick = onOpenMyRoutes)
                HorizontalDivider()
                MenuItem("我的收藏", Icons.Filled.Favorite, onClick = onOpenFavorites)
                HorizontalDivider()
                MenuItem("费用预算", Icons.Filled.AccountBalanceWallet, onClick = onOpenBudget)
                HorizontalDivider()
                MenuItem("服务器设置", Icons.Filled.Settings, onClick = onOpenSettings)
                if (user != null) {
                    HorizontalDivider()
                    MenuItem("退出登录", null, onClick = onLogout, danger = true)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MenuItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(12.dp))
        }
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
    }
}
