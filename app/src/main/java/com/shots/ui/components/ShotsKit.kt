package com.shots.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.shots.ui.theme.ShotsTheme
import kotlin.math.roundToInt


enum class ShotsButtonVariant { Filled, Outline, Text }

@Composable
fun ShotsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ShotsButtonVariant = ShotsButtonVariant.Filled,
    destructive: Boolean = false,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val colors = ShotsTheme.colorScheme
    val container: Color
    val contentColor: Color
    var borderMod: Modifier = Modifier
    when (variant) {
        ShotsButtonVariant.Filled -> {
            container = if (destructive) colors.error else colors.primary
            contentColor = if (destructive) colors.onError else colors.onPrimary
        }
        ShotsButtonVariant.Outline -> {
            container = Color.Transparent
            contentColor = if (destructive) colors.error else colors.onSurface
            borderMod = Modifier.border(1.dp, colors.outline, RoundedCornerShape(12.dp))
        }
        ShotsButtonVariant.Text -> {
            container = Color.Transparent
            contentColor = if (destructive) colors.error else colors.secondary
        }
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduced = isReducedMotion()
    val pressScale by animateFloatAsState(
        targetValue = if (reduced) 1f else if (pressed) MotionTokens.PressScale else 1f,
        animationSpec = if (pressed) tween(MotionTokens.PressMs, easing = MotionTokens.EaseOut)
        else tween(MotionTokens.PressReleaseMs, easing = MotionTokens.EaseOut),
        label = "btnPress"
    )
    Box(
        modifier = modifier
            .scale(pressScale)
            .clip(RoundedCornerShape(12.dp))
            .background(container.copy(alpha = if (enabled) 1f else 0.5f))
            .then(borderMod)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick
            )
            .padding(contentPadding)
            .defaultMinSize(minHeight = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun ShotsText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: androidx.compose.ui.text.TextStyle? = null,
    textAlign: androidx.compose.ui.text.style.TextAlign = androidx.compose.ui.text.style.TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE
) {
    androidx.compose.foundation.text.BasicText(
        text = text,
        modifier = modifier,
        style = (style ?: ShotsTheme.typography.bodyMedium).merge(
            color = if (color != Color.Unspecified) color else ShotsTheme.colorScheme.onSurface,
            textAlign = textAlign,
        ),
        maxLines = maxLines
    )
}

@Composable
fun ShotsDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ShotsTheme.colorScheme.outlineVariant)
    )
}

@Composable
fun ShotsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = ShotsTheme.colorScheme.primary
) {
    val colors = ShotsTheme.colorScheme
    val density = androidx.compose.ui.platform.LocalDensity.current
    val thumbPx = with(density) { 20.dp.toPx() }
    val reducedSwitch = isReducedMotion()
    val thumbOffsetPx by animateFloatAsState(
        targetValue = if (reducedSwitch) 0f else if (checked) thumbPx else 0f,
        animationSpec = tween(MotionTokens.SwitchMs, easing = MotionTokens.EaseOut),
        label = "switchThumb"
    )
    val trackColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (checked) activeColor else colors.outline,
        animationSpec = tween(MotionTokens.ColorMs, easing = MotionTokens.EaseOut),
        label = "switchTrack"
    )
    Box(
        modifier = modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(trackColor.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(
                enabled = enabled,
                role = Role.Switch,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { onCheckedChange(!checked) }
            )
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer { translationX = thumbOffsetPx }
                .size(27.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
        )
    }
}

