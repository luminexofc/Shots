package com.screenshotguard.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screenshotguard.ui.components.ShotsCard
import com.screenshotguard.ui.theme.LocalShotsColors

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
    val colors = LocalShotsColors.current
    var selectedMinutes by remember { mutableStateOf(deleteDelayMinutes.coerceAtLeast(5)) }
    var showDelayPicker by remember { mutableStateOf(defaultAction == 1) }

    ShotsCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Screenshot Detected",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Text(
                text = fileName,
                fontSize = 14.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border)
            )

            AnimatedVisibility(
                visible = showDelayPicker,
                enter = fadeIn() + expandVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Delete after", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val hours = selectedMinutes / 60
                        val mins = selectedMinutes % 60
                        FilledTonalButton(
                            onClick = { selectedMinutes = ((hours + 1) * 60 + mins).coerceAtMost(2880) }
                        ) {
                            Text("${hours + 1}h")
                        }
                        FilledTonalButton(
                            onClick = {
                                val newMins = mins + 5
                                if (newMins >= 60) selectedMinutes = selectedMinutes - mins + 60
                                else selectedMinutes = selectedMinutes - mins + newMins
                                selectedMinutes = selectedMinutes.coerceAtMost(2880)
                            }
                        ) {
                            Text("${mins}m")
                        }
                    }

                    Slider(
                        value = selectedMinutes.toFloat(),
                        onValueChange = { selectedMinutes = it.toInt().coerceAtLeast(5) },
                        valueRange = 5f..2880f
                    )

                    Text(
                        text = formatDelay(selectedMinutes),
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = onKeep,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Keep", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }

            OutlinedButton(
                onClick = {
                    if (showDelayPicker) onDeleteAfter(selectedMinutes)
                    else showDelayPicker = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (showDelayPicker) "Delete in ${formatDelay(selectedMinutes)}"
                    else "Delete After",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip", fontSize = 15.sp, color = colors.textTertiary)
            }
        }
    }
}

fun formatDelay(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}
