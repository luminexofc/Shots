package com.shots.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import coil.compose.rememberAsyncImagePainter
import com.komoui.components.Card
import com.komoui.components.DialogAction
import com.shots.HistoryActivity
import com.shots.ShotsApp
import com.shots.data.PreferencesManager
import com.shots.data.ScreenshotDatabase
import com.shots.ui.components.SettingsRow
import com.shots.ui.components.SettingsSectionLabel
import com.shots.ui.components.ShotsBottomNav
import com.shots.ui.components.ShotsDestination
import com.shots.ui.components.AccentSlider
import com.shots.ui.components.ShotsIcon
import com.shots.ui.components.ShotsSwitch
import com.shots.ui.components.ShotsText
import com.shots.ui.components.SmoothDialog
import com.shots.ui.components.SmoothTabs
import com.shots.ui.theme.AppAccents
import com.shots.ui.theme.ShotsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = PreferencesManager(context)
    val app = context.applicationContext as ShotsApp
    val db = ScreenshotDatabase.getInstance(context)

    val timerMinutes by prefs.timerMinutes.collectAsState(initial = 5)
    val snoozeMinutes by prefs.snoozeMinutes.collectAsState(initial = 10)
    val darkMode by prefs.darkMode.collectAsState(initial = 0)
    val showEditButton by prefs.showEditButton.collectAsState(initial = false)
    val accentIndex by prefs.accent.collectAsState(initial = 0)
    val allScreenshots by db.screenshotDao().getAll().collectAsState(initial = emptyList())

    var showAbout by remember { mutableStateOf(false) }

    // Optimistic toggle — flips instantly instead of waiting for the DataStore round-trip.
    var editToggle by remember(showEditButton) { mutableStateOf(showEditButton) }

    val themeOptions = listOf("System", "Dark", "Light")
    // Optimistic theme flip: the 250ms palette crossfade starts on the tap
    // frame instead of waiting for the DataStore round-trip. Cleared once the
    // Flow catches up; a failed write snaps back to the stored value.
    var pendingTheme by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(darkMode) { pendingTheme = null }
    val accent = AppAccents.get(accentIndex)
    val versionName = remember {
        try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.2"
        } catch (_: Exception) {
            "1.0.2"
        }
    }
    val notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val keptCount = allScreenshots.count { it.status == "kept" }
    val freedBytes = allScreenshots.filter { it.status == "deleted" }.sumOf { it.fileSizeBytes }

    // Local theme override so the optimistic flip repaints this screen on the
    // tap frame; the activity-level theme follows when the Flow emits.
    ShotsTheme(darkMode = pendingTheme ?: darkMode) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShotsTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.clickable(onClick = onBack).padding(8.dp)
            ) {
                ShotsIcon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            ShotsText(text = "Settings", style = ShotsTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Card(modifier = Modifier.clickable { showAbout = true }) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    val appIcon = remember {
                        try {
                            context.packageManager.getApplicationIcon(context.packageName)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (appIcon != null) {
                        Image(
                            painter = rememberAsyncImagePainter(model = appIcon),
                            contentDescription = null,
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        ShotsIcon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = ShotsTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ShotsTheme.colorScheme.surfaceVariant)
                                .padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ShotsText(text = "Shots - Screenshot manager", style = ShotsTheme.typography.titleMedium, color = ShotsTheme.colorScheme.onSurface)
                        ShotsText(text = "v$versionName", style = ShotsTheme.typography.bodySmall, color = ShotsTheme.colorScheme.secondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SettingsSectionLabel("General")

            Spacer(modifier = Modifier.height(4.dp))
            TimerSlidersCard(prefs = prefs, app = app, timerMinutes = timerMinutes, snoozeMinutes = snoozeMinutes, accentBg = accent.bg)

            Spacer(modifier = Modifier.height(12.dp))
            Card {
                SettingsRow(
                    icon = Icons.Default.Folder,
                    title = "Storage",
                    subtitle = "${formatBytes(freedBytes)} freed · $keptCount kept",
                    onClick = {
                        context.startActivity(
                            Intent(context, HistoryActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Card {
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    ShotsText(text = "Appearance", style = ShotsTheme.typography.titleLarge, color = ShotsTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    ShotsText(text = "Choose your theme", style = ShotsTheme.typography.bodyMedium, color = ShotsTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    SmoothTabs(
                        tabs = themeOptions,
                        selectedTabIndex = pendingTheme ?: darkMode,
                        onTabSelected = { index ->
                            pendingTheme = index
                            CoroutineScope(Dispatchers.IO).launch {
                                prefs.setDarkMode(index)
                                app.trackSettingChanged("theme", themeOptions[index])
                            }
                        },
                        selectedBg = accent.bg,
                        selectedFg = accent.ink,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ShotsText(text = "Accent color", style = ShotsTheme.typography.titleMedium, color = ShotsTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    ShotsText(text = "Highlight for buttons and selections", style = ShotsTheme.typography.bodyMedium, color = ShotsTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppAccents.options.forEachIndexed { index, option ->
                            val selected = index == accentIndex
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(option.bg)
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) ShotsTheme.colorScheme.onSurface
                                        else ShotsTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable(
                                        role = Role.Button,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                        onClick = {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                prefs.setAccent(index)
                                                app.trackSettingChanged("accent", index)
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    ShotsIcon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected accent",
                                        tint = option.ink,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Card {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        ShotsText(text = "Edit Button", style = ShotsTheme.typography.titleLarge, color = ShotsTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        ShotsText(text = "Show system editor shortcut in the popup", style = ShotsTheme.typography.bodyMedium, color = ShotsTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.width(56.dp), contentAlignment = Alignment.Center) {
                        ShotsSwitch(
                            checked = editToggle,
                            onCheckedChange = { enabled ->
                                editToggle = enabled
                                CoroutineScope(Dispatchers.IO).launch { prefs.setShowEditButton(enabled) }
                                app.trackSettingChanged("edit_button", enabled)
                            },
                            activeColor = accent.bg
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SettingsSectionLabel("Privacy & security")

            Spacer(modifier = Modifier.height(4.dp))
            Card {
                SettingsRow(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = if (notificationsOn) "On" else "Off",
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                            )
                        } catch (_: Exception) {
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Card {
                SettingsRow(
                    icon = Icons.Default.Lock,
                    title = "Privacy & Security",
                    subtitle = "Permissions and battery",
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        } catch (_: Exception) {
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Card {
                Column(Modifier.fillMaxWidth()) {
                    SettingsRow(
                        icon = Icons.Default.BugReport,
                        title = "Report a bug",
                        subtitle = "Mail us with device details included",
                        onClick = {
                            try {
                                val model = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
                                val body = "App version: $versionName\nPhone model: $model\n\nDescribe the issue:\n"
                                context.startActivity(
                                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:luminexofc@gmail.com")).apply {
                                        putExtra(Intent.EXTRA_SUBJECT, "Shots bug report")
                                        putExtra(Intent.EXTRA_TEXT, body)
                                    }
                                )
                            } catch (_: Exception) {
                            }
                        }
                    )
                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.Help,
                        title = "Support Shots",
                        subtitle = "Donate to keep the app alive",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.supportkori.com/luminex"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Card {
                SettingsRow(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "Shots - Screenshot manager v$versionName",
                    onClick = { showAbout = true }
                )
            }
        }
        ShotsBottomNav(
            current = ShotsDestination.Settings,
            activeColor = accent.bg,
            onNavigate = { dest ->
                when (dest) {
                    ShotsDestination.Home ->
                        context.startActivity(
                            Intent(context, com.shots.MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                        )
                    ShotsDestination.History ->
                        context.startActivity(
                            Intent(context, HistoryActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                        )
                    ShotsDestination.Settings -> Unit
                }
            }
        )
    }

    if (showAbout) {
        SmoothDialog(
            onDismissRequest = { showAbout = false },
            title = "Shots - Screenshot manager",
            description = "Version $versionName\nYour screenshots, your rules.",
            buttons = {
                DialogAction(onClick = { showAbout = false }) {
                    Text("Close", color = accent.bg)
                }
            }
        )
    }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024 * 1024L) return "${bytes / 1024L} KB"
    val mb = bytes / (1024f * 1024f)
    return if (mb < 1024) "%.1f MB".format(mb) else "%.2f GB".format(mb / 1024)
}

@Composable
private fun TimerSlidersCard(
    prefs: PreferencesManager,
    app: ShotsApp,
    timerMinutes: Int,
    snoozeMinutes: Int,
    accentBg: androidx.compose.ui.graphics.Color
) {
    var sliderValue by remember { mutableFloatStateOf(timerMinutes.toFloat()) }
    var snoozeSlider by remember { mutableFloatStateOf(snoozeMinutes.toFloat()) }
    LaunchedEffect(timerMinutes) { sliderValue = timerMinutes.toFloat() }
    LaunchedEffect(snoozeMinutes) { snoozeSlider = snoozeMinutes.toFloat() }
    LaunchedEffect(sliderValue) {
        delay(400)
        val minutes = sliderValue.toInt()
        withContext(Dispatchers.IO) { prefs.setTimerMinutes(minutes) }
        app.trackSettingChanged("timer_minutes", minutes)
    }
    LaunchedEffect(snoozeSlider) {
        delay(400)
        val minutes = snoozeSlider.toInt()
        withContext(Dispatchers.IO) { prefs.setSnoozeMinutes(minutes) }
    }
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShotsIcon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = ShotsTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShotsText(text = "Auto-delete", style = ShotsTheme.typography.titleLarge, color = ShotsTheme.colorScheme.onSurface)
                    ShotsText(text = "Set timer for screenshots", style = ShotsTheme.typography.bodyMedium, color = ShotsTheme.colorScheme.secondary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ShotsText(text = "Timer: ${sliderValue.toInt()} minutes", style = ShotsTheme.typography.bodyMedium, color = ShotsTheme.colorScheme.onSurface)
            AccentSlider(
                value = ((sliderValue - 1f) / 59f).coerceIn(0f, 1f),
                onValueChange = { sliderValue = 1f + it * 59f },
                accent = accentBg,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            ShotsText(text = "Snooze: ${snoozeSlider.toInt()} minutes", style = ShotsTheme.typography.bodyMedium, color = ShotsTheme.colorScheme.onSurface)
            AccentSlider(
                value = ((snoozeSlider - 1f) / 59f).coerceIn(0f, 1f),
                onValueChange = { snoozeSlider = 1f + it * 59f },
                accent = accentBg,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
