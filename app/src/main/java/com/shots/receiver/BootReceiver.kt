package com.shots.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.shots.ShotsApp
import com.shots.util.TimerAlarmScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed, starting detection service")
            ShotsApp.startDetectionService(context)
            // Exact alarms don't survive reboot — reschedule all pending timers
            TimerAlarmScheduler.rescheduleAll(context)
        }
    }
}
