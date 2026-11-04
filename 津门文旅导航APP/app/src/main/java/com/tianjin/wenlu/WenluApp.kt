package com.tianjin.wenlu

import android.app.Application
import com.tianjin.wenlu.data.BehaviorStore
import com.tianjin.wenlu.data.FavoritesStore
import com.tianjin.wenlu.data.LocalUserStore
import com.tianjin.wenlu.data.RouteStore
import com.tianjin.wenlu.data.SessionManager
import com.tianjin.wenlu.data.offline.OfflineData
import com.tianjin.wenlu.data.offline.WeatherService

class WenluApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(applicationContext)
        FavoritesStore.init(applicationContext)
        BehaviorStore.init(applicationContext)
        RouteStore.init(applicationContext)
        LocalUserStore.init(applicationContext)
        OfflineData.load(applicationContext)
        WeatherService.init(applicationContext)
    }
}
