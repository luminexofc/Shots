package com.shots.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shots.data.PreferencesManager
import com.shots.ui.components.SegmentedControl
import com.shots.ui.components.ShotsCard
import com.shots.ui.theme.ShotsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = PreferencesManager(context)

    val defaultAction by prefs.defaultAction.collectAsState(initial = "keep")
    val timerMinutes by prefs.timerMinutes.collectAsState(initial = 5)
    val autoDelete by prefs.autoDelete.collectAsState(initial = true)
    val notifications by prefs.notifications.collectAsState(initial = true)

    var sliderValue by remember { mutableFloatStateOf(timerMinutes.toFloat()) }

    LaunchedEffect(timerMinutes) {
        sliderValue = timerMinutes.toFloat()
    }

    val actions = listOf("Keep", "Timer", "Skip")
    val selectedIndex = when (defaultAction) {
        "keep" -> 0
        "timer" -> 1
        "skip" -> 2
        else -> 0
    }

    ShotsTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                ShotsCard {
                    Text(
                        text = "Default Action",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "What happens when a screenshot is detected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SegmentedControl(
                        options = actions,
                        selectedIndex = selectedIndex,
                        onSelected = { index ->
                            val action = when (index) {
                                0 -> "keep"
                                1 -> "timer"
                                2 -> "skip"
                                else -> "keep"
                            }
                            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                prefs.setDefaultAction(action)
                            }
                        }
                    )

                    if (defaultAction == "timer") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Timer: ${sliderValue.toInt()} minutes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = {
                                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                    prefs.setTimerMinutes(sliderValue.toInt())
                                }
                            },
                            valueRange = 1f..60f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ShotsCard {
                    Text(
                        text = "Auto-Delete",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Automatically delete screenshots after timer expires",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Switch(
                        checked = autoDelete,
                        onCheckedChange = { enabled ->
                            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                prefs.setAutoDelete(enabled)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                ShotsCard {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Show notifications for deletion warnings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Switch(
                        checked = notifications,
                        onCheckedChange = { enabled ->
                            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                prefs.setNotifications(enabled)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}
