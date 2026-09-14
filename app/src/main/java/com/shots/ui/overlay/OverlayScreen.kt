package com.shots.ui.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OverlayScreen(
    screenshotPath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val db = ScreenshotDatabase.getInstance(context)
    val prefs = PreferencesManager(context)
    var showTimerDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val app = context.applicationContext as ShotsApp

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
                    Text(
                        text = "Screenshot Detected",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "What would you like to do?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    // If a timer was already scheduled for this path, cancel it
                                    val existing = db.screenshotDao().getByPath(screenshotPath)
                                    if (existing != null) {
                                        TimerAlarmScheduler.cancel(context, existing.id)
                                        db.screenshotDao().update(
                                            existing.copy(status = "kept", scheduledDeletionAt = 0L)
                                        )
                                    } else {
                                        db.screenshotDao().insert(
                                            Screenshot(
                                                path = screenshotPath,
                                                timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                                                status = "kept"
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Keep", modifier = Modifier.padding(vertical = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                // Suppress popup for our own delete (instant, no race)
                                DeleteSuppressor.suppress(screenshotPath)
                                val actuallyDeleted = withContext(Dispatchers.IO) {
                                    MediaStoreUtils.deleteScreenshot(context, screenshotPath)
                                }
                                if (actuallyDeleted) {
                                    withContext(Dispatchers.IO) {
                                        val latest = db.screenshotDao().getAllOnce()
                                            .firstOrNull { it.path == screenshotPath }
                                        if (latest != null) {
                                            db.screenshotDao().updateStatus(latest.id, "deleted")
                                        } else {
                                            db.screenshotDao().insert(
                                                Screenshot(
                                                    path = screenshotPath,
                                                    timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                                                    status = "deleted"
                                                )
                                            )
                                        }
                                    }
                                    app.trackScreenshotAction("deleted")
                                    withContext(Dispatchers.Main) { onDismiss() }
                                } else {
                                    // Direct delete blocked (no All Files Access) —
                                    // use the system confirmation dialog, which always works
                                    val rowId = withContext(Dispatchers.IO) {
                                        val existing = db.screenshotDao().getAllOnce()
                                            .firstOrNull { it.path == screenshotPath }
                                        if (existing != null) {
                                            db.screenshotDao().updateStatus(existing.id, "pending")
                                            existing.id
                                        } else {
                                            db.screenshotDao().insert(
                                                Screenshot(
                                                    path = screenshotPath,
                                                    timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                                                    status = "pending"
                                                )
                                            )
                                        }
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
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Delete", modifier = Modifier.padding(vertical = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showTimerDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Set Timer", modifier = Modifier.padding(vertical = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = {
                        app.trackScreenshotAction("skipped")
                        onDismiss()
                    }) {
                        Text(
                            "Skip",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        if (showTimerDialog) {
            TimerPickerDialog(
                onDismiss = { showTimerDialog = false },
                onTimerSelected = { minutes ->
                    coroutineScope.launch {
                        val scheduledAt = System.currentTimeMillis() + minutes * 60_000L
                        val rowId = withContext(Dispatchers.IO) {
                            val existing = db.screenshotDao().getByPath(screenshotPath)
                            if (existing != null) {
                                db.screenshotDao().update(
                                    existing.copy(
                                        status = "pending",
                                        scheduledDeletionAt = scheduledAt
                                    )
                                )
                                existing.id
                            } else {
                                db.screenshotDao().insert(
                                    Screenshot(
                                        path = screenshotPath,
                                        timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                                        status = "pending",
                                        scheduledDeletionAt = scheduledAt
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
            )
        }
    }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Delete After",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                options.forEach { minutes ->
                    val isDefault = minutes == savedMinutes
                    Text(
                        text = if (isDefault) "$minutes min  •" else "$minutes min",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDefault) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTimerSelected(minutes) }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = {},
        containerColor = MaterialTheme.colorScheme.surface
    )
}
