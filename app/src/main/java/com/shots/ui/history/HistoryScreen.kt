package com.shots.ui.history

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.shots.ShotsApp
import com.shots.data.Screenshot
import com.shots.data.ScreenshotDatabase
import com.shots.util.MediaStoreUtils
import com.shots.util.TimerAlarmScheduler
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoIcon
import com.slapps.cupertino.CupertinoIconButton
import com.slapps.cupertino.CupertinoNavigateBackButton
import com.slapps.cupertino.CupertinoSegmentedControl
import com.slapps.cupertino.CupertinoSegmentedControlTab
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.CupertinoTopAppBar
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.icons.CupertinoIcons
import com.slapps.cupertino.icons.outlined.ChevronBackward
import com.slapps.cupertino.icons.outlined.MagnifyingGlass
import com.slapps.cupertino.section.CupertinoSection
import com.slapps.cupertino.section.SectionItem
import com.slapps.cupertino.section.SectionScope
import com.slapps.cupertino.theme.CupertinoTheme
import com.slapps.cupertino.theme.CupertinoColors
import com.slapps.cupertino.theme.systemOrange
import com.slapps.cupertino.theme.systemRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private fun formatCountdown(at: Long): String {
    val left = at - System.currentTimeMillis()
    if (left <= 0) return "overdue"
    val m = left / 60_000L
    if (m < 60) return "in ${m}m"
    val h = m / 60
    if (h < 24) return "in ${h}h ${m % 60}m"
    return "in ${h / 24}d ${h % 24}h"
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = ScreenshotDatabase.getInstance(context)
    val app = context.applicationContext as ShotsApp
    val scope = rememberCoroutineScope()
    val allScreenshots by db.screenshotDao().getAll().collectAsState(initial = emptyList())

    var selectedFilter by remember { mutableStateOf(0) }
    val filters = listOf("All", "Kept", "Pending", "Snoozed", "Deleted")

    val filteredScreenshots = when (filters[selectedFilter]) {
        "Kept" -> allScreenshots.filter { it.status == "kept" }
        "Pending" -> allScreenshots.filter { it.status == "pending" }
        "Snoozed" -> allScreenshots.filter { it.status == "snoozed" }
        "Deleted" -> allScreenshots.filter { it.status == "deleted" }
        else -> allScreenshots
    }

    Column(modifier = Modifier.fillMaxSize().background(CupertinoTheme.colorScheme.systemGroupedBackground)) {
        CupertinoTopAppBar(
            title = { CupertinoText("History") },
            navigationIcon = {
                CupertinoNavigateBackButton(onClick = onBack) {
                CupertinoText("Back")
            }
            }
        )
        CupertinoSegmentedControl(
            selectedTabIndex = selectedFilter,
            modifier = Modifier.fillMaxWidth(),
            tabs = {
                filters.forEachIndexed { i, filter ->
                    CupertinoSegmentedControlTab(
                        onClick = { selectedFilter = i },
                        isSelected = selectedFilter == i
                    ) {
                        CupertinoText(filter)
                    }
                }
            }
        )

        if (filteredScreenshots.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CupertinoIcon(
                    imageVector = CupertinoIcons.Outlined.MagnifyingGlass,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = CupertinoTheme.colorScheme.tertiaryLabel
                )
                Spacer(modifier = Modifier.height(16.dp))
                CupertinoText(
                    text = "No screenshots yet"
                )
            }
        } else {
            LazyColumn {
                item {
                    CupertinoSection {
                        filteredScreenshots.forEach { screenshot ->
                            val isPending = screenshot.status == "pending" && screenshot.scheduledDeletionAt > 0
                            val isSnoozed = screenshot.status == "snoozed" && screenshot.scheduledDeletionAt > 0
                            val isTimed = isPending || isSnoozed
                            ScreenshotItem(
                                screenshot = screenshot,
                                countdown = if (isTimed) formatCountdown(screenshot.scheduledDeletionAt) else null,
                                countdownLabel = if (isSnoozed) "Reminds" else "Deletes",
                                onCancel = if (isTimed) {
                                    {
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                if (isSnoozed) {
                                                    TimerAlarmScheduler.cancelSnooze(context, screenshot.path)
                                                } else {
                                                    TimerAlarmScheduler.cancel(context, screenshot.id)
                                                }
                                                db.screenshotDao().getByPath(screenshot.path)?.let {
                                                    db.screenshotDao().update(
                                                        it.copy(status = "kept", scheduledDeletionAt = 0L)
                                                    )
                                                }
                                            }
                                            app.trackScreenshotAction(if (isSnoozed) "snooze_cancelled" else "timer_cancelled")
                                        }
                                    }
                                } else null,
                                onExtend = if (isTimed) {
                                    {
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                val row = db.screenshotDao().getByPath(screenshot.path)
                                                if (row != null) {
                                                    val base = maxOf(row.scheduledDeletionAt, System.currentTimeMillis())
                                                    val next = base + 15 * 60_000L
                                                    db.screenshotDao().update(
                                                        row.copy(status = row.status, scheduledDeletionAt = next)
                                                    )
                                                    if (row.status == "snoozed") {
                                                        TimerAlarmScheduler.snoozeAt(context, row.path, next)
                                                    } else {
                                                        TimerAlarmScheduler.schedule(context, row.path, row.id, next)
                                                    }
                                                }
                                            }
                                            app.trackScreenshotAction(if (isSnoozed) "snooze_extended" else "timer_extended")
                                        }
                                    }
                                } else null,
                                onClick = {
                                    try {
                                        val uri = MediaStoreUtils.getUriForScreenshot(context, screenshot.path)
                                        if (uri != null) {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "image/*")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(intent)
                                        }
                                    } catch (_: Exception) { }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun SectionScope.ScreenshotItem(
    screenshot: Screenshot,
    countdown: String?,
    countdownLabel: String = "Deletes",
    onCancel: (() -> Unit)?,
    onExtend: (() -> Unit)?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val statusColor = when (screenshot.status) {
        "kept" -> CupertinoTheme.colorScheme.accent
        "deleted" -> CupertinoColors.systemRed
        "pending" -> CupertinoColors.systemOrange
        "snoozed" -> CupertinoColors.systemOrange
        else -> CupertinoTheme.colorScheme.secondaryLabel
    }

    SectionItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            val uri = remember(screenshot.path) {
                MediaStoreUtils.getUriForScreenshot(context, screenshot.path)
            }
            val model: Any? = uri ?: run {
                val f = File(screenshot.path)
                if (f.exists()) f else null
            }
            if (model != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = model),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CupertinoTheme.colorScheme.quaternarySystemFill),
                    contentAlignment = Alignment.Center
                ) {
                    CupertinoIcon(
                        imageVector = CupertinoIcons.Outlined.MagnifyingGlass,
                        contentDescription = null,
                        tint = CupertinoTheme.colorScheme.tertiaryLabel,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        trailingContent = {
            CupertinoText(
                text = screenshot.status.replaceFirstChar { it.uppercase() }
            )
        },
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                CupertinoText(
                    text = MediaStoreUtils.displayName(screenshot.path).take(30)
                )
                CupertinoText(
                    text = screenshot.timestamp
                )
                if (countdown != null) {
                    CupertinoText(
                        text = "$countdownLabel $countdown"
                    )
                    Row {
                        if (onCancel != null) {
                            CupertinoButton(
                                onClick = onCancel,
                                colors = CupertinoButtonDefaults.plainButtonColors()
                            ) {
                                CupertinoText("Cancel")
                            }
                        }
                        if (onExtend != null) {
                            CupertinoButton(
                                onClick = onExtend,
                                colors = CupertinoButtonDefaults.plainButtonColors()
                            ) {
                                CupertinoText("+15 min")
                            }
                        }
                    }
                }
            }
        }
    )
}
