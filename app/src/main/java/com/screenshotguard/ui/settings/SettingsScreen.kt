package com.screenshotguard.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screenshotguard.data.PreferencesManager
import com.screenshotguard.ui.components.SegmentedControl
import com.screenshotguard.ui.components.ShotsCard
import com.screenshotguard.ui.overlay.formatDelay
import com.screenshotguard.ui.theme.LocalShotsColors

@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager,
    onBack: () -> Unit,
    onPermissions: () -> Unit
) {
    val colors = LocalShotsColors.current
    val context = LocalContext.current
    val deleteDelayMinutes by preferencesManager.deleteDelayMinutes.collectAsState()
    val defaultAction by preferencesManager.defaultAction.collectAsState()
    val autoDismissTimeout by preferencesManager.autoDismissTimeout.collectAsState()
    val notifyBeforeDelete by preferencesManager.notifyBeforeDelete.collectAsState()
    val darkTheme by preferencesManager.darkTheme.collectAsState()

    var hoursInput by remember { mutableStateOf((deleteDelayMinutes / 60).toString()) }
    var minutesInput by remember { mutableStateOf((deleteDelayMinutes % 60).toString()) }
    var dismissInput by remember { mutableStateOf(autoDismissTimeout.toString()) }

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
                "Settings",
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Deletion", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)

            ShotsCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Auto delete delay", fontSize = 14.sp, color = colors.textSecondary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = hoursInput,
                            onValueChange = { v ->
                                hoursInput = v.filter { it.isDigit() }
                                val h = hoursInput.toIntOrNull() ?: 0
                                val m = minutesInput.toIntOrNull() ?: 0
                                preferencesManager.setDeleteDelayMinutes((h * 60 + m).coerceIn(1, 2880))
                            },
                            modifier = Modifier.width(70.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            label = { Text("Hrs") }
                        )
                        OutlinedTextField(
                            value = minutesInput,
                            onValueChange = { v ->
                                minutesInput = v.filter { it.isDigit() }
                                val h = hoursInput.toIntOrNull() ?: 0
                                val m = minutesInput.toIntOrNull() ?: 0
                                preferencesManager.setDeleteDelayMinutes((h * 60 + m).coerceIn(1, 2880))
                            },
                            modifier = Modifier.width(70.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            label = { Text("Min") }
                        )
                        Slider(
                            value = deleteDelayMinutes.toFloat(),
                            onValueChange = {
                                val total = it.toInt().coerceIn(1, 2880)
                                preferencesManager.setDeleteDelayMinutes(total)
                                hoursInput = (total / 60).toString()
                                minutesInput = (total % 60).toString()
                            },
                            valueRange = 1f..2880f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(formatDelay(deleteDelayMinutes), fontSize = 13.sp, color = colors.textTertiary)
                }
            }

            Text("Default Action", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)

            SegmentedControl(
                options = listOf("Keep", "Delete", "Skip"),
                selectedIndex = defaultAction,
                onSelected = { preferencesManager.setDefaultAction(it) }
            )

            Text("Overlay", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)

            ShotsCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Auto-dismiss timeout", fontSize = 14.sp, color = colors.textSecondary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = dismissInput,
                            onValueChange = { v ->
                                dismissInput = v.filter { it.isDigit() }
                                preferencesManager.setAutoDismissTimeout((dismissInput.toIntOrNull() ?: 30).coerceIn(5, 300))
                            },
                            modifier = Modifier.width(70.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            label = { Text("Sec") }
                        )
                        Slider(
                            value = autoDismissTimeout.toFloat(),
                            onValueChange = {
                                val s = it.toInt().coerceIn(5, 300)
                                preferencesManager.setAutoDismissTimeout(s)
                                dismissInput = s.toString()
                            },
                            valueRange = 5f..300f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    val t = autoDismissTimeout
                    Text(
                        if (t < 60) "${t}s" else "${t / 60}m ${t % 60}s",
                        fontSize = 13.sp,
                        color = colors.textTertiary
                    )
                }
            }

            Text("Appearance", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)

            ShotsCard {
                Column {
                    SwitchRow("Dark Theme", darkTheme) { preferencesManager.setDarkTheme(it) }
                }
            }

            Text("Notifications", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)

            ShotsCard {
                SwitchRow("Notify before delete", notifyBeforeDelete) { preferencesManager.setNotifyBeforeDelete(it) }
            }

            Text("App", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)

            ShotsCard {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPermissions() }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Security, contentDescription = null, tint = colors.iconPrimary)
                            Text("Permissions", fontSize = 15.sp, color = colors.textPrimary)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textTertiary)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/EchoBolt-07/Shots")))
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = colors.iconPrimary)
                            Text("GitHub", fontSize = 15.sp, color = colors.textPrimary)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textTertiary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Version 1.1",
                fontSize = 12.sp,
                color = colors.textTertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalShotsColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, color = colors.textPrimary)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
