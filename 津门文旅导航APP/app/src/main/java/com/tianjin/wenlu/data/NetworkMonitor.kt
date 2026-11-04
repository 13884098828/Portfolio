package com.tianjin.wenlu.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 监听系统网络连接变化：网络断开时提示，恢复后立即触发服务器健康探测。
 */
object NetworkMonitor {

    private val _connected = MutableStateFlow(true)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private var connectivityManager: ConnectivityManager? = null

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _connected.value = true
            ServerMonitor.forceCheck()
        }

        override fun onLost(network: Network) {
            _connected.value = false
            ServerMonitor.onNetworkLost()
        }
    }

    fun init(context: Context) {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager = manager
        _connected.value = hasInternet(manager)
        if (!_connected.value) {
            ServerMonitor.onNetworkLost()
        }
        manager.registerDefaultNetworkCallback(callback)
    }

    private fun hasInternet(manager: ConnectivityManager): Boolean {
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
