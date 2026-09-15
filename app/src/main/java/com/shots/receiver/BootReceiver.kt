package com.shots.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.shots.ShotsApp
import com.shots.util.TimerAlarmScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED,
            ACTION_RESTART_DETECTION -> {
                Log.d("BootReceiver", "Restart event ${intent.action}, starting detection service")
                ShotsApp.startDetectionService(context)
                TimerAlarmScheduler.rescheduleAll(context)
            }
        }
    }

    companion object {
        const val ACTION_RESTART_DETECTION = "com.shots.action.RESTART_DETECTION"
    }
}
