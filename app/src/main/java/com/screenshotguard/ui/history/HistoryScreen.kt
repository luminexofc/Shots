package com.screenshotguard.ui.history

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.screenshotguard.data.ScreenshotEntity
import com.screenshotguard.data.ScreenshotStatus
import com.screenshotguard.ui.components.ShotsCard
import com.screenshotguard.ui.theme.LocalShotsColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    screenshots: List<ScreenshotEntity>,
    onBack: () -> Unit,
    onKeep: (ScreenshotEntity) -> Unit,
    onDelete: (ScreenshotEntity) -> Unit,
    onKeepAll: () -> Unit,
    onDeleteAll: () -> Unit
) {
    val colors = LocalShotsColors.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = colors.destructive) },
            title = { Text("Delete all?", color = colors.textPrimary) },
            text = { Text("This will permanently delete all screenshots from your device. This action cannot be undone.", color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDeleteAll() }) {
                    Text("Delete All", color = colors.destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = colors.textSecondary) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.iconPrimary)
                }
                Text(
                    "History",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
            if (screenshots.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onKeepAll) { Text("Keep All", color = colors.primary) }
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text("Delete All", color = colors.destructive)
                    }
                }
            }
        }

        if (screenshots.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = colors.textTertiary
                    )
                    Text(
                        "No screenshots",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                    Text(
                        "Screenshots you take will appear here",
                        fontSize = 14.sp,
                        color = colors.textTertiary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(screenshots, key = { it.id }) { screenshot ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { visible = true }
                    AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically { it / 4 }) {
                        HistoryItem(screenshot, onKeep = { onKeep(screenshot) }, onDelete = { onDelete(screenshot) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(s: ScreenshotEntity, onKeep: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalShotsColors.current
    val fmt = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    ShotsCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = Uri.parse(s.uri),
                contentDescription = null,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    s.fileName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.textPrimary
                )
                Text(
                    fmt.format(Date(s.timestamp)),
                    fontSize = 12.sp,
                    color = colors.textTertiary
                )
                Text(
                    when (s.status) {
                        ScreenshotStatus.KEPT -> "Kept"
                        ScreenshotStatus.SCHEDULED_FOR_DELETE -> "Scheduled"
                        ScreenshotStatus.DELETED -> "Deleted"
                        ScreenshotStatus.SKIPPED -> "Skipped"
                        else -> "New"
                    },
                    fontSize = 11.sp,
                    color = when (s.status) {
                        ScreenshotStatus.KEPT -> colors.success
                        ScreenshotStatus.SCHEDULED_FOR_DELETE -> colors.warning
                        ScreenshotStatus.DELETED -> colors.destructive
                        else -> colors.textTertiary
                    }
                )
            }

            if (s.status == ScreenshotStatus.DETECTED || s.status == ScreenshotStatus.SCHEDULED_FOR_DELETE) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (s.status == ScreenshotStatus.DETECTED) {
                        FilledTonalButton(onClick = onKeep) { Text("Keep") }
                    }
                    FilledTonalButton(onClick = onDelete) { Text("Delete") }
                }
            }
        }
    }
}
