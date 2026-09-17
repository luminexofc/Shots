package com.shots.ui.main

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.komoui.components.Card
import com.komoui.components.DialogAction
import com.komoui.components.IconButton
import com.shots.HistoryActivity
import com.shots.PermissionsActivity
import com.shots.data.PreferencesManager
import com.shots.data.Screenshot
import com.shots.data.ScreenshotDatabase
import com.shots.ui.components.ShotsBottomNav
import com.shots.ui.components.ShotsDestination
import com.shots.ui.components.ShotsIcon
import com.shots.ui.components.ShotsText
import com.shots.ui.components.SmoothDialog
import com.shots.ui.theme.AppAccents
import com.shots.ui.theme.ShotsTheme
import com.shots.util.MediaStoreUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val ChipActive = Color(0xFFA7F3D0)
private val ChipActiveText = Color(0xFF0F1115)

private val DayBuckets = listOf("All", "Today", "Yesterday", "Week")

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val db = ScreenshotDatabase.getInstance(context)
    @Suppress("UNUSED_VARIABLE")
    val prefs = PreferencesManager(context)
    val scope = rememberCoroutineScope()

    val allScreenshots by db.screenshotDao().getAll().collectAsState(initial = emptyList())
    val accentIndex by prefs.accent.collectAsState(initial = 0)
    val accent = AppAccents.get(accentIndex)
    val totalBytes = allScreenshots.sumOf { it.fileSizeBytes }
    var selectedBucket by remember { mutableStateOf(0) }
    var showHowItWorks by remember { mutableStateOf(false) }

    val overlayGranted = Settings.canDrawOverlays(context)
    val filtered = remember(allScreenshots, selectedBucket) {
        filterBucket(allScreenshots, selectedBucket)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShotsTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                ShotsText(
                    text = "Screenshots",
                    style = ShotsTheme.typography.titleLarge
                )
                ShotsText(
                    text = "${allScreenshots.size} items · ${formatBytes(totalBytes)}",
                    style = ShotsTheme.typography.bodySmall,
                    color = ShotsTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = { showHowItWorks = true }) {
                ShotsIcon(Icons.Default.Info, contentDescription = "How it works")
            }
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
        if (!overlayGranted) {
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).clickable {
                    context.startActivity(Intent(context, PermissionsActivity::class.java))
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShotsIcon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ShotsTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    ShotsText(
                        text = "Grant overlay permission to detect screenshots",
                        style = ShotsTheme.typography.bodySmall,
                        color = ShotsTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DayBuckets.forEachIndexed { index, label ->
                TimeChip(
                    label = label,
                    selected = index == selectedBucket,
                    onClick = { selectedBucket = index },
                    selectedBg = accent.bg,
                    selectedFg = accent.ink
                )
            }
        }
        if (filtered.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(96.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ShotsTheme.colorScheme.surfaceVariant)
                        .border(1.dp, ShotsTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    ShotsIcon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = ShotsTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                ShotsText(
                    text = if (selectedBucket == 0) "No screenshots yet" else "Nothing ${DayBuckets[selectedBucket].lowercase()}",
                    style = ShotsTheme.typography.titleMedium,
                    color = ShotsTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { screenshot ->
                    HomeGridItem(
                        screenshot = screenshot,
                        onClick = {
                            scope.launch {
                                try {
                                    val uri = withContext(Dispatchers.IO) {
                                        MediaStoreUtils.getUriForScreenshot(context, screenshot.path)
                                    }
                                    if (uri != null) {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "image/*")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    }
                                } catch (_: Exception) {
                                }
                            }
                        }
                    )
                }
            }
        }
        ShotsBottomNav(
            current = ShotsDestination.Home,
            activeColor = accent.bg,
            onNavigate = { dest ->
                when (dest) {
                    ShotsDestination.History ->
                        context.startActivity(
                            Intent(context, HistoryActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                        )
                    ShotsDestination.Settings ->
                        context.startActivity(
                            Intent(context, com.shots.SettingsActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                        )
                    ShotsDestination.Home -> Unit
                }
            }
        )
    }

    if (showHowItWorks) {
        SmoothDialog(
            onDismissRequest = { showHowItWorks = false },
            title = "How It Works",
            body = {
                Column {
                    HowItWorksStep("1", "Take a screenshot like normal")
                    Spacer(modifier = Modifier.height(8.dp))
                    HowItWorksStep("2", "A popup appears instantly")
                    Spacer(modifier = Modifier.height(8.dp))
                    HowItWorksStep("3", "Keep it, delete it, or set a timer")
                    Spacer(modifier = Modifier.height(8.dp))
                    HowItWorksStep("4", "View all screenshots in History")
                }
            },
            buttons = {
                DialogAction(onClick = { showHowItWorks = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
private fun TimeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedBg: androidx.compose.ui.graphics.Color = ChipActive,
    selectedFg: androidx.compose.ui.graphics.Color = ChipActiveText
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) selectedBg else ShotsTheme.colorScheme.surfaceVariant)
            .clickable(
                role = Role.Tab,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        ShotsText(
            text = label,
            style = ShotsTheme.typography.labelMedium,
            color = if (selected) selectedFg else ShotsTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun HomeGridItem(
    screenshot: Screenshot,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val uri by produceState<android.net.Uri?>(initialValue = null, screenshot.path) {
        value = withContext(Dispatchers.IO) {
            MediaStoreUtils.getUriForScreenshot(context, screenshot.path)
        }
    }
    val model: Any? = uri ?: run {
        val f = File(screenshot.path)
        if (f.exists()) f else null
    }
    Column(
        modifier = Modifier
            .clickable(
                role = Role.Button,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
    ) {
        if (model != null) {
            Image(
                painter = rememberAsyncImagePainter(model = model),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ShotsTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                ShotsIcon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = ShotsTheme.colorScheme.outline,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        ShotsText(
            text = relativeLabel(screenshot.timestamp),
            style = ShotsTheme.typography.bodySmall,
            color = ShotsTheme.colorScheme.secondary,
            maxLines = 1
        )
    }
}

private fun parseStamp(raw: String): Date? = try {
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(raw)
} catch (_: Exception) {
    null
}

private fun dayStart(time: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = time
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun filterBucket(all: List<Screenshot>, bucket: Int): List<Screenshot> {
    if (bucket == 0) return all
    val now = System.currentTimeMillis()
    val today = dayStart(now)
    val dayMs = 24 * 60 * 60 * 1000L
    return all.filter { s ->
        val t = parseStamp(s.timestamp)?.time ?: return@filter false
        when (bucket) {
            1 -> t >= today
            2 -> t >= today - dayMs && t < today
            else -> t >= today - 7 * dayMs && t < today - dayMs
        }
    }
}

private fun relativeLabel(raw: String): String {
    val t = parseStamp(raw)?.time ?: return ""
    val now = System.currentTimeMillis()
    val diff = (now - t).coerceAtLeast(0)
    val minutes = diff / 60_000L
    if (minutes < 1) return "Just now"
    if (minutes < 60) return "${minutes}m ago"
    val hours = minutes / 60
    if (hours < 24 && t >= dayStart(now)) return "${hours}h ago"
    if (t >= dayStart(now) - 24 * 60 * 60 * 1000L) return "Yesterday"
    val days = (dayStart(now) - dayStart(t)) / (24 * 60 * 60 * 1000L)
    if (days <= 7) return "${days} days ago"
    return SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(t))
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024 * 1024L) return "${bytes / 1024L} KB"
    val mb = bytes / (1024f * 1024f)
    return if (mb < 1024) "%.1f MB".format(mb) else "%.2f GB".format(mb / 1024)
}

@Composable
private fun HowItWorksStep(step: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ShotsIcon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = ShotsTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        ShotsText(
            text = text,
            style = ShotsTheme.typography.bodyLarge,
            color = ShotsTheme.colorScheme.onSurface
        )
    }
}
