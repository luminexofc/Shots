package com.shots.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.shots.ui.theme.ShotsTheme

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ShotsTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                val interactionSource = remember { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                val reduced = isReducedMotion()
                val pressScale by animateFloatAsState(
                    targetValue = if (reduced) 1f else if (pressed) MotionTokens.PressScale else 1f,
                    animationSpec = if (pressed) tween(MotionTokens.PressMs, easing = MotionTokens.EaseOut)
                    else tween(MotionTokens.PressReleaseMs, easing = MotionTokens.EaseOut),
                    label = "segPress$index"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .scale(pressScale)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) ShotsTheme.colorScheme.primary
                            else ShotsTheme.colorScheme.surface
                        )
                        .clickable(
                            role = Role.Tab,
                            indication = null,
                            interactionSource = interactionSource,
                            onClick = { onSelected(index) }
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ShotsText(
                        text = option,
                        color = if (isSelected) ShotsTheme.colorScheme.onPrimary
                        else ShotsTheme.colorScheme.secondary,
                        style = ShotsTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
