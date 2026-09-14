package com.shots.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.shots.TimerDeleteReceiver

/**
 * Schedules exact alarms for timer-based deletions.
 * Unlike WorkManager (deferred in Doze), exact alarms fire at the precise time
 * even when the device is idle or the app is backgrounded.
 */
object TimerAlarmScheduler {

    private const val TAG = "TimerAlarmScheduler"
    private const val EXTRA_PATH = "path"
    private const val EXTRA_ID = "screenshot_id"

    fun schedule(context: Context, path: String, screenshotId: Long, triggerAtMillis: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, TimerDeleteReceiver::class.java).apply {
            putExtra(EXTRA_PATH, path)
            putExtra(EXTRA_ID, screenshotId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            screenshotId.toInt(), // unique per screenshot row
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                // No exact-alarm permission — use inexact as fallback (still better than nothing)
                Log.w(TAG, "Exact alarms not permitted, falling back to inexact")
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                return
            }
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            Log.d(TAG, "Exact alarm scheduled for id=$screenshotId at $triggerAtMillis ($path)")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm rejected, falling back to inexact", e)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm", e)
        }
    }

    fun cancel(context: Context, screenshotId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TimerDeleteReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            screenshotId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pendingIntent)
    }

    /**
     * Reschedule all pending timers (e.g. after device reboot).
     */
    fun rescheduleAll(context: Context) {
        Thread {
            try {
                val db = com.shots.data.ScreenshotDatabase.getInstance(context)
                val pending = kotlinx.coroutines.runBlocking {
                    db.screenshotDao().getPendingScheduledOnce()
                }
                val now = System.currentTimeMillis()
                var rescheduled = 0
                for (s in pending) {
                    if (s.scheduledDeletionAt > now) {
                        schedule(context, s.path, s.id, s.scheduledDeletionAt)
                        rescheduled++
                    } else {
                        // Already expired — fire a one-shot worker to clean up now
                        Log.d(TAG, "Expired pending: ${s.path}")
                    }
                }
                Log.d(TAG, "Rescheduled $rescheduled/$pending.size pending timers after boot")
            } catch (e: Exception) {
                Log.e(TAG, "rescheduleAll failed", e)
            }
        }.start()
    }
}
