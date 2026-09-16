package com.shots.ui.permissions

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoIcon
import com.slapps.cupertino.CupertinoNavigateBackButton
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.CupertinoTopAppBar
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.icons.CupertinoIcons
import com.slapps.cupertino.icons.outlined.Bell
import com.slapps.cupertino.icons.outlined.CheckmarkCircle
import com.slapps.cupertino.icons.outlined.ChevronForward
import com.slapps.cupertino.icons.outlined.Gearshape
import com.slapps.cupertino.icons.outlined.House
import com.slapps.cupertino.icons.outlined.Lock
import com.slapps.cupertino.icons.outlined.Play
import com.slapps.cupertino.section.CupertinoSection
import com.slapps.cupertino.section.SectionItem
import com.slapps.cupertino.theme.CupertinoTheme

private fun openAllFilesSettings(context: android.content.Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    try {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun PermissionManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var storageGranted by remember { mutableStateOf(false) }
    var overlayGranted by remember { mutableStateOf(false) }
    var notificationGranted by remember { mutableStateOf(false) }
    var allFilesGranted by remember { mutableStateOf(false) }
    var batteryWhitelisted by remember { mutableStateOf(false) }

    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> storageGranted = granted }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationGranted = granted }

    fun checkPermissions() {
        storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        overlayGranted = Settings.canDrawOverlays(context)
        notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
        allFilesGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else true
        batteryWhitelisted = com.shots.util.BatteryOptHelper.isWhitelisted(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    checkPermissions()

    Column(modifier = Modifier.fillMaxSize().background(CupertinoTheme.colorScheme.systemGroupedBackground)) {
        CupertinoTopAppBar(
            title = { CupertinoText("Permissions") },
            navigationIcon = {
                CupertinoNavigateBackButton(onClick = onBack) {
                    CupertinoText("Back")
                }
            }
        )
        CupertinoSection(
            caption = { CupertinoText("Shots needs these to catch and manage screenshots") }
        ) {
            SectionItem(
                leadingContent = {
                    CupertinoIcon(
                        imageVector = CupertinoIcons.Outlined.House,
                        contentDescription = null,
                        tint = CupertinoTheme.colorScheme.accent
                    )
                },
                trailingContent = {
                    if (!storageGranted) {
                        CupertinoButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    storageLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                                } else {
                                    storageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                            },
                            colors = CupertinoButtonDefaults.plainButtonColors()
                        ) { CupertinoText("Grant") }
                    }
                },
                title = { CupertinoText("Storage Access") }
            )
            SectionItem(
                leadingContent = {
                    CupertinoIcon(
                        imageVector = CupertinoIcons.Outlined.Play,
                        contentDescription = null,
                        tint = CupertinoTheme.colorScheme.accent
                    )
                },
                trailingContent = {
                    if (!overlayGranted) {
                        CupertinoButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                    data = android.net.Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            },
                            colors = CupertinoButtonDefaults.plainButtonColors()
                        ) { CupertinoText("Grant") }
                    }
                },
                title = { CupertinoText("Display Over Apps") }
            )
            SectionItem(
                leadingContent = {
                    CupertinoIcon(
                        imageVector = CupertinoIcons.Outlined.Lock,
                        contentDescription = null,
                        tint = CupertinoTheme.colorScheme.accent
                    )
                },
                trailingContent = {
                    if (!allFilesGranted) {
                        CupertinoButton(
                            onClick = { openAllFilesSettings(context) },
                            colors = CupertinoButtonDefaults.plainButtonColors()
                        ) { CupertinoText("Grant") }
                    }
                },
                title = { CupertinoText("All Files Access") }
            )
            SectionItem(
                leadingContent = {
                    CupertinoIcon(
                        imageVector = CupertinoIcons.Outlined.Bell,
                        contentDescription = null,
                        tint = CupertinoTheme.colorScheme.accent
                    )
                },
                trailingContent = {
                    if (!notificationGranted) {
                        CupertinoButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            colors = CupertinoButtonDefaults.plainButtonColors()
                        ) { CupertinoText("Grant") }
                    }
                },
                title = { CupertinoText("Notifications") }
            )
        }

        CupertinoSection(
            title = { CupertinoText("Stay Alive") },
            caption = { CupertinoText("Required on Tecno, Xiaomi, Oppo and other aggressive skins") }
        ) {
            SectionItem(
                leadingContent = {
                    CupertinoIcon(
                        imageVector = CupertinoIcons.Outlined.Gearshape,
                        contentDescription = null,
                        tint = CupertinoTheme.colorScheme.accent
                    )
                },
                trailingContent = {
                    if (!batteryWhitelisted) {
                        CupertinoButton(
                            onClick = {
                                com.shots.util.BatteryOptHelper.requestWhitelist(context)
                            },
                            colors = CupertinoButtonDefaults.plainButtonColors()
                        ) { CupertinoText("Grant") }
                    }
                },
                title = { CupertinoText("Run in Background") }
            )
            SectionItem(
                trailingContent = {
                    CupertinoButton(
                        onClick = {
                            com.shots.util.AutoStartHelper.openSettings(context)
                        },
                        colors = CupertinoButtonDefaults.plainButtonColors()
                    ) { CupertinoText("Open") }
                },
                title = { CupertinoText("Allow auto-start") }
            )
        }
    }
}
