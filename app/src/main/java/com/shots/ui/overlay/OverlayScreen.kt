package com.shots.ui.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.shots.ShotsApp
import com.shots.data.PreferencesManager
import com.shots.data.Screenshot
import com.shots.data.ScreenshotDatabase
import com.shots.util.DeleteSuppressor
import com.shots.util.MediaStoreUtils
import com.shots.util.TimerAlarmScheduler
import com.slapps.cupertino.CupertinoActionSheet
import com.slapps.cupertino.CupertinoAlertDialog
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val UNDO_WINDOW_MS = 5000L

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun OverlayScreen(
    screenshotPath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val db = ScreenshotDatabase.getInstance(context)
    val prefs = PreferencesManager(context)
    var showTimerSheet by remember { mutableStateOf(false) }
    var undoActive by remember { mutableStateOf(false) }
    var undoRowId by remember { mutableStateOf(-1L) }
    val coroutineScope = rememberCoroutineScope()
    val app = context.applicationContext as ShotsApp
    val savedMinutes by prefs.timerMinutes.collectAsState(initial = 5)
    val snoozeAfter by prefs.snoozeMinutes.collectAsState(initial = 10)
    val showEdit by prefs.showEditButton.collectAsState(initial = false)

    fun undoDelete() {
        val id = undoRowId
        undoActive = false
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                TimerAlarmScheduler.cancel(context, id)
                db.screenshotDao().getByPath(screenshotPath)?.let {
                    db.screenshotDao().update(it.copy(status = "kept", scheduledDeletionAt = 0L))
                }
            }
            app.trackScreenshotAction("undo")
            withContext(Dispatchers.Main) {
                onDismiss()
            }
        }
    }

    fun confirmDeleteNow() {
        val id = undoRowId
        undoActive = false
        coroutineScope.launch {
            DeleteSuppressor.suppress(screenshotPath)
            val deleted = withContext(Dispatchers.IO) {
                TimerAlarmScheduler.cancel(context, id)
                MediaStoreUtils.deleteScreenshot(context, screenshotPath)
            }
            if (deleted) {
                withContext(Dispatchers.IO) {
                    db.screenshotDao().getByPath(screenshotPath)?.let {
                        db.screenshotDao().updateStatus(it.id, "deleted")
                    }
                }
                app.trackScreenshotAction("delete_confirmed")
                withContext(Dispatchers.Main) { onDismiss() }
            } else {
                val rowId = withContext(Dispatchers.IO) {
                    db.screenshotDao().getByPath(screenshotPath)?.let {
                        db.screenshotDao().updateStatus(it.id, "pending")
                        it.id
                    } ?: id
                }
                app.trackScreenshotAction("delete_confirm_dialog")
                withContext(Dispatchers.Main) {
                    onDismiss()
                    if (rowId > 0) {
                        com.shots.ConfirmDeleteActivity.launch(context, screenshotPath, rowId)
                    }
                }
            }
        }
    }

    fun scheduleMinutes(minutes: Int) {
        coroutineScope.launch {
            val scheduledAt = System.currentTimeMillis() + minutes * 60_000L
            val rowId = withContext(Dispatchers.IO) {
                val existing = db.screenshotDao().getByPath(screenshotPath)
                TimerAlarmScheduler.cancelSnooze(context, screenshotPath)
                if (existing != null) {
                    db.screenshotDao().update(
                        existing.copy(
                            status = "pending",
                            scheduledDeletionAt = scheduledAt,
                            fileSizeBytes = fileSize(screenshotPath)
                        )
                    )
                    existing.id
                } else {
                    db.screenshotDao().insert(
                        Screenshot(
                            path = screenshotPath,
                            timestamp = nowStamp(),
                            status = "pending",
                            scheduledDeletionAt = scheduledAt,
                            fileSizeBytes = fileSize(screenshotPath)
                        )
                    )
                }
            }
            // Exact alarm — fires precisely even in Doze mode
            TimerAlarmScheduler.schedule(context, screenshotPath, rowId, scheduledAt)
            app.trackScreenshotAction("timer_set")
            withContext(Dispatchers.Main) {
                onDismiss()
            }
        }
    }

    fun keepScreenshot() {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val existing = db.screenshotDao().getByPath(screenshotPath)
                TimerAlarmScheduler.cancelSnooze(context, screenshotPath)
                if (existing != null) {
                    TimerAlarmScheduler.cancel(context, existing.id)
                    db.screenshotDao().update(
                        existing.copy(status = "kept", scheduledDeletionAt = 0L, fileSizeBytes = fileSize(screenshotPath))
                    )
                } else {
                    db.screenshotDao().insert(
                        Screenshot(
                            path = screenshotPath,
                            timestamp = nowStamp(),
                            status = "kept",
                            fileSizeBytes = fileSize(screenshotPath)
                        )
                    )
                }
            }
            app.trackScreenshotAction("kept")
            withContext(Dispatchers.Main) {
                onDismiss()
            }
        }
    }

    fun deleteScreenshot() {
        coroutineScope.launch {
            DeleteSuppressor.suppress(screenshotPath)
            // Delayed delete: receiver executes in 5s unless undone
            val fireAt = System.currentTimeMillis() + UNDO_WINDOW_MS
            val rowId = withContext(Dispatchers.IO) {
                val existing = db.screenshotDao().getByPath(screenshotPath)
                if (existing != null) {
                    TimerAlarmScheduler.cancel(context, existing.id)
                    db.screenshotDao().update(
                        existing.copy(status = "pending", scheduledDeletionAt = fireAt, fileSizeBytes = fileSize(screenshotPath))
                    )
                    existing.id
                } else {
                    db.screenshotDao().insert(
                        Screenshot(
                            path = screenshotPath,
                            timestamp = nowStamp(),
                            status = "pending",
                            scheduledDeletionAt = fireAt,
                            fileSizeBytes = fileSize(screenshotPath)
                        )
                    )
                }
            }
            TimerAlarmScheduler.schedule(context, screenshotPath, rowId, fireAt)
            undoRowId = rowId
            undoActive = true
            app.trackScreenshotAction("deleted")
        }
    }

    fun snoozeScreenshot() {
        coroutineScope.launch {
            val remindAt = System.currentTimeMillis() + snoozeAfter * 60_000L
            withContext(Dispatchers.IO) {
                val existing = db.screenshotDao().getByPath(screenshotPath)
                if (existing != null) {
                    TimerAlarmScheduler.cancel(context, existing.id)
                    db.screenshotDao().update(
                        existing.copy(
                            status = "snoozed",
                            scheduledDeletionAt = remindAt,
                            fileSizeBytes = fileSize(screenshotPath)
                        )
                    )
                } else {
                    db.screenshotDao().insert(
                        Screenshot(
                            path = screenshotPath,
                            timestamp = nowStamp(),
                            status = "snoozed",
                            scheduledDeletionAt = remindAt,
                            fileSizeBytes = fileSize(screenshotPath)
                        )
                    )
                }
            }
            TimerAlarmScheduler.snooze(context, screenshotPath, snoozeAfter)
            app.trackScreenshotAction("snoozed")
            withContext(Dispatchers.Main) {
                onDismiss()
            }
        }
    }

    fun editScreenshot() {
        coroutineScope.launch {
            try {
                val uri = withContext(Dispatchers.IO) {
                    MediaStoreUtils.getUriForScreenshot(context, screenshotPath)
                }
                if (uri != null) {
                    val editIntent = android.content.Intent(
                        android.content.Intent.ACTION_EDIT
                    ).apply {
                        setDataAndType(uri, "image/*")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(editIntent)
                    app.trackScreenshotAction("edit_opened")
                }
            } catch (_: Exception) {
            }
        }
    }

    CupertinoActionSheet(
        visible = true,
        onDismissRequest = {
            app.trackScreenshotAction("skipped")
            onDismiss()
        },
        title = { CupertinoText("Screenshot Detected") },
        message = { CupertinoText(MediaStoreUtils.displayName(screenshotPath)) },
        buttons = {
            default(onClick = { keepScreenshot() }) {
                CupertinoText("Keep")
            }
            destructive(onClick = { deleteScreenshot() }) {
                CupertinoText("Delete")
            }
            default(onClick = { scheduleMinutes(savedMinutes) }) {
                CupertinoText("$savedMinutes min")
            }
            default(onClick = { scheduleMinutes(60) }) {
                CupertinoText("1 hour")
            }
            default(onClick = { scheduleMinutes(1440) }) {
                CupertinoText("1 day")
            }
            default(onClick = { showTimerSheet = true }) {
                CupertinoText("More timers…")
            }
            if (showEdit) {
                default(onClick = { editScreenshot() }) {
                    CupertinoText("Edit")
                }
            }
            default(onClick = { snoozeScreenshot() }) {
                CupertinoText("Snooze ${snoozeAfter}m")
            }
            cancel(onClick = {
                app.trackScreenshotAction("skipped")
                onDismiss()
            }) {
                CupertinoText("Skip")
            }
        }
    )

    CupertinoActionSheet(
        visible = showTimerSheet,
        onDismissRequest = { showTimerSheet = false },
        title = { CupertinoText("Delete After") },
        buttons = {
            val options = (listOf(savedMinutes) + listOf(1, 5, 15, 30, 60)).distinct().sorted()
            options.forEach { minutes ->
                default(onClick = {
                    showTimerSheet = false
                    scheduleMinutes(minutes)
                }) {
                    CupertinoText(if (minutes == savedMinutes) "$minutes min •" else "$minutes min")
                }
            }
            cancel(onClick = { showTimerSheet = false }) {
                CupertinoText("Close")
            }
        }
    )

    if (undoActive) {
        LaunchedEffect(undoRowId) {
            delay(UNDO_WINDOW_MS + 500)
            onDismiss()
        }
        CupertinoAlertDialog(
            onDismissRequest = { },
            title = { CupertinoText("Deleted") },
            message = { CupertinoText("Screenshot will be permanently deleted.") },
            buttons = {
                cancel(onClick = { undoDelete() }) {
                    CupertinoText("Undo")
                }
                destructive(onClick = { confirmDeleteNow() }) {
                    CupertinoText("Confirm")
                }
            }
        )
    }
}

private fun fileSize(path: String): Long = try { File(path).length() } catch (_: Exception) { 0L }

private fun nowStamp(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
