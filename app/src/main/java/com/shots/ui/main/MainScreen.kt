package com.shots.ui.main

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shots.PermissionsActivity
import com.shots.data.PreferencesManager
import com.shots.data.ScreenshotDatabase
import com.slapps.cupertino.CupertinoAlertDialog
import com.slapps.cupertino.default
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoIcon
import com.slapps.cupertino.theme.CupertinoColors
import com.slapps.cupertino.CupertinoIconButton
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.CupertinoTopAppBar
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.icons.CupertinoIcons
import com.slapps.cupertino.icons.outlined.ExclamationmarkTriangle
import com.slapps.cupertino.icons.outlined.Gearshape
import com.slapps.cupertino.section.CupertinoSection
import com.slapps.cupertino.section.SectionItem
import com.slapps.cupertino.section.SectionLink
import com.slapps.cupertino.theme.CupertinoTheme
import com.slapps.cupertino.theme.systemRed

@OptIn(ExperimentalCupertinoApi::class)
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

    Column(modifier = Modifier.fillMaxSize()) {
        CupertinoTopAppBar(
            title = { CupertinoText("Shots") },
            actions = {
                CupertinoIconButton(onClick = {
                    context.startActivity(Intent(context, com.shots.SettingsActivity::class.java))
                }) {
                    CupertinoIcon(
                        imageVector = CupertinoIcons.Outlined.Gearshape,
                        contentDescription = "Settings"
                    )
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (!overlayGranted) {
                CupertinoSection(
                    title = { CupertinoText("Attention Needed") }
                ) {
                    SectionLink(
                        onClick = {
                            context.startActivity(Intent(context, PermissionsActivity::class.java))
                        },
                        icon = {
                            CupertinoIcon(
                                imageVector = CupertinoIcons.Outlined.ExclamationmarkTriangle,
                                contentDescription = null,
                                tint = CupertinoColors.systemRed
                            )
                        },
                        title = { CupertinoText("Permissions Required") },
                        caption = { CupertinoText("Overlay permission needed") }
                    )
                }
            }

            CupertinoSection(
                title = { CupertinoText("Overview") },
                caption = if (freedBytes > 0) {
                    { CupertinoText("${formatBytes(freedBytes)} freed") }
                } else null
            ) {
                SectionItem(
                    title = {
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
                    }
                )
            }

            CupertinoSection(
                title = { CupertinoText("Quick Actions") }
            ) {
                SectionLink(
                    onClick = {
                        context.startActivity(Intent(context, com.shots.HistoryActivity::class.java))
                    },
                    title = { CupertinoText("View History") }
                )
                SectionLink(
                    onClick = {
                        context.startActivity(Intent(context, PermissionsActivity::class.java))
                    },
                    title = { CupertinoText("Permissions") }
                )
                SectionLink(
                    onClick = { showHowItWorks = true },
                    title = { CupertinoText("How It Works") }
                )
            }
        }
    }

    if (showHowItWorks) {
        CupertinoAlertDialog(
            onDismissRequest = { showHowItWorks = false },
            title = { CupertinoText("How It Works") },
            message = {
                CupertinoText(
                    "1. Take a screenshot like normal\n" +
                        "2. A popup appears instantly\n" +
                        "3. Keep it, delete it, or set a timer\n" +
                        "4. View all screenshots in History"
                )
            },
            buttons = {
                default(onClick = { showHowItWorks = false }) {
                    CupertinoText("Got it")
                }
            }
        )
    }
}

@Composable
private fun StatItem(count: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CupertinoText(
            text = "$count"
        )
        CupertinoText(
            text = label
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024 * 1024L) return "${bytes / 1024L} KB"
    val mb = bytes / (1024f * 1024f)
    return if (mb < 1024) "%.1f MB".format(mb) else "%.2f GB".format(mb / 1024)
}


