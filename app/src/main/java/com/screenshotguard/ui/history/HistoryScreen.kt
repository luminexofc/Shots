package com.screenshotguard.ui.history

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.screenshotguard.R
import com.screenshotguard.data.ScreenshotEntity
import com.screenshotguard.data.ScreenshotStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    screenshots: List<ScreenshotEntity>,
    onBack: () -> Unit,
    onKeep: (ScreenshotEntity) -> Unit,
    onDelete: (ScreenshotEntity) -> Unit,
    onKeepAll: () -> Unit,
    onDeleteAll: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete all?") },
            text = { Text("This will permanently delete all screenshots from your device.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDeleteAll() }) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    if (screenshots.isNotEmpty()) {
                        TextButton(onClick = onKeepAll) { Text("Keep All") }
                        TextButton(onClick = { showDeleteConfirm = true }) {
                            Text("Delete All", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (screenshots.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No screenshots", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
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
    val fmt = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
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
                Text(s.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(fmt.format(Date(s.timestamp)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    when (s.status) {
                        ScreenshotStatus.KEPT -> "Kept"
                        ScreenshotStatus.SCHEDULED_FOR_DELETE -> "Scheduled"
                        ScreenshotStatus.DELETED -> "Deleted"
                        ScreenshotStatus.SKIPPED -> "Skipped"
                        else -> "New"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (s.status) {
                        ScreenshotStatus.KEPT -> MaterialTheme.colorScheme.primary
                        ScreenshotStatus.SCHEDULED_FOR_DELETE -> MaterialTheme.colorScheme.tertiary
                        ScreenshotStatus.DELETED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            if (s.status == ScreenshotStatus.DETECTED || s.status == ScreenshotStatus.SCHEDULED_FOR_DELETE) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (s.status == ScreenshotStatus.DETECTED) {
                        FilledTonalButton(onClick = onKeep) { Text("Keep") }
                    }
                    FilledTonalButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) { Text("Delete") }
                }
            }
        }
    }
}
