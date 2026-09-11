package com.screenshotguard.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.screenshotguard.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayScreen(
    screenshotUri: String,
    fileName: String,
    deleteDelayMinutes: Int,
    defaultAction: Int,
    onKeep: () -> Unit,
    onDeleteAfter: (Int) -> Unit,
    onSkip: () -> Unit
) {
    var selectedMinutes by remember { mutableStateOf(deleteDelayMinutes.coerceAtLeast(5)) }
    var showDelayPicker by remember { mutableStateOf(defaultAction == 1) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Screenshot Detected",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            HorizontalDivider()

            AnimatedVisibility(
                visible = showDelayPicker,
                enter = fadeIn() + expandVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Delete after", style = MaterialTheme.typography.labelLarge)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val hours = selectedMinutes / 60
                        val mins = selectedMinutes % 60
                        FilterChip(
                            selected = hours > 0,
                            onClick = { selectedMinutes = ((hours + 1) * 60 + mins).coerceAtMost(2880) },
                            label = { Text("${hours + 1}h") }
                        )
                        FilterChip(
                            selected = mins > 0,
                            onClick = {
                                val newMins = mins + 5
                                if (newMins >= 60) selectedMinutes = selectedMinutes - mins + 60
                                else selectedMinutes = selectedMinutes - mins + newMins
                                selectedMinutes = selectedMinutes.coerceAtMost(2880)
                            },
                            label = { Text("${mins}m") }
                        )
                    }

                    Slider(
                        value = selectedMinutes.toFloat(),
                        onValueChange = { selectedMinutes = it.toInt().coerceAtLeast(5) },
                        valueRange = 5f..2880f,
                        steps = 0
                    )

                    Text(
                        text = formatDelay(selectedMinutes),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = onKeep,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Keep")
            }

            OutlinedButton(
                onClick = {
                    if (showDelayPicker) onDeleteAfter(selectedMinutes)
                    else showDelayPicker = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (showDelayPicker) "Delete in ${formatDelay(selectedMinutes)}"
                    else "Delete After"
                )
            }

            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip")
            }
        }
    }
}

fun formatDelay(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}
