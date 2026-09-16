package com.shots.ui.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.shots.ui.components.ShotsButton
import com.shots.ui.components.ShotsButtonVariant
import com.shots.ui.components.ShotsDialog
import com.shots.ui.components.ShotsText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shots.ShotsApp
import com.shots.data.PreferencesManager
import com.shots.data.Screenshot
import com.shots.data.ScreenshotDatabase
import com.shots.ui.components.ShotsCard
import com.shots.util.DeleteSuppressor
import com.shots.util.MediaStoreUtils
import com.shots.util.TimerAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val UNDO_WINDOW_MS = 5000L

@Composable
fun OverlayScreen(
    screenshotPath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val db = ScreenshotDatabase.getInstance(context)
    val prefs = PreferencesManager(context)
    var showTimerDialog by remember { mutableStateOf(false) }
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

    // No full-screen scrim — tapping outside the card dismisses.
    // The window itself is translucent (Theme.Shots.Overlay).
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        ShotsCard(
                modifier = Modifier
                    .padding(32.dp)
                    .clickable(
                        enabled = false,
                        onClick = {},
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ShotsText(
                        text = "Screenshot Detected",
                        style = ShotsTheme.typography.headlineMedium,
                        color = ShotsTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ShotsText(
                        text = "What would you like to do?",
                        style = ShotsTheme.typography.bodyMedium,
                        color = ShotsTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    ShotsButton(
                        onClick = {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    // If a timer was already scheduled for this path, cancel it
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
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ShotsText("Keep", modifier = Modifier.padding(vertical = 4.dp), color = ShotsTheme.colorScheme.onPrimary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ShotsButton(
                        onClick = {
                            coroutineScope.launch {
                                // Suppress popup for our own delete (no race)
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
                        },
                        modifier = Modifier.fillMaxWidth(),
                        destructive = true
                    ) {
                        ShotsText("Delete", modifier = Modifier.padding(vertical = 4.dp), color = ShotsTheme.colorScheme.onError)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            "$savedMinutes m" to savedMinutes,
                            "1 h" to 60,
                            "1 d" to 1440
                        )
                        presets.forEach { (label, minutes) ->
                            ShotsButton(
                                onClick = { scheduleMinutes(minutes) },
                                modifier = Modifier.weight(1f),
                                variant = ShotsButtonVariant.Outline
                            ) {
                                ShotsText(label)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (showEdit) {
                        ShotsButton(
                            onClick = {
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
                            },
                            modifier = Modifier.fillMaxWidth(),
                            variant = ShotsButtonVariant.Outline
                        ) {
                            ShotsText("Edit", modifier = Modifier.padding(vertical = 4.dp))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    ShotsButton(
                        onClick = { showTimerDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ShotsButtonVariant.Outline
                    ) {
                        ShotsText("Set Timer", modifier = Modifier.padding(vertical = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShotsButton(
                            variant = ShotsButtonVariant.Text,
                            onClick = {
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
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            ShotsText(
                                "Snooze ${snoozeAfter}m",
                                color = ShotsTheme.colorScheme.secondary
                            )
                        }
                        ShotsButton(
                            variant = ShotsButtonVariant.Text,
                            onClick = {
                                app.trackScreenshotAction("skipped")
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            ShotsText(
                                "Skip",
                                color = ShotsTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        if (showTimerDialog) {
            TimerPickerDialog(
                onDismiss = { showTimerDialog = false },
                onTimerSelected = { minutes -> scheduleMinutes(minutes) }
            )
        }

        if (undoActive) {
            LaunchedEffect(undoRowId) {
                delay(UNDO_WINDOW_MS + 500)
                onDismiss()
            }
            ShotsDialog(
                onDismissRequest = { },
                title = {
                    ShotsText(
                        "Deleted",
                        style = ShotsTheme.typography.titleLarge,
                        color = ShotsTheme.colorScheme.onSurface
                    )
                },
                body = {
                    ShotsText(
                        "Screenshot will be permanently deleted.",
                        style = ShotsTheme.typography.bodyMedium,
                        color = ShotsTheme.colorScheme.secondary
                    )
                },
                buttons = {
                    ShotsButton(onClick = { undoDelete() }) {
                        ShotsText("Undo", color = ShotsTheme.colorScheme.onPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ShotsButton(onClick = { confirmDeleteNow() }, destructive = true) {
                        ShotsText(
                            "Confirm",
                            color = ShotsTheme.colorScheme.onError
                        )
                    }
                }
            )
        }
    }

private fun fileSize(path: String): Long = try { File(path).length() } catch (_: Exception) { 0L }

private fun nowStamp(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

@Composable
private fun TimerPickerDialog(
    onDismiss: () -> Unit,
    onTimerSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    val prefs = PreferencesManager(context)
    val savedMinutes by prefs.timerMinutes.collectAsState(initial = 5)
    val options = remember(savedMinutes) {
        // Saved default first, then the standard options
        (listOf(savedMinutes) + listOf(1, 5, 15, 30, 60)).distinct().sorted()
    }

    ShotsDialog(
        onDismissRequest = onDismiss,
        title = {
            ShotsText(
                "Delete After",
                style = ShotsTheme.typography.titleLarge,
                color = ShotsTheme.colorScheme.onSurface
            )
        },
        body = {
            Column {
                options.forEach { minutes ->
                    val isDefault = minutes == savedMinutes
                    ShotsText(
                        text = if (isDefault) "$minutes min  •" else "$minutes min",
                        style = ShotsTheme.typography.bodyLarge,
                        color = if (isDefault) ShotsTheme.colorScheme.primary
                        else ShotsTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTimerSelected(minutes) }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        },
        buttons = { }
    )
}
