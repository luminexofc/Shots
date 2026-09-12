package com.screenshotguard.ui.permissions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.screenshotguard.ui.components.ShotsCard
import com.screenshotguard.ui.theme.LocalShotsColors

@Composable
fun PermissionManagerContent(onBack: () -> Unit) {
    val colors = LocalShotsColors.current
    val context = LocalContext.current

    var storageGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var notifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> storageGranted = results.values.all { it } }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifGranted = granted }

    val grantedCount = listOf(storageGranted, overlayGranted, notifGranted).count { it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.iconPrimary)
            }
            Text(
                "Permissions",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Required",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )

            PermissionItem(
                icon = Icons.Filled.Folder,
                name = "Storage Access",
                granted = storageGranted,
                onGrant = {
                    val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                    } else {
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    storageLauncher.launch(perms)
                }
            )

            PermissionItem(
                icon = Icons.Filled.Layers,
                name = "Display Over Apps",
                granted = overlayGranted,
                onGrant = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )

            Text(
                "Optional",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )

            PermissionItem(
                icon = Icons.Filled.Notifications,
                name = "Notifications",
                granted = notifGranted,
                onGrant = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "$grantedCount of 3 permissions granted",
                fontSize = 14.sp,
                color = colors.textTertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Text(
                "All permissions are needed for full functionality.",
                fontSize = 12.sp,
                color = colors.textTertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    name: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    val colors = LocalShotsColors.current
    ShotsCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                Text(
                    if (granted) "Granted" else "Not granted",
                    fontSize = 13.sp,
                    color = if (granted) colors.success else colors.destructive
                )
            }
            if (granted) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Granted",
                    tint = colors.success
                )
            } else {
                FilledTonalButton(onClick = onGrant) {
                    Text("Grant")
                }
            }
        }
    }
}
