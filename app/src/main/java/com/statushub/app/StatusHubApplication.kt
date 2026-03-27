package com.statushub.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class StatusHubApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        
        // Initialize AdMob in background
        applicationScope.launch {
            initializeAdMob()
        }
        
        // Create notification channel
        createNotificationChannel()
    }

    private fun initializeAdMob() {
        MobileAds.initialize(this) {
            // AdMob initialized
        }
        
        // Configure test devices for debug builds
        if (BuildConfig.DEBUG) {
            val testDeviceIds = listOf(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf("TEST_DEVICE_ID"))
                    .build()
            )
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf("TEST_DEVICE_ID"))
                    .build()
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Status update notifications"
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "status_updates"
    }
}
