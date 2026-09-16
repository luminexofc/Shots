package com.shots.ui.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Visibility
import com.shots.ui.components.ShotsButton
import com.shots.ui.components.ShotsButtonVariant
import com.shots.ui.components.ShotsIcon
import com.shots.ui.components.ShotsText
import com.shots.ui.theme.ShotsTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

private fun openAllFilesSettings(context: android.content.Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    try {
        // Per-app screen — works on most devices
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            // Fallback: the all-apps list screen
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            // Last resort: general app details
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 4 })
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
            .background(ShotsTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> PermissionsPage(
                    storageGranted = storageGranted,
                    overlayGranted = overlayGranted,
                    notificationGranted = notificationGranted,
                    allFilesGranted = allFilesGranted,
                    batteryWhitelisted = batteryWhitelisted,
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
                    onBatteryClick = {
                        com.shots.util.BatteryOptHelper.requestWhitelist(context)
                    },
                    onNotificationClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
                2 -> HowItWorksPage()
                3 -> ReadyPage()
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
                repeat(4) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 10.dp else 8.dp)
                            .background(
                                if (isSelected) ShotsTheme.colorScheme.primary
                                else ShotsTheme.colorScheme.outline,
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
                if (pagerState.currentPage < 3) {
                    ShotsButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(3)
                            }
                        },
                        variant = ShotsButtonVariant.Text
                    ) {
                        ShotsText("Skip", color = ShotsTheme.colorScheme.secondary)
                    }
                } else {
                    Spacer(modifier = Modifier.width(64.dp))
                }

                ShotsButton(
                    onClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage < 3) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                onComplete()
                            }
                        }
                    }
                ) {
                    ShotsText(
                        if (pagerState.currentPage == 3) "Get Started" else "Next",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = ShotsTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    var scale by remember { mutableStateOf(0.5f) }
    val animatedScale by animateFloatAsState(targetValue = scale, animationSpec = tween(durationMillis = 1000), label = "scale")
    LaunchedEffect(Unit) { scale = 1f }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ShotsIcon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(120.dp).scale(animatedScale),
            tint = ShotsTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))
        ShotsText("Shots", style = ShotsTheme.typography.headlineLarge, color = ShotsTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        ShotsText("Your screenshots, your rules.", style = ShotsTheme.typography.bodyLarge, color = ShotsTheme.colorScheme.secondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PermissionsPage(
    storageGranted: Boolean, overlayGranted: Boolean, notificationGranted: Boolean,
    allFilesGranted: Boolean, batteryWhitelisted: Boolean,
    onStorageClick: () -> Unit, onOverlayClick: () -> Unit, onAllFilesClick: () -> Unit,
    onBatteryClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ShotsText("Permissions", style = ShotsTheme.typography.headlineMedium, color = ShotsTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        ShotsText("We need a few permissions to protect your screenshots.", style = ShotsTheme.typography.bodyMedium, color = ShotsTheme.colorScheme.secondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        PermissionItem(icon = Icons.Default.PhotoLibrary, title = "Storage Access", granted = storageGranted, onClick = onStorageClick)
        Spacer(modifier = Modifier.height(16.dp))
        PermissionItem(icon = Icons.Default.Visibility, title = "Display Over Apps", granted = overlayGranted, onClick = onOverlayClick)
        Spacer(modifier = Modifier.height(16.dp))
        PermissionItem(icon = Icons.Default.Delete, title = "All Files Access", granted = allFilesGranted, onClick = onAllFilesClick)
        Spacer(modifier = Modifier.height(16.dp))
        PermissionItem(icon = Icons.Default.BatteryChargingFull, title = "Run in Background", granted = batteryWhitelisted, onClick = onBatteryClick)
        Spacer(modifier = Modifier.height(16.dp))
        PermissionItem(icon = Icons.Default.Notifications, title = "Notifications", granted = notificationGranted, onClick = onNotificationClick)
    }
}

@Composable
private fun PermissionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(ShotsTheme.colorScheme.surface, RoundedCornerShape(12.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShotsIcon(icon, contentDescription = null, tint = ShotsTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        ShotsText(title, style = ShotsTheme.typography.bodyLarge, color = ShotsTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        if (granted) {
            ShotsIcon(Icons.Default.Check, contentDescription = "Granted", tint = ShotsTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        } else {
            ShotsButton(onClick = onClick) {
                ShotsText("Grant", color = ShotsTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun HowItWorksPage() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ShotsText("How It Works", style = ShotsTheme.typography.headlineMedium, color = ShotsTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(32.dp))
        StepItem("1", "Take a Screenshot", "Just take a screenshot like normal")
        Spacer(modifier = Modifier.height(24.dp))
        StepItem("2", "Popup Appears", "A popup will appear instantly")
        Spacer(modifier = Modifier.height(24.dp))
        StepItem("3", "You Decide", "Keep, delete, or set a timer")
    }
}

@Composable
private fun StepItem(step: String, title: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(40.dp).background(ShotsTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            ShotsText(step, color = ShotsTheme.colorScheme.onPrimary, style = ShotsTheme.typography.labelLarge)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            ShotsText(title, style = ShotsTheme.typography.titleMedium, color = ShotsTheme.colorScheme.onBackground)
            ShotsText(description, style = ShotsTheme.typography.bodyMedium, color = ShotsTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun ReadyPage() {
    var scale by remember { mutableStateOf(0.5f) }
    val animatedScale by animateFloatAsState(targetValue = scale, animationSpec = tween(durationMillis = 1000), label = "scale")
    LaunchedEffect(Unit) { scale = 1f }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(120.dp).scale(animatedScale).background(ShotsTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            ShotsIcon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(60.dp), tint = ShotsTheme.colorScheme.onPrimary)
        }
        Spacer(modifier = Modifier.height(32.dp))
        ShotsText("You're All Set!", style = ShotsTheme.typography.headlineLarge, color = ShotsTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        ShotsText("Shots is ready to protect your screenshots.", style = ShotsTheme.typography.bodyLarge, color = ShotsTheme.colorScheme.secondary, textAlign = TextAlign.Center)
    }
}
