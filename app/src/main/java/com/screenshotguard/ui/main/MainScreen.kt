package com.screenshotguard.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.screenshotguard.R
import com.screenshotguard.data.ScreenshotEntity
import com.screenshotguard.data.ScreenshotStatus
import com.screenshotguard.ui.components.ShotsCard
import com.screenshotguard.ui.theme.LocalShotsColors
import android.net.Uri

@Composable
fun MainScreen(
    screenshots: List<ScreenshotEntity>,
    totalCount: Int,
    onSettings: () -> Unit,
    onHistory: () -> Unit,
    onKeep: (ScreenshotEntity) -> Unit,
    onDelete: (ScreenshotEntity) -> Unit
) {
    val colors = LocalShotsColors.current
    val active = remember(screenshots) { screenshots.filter { it.status != ScreenshotStatus.DELETED }.take(10) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Shots",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onHistory) {
                    Icon(Icons.Filled.History, contentDescription = "History", tint = colors.iconPrimary)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = colors.iconPrimary)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ShotsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "$totalCount",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                        Text(
                            stringResource(R.string.total_screenshots, totalCount),
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            if (active.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
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
                            "No screenshots yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                        Text(
                            "Take a screenshot to get started",
                            fontSize = 14.sp,
                            color = colors.textTertiary
                        )
                    }
                }
            } else {
                item {
                    Text(
                        "Recent",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }

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
                            Text("View all ${screenshots.size}", color = colors.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenshotRow(s: ScreenshotEntity, onKeep: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalShotsColors.current
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    s.fileName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    maxLines = 1
                )
                Text(
                    when (s.status) {
                        ScreenshotStatus.KEPT -> "Kept"
                        ScreenshotStatus.SCHEDULED_FOR_DELETE -> "Scheduled"
                        ScreenshotStatus.SKIPPED -> "Skipped"
                        else -> "New"
                    },
                    fontSize = 12.sp,
                    color = when (s.status) {
                        ScreenshotStatus.KEPT -> colors.success
                        ScreenshotStatus.SCHEDULED_FOR_DELETE -> colors.warning
                        else -> colors.textTertiary
                    }
                )
            }
            if (s.status == ScreenshotStatus.DETECTED) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(onClick = onKeep) { Text("Keep") }
                    FilledTonalButton(onClick = onDelete) { Text("Delete") }
                }
            }
        }
    }
}
