package com.shots

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.shots.util.NotificationHelper
import java.io.File

class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val path = intent.getStringExtra(EXTRA_PATH) ?: return
        if (!File(path).exists()) {
            Log.d(TAG, "Snoozed file gone, skipping: $path")
            return
        }
        Log.d(TAG, "Snooze fired for: $path")
        NotificationHelper.showSnoozeNotification(context, path)
    }

    companion object {
        private const val TAG = "SnoozeReceiver"
        const val EXTRA_PATH = "path"
    }
}
