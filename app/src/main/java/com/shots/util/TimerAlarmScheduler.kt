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

    /**
     * Snooze: re-notify about this screenshot after delayMin minutes.
     * Fires SnoozeReceiver which posts a tap-to-review notification
     * (an alarm cannot start the popup activity directly from background).
     * The row stays status='snoozed' — never deleted by this path.
     */
    fun snooze(context: Context, path: String, delayMin: Int = 10) {
        snoozeAt(context, path, System.currentTimeMillis() + delayMin * 60_000L)
    }

    fun snoozeAt(context: Context, path: String, triggerAt: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, com.shots.SnoozeReceiver::class.java).apply {
            putExtra(com.shots.SnoozeReceiver.EXTRA_PATH, path)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (path.hashCode() xor 0x5eed),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                Log.w(TAG, "Exact alarms not permitted, falling back to inexact for snooze")
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                return
            }
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            Log.d(TAG, "Snooze scheduled at $triggerAt for $path")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm rejected, falling back to inexact for snooze", e)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule snooze", e)
        }
    }

    fun cancelSnooze(context: Context, path: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, com.shots.SnoozeReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (path.hashCode() xor 0x5eed),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pendingIntent)
    }

    fun cancel(context: Context, screenshotId: Long) {        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
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
     * Reschedule all pending timers + snoozed reminders (e.g. after device reboot).
     */
    fun rescheduleAll(context: Context) {
        Thread {
            try {
                val db = com.shots.data.ScreenshotDatabase.getInstance(context)
                val pending = kotlinx.coroutines.runBlocking {
                    db.screenshotDao().getPendingScheduledOnce()
                }
                val snoozed = kotlinx.coroutines.runBlocking {
                    db.screenshotDao().getSnoozedScheduledOnce()
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
                for (s in snoozed) {
                    if (s.scheduledDeletionAt > now) {
                        snoozeAt(context, s.path, s.scheduledDeletionAt)
                        rescheduled++
                    } else {
                        Log.d(TAG, "Expired snooze: ${s.path}")
                    }
                }
                Log.d(TAG, "Rescheduled $rescheduled pending timers/snoozes after boot")
            } catch (e: Exception) {
                Log.e(TAG, "rescheduleAll failed", e)
            }
        }.start()
    }
}
