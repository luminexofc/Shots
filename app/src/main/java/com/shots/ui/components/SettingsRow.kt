package com.shots.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.shots.ui.theme.ShotsTheme

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShotsIcon(
            imageVector = icon,
            contentDescription = null,
            tint = ShotsTheme.colorScheme.primary,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ShotsTheme.colorScheme.surfaceVariant)
                .padding(9.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShotsText(
                text = title,
                style = ShotsTheme.typography.bodyLarge,
                color = ShotsTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                ShotsText(
                    text = subtitle,
                    style = ShotsTheme.typography.bodySmall,
                    color = ShotsTheme.colorScheme.secondary
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            ShotsIcon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = ShotsTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun SettingsSectionLabel(text: String) {
    ShotsText(
        text = text.uppercase(),
        style = ShotsTheme.typography.labelMedium,
        color = ShotsTheme.colorScheme.secondary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}
