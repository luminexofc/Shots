package com.screenshotguard.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screenshotguard.ui.theme.LocalShotsColors

@Composable
fun SplashContent() {
    val colors = LocalShotsColors.current
    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(400))
        alpha.animateTo(1f, animationSpec = tween(400))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = colors.primary
            )
            Text(
                text = "Shots",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = "Your screenshots, your rules",
                fontSize = 14.sp,
                color = colors.textTertiary
            )
        }
    }
}
