package com.screenshotguard.ui.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.screenshotguard.ui.components.ShotsCard
import com.screenshotguard.ui.theme.LocalShotsColors
import kotlinx.coroutines.launch

@Composable
fun OnboardingContent(onComplete: () -> Unit) {
    val colors = LocalShotsColors.current
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 5 })

    val coroutineScope = rememberCoroutineScope()

    val storageGranted = remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    val overlayGranted = remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val notifGranted = remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> storageGranted.value = results.values.all { it } }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifGranted.value = granted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (pagerState.currentPage < 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(4) } }
                ) { Text("Skip", color = colors.textTertiary) }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> PermissionPage(
                    icon = Icons.Filled.Folder,
                    title = "See Your Screenshots",
                    description = "Shots needs access to view and manage your screenshots.",
                    granted = storageGranted.value,
                    onGrant = {
                        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        storageLauncher.launch(perms)
                    },
                    deniedMessage = "Without this, Shots can't detect or manage your screenshots."
                )
                2 -> PermissionPage(
                    icon = Icons.Filled.Layers,
                    title = "Pop Up After Screenshots",
                    description = "Shots appears instantly so you decide what to do.",
                    granted = overlayGranted.value,
                    onGrant = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    deniedMessage = "Without this, the popup won't appear after screenshots."
                )
                3 -> PermissionPage(
                    icon = Icons.Filled.Notifications,
                    title = "Stay Notified",
                    description = "Get warned before auto-deletion so nothing important is lost.",
                    granted = notifGranted.value,
                    onGrant = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    deniedMessage = "You won't receive deletion warnings."
                )
                4 -> TestPage()
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            repeat(5) { i ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (i == pagerState.currentPage) colors.primary
                            else colors.border
                        )
                )
            }
        }

        Button(
            onClick = {
                if (pagerState.currentPage < 4) {
                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    onComplete()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                if (pagerState.currentPage < 4) "Next" else "Start Using Shots",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun WelcomePage() {
    val colors = LocalShotsColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = colors.primary
        )
        Text(
            "Meet Shots",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        Text(
            "Your screenshots, your rules.\nKeep what matters, auto-delete the rest.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun PermissionPage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onGrant: () -> Unit,
    deniedMessage: String
) {
    val colors = LocalShotsColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = colors.primary
        )
        Text(
            title,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = colors.textPrimary
        )
        Text(
            description,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = colors.textSecondary
        )

        ShotsCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Error,
                    contentDescription = null,
                    tint = if (granted) colors.success else colors.destructive
                )
                Text(
                    if (granted) "Access granted" else "Not granted",
                    fontSize = 15.sp,
                    color = if (granted) colors.success else colors.destructive
                )
            }
        }

        if (!granted) {
            OutlinedButton(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Allow Access", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            Text(
                deniedMessage,
                fontSize = 13.sp,
                color = colors.textTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TestPage() {
    val colors = LocalShotsColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.PhoneIphone,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = colors.accent
        )
        Text(
            "Ready to Try",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        Text(
            "Take a screenshot now!\nShots will pop up so you can\nkeep, delete, or skip.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = colors.textSecondary
        )

        ShotsCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "How it works:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Text("1. Take a screenshot", fontSize = 14.sp, color = colors.textSecondary)
                Text("2. Shots pops up", fontSize = 14.sp, color = colors.textSecondary)
                Text("3. Choose: Keep / Delete / Skip", fontSize = 14.sp, color = colors.textSecondary)
            }
        }
    }
}
