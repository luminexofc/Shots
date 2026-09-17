package com.shots.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.shots.ui.theme.GlassTokens

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(GlassTokens.CardRadius))
            .clip(RoundedCornerShape(GlassTokens.CardRadius))
            .background(if (dark) GlassTokens.FrostSurfaceDark else GlassTokens.FrostSurfaceLight)
            .border(1.dp, if (dark) GlassTokens.FrostBorderDark else GlassTokens.FrostBorderLight, RoundedCornerShape(GlassTokens.CardRadius))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
