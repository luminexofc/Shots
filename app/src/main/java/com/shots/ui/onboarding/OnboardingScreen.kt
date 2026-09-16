package com.shots.ui.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoIcon
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.icons.CupertinoIcons
import com.slapps.cupertino.icons.outlined.Bell
import com.slapps.cupertino.icons.outlined.CheckmarkCircle
import com.slapps.cupertino.icons.outlined.Gearshape
import com.slapps.cupertino.icons.outlined.House
import com.slapps.cupertino.icons.outlined.Lock
import com.slapps.cupertino.icons.outlined.Play
import com.slapps.cupertino.section.CupertinoSection
import com.slapps.cupertino.section.SectionItem
import com.slapps.cupertino.section.SectionScope
import com.slapps.cupertino.theme.CupertinoTheme
import kotlinx.coroutines.launch

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

private const val PAGE_COUNT = 5
private const val LAST_PAGE = PAGE_COUNT - 1

@OptIn(ExperimentalFoundationApi::class, ExperimentalCupertinoApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()

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

    LaunchedEffect(Unit) {
        checkPermissions()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CupertinoTheme.colorScheme.systemBackground)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> HowItWorksPage()
                2 -> PermissionsPage(
                    storageGranted = storageGranted,
                    overlayGranted = overlayGranted,
                    notificationGranted = notificationGranted,
                    allFilesGranted = allFilesGranted,
                    onStorageClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            storageLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            storageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    },
                    onOverlayClick = {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    },
                    onAllFilesClick = {
                        openAllFilesSettings(context)
                    },
                    onNotificationClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
                3 -> BackgroundPage(
                    batteryWhitelisted = batteryWhitelisted,
                    onBatteryClick = {
                        com.shots.util.BatteryOptHelper.requestWhitelist(context)
                    },
                    onAutoStartClick = {
                        com.shots.util.AutoStartHelper.openSettings(context)
                    }
                )
                else -> ReadyPage()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(PAGE_COUNT) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 10.dp else 8.dp)
                            .background(
                                if (isSelected) CupertinoTheme.colorScheme.accent
                                else CupertinoTheme.colorScheme.separator,
                                CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage < LAST_PAGE) {
                    CupertinoButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(LAST_PAGE)
                            }
                        },
                        colors = CupertinoButtonDefaults.plainButtonColors()
                    ) {
                        CupertinoText("Skip")
                    }
                } else {
                    Spacer(modifier = Modifier.width(64.dp))
                }

                CupertinoButton(
                    onClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage < LAST_PAGE) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                onComplete()
                            }
                        }
                    }
                ) {
                    CupertinoText(
                        if (pagerState.currentPage == LAST_PAGE) "Get Started" else "Next"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CupertinoIcon(
            imageVector = CupertinoIcons.Outlined.House,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = CupertinoTheme.colorScheme.accent
        )
        Spacer(modifier = Modifier.height(32.dp))
        CupertinoText("Shots")
        Spacer(modifier = Modifier.height(8.dp))
        CupertinoText(
            text = "Your screenshots, your rules.",
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun PermissionsPage(
    storageGranted: Boolean, overlayGranted: Boolean, notificationGranted: Boolean,
    allFilesGranted: Boolean,
    onStorageClick: () -> Unit, onOverlayClick: () -> Unit, onAllFilesClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CupertinoText("Permissions")
        Spacer(modifier = Modifier.height(8.dp))
        CupertinoText(
            text = "Shots needs these to catch screenshots the second you take them.",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        CupertinoSection {
            PermissionItem(title = "Storage Access", granted = storageGranted, onClick = onStorageClick)
            PermissionItem(title = "Display Over Apps", granted = overlayGranted, onClick = onOverlayClick)
            PermissionItem(title = "All Files Access", granted = allFilesGranted, onClick = onAllFilesClick)
            PermissionItem(title = "Notifications", granted = notificationGranted, onClick = onNotificationClick)
        }
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun BackgroundPage(
    batteryWhitelisted: Boolean,
    onBatteryClick: () -> Unit,
    onAutoStartClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CupertinoIcon(
            imageVector = CupertinoIcons.Outlined.Gearshape,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = CupertinoTheme.colorScheme.accent
        )
        Spacer(modifier = Modifier.height(24.dp))
        CupertinoText("Stay Alive")
        Spacer(modifier = Modifier.height(8.dp))
        CupertinoText(
            text = "Android kills background apps. These two steps keep Shots watching.",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        CupertinoSection {
            PermissionItem(title = "Run in Background", granted = batteryWhitelisted, onClick = onBatteryClick)
            SectionItem(
                trailingContent = {
                    CupertinoButton(
                        onClick = onAutoStartClick,
                        colors = CupertinoButtonDefaults.plainButtonColors()
                    ) { CupertinoText("Open") }
                },
                title = { CupertinoText("Allow auto-start") }
            )
        }
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun SectionScope.PermissionItem(title: String, granted: Boolean, onClick: () -> Unit) {
    SectionItem(
        trailingContent = {
            if (!granted) {
                CupertinoButton(
                    onClick = onClick,
                    colors = CupertinoButtonDefaults.plainButtonColors()
                ) { CupertinoText("Grant") }
            }
        },
        title = { CupertinoText(title) }
    )
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun HowItWorksPage() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CupertinoText("How It Works")
        Spacer(modifier = Modifier.height(32.dp))
        StepItem("Take a Screenshot", "Just take a screenshot like normal")
        Spacer(modifier = Modifier.height(24.dp))
        StepItem("Popup Appears", "A popup will appear instantly")
        Spacer(modifier = Modifier.height(24.dp))
        StepItem("You Decide", "Keep, delete, or set a timer")
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun StepItem(title: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CupertinoIcon(
            imageVector = CupertinoIcons.Outlined.CheckmarkCircle,
            contentDescription = null,
            tint = CupertinoTheme.colorScheme.accent,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            CupertinoText(title)
            CupertinoText(description)
        }
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun ReadyPage() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CupertinoIcon(
            imageVector = CupertinoIcons.Outlined.CheckmarkCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = CupertinoTheme.colorScheme.accent
        )
        Spacer(modifier = Modifier.height(32.dp))
        CupertinoText("You're All Set!")
        Spacer(modifier = Modifier.height(8.dp))
        CupertinoText(
            text = "Shots is ready to protect your screenshots.",
            textAlign = TextAlign.Center
        )
    }
}
