package com.shots.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.shots.ui.theme.ShotsTheme

enum class ShotsDestination { Home, History, Settings }

@Composable
fun ShotsBottomNav(
    current: ShotsDestination,
    onNavigate: (ShotsDestination) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFFA7F3D0)
) {
    Column(modifier = modifier.fillMaxWidth().background(ShotsTheme.colorScheme.surface)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(1.dp)
                .background(ShotsTheme.colorScheme.outlineVariant)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShotsNavItem(
                icon = Icons.Default.Home,
                label = "Home",
                active = current == ShotsDestination.Home,
                activeTint = activeColor,
                onClick = { onNavigate(ShotsDestination.Home) }
            )
            ShotsNavItem(
                icon = Icons.Default.History,
                label = "History",
                active = current == ShotsDestination.History,
                activeTint = activeColor,
                onClick = { onNavigate(ShotsDestination.History) }
            )
            ShotsNavItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                active = current == ShotsDestination.Settings,
                activeTint = activeColor,
                onClick = { onNavigate(ShotsDestination.Settings) }
            )
        }
    }
}

@Composable
private fun ShotsNavItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    activeTint: Color = Color(0xFFA7F3D0)
) {
    val tint = if (active) activeTint else ShotsTheme.colorScheme.secondary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                role = Role.Tab,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        ShotsIcon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        ShotsText(
            text = label,
            style = ShotsTheme.typography.labelMedium,
            color = tint
        )
    }
}