@Composable
fun ShotsSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    activeColor: Color = ShotsTheme.colorScheme.primary
) {
    val colors = ShotsTheme.colorScheme
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        val widthPx = with(androidx.compose.ui.platform.LocalDensity.current) {
            maxWidth.toPx()
        }
        fun setFromX(x: Float) {
            val fraction = (x / widthPx).coerceIn(0f, 1f)
            onValueChange(valueRange.start + fraction * (valueRange.endInclusive - valueRange.start))
        }
        var dragValue by remember(valueRange) { mutableStateOf(value) }
        var thumbActive by remember { mutableStateOf(false) }
        val reducedSlider = isReducedMotion()
        val thumbScale by animateFloatAsState(
            targetValue = if (reducedSlider) 1f else if (thumbActive) 1.25f else 1f,
            animationSpec = tween(MotionTokens.ColorMs, easing = MotionTokens.EaseOut),
            label = "sliderThumb"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .pointerInput(valueRange, widthPx) {
                    detectTapGestures(
                        onPress = {
                            thumbActive = true
                            setFromX(it.x)
                            tryAwaitRelease()
                            thumbActive = false
                            onValueChangeFinished?.invoke()
                        }
                    )
                }
                .pointerInput(valueRange, widthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragValue = value; thumbActive = true },
                        onHorizontalDrag = { _, dragAmount ->
                            val fraction =
                                ((dragValue - valueRange.start) / (valueRange.endInclusive - valueRange.start) + dragAmount / widthPx)
                                    .coerceIn(0f, 1f)
                            dragValue =
                                valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                            onValueChange(dragValue)
                        },
                        onDragEnd = { thumbActive = false; onValueChangeFinished?.invoke() },
                        onDragCancel = { thumbActive = false }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val fraction =
                ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.outline)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(activeColor)
            )
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().height(32.dp)
            ) {
                val thumbR = 7.dp
                val maxX = maxWidth - thumbR * 2
                val thumbX = maxX * fraction
                Box(
                    modifier = Modifier
                        .offset(x = thumbX)
                        .graphicsLayer { scaleX = thumbScale; scaleY = thumbScale }
                        .size(14.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(activeColor)
                )
            }
        }
    }
}

@Composable
fun AccentSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = ShotsTheme.colorScheme.primary,
    track: Color = ShotsTheme.colorScheme.surfaceVariant
) {
    BoxWithConstraints(
        modifier = modifier.height(48.dp)
    ) {
        val widthPx = with(androidx.compose.ui.platform.LocalDensity.current) {
            maxWidth.toPx()
        }
        fun setFromX(x: Float) {
            onValueChange((x / widthPx).coerceIn(0f, 1f))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pointerInput(widthPx) {
                    detectTapGestures(onPress = {
                        setFromX(it.x)
                        tryAwaitRelease()
                    })
                }
                .pointerInput(widthPx) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            setFromX(change.position.x)
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(track)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(value.coerceIn(0f, 1f).coerceAtLeast(0.02f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent)
            )
            Box(
                modifier = Modifier
                    .offset {
                        val fraction = value.coerceIn(0f, 1f)
                        IntOffset(
                            ((fraction * (widthPx - 28.dp.toPx()))).roundToInt(),
                            0
                        )
                    }
                    .size(28.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color.White)
                    .border(2.dp, accent, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(accent)
                )
            }
        }
    }
}

@Composable
fun ShotsDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    body: (@Composable () -> Unit)? = null,
    buttons: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        ShotsCard {
            Column {
                title()
                if (body != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    body()
                }
                Spacer(modifier = Modifier.height(16.dp))
                buttons()
            }
        }
    }
}

@Composable
fun ShotsTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            ShotsButton(
                onClick = onBack,
                variant = ShotsButtonVariant.Text,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                ShotsText(text = "‹ Back", color = ShotsTheme.colorScheme.primary)
            }
        }
        ShotsText(
            text = title,
            style = ShotsTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
    ShotsDivider()
}

@Composable
fun ShotsIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = ShotsTheme.colorScheme.onSurface
) {
    androidx.compose.foundation.Image(
        imageVector = imageVector,
        contentDescription = contentDescription,
        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tint),
        modifier = modifier
    )
}

@Composable
fun ShotsIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduced = isReducedMotion()
    val pressScale by animateFloatAsState(
        targetValue = if (reduced) 1f else if (pressed) MotionTokens.PressScale else 1f,
        animationSpec = if (pressed) tween(MotionTokens.PressMs, easing = MotionTokens.EaseOut)
        else tween(MotionTokens.PressReleaseMs, easing = MotionTokens.EaseOut),
        label = "iconBtnPress"
    )
    Box(
        modifier = modifier
            .scale(pressScale)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                enabled = enabled,
                role = Role.Button,
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center,
        content = { content() }
    )
}
