package com.shots.ui.main

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import com.shots.ui.components.ShotsIcon
import com.shots.ui.components.ShotsText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shots.PermissionsActivity
import com.shots.data.PreferencesManager
import com.shots.data.ScreenshotDatabase
import com.shots.ui.components.ShotsButton
import com.shots.ui.components.ShotsCard
import com.shots.ui.components.ShotsDialog
import com.shots.ui.components.ShotsIconButton
import com.shots.ui.components.ShotsTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val db = ScreenshotDatabase.getInstance(context)
    val prefs = PreferencesManager(context)

    val allScreenshots by db.screenshotDao().getAll().collectAsState(initial = emptyList())
    val pendingCount = allScreenshots.count { it.status == "pending" }
    val keptCount = allScreenshots.count { it.status == "kept" }
    val deletedCount = allScreenshots.count { it.status == "deleted" }
    val freedBytes = allScreenshots.filter { it.status == "deleted" }.sumOf { it.fileSizeBytes }

    var showHowItWorks by remember { mutableStateOf(false) }

    val overlayGranted = Settings.canDrawOverlays(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShotsTheme.colorScheme.background)
    ) {
        ShotsTopBar(
            title = "Shots",
            actions = {
                ShotsIconButton(onClick = {
                    context.startActivity(Intent(context, com.shots.SettingsActivity::class.java))
                }) {
                    ShotsShotsIcon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (!overlayGranted) {
                ShotsCard(
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(context, PermissionsActivity::class.java))
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShotsIcon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = ShotsTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            ShotsText(
                                text = "Permissions Required",
                                style = ShotsTheme.typography.titleMedium,
                                color = ShotsTheme.colorScheme.onSurface
                            )
                            ShotsText(
                                text = "Grant overlay permission to detect screenshots",
                                style = ShotsTheme.typography.bodySmall,
                                color = ShotsTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            ShotsCard {
                ShotsText(
                    text = "Overview",
                    style = ShotsTheme.typography.titleLarge,
                    color = ShotsTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatItem(
                        count = allScreenshots.size,
                        label = "Total",
                        modifier = Modifier.weight(1f)
                    )
                    StatItem(
                        count = keptCount,
                        label = "Kept",
                        modifier = Modifier.weight(1f)
                    )
                    StatItem(
                        count = pendingCount,
                        label = "Pending",
                        modifier = Modifier.weight(1f)
                    )
                    StatItem(
                        count = deletedCount,
                        label = "Deleted",
                        modifier = Modifier.weight(1f)
                    )
                }
                if (freedBytes > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ShotsText(
                        text = "${formatBytes(freedBytes)} freed",
                        style = ShotsTheme.typography.bodyMedium,
                        color = ShotsTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ShotsCard {
                ShotsText(
                    text = "Quick Actions",
                    style = ShotsTheme.typography.titleLarge,
                    color = ShotsTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                QuickActionRow(
                    icon = Icons.Default.History,
                    title = "View History",
                    onClick = {
                        context.startActivity(Intent(context, com.shots.HistoryActivity::class.java))
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                QuickActionRow(
                    icon = Icons.Default.CameraAlt,
                    title = "Permissions",
                    onClick = {
                        context.startActivity(Intent(context, PermissionsActivity::class.java))
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                QuickActionRow(
                    icon = Icons.Default.Info,
                    title = "How It Works",
                    onClick = { showHowItWorks = true }
                )
            }
        }
    }

    if (showHowItWorks) {
        ShotsDialog(
            onDismissRequest = { showHowItWorks = false },
            title = {
                ShotsText(
                    "How It Works",
                    style = ShotsTheme.typography.titleLarge,
                    color = ShotsTheme.colorScheme.onSurface
                )
            },
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
                ShotsButton(onClick = { showHowItWorks = false }) {
                    ShotsText("Got it", color = ShotsTheme.colorScheme.onPrimary)
                }
            }
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024 * 1024L) return "${bytes / 1024L} KB"
    val mb = bytes / (1024f * 1024f)
    return if (mb < 1024) "%.1f MB".format(mb) else "%.2f GB".format(mb / 1024)
}

@Composable
private fun StatItem(count: Int, label: String, modifier: Modifier = Modifier) {    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ShotsText(
            text = "$count",
            style = ShotsTheme.typography.headlineMedium,
            color = ShotsTheme.colorScheme.onSurface
        )
        ShotsText(
            text = label,
            style = ShotsTheme.typography.bodySmall,
            color = ShotsTheme.colorScheme.secondary
        )
    }
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

@Composable
private fun QuickActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShotsIcon(
            imageVector = icon,
            contentDescription = null,
            tint = ShotsTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        ShotsText(
            text = title,
            style = ShotsTheme.typography.bodyLarge,
            color = ShotsTheme.colorScheme.onSurface
        )
    }
}
