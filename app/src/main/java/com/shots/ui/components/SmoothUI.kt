package com.shots.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.komoui.components.Card
import com.komoui.themes.komoTypography
import com.komoui.themes.radius
import com.komoui.themes.styles

object MotionTokens {
    val EaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    const val PressMs = 120
    const val PressReleaseMs = 100
    const val FadeMs = 180
    const val DialogMs = 240
    const val ColorMs = 150
    const val SwitchMs = 180
    const val OnboardingMs = 500
    const val StaggerMs = 40
    const val SlideUpDp = 12
    const val EnterScale = 0.96f
    const val PressScale = 0.97f
}

@Composable
fun SmoothDialog(
    onDismissRequest: () -> Unit,
    title: String,
    description: String? = null,
    body: (@Composable () -> Unit)? = null,
    buttons: @Composable RowScope.() -> Unit = {}
) {
    Dialog(onDismissRequest = onDismissRequest) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }
        val reduced = isReducedMotion()
        AnimatedVisibility(
            visible = visible,
            enter = if (reduced) fadeIn(tween(200))
            else fadeIn(tween(MotionTokens.FadeMs, easing = MotionTokens.EaseOut)) +
                    scaleIn(
                        tween(MotionTokens.DialogMs, easing = MotionTokens.EaseOut),
                        initialScale = MotionTokens.EnterScale
                    ) +
                    slideInVertically(
                        tween(MotionTokens.DialogMs, easing = MotionTokens.EaseOut)
                    ) { it / 20 }
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.komoTypography.titleLarge,
                        color = MaterialTheme.styles.foreground
                    )
                    if (description != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.komoTypography.body,
                            color = MaterialTheme.styles.mutedForeground
                        )
                    }
                    if (body != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        body()
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        content = buttons
                    )
                }
            }
        }
    }
}

@Composable
fun SmoothTabs(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedBg: Color? = null,
    selectedFg: Color? = null,
    unselectedFg: Color? = null
) {
    val styles = MaterialTheme.styles
    val selBgTarget = selectedBg ?: styles.background
    val selFgTarget = selectedFg ?: styles.foreground
    val unselFg = unselectedFg ?: styles.mutedForeground
    val reduced = isReducedMotion()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val sel = selectedTabIndex.coerceIn(tabs.indices)
    val textStyle = MaterialTheme.komoTypography.bodyMedium
    val scrollState = androidx.compose.foundation.rememberScrollState()
    // Measured tab widths: every label keeps full uniform size, cells size to
    // content, and the pill below uses the same cumulative numbers — exact by
    // construction, nothing to drift.
    var tabWs by remember(tabs.size) { mutableStateOf(List(tabs.size) { 0 }) }
    // Few tabs divide the bar evenly (no dead runway); more tabs size to
    // content and scroll. Pill math below follows the same branch.
    val spreadEvenly = tabs.size <= 3
    val padPx = with(density) { if (spreadEvenly) 0.dp.toPx() else 4.dp.toPx() }
    val gapPx = with(density) { if (spreadEvenly) 0.dp.toPx() else 8.dp.toPx() }
    fun contentX(i: Int): Float {
        var x = padPx
        for (k in 0 until i.coerceIn(0, tabs.size)) x += tabWs.getOrElse(k) { 0 } + gapPx
        return x
    }
    // One shared 150ms clock for the glide — History filters and Appearance
    // tabs run the identical path, so both animate the same way.
    val animX = contentX(sel)
    val pillW = tabWs.getOrElse(sel) { 0 }
    val animPillX by animateFloatAsState(
        targetValue = animX,
        animationSpec = if (reduced) tween(1)
        else tween(MotionTokens.ColorMs, easing = MotionTokens.EaseOut),
        label = "tabIndicatorX"
    )
    val animPillW by animateFloatAsState(
        targetValue = pillW.toFloat(),
        animationSpec = if (reduced) tween(1)
        else tween(MotionTokens.ColorMs, easing = MotionTokens.EaseOut),
        label = "tabIndicatorW"
    )
    val selBg by animateColorAsState(
        targetValue = selBgTarget,
        animationSpec = tween(MotionTokens.ColorMs, easing = MotionTokens.EaseOut),
        label = "tabSelBg"
    )
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(styles.muted)
            .padding(4.dp)
    ) {
        // Content width minus this container's own 4dp outer padding each
        // side — the row and the pill share it, so centering can't drift.
        val viewportPx = with(density) { (maxWidth - 8.dp).toPx() }
        val totalPx = tabWs.sum() + gapPx * (tabs.size - 1).coerceAtLeast(0)
        // Center the row when everything fits; scroll natively otherwise.
        val centerPad = ((viewportPx - totalPx) / 2).coerceAtLeast(0f)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Scrolling only matters when tabs overflow; in spread mode
                // weights divide the viewport, so no scroll container at all
                // (weights are meaningless inside a scrolling row).
                .then(if (spreadEvenly) Modifier else Modifier.horizontalScroll(scrollState))
                .drawBehind {
                    val x = centerPad + animPillX
                    drawRoundRect(
                        color = selBg,
                        topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                        size = androidx.compose.ui.geometry.Size(animPillW, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
                    )
                }
                .padding(horizontal = if (spreadEvenly) 0.dp else 4.dp),
            horizontalArrangement = if (spreadEvenly) Arrangement.spacedBy(0.dp)
            else Arrangement.spacedBy(8.dp)
        ) {
            // Leading spacer carries the centering offset inside the layout
            // itself, so tabs and pill share one origin. Trailing spacer
            // keeps edge breathing when scrolled to the end.
            if (centerPad > 1f) {
                Spacer(modifier = Modifier.width(with(density) { centerPad.toDp() }))
            }
            tabs.forEachIndexed { index, tab ->
                val selected = index == sel
                val fg by animateColorAsState(
                    targetValue = if (selected) selFgTarget else unselFg,
                    animationSpec = if (selected) tween(MotionTokens.ColorMs, easing = MotionTokens.EaseOut)
                    else tween(200, easing = MotionTokens.EaseOut),
                    label = "tabFg"
                )
                val scale by animateFloatAsState(
                    targetValue = if (reduced) 1f else if (selected) 1f else 0.96f,
                    animationSpec = if (selected) tween(MotionTokens.ColorMs, easing = MotionTokens.EaseOut)
                    else tween(200, easing = MotionTokens.EaseOut),
                    label = "tabScale"
                )
                Box(
                    modifier = Modifier
                        .then(if (spreadEvenly) Modifier.weight(1f) else Modifier)
                        .onSizeChanged { size ->
                            if (tabWs.getOrElse(index) { 0 } != size.width) {
                                tabWs = tabWs.toMutableList().also { list -> list[index] = size.width }
                            }
                        }
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = fg,
                        style = textStyle,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
                    )
                }
            }
            if (!spreadEvenly) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}
