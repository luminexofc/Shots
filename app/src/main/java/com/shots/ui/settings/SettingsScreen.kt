package com.shots.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shots.ShotsApp
import com.shots.data.PreferencesManager
import com.slapps.cupertino.CupertinoIcon
import com.slapps.cupertino.CupertinoNavigateBackButton
import com.slapps.cupertino.CupertinoSegmentedControl
import com.slapps.cupertino.CupertinoSegmentedControlTab
import com.slapps.cupertino.CupertinoSlider
import com.slapps.cupertino.CupertinoSwitch
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.CupertinoTopAppBar
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.icons.CupertinoIcons
import com.slapps.cupertino.icons.outlined.ChevronForward
import com.slapps.cupertino.icons.outlined.SquareAndArrowUp
import com.slapps.cupertino.section.CupertinoSection
import com.slapps.cupertino.section.SectionItem
import com.slapps.cupertino.section.SectionLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = PreferencesManager(context)
    val app = context.applicationContext as ShotsApp

    val timerMinutes by prefs.timerMinutes.collectAsState(initial = 5)
    val snoozeMinutes by prefs.snoozeMinutes.collectAsState(initial = 10)
    val darkMode by prefs.darkMode.collectAsState(initial = 0)
    val showEditButton by prefs.showEditButton.collectAsState(initial = false)

    var sliderValue by remember { mutableFloatStateOf(timerMinutes.toFloat()) }
    var snoozeSlider by remember { mutableFloatStateOf(snoozeMinutes.toFloat()) }

    LaunchedEffect(timerMinutes) {
        sliderValue = timerMinutes.toFloat()
    }

    LaunchedEffect(snoozeMinutes) {
        snoozeSlider = snoozeMinutes.toFloat()
    }

    val themeOptions = listOf("System", "Dark", "Light")

    Column(modifier = Modifier.fillMaxSize().background(CupertinoTheme.colorScheme.systemGroupedBackground)) {
        CupertinoTopAppBar(
            title = { CupertinoText("Settings") },
            navigationIcon = {
                CupertinoNavigateBackButton(onClick = onBack) {
                    CupertinoText("Back")
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            CupertinoSection(
                title = { CupertinoText("Timers") },
                caption = { CupertinoText("Defaults used in the popup") }
            ) {
                SectionItem(
                    title = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            CupertinoText("Timer: ${sliderValue.toInt()} minutes")
                            CupertinoSlider(
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                onValueChangeFinished = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        prefs.setTimerMinutes(sliderValue.toInt())
                                        app.trackSettingChanged("timer_minutes", sliderValue.toInt())
                                    }
                                },
                                valueRange = 1f..60f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CupertinoText("Snooze: ${snoozeSlider.toInt()} minutes")
                            CupertinoSlider(
                                value = snoozeSlider,
                                onValueChange = { snoozeSlider = it },
                                onValueChangeFinished = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        prefs.setSnoozeMinutes(snoozeSlider.toInt())
                                        app.trackSettingChanged("snooze_minutes", snoozeSlider.toInt())
                                    }
                                },
                                valueRange = 1f..60f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                )
            }

            CupertinoSection(
                title = { CupertinoText("Appearance") }
            ) {
                SectionItem(
                    title = {
                        CupertinoSegmentedControl(
                            selectedTabIndex = darkMode,
                            modifier = Modifier.fillMaxWidth(),
                            tabs = {
                                themeOptions.forEachIndexed { index, option ->
                                    CupertinoSegmentedControlTab(
                                        onClick = {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                prefs.setDarkMode(index)
                                                app.trackSettingChanged("theme", themeOptions[index])
                                            }
                                        },
                                        isSelected = darkMode == index
                                    ) {
                                        CupertinoText(option)
                                    }
                                }
                            }
                        )
                    }
                )
            }

            CupertinoSection(
                title = { CupertinoText("Popup") }
            ) {
                SectionItem(
                    trailingContent = {
                        CupertinoSwitch(
                            checked = showEditButton,
                            onCheckedChange = { enabled ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    prefs.setShowEditButton(enabled)
                                }
                                app.trackSettingChanged("edit_button", enabled)
                            }
                        )
                    },
                    title = {
                        Column {
                            CupertinoText("Edit Button")
                            CupertinoText("System editor shortcut in the popup")
                        }
                    }
                )
            }

            CupertinoSection {
                SectionLink(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.supportkori.com/luminex"))
                        context.startActivity(intent)
                    },
                    icon = {
                        CupertinoIcon(
                            imageVector = CupertinoIcons.Outlined.SquareAndArrowUp,
                            contentDescription = null
                        )
                    },
                    title = { CupertinoText("Support Shots") },
                    caption = { CupertinoText("Donate to keep the app alive") }
                )
            }
        }
    }
}
