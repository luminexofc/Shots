package com.shots.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.shots.ConfirmDeleteActivity

object NotificationHelper {

    private const val CONFIRM_CHANNEL_ID = "shots_confirm_delete"

    fun showConfirmDeleteNotification(context: Context, path: String, screenshotId: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CONFIRM_CHANNEL_ID,
                "Deletion Confirmations",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val confirmIntent = Intent(context, ConfirmDeleteActivity::class.java).apply {
            putExtra(ConfirmDeleteActivity.EXTRA_PATH, path)
            putExtra(ConfirmDeleteActivity.EXTRA_ID, screenshotId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val confirmPending = PendingIntent.getActivity(
            context,
            (screenshotId % Int.MAX_VALUE).toInt(),
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val filename = path.substringAfterLast('/')
        val notification = NotificationCompat.Builder(context, CONFIRM_CHANNEL_ID)
            .setContentTitle("Confirm deletion")
            .setContentText("Tap to permanently delete \"$filename\"")
            .setSmallIcon(android.R.drawable.ic_menu_delete)
            .setContentIntent(confirmPending)
            .setAutoCancel(true)
            .build()

        nm.notify((screenshotId % 100000).toInt(), notification)
    }

    fun cancelConfirmDeleteNotification(context: Context, screenshotId: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel((screenshotId % 100000).toInt())
    }
}
