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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import com.shots.ui.theme.GlassTokens
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.shots.ui.components.MotionTokens
import com.shots.ui.components.isReducedMotion
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import coil.compose.rememberAsyncImagePainter
import com.komoui.components.Card
import com.komoui.components.IconButton
import com.shots.ui.components.ShotsBottomNav
import com.shots.ui.components.ShotsDestination
import com.shots.ui.components.SmoothTabs
import com.shots.ShotsApp
import com.shots.data.PreferencesManager
import com.shots.data.Screenshot
import com.shots.data.ScreenshotDatabase
import com.shots.ui.components.ShotsIcon
import com.shots.ui.components.ShotsText
import com.shots.ui.theme.AppAccents
import com.shots.ui.theme.ShotsTheme
import com.shots.util.DeleteSuppressor
import com.shots.util.MediaStoreUtils
import com.shots.util.TimerAlarmScheduler
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

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = ScreenshotDatabase.getInstance(context)
    val app = context.applicationContext as ShotsApp
    val scope = rememberCoroutineScope()
    val prefs = PreferencesManager(context)
    val accentIndex by prefs.accent.collectAsState(initial = 0)
    val accent = AppAccents.get(accentIndex)
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

    Column(
        modifier = Modifier.fillMaxSize().background(ShotsTheme.colorScheme.background)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.clickable(onClick = onBack).padding(8.dp)) {
                ShotsIcon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            ShotsText(text = "History", style = ShotsTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                context.startActivity(
                    Intent(context, com.shots.SettingsActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                )
            }) {
                ShotsIcon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
        Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
            SmoothTabs(
                tabs = filters,
                selectedTabIndex = selectedFilter,
                onTabSelected = { selectedFilter = it },
                selectedBg = accent.bg,
                selectedFg = accent.ink,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredScreenshots.isEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(96.dp)
                            .clip(RoundedCornerShape(GlassTokens.CardRadius))
                            .background(ShotsTheme.colorScheme.surfaceVariant)
                            .border(1.dp, ShotsTheme.colorScheme.outline, RoundedCornerShape(GlassTokens.CardRadius)),
                        contentAlignment = Alignment.Center
                    ) {
                        ShotsIcon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(40.dp), tint = ShotsTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ShotsText(text = "No screenshots yet", style = ShotsTheme.typography.titleMedium, color = ShotsTheme.colorScheme.secondary, textAlign = TextAlign.Center)
                }
            } else {
                val reducedList = isReducedMotion()
                LazyColumn {
                    itemsIndexed(filteredScreenshots, key = { _, s -> s.id }) { index, screenshot ->
                        var itemVisible by remember(screenshot.id, selectedFilter) { mutableStateOf(false) }
                        LaunchedEffect(screenshot.id, selectedFilter) {
                            if (reducedList) itemVisible = true
                            else {
                                delay((index * MotionTokens.StaggerMs).toLong().coerceAtMost(200L))
                                itemVisible = true
                            }
                        }
                        val isPending = screenshot.status == "pending" && screenshot.scheduledDeletionAt > 0
                        val isSnoozed = screenshot.status == "snoozed" && screenshot.scheduledDeletionAt > 0
                        val isTimed = isPending || isSnoozed
                        androidx.compose.foundation.layout.Box {

                        androidx.compose.animation.AnimatedVisibility(
                            visible = itemVisible || reducedList,
                            enter = fadeIn(tween(200, easing = MotionTokens.EaseOut)) +
                                    slideInVertically(tween(200, easing = MotionTokens.EaseOut)) { it / 10 }
                        ) {
                        ScreenshotItem(
                            screenshot = screenshot,
                            countdown = if (isTimed) formatCountdown(screenshot.scheduledDeletionAt) else null,
                            countdownLabel = if (isSnoozed) "Reminds" else "Deletes",
                            onCancel = if (isTimed) {
                                {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            if (isSnoozed) TimerAlarmScheduler.cancelSnooze(context, screenshot.path)
                                            else TimerAlarmScheduler.cancel(context, screenshot.id)
                                            db.screenshotDao().getByPath(screenshot.path)?.let {
                                                db.screenshotDao().update(it.copy(status = "kept", scheduledDeletionAt = 0L))
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
                                                db.screenshotDao().update(row.copy(status = row.status, scheduledDeletionAt = next))
                                                if (row.status == "snoozed") TimerAlarmScheduler.snoozeAt(context, row.path, next)
                                                else TimerAlarmScheduler.schedule(context, row.path, row.id, next)
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
                            },
                            onDelete = {
                                scope.launch {
                                    DeleteSuppressor.suppress(screenshot.path)
                                    withContext(Dispatchers.IO) {
                                        MediaStoreUtils.deleteScreenshot(context, screenshot.path)
                                        db.screenshotDao().getByPath(screenshot.path)?.let {
                                            db.screenshotDao().updateStatus(it.id, "deleted")
                                        }
                                    }
                                    app.trackScreenshotAction("deleted")
                                }
                            }
                        )
                        }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
        ShotsBottomNav(
            current = ShotsDestination.History,
            activeColor = accent.bg,
            onNavigate = { dest ->
                when (dest) {
                    ShotsDestination.Home ->
                        context.startActivity(
                            Intent(context, com.shots.MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                        )
                    ShotsDestination.Settings ->
                        context.startActivity(
                            Intent(context, com.shots.SettingsActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                        )
                    ShotsDestination.History -> Unit
                }
            }
        )
    }
}

@Composable
private fun ScreenshotItem(
    screenshot: Screenshot,
    countdown: String?,
    countdownLabel: String = "Deletes",
    onCancel: (() -> Unit)?,
    onExtend: (() -> Unit)?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    val statusColor = when (screenshot.status) {
        "kept" -> ShotsTheme.colorScheme.primary
        "deleted" -> ShotsTheme.colorScheme.error
        "pending" -> ShotsTheme.colorScheme.warning
        "snoozed" -> ShotsTheme.colorScheme.warning
        else -> ShotsTheme.colorScheme.secondary
    }

    Card {
        Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val uri by androidx.compose.runtime.produceState<android.net.Uri?>(initialValue = null, screenshot.path) {
                value = withContext(Dispatchers.IO) { MediaStoreUtils.getUriForScreenshot(context, screenshot.path) }
            }
            val model: Any? = uri ?: run {
                val f = File(screenshot.path)
                if (f.exists()) f else null
            }
            if (model != null) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = model,
                        placeholder = null,
                        error = null,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(ShotsTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    ShotsIcon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = ShotsTheme.colorScheme.outline, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                ShotsText(text = MediaStoreUtils.displayName(screenshot.path).take(30), style = ShotsTheme.typography.bodyMedium, color = ShotsTheme.colorScheme.onSurface)
                ShotsText(text = screenshot.timestamp, style = ShotsTheme.typography.bodySmall, color = ShotsTheme.colorScheme.secondary)
                if (countdown != null) {
                    ShotsText(text = "$countdownLabel $countdown", style = ShotsTheme.typography.bodySmall, color = ShotsTheme.colorScheme.warning)
                }
            }
            Column(modifier = Modifier.padding(start = 8.dp), horizontalAlignment = Alignment.End) {
                ShotsText(text = screenshot.status.replaceFirstChar { it.uppercase() }, style = ShotsTheme.typography.labelMedium, color = statusColor)
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        ShotsIcon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Row actions",
                            tint = ShotsTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open") },
                            onClick = { menuOpen = false; onClick() }
                        )
                        if (onCancel != null) {
                            DropdownMenuItem(
                                text = { Text("Cancel timer") },
                                onClick = { menuOpen = false; onCancel() }
                            )
                        }
                        if (onExtend != null) {
                            DropdownMenuItem(
                                text = { Text("+15 min") },
                                onClick = { menuOpen = false; onExtend() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete", color = ShotsTheme.colorScheme.error) },
                            onClick = { menuOpen = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}
