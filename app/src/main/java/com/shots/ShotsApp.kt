package com.shots

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.shots.worker.AutoDeleteWorker
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ShotsApp : Application() {

    lateinit var mixpanel: MixpanelAPI
        private set

    override fun onCreate() {
        super.onCreate()
        mixpanel = MixpanelAPI.getInstance(this, MIXPANEL_TOKEN, true)
        mixpanel.identify(MIXPANEL_DISTINCT_ID)
        val props = JSONObject().apply {
            put("platform", "android")
            put("app_version", BuildConfig.VERSION_NAME)
        }
        mixpanel.registerSuperProperties(props)
        mixpanel.flush()
        scheduleAutoDelete()
    }

    private fun scheduleAutoDelete() {
        val workRequest = PeriodicWorkRequestBuilder<AutoDeleteWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "auto_delete",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    companion object {
        private const val TAG = "ShotsApp"
        private const val MIXPANEL_TOKEN = "8abb513bc1c4ecafc9251a8d5e60b997"
        private const val MIXPANEL_DISTINCT_ID = "shots-android-user"

        fun startDetectionService(context: android.content.Context) {
            try {
                val serviceIntent = Intent(context, com.shots.service.ScreenshotDetectionService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start detection service", e)
            }
        }
    }

    fun trackAppOpened() {
        mixpanel.track("App Opened")
        mixpanel.flush()
    }

    fun trackScreenshotDetected() {
        mixpanel.track("Screenshot Detected")
        mixpanel.flush()
    }

    fun trackScreenshotAction(action: String, source: String = "notification") {
        val props = JSONObject().apply {
            put("action", action)
            put("source", source)
        }
        mixpanel.track("Screenshot Action", props)
        mixpanel.flush()
    }

    fun trackOnboardingCompleted() {
        mixpanel.track("Onboarding Completed")
        mixpanel.flush()
    }

    fun trackScreenView(screenName: String) {
        val props = JSONObject().apply {
            put("screen_name", screenName)
        }
        mixpanel.track("Screen Viewed", props)
        mixpanel.flush()
    }

    fun trackSettingChanged(setting: String, value: Any) {
        val props = JSONObject().apply {
            put("setting", setting)
            put("value", value)
        }
        mixpanel.track("Setting Changed", props)
        mixpanel.flush()
    }
}
