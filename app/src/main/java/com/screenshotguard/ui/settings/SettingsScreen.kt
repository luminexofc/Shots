package com.screenshotguard.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
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
import com.screenshotguard.data.PreferencesManager
import com.screenshotguard.ui.overlay.formatDelay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager,
    onBack: () -> Unit,
    onPermissions: () -> Unit
) {
    val context = LocalContext.current
    val deleteDelayMinutes by preferencesManager.deleteDelayMinutes.collectAsState()
    val defaultAction by preferencesManager.defaultAction.collectAsState()
    val autoDismissTimeout by preferencesManager.autoDismissTimeout.collectAsState()
    val notifyBeforeDelete by preferencesManager.notifyBeforeDelete.collectAsState()
    val dynamicColor by preferencesManager.dynamicColor.collectAsState()
    val darkTheme by preferencesManager.darkTheme.collectAsState()

    var hoursInput by remember { mutableStateOf((deleteDelayMinutes / 60).toString()) }
    var minutesInput by remember { mutableStateOf((deleteDelayMinutes % 60).toString()) }
    var dismissInput by remember { mutableStateOf(autoDismissTimeout.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Deletion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Auto delete delay", style = MaterialTheme.typography.bodyLarge)
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
                Text(formatDelay(deleteDelayMinutes), style = MaterialTheme.typography.bodyMedium)
            }

            HorizontalDivider()

            Text("Default Action", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = defaultAction == 0,
                    onClick = { preferencesManager.setDefaultAction(0) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) { Text("Keep") }
                SegmentedButton(
                    selected = defaultAction == 1,
                    onClick = { preferencesManager.setDefaultAction(1) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) { Text("Delete") }
                SegmentedButton(
                    selected = defaultAction == 2,
                    onClick = { preferencesManager.setDefaultAction(2) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) { Text("Skip") }
            }

            HorizontalDivider()

            Text("Overlay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Auto-dismiss timeout", style = MaterialTheme.typography.bodyLarge)
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
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider()

            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            SwitchRow("Dynamic Color", dynamicColor) { preferencesManager.setDynamicColor(it) }
            SwitchRow("Dark Theme", darkTheme) { preferencesManager.setDarkTheme(it) }

            HorizontalDivider()

            Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            SwitchRow("Notify before delete", notifyBeforeDelete) { preferencesManager.setNotifyBeforeDelete(it) }

            HorizontalDivider()

            Text("App", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPermissions() }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Permissions", style = MaterialTheme.typography.bodyLarge)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("GitHub", style = MaterialTheme.typography.bodyLarge)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Version 1.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
