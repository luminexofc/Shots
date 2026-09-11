package com.screenshotguard.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.screenshotguard.R
import com.screenshotguard.data.ScreenshotEntity
import com.screenshotguard.data.ScreenshotStatus
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    screenshots: List<ScreenshotEntity>,
    totalCount: Int,
    onSettings: () -> Unit,
    onHistory: () -> Unit,
    onKeep: (ScreenshotEntity) -> Unit,
    onDelete: (ScreenshotEntity) -> Unit
) {
    val active = remember(screenshots) { screenshots.filter { it.status != ScreenshotStatus.DELETED }.take(10) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shots") },
                actions = {
                    IconButton(onClick = onHistory) { Icon(Icons.Filled.History, "History") }
                    IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Settings") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "$totalCount",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            stringResource(R.string.total_screenshots, totalCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (active.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No screenshots yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text("Take a screenshot to get started", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                item { Text("Recent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }

                items(active, key = { it.id }) { screenshot ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { visible = true }
                    AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically { it / 4 }) {
                        ScreenshotRow(screenshot, onKeep = { onKeep(screenshot) }, onDelete = { onDelete(screenshot) })
                    }
                }

                if (screenshots.size > 10) {
                    item {
                        TextButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) {
                            Text("View all ${screenshots.size}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenshotRow(s: ScreenshotEntity, onKeep: () -> Unit, onDelete: () -> Unit) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(s.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    when (s.status) {
                        ScreenshotStatus.KEPT -> "Kept"
                        ScreenshotStatus.SCHEDULED_FOR_DELETE -> "Scheduled"
                        ScreenshotStatus.SKIPPED -> "Skipped"
                        else -> "New"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (s.status == ScreenshotStatus.DETECTED) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(onClick = onKeep) { Text("Keep") }
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
