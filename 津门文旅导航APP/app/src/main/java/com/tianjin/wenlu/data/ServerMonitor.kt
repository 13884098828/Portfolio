package com.tianjin.wenlu.data

import com.tianjin.wenlu.ApiClient
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ServerStatus { CHECKING, REACHABLE, UNREACHABLE }

/**
 * 服务器连接状态监控：
 * - 页面请求成功/失败时上报，供全局提示使用；
 * - 服务器不可达时每 8 秒自动探测 /api/health；
 * - 恢复后递增 retryTick，数据页面据此自动重新加载。
 */
object ServerMonitor {

    private const val POLL_INTERVAL_MS = 8_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _status = MutableStateFlow(ServerStatus.CHECKING)
    val status: StateFlow<ServerStatus> = _status.asStateFlow()

    private val _retryTick = MutableStateFlow(0)
    val retryTick: StateFlow<Int> = _retryTick.asStateFlow()

    private var pollJob: Job? = null

    /** 立即探测一次后端健康接口，用于启动、网络恢复和修改服务器地址后。 */
    @Synchronized
    fun forceCheck() {
        scope.launch { healthCheck() }
    }

    /** 页面请求收到正常响应（或 HTTP 错误）时调用。 */
    fun onRequestSuccess() {
        markReachable(incrementTick = false)
    }

    /** 页面请求因 IO 失败时调用。 */
    fun onRequestFailure(cause: Throwable) {
        if (cause is IOException) {
            markUnreachable()
        } else {
            markReachable(incrementTick = false)
        }
    }

    /** 系统网络断开时调用。 */
    fun onNetworkLost() {
        markUnreachable()
    }

    private suspend fun healthCheck() {
        try {
            ApiClient.api.health()
            markReachable(incrementTick = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            markUnreachable()
        }
    }

    @Synchronized
    private fun markReachable(incrementTick: Boolean) {
        val previous = _status.value
        if (previous == ServerStatus.REACHABLE) return
        _status.value = ServerStatus.REACHABLE
        pollJob?.cancel()
        pollJob = null
        if (incrementTick && previous == ServerStatus.UNREACHABLE) {
            _retryTick.value += 1
        }
    }

    @Synchronized
    private fun markUnreachable() {
        _status.value = ServerStatus.UNREACHABLE
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                if (_status.value == ServerStatus.UNREACHABLE) {
                    healthCheck()
                }
            }
        }
    }
}
