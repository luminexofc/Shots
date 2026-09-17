package com.shots.ui.overlay

import android.os.SystemClock
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shots.ShotsApp
import com.shots.data.PreferencesManager
import com.shots.data.Screenshot
import com.shots.data.ScreenshotDatabase
import com.shots.ui.components.AccentSlider
import com.shots.ui.components.MotionTokens
import com.shots.ui.components.ShotsIcon
import com.shots.ui.components.ShotsText
import com.shots.ui.components.isReducedMotion
import com.shots.ui.theme.AppAccent
import com.shots.ui.theme.AppAccents
import com.shots.ui.theme.GlassTokens
import com.shots.ui.theme.ShotsTheme
import com.shots.util.DeleteSuppressor
import com.shots.util.MediaStoreUtils
import com.shots.util.TimerAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

private const val UNDO_WINDOW_MS = 5000L

private val PopupGreen = Color(0xFF22C55E)
private val PopupRed = Color(0xFFEF4444)

private val TimerPresets = listOf(5 to "5m", 15 to "15m", 60 to "1h", 360 to "6h", 1440 to "24h")
private const val MinMinutes = 5f
private const val MaxMinutes = 1440f

private fun minutesForT(t: Float): Int {
    val ratio = MaxMinutes / MinMinutes
    return (MinMinutes * Math.pow(ratio.toDouble(), t.toDouble())).roundToInt().coerceIn(5, 1440)
}

private fun tForMinutes(m: Int): Float {
    val ratio = MaxMinutes / MinMinutes
    return (ln(m / MinMinutes) / ln(ratio)).toFloat().coerceIn(0f, 1f)
}

private fun formatDuration(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes == 60 -> "1 hour"
    minutes < 1440 && minutes % 60 == 0 -> "${minutes / 60} hours"
    minutes == 1440 -> "24 hours"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

@Composable
fun OverlayScreen(
    screenshotPath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val db = ScreenshotDatabase.getInstance(context)
    val prefs = PreferencesManager(context)
    val coroutineScope = rememberCoroutineScope()
    val app = context.applicationContext as ShotsApp
    var showTimerSheet by remember { mutableStateOf(false) }
    var undoActive by remember { mutableStateOf(false) }
    var undoRowId by remember { mutableStateOf(-1L) }
    var snoozedActive by remember { mutableStateOf(false) }
    // Serializes every popup action so a tap can never interleave with a
    // still-running cancel/snooze and silently overwrite its DB result.
    val popupMutex = remember { Mutex() }
    suspend fun <T> serialIO(block: suspend () -> T): T =
        popupMutex.withLock { withContext(Dispatchers.IO) { block() } }
    val accentIndex by prefs.accent.collectAsState(initial = 0)
    val accent = AppAccents.get(accentIndex)
    val snoozeMinutes by prefs.snoozeMinutes.collectAsState(initial = 10)
    val showEdit by prefs.showEditButton.collectAsState(initial = false)

    fun undoDelete() {
        val id = undoRowId
        undoRowId = -1L
        undoActive = false
        Log.d("ShotsPopup", "undo tap id=$id path=$screenshotPath")
        coroutineScope.launch {
            serialIO {
                TimerAlarmScheduler.cancel(context, id)
                db.screenshotDao().getByPath(screenshotPath)?.let {
                    db.screenshotDao().update(it.copy(status = "kept", scheduledDeletionAt = 0L))
                }
            }
            Log.d("ShotsPopup", "undo done id=$id")
            app.trackScreenshotAction("undo")
        }
    }

    fun keepNow() {
        Log.d("ShotsPopup", "keep tap path=$screenshotPath")
        coroutineScope.launch {
            serialIO {
                val existing = db.screenshotDao().getByPath(screenshotPath)
                TimerAlarmScheduler.cancelSnooze(context, screenshotPath)
                if (existing != null) {
                    TimerAlarmScheduler.cancel(context, existing.id)
                    db.screenshotDao().update(
                        existing.copy(status = "kept", scheduledDeletionAt = 0L, fileSizeBytes = fileSize(screenshotPath))
                    )
                } else {
                    db.screenshotDao().insert(
                        Screenshot(
                            path = screenshotPath,
                            timestamp = nowStamp(),
                            status = "kept",
                            fileSizeBytes = fileSize(screenshotPath)
                        )
                    )
                }
            }
            app.trackScreenshotAction("kept")
            withContext(Dispatchers.Main) {
                onDismiss()
            }
        }
    }

    fun deleteWithUndo() {
        Log.d("ShotsPopup", "delete tap path=$screenshotPath")
        coroutineScope.launch {
            DeleteSuppressor.suppress(screenshotPath)
            TimerAlarmScheduler.cancelSnooze(context, screenshotPath)
            val fireAt = System.currentTimeMillis() + UNDO_WINDOW_MS
            val rowId = serialIO {
                val existing = db.screenshotDao().getByPath(screenshotPath)
                if (existing != null) {
                    TimerAlarmScheduler.cancel(context, existing.id)
                    db.screenshotDao().update(
                        existing.copy(status = "pending", scheduledDeletionAt = fireAt, fileSizeBytes = fileSize(screenshotPath))
                    )
                    existing.id
                } else {
                    db.screenshotDao().insert(
                        Screenshot(
                            path = screenshotPath,
                            timestamp = nowStamp(),
                            status = "pending",
                            scheduledDeletionAt = fireAt,
                            fileSizeBytes = fileSize(screenshotPath)
                        )
                    )
                }
            }
            TimerAlarmScheduler.schedule(context, screenshotPath, rowId, fireAt)
            undoRowId = rowId
            undoActive = true
            Log.d("ShotsPopup", "delete scheduled rowId=$rowId fireAt=$fireAt")
            app.trackScreenshotAction("deleted")
        }
    }

    fun scheduleMinutes(minutes: Int) {
        Log.d("ShotsPopup", "timer tap minutes=$minutes path=$screenshotPath")
        coroutineScope.launch {
            val scheduledAt = System.currentTimeMillis() + minutes * 60_000L
            val rowId = serialIO {
                val existing = db.screenshotDao().getByPath(screenshotPath)
                TimerAlarmScheduler.cancelSnooze(context, screenshotPath)
                if (existing != null) {
                    db.screenshotDao().update(
                        existing.copy(
                            status = "pending",
                            scheduledDeletionAt = scheduledAt,
                            fileSizeBytes = fileSize(screenshotPath)
                        )
                    )
                    existing.id
                } else {
                    db.screenshotDao().insert(
                        Screenshot(
                            path = screenshotPath,
                            timestamp = nowStamp(),
                            status = "pending",
                            scheduledDeletionAt = scheduledAt,
                            fileSizeBytes = fileSize(screenshotPath)
                        )
                    )
                }
            }
            TimerAlarmScheduler.schedule(context, screenshotPath, rowId, scheduledAt)
            app.trackScreenshotAction("timer_set")
            withContext(Dispatchers.Main) {
                onDismiss()
            }
        }
    }

    fun snoozeNow() {
        Log.d("ShotsPopup", "snooze tap path=$screenshotPath")
        coroutineScope.launch {
            val remindAt = System.currentTimeMillis() + snoozeMinutes * 60_000L
            serialIO {
                val existing = db.screenshotDao().getByPath(screenshotPath)
                TimerAlarmScheduler.cancelSnooze(context, screenshotPath)
                if (existing != null) {
                    TimerAlarmScheduler.cancel(context, existing.id)
                    db.screenshotDao().update(
                        existing.copy(
                            status = "snoozed",
                            scheduledDeletionAt = remindAt,
                            fileSizeBytes = fileSize(screenshotPath)
                        )
                    )
                } else {
                    db.screenshotDao().insert(
                        Screenshot(
                            path = screenshotPath,
                            timestamp = nowStamp(),
                            status = "snoozed",
                            scheduledDeletionAt = remindAt,
                            fileSizeBytes = fileSize(screenshotPath)
                        )
                    )
                }
            }
            TimerAlarmScheduler.snooze(context, screenshotPath, snoozeMinutes)
            snoozedActive = true
            app.trackScreenshotAction("snoozed")
        }
    }

    fun cancelSnooze() {
        Log.d("ShotsPopup", "snooze-cancel tap path=$screenshotPath")
        snoozedActive = false
        coroutineScope.launch {
            serialIO {
                TimerAlarmScheduler.cancelSnooze(context, screenshotPath)
                db.screenshotDao().getByPath(screenshotPath)?.let {
                    db.screenshotDao().update(it.copy(status = "kept", scheduledDeletionAt = 0L))
                }
            }
            app.trackScreenshotAction("snooze_cancelled")
        }
    }

    fun openEditor() {
        DeleteSuppressor.suppress(screenshotPath)
        coroutineScope.launch {
            try {
                val uri = serialIO {
                    MediaStoreUtils.getUriForScreenshot(context, screenshotPath)
                }
                if (uri != null) {
                    val editIntent = android.content.Intent(
                        android.content.Intent.ACTION_EDIT
                    ).apply {
                        setDataAndType(uri, "image/*")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        // Overlay window holds a non-Activity context: without
                        // this the launch throws and edit silently does nothing.
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(editIntent)
                    app.trackScreenshotAction("edit_opened")
                }
            } catch (_: Exception) {
            }
        }
    }

    var popupVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { popupVisible = true }

    if (undoActive) {
        LaunchedEffect(undoRowId) {
            delay(UNDO_WINDOW_MS + 500)
            onDismiss()
        }
    }

    // No full-screen scrim — tapping outside the card dismisses.
    // The window itself is translucent (Theme.Shots.Overlay).
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                app.trackScreenshotAction("skipped")
                onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = popupVisible,
            enter = if (isReducedMotion()) fadeIn(tween(200))
            else fadeIn(tween(MotionTokens.FadeMs, easing = MotionTokens.EaseOut)) +
                    scaleIn(
                        tween(MotionTokens.DialogMs, easing = MotionTokens.EaseOut),
                        initialScale = MotionTokens.EnterScale
                    ) +
                    slideInVertically(
                        tween(MotionTokens.DialogMs, easing = MotionTokens.EaseOut)
                    ) { it / 20 }
        ) {
            Box(
                modifier = Modifier
                    .padding(32.dp)
                    // Consumes taps on the card itself so only true
                    // outside-taps reach the dismiss handler above.
                    .clickable(
                        onClick = {},
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
            ) {
                when {
                    undoActive -> DeletedPanel(onUndo = { undoDelete() })
                    snoozedActive -> SnoozedPanel(
                        minutes = snoozeMinutes,
                        onCancel = { cancelSnooze() }
                    )
                    showTimerSheet -> TimerSheet(
                        accent = accent,
                        onClose = { showTimerSheet = false },
                        onSetTimer = { minutes -> scheduleMinutes(minutes) }
                    )
                    else -> SwipeShotCard(
                        screenshotPath = screenshotPath,
                        accent = accent,
                        showEdit = showEdit,
                        onKeep = { keepNow() },
                        onDelete = { deleteWithUndo() },
                        onTimer = { showTimerSheet = true },
                        onSnooze = { snoozeNow() },
                        onEdit = { openEditor() },
                        onClose = {
                            app.trackScreenshotAction("skipped")
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PopupCardShell(content: @Composable () -> Unit) {
    val scheme = ShotsTheme.colorScheme
    Box(
        modifier = Modifier
            .shadow(16.dp, RoundedCornerShape(GlassTokens.CardRadius))
            .clip(RoundedCornerShape(GlassTokens.CardRadius))
            .background(scheme.surface)
            .border(1.dp, scheme.outline, RoundedCornerShape(GlassTokens.CardRadius))
    ) {
        content()
    }
}

@Composable
private fun SwipeShotCard(
    screenshotPath: String,
    accent: AppAccent,
    showEdit: Boolean,
    onKeep: () -> Unit,
    onDelete: () -> Unit,
    onTimer: () -> Unit,
    onSnooze: () -> Unit,
    onEdit: () -> Unit,
    onClose: () -> Unit
) {
    val scheme = ShotsTheme.colorScheme
    val scope = rememberCoroutineScope()
    val view = androidx.compose.ui.platform.LocalView.current
    val reduced = isReducedMotion()
    val offX = remember { Animatable(0f) }
    val offY = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    var exiting by remember { mutableStateOf(false) }
    var cardWidthPx by remember { mutableStateOf(1) }
    val samples = remember { mutableListOf<Pair<Long, Offset>>() }

    fun snapBack() {
        scope.launch {
            offX.animateTo(0f, spring(dampingRatio = 1f, stiffness = 400f))
        }
        scope.launch {
            offY.animateTo(0f, spring(dampingRatio = 1f, stiffness = 400f))
        }
    }

    fun exitTo(targetX: Float, targetY: Float, action: () -> Unit) {
        if (exiting) return
        exiting = true
        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        scope.launch { offX.animateTo(targetX, tween(200, easing = MotionTokens.EaseOut)) }
        scope.launch {
            offY.animateTo(targetY, tween(200, easing = MotionTokens.EaseOut))
            alpha.animateTo(0f, tween(180, easing = MotionTokens.EaseOut))
            action()
        }
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val upThresholdPx = remember(density) { with(density) { 120.dp.toPx() } }
    val w = cardWidthPx.toFloat().coerceAtLeast(1f)
    val washColor = when {
        offX.value > 8f -> PopupGreen
        offX.value < -8f -> PopupRed
        offY.value < -8f -> accent.bg
        offY.value > 8f -> accent.bg
        else -> Color.Transparent
    }
    val washAlpha = ((abs(offX.value) / (w * 0.25f))
        .coerceAtLeast(abs(offY.value) / 300f))
        .coerceIn(0f, 1f) * 0.12f
    val rotation = if (reduced) 0f else (offX.value / 1200f).coerceIn(-6f, 6f)

    val metaLabel = remember(screenshotPath) {
        val name = File(screenshotPath).name.ifEmpty { "Screenshot" }.take(32)
        val size = formatBytes(fileSize(screenshotPath))
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        Triple(name, "$size · $time", name)
    }

    PopupCardShell {
        Box(
            modifier = Modifier
                .onSizeChanged { cardWidthPx = it.width }
                .graphicsLayer {
                    translationX = if (reduced) 0f else offX.value
                    translationY = if (reduced) 0f else offY.value
                    rotationZ = rotation
                    this.alpha = alpha.value
                }
                .pointerInput(reduced, exiting) {
                    if (reduced) return@pointerInput
                    detectDragGestures(
                        onDragStart = {
                            samples.clear()
                            samples.add(SystemClock.uptimeMillis() to Offset(offX.value, offY.value))
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offX.snapTo(offX.value + dragAmount.x)
                                offY.snapTo(offY.value + dragAmount.y)
                            }
                            val now = SystemClock.uptimeMillis()
                            samples.add(now to Offset(offX.value, offY.value))
                            while (samples.size > 2 && now - samples.first().first > 120) {
                                samples.removeAt(0)
                            }
                        },
                        onDragEnd = {
                            val now = SystemClock.uptimeMillis()
                            var vx = 0f
                            var vy = 0f
                            if (samples.size >= 2) {
                                val (t0, p0) = samples.first()
                                val (t1, p1) = samples.last()
                                val dt = (t1 - t0).coerceAtLeast(1)
                                vx = (p1.x - p0.x) / dt * 1000f
                                vy = (p1.y - p0.y) / dt * 1000f
                            }
                            val width = cardWidthPx.toFloat().coerceAtLeast(1f)
                            when {
                                vx > 600f || offX.value > width * 0.25f ->
                                    exitTo(width * 1.5f, offY.value, onKeep)
                                vx < -600f || offX.value < -width * 0.25f ->
                                    exitTo(-width * 1.5f, offY.value, onDelete)
                                vy < -600f || offY.value < -upThresholdPx ->
                                    { snapBack(); onTimer() }
                                vy > 600f || offY.value > upThresholdPx ->
                                    exitTo(offX.value, 1000f, onSnooze)
                                else -> snapBack()
                            }
                            samples.clear()
                        },
                        onDragCancel = {
                            snapBack()
                            samples.clear()
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    val file = File(screenshotPath)
                    if (file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = "Detected screenshot",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 10f)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 10f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(scheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            ShotsText(text = "Preview unavailable", color = scheme.secondary)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset((-8).dp, 8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable(
                                role = Role.Button,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = onClose
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        ShotsIcon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = scheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                ShotsText(
                    text = metaLabel.first,
                    style = ShotsTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                ShotsText(
                    text = metaLabel.second,
                    style = ShotsTheme.typography.bodySmall,
                    color = scheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SwipeActionButton(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        color = PopupRed,
                        contentDescription = "Delete screenshot",
                        onClick = onDelete
                    )
                    ShotsText(text = "·", color = scheme.secondary)
                    SwipeActionButton(
                        icon = Icons.Default.Check,
                        label = "Keep",
                        color = PopupGreen,
                        contentDescription = "Keep screenshot",
                        onClick = onKeep
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ShotsIcon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = scheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ShotsText(
                        text = "Swipe up for timer · Pull down to snooze",
                        style = ShotsTheme.typography.bodySmall,
                        color = scheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
                if (reduced) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ShotsText(
                        text = "Snooze",
                        style = ShotsTheme.typography.bodyMedium,
                        color = scheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                role = Role.Button,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = onSnooze
                            )
                            .padding(vertical = 8.dp)
                    )
                }
                if (showEdit) {
                    Spacer(modifier = Modifier.height(4.dp))
                    ShotsText(
                        text = "Edit",
                        style = ShotsTheme.typography.bodyMedium,
                        color = scheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                role = Role.Button,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = onEdit
                            )
                            .padding(vertical = 8.dp)
                    )
                }
            }
            if (washAlpha > 0.005f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(GlassTokens.CardRadius))
                        .background(washColor.copy(alpha = washAlpha))
                )
            }
        }
    }
}

@Composable
private fun SwipeActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduced = isReducedMotion()
    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (reduced) 1f else if (pressed) MotionTokens.PressScale else 1f,
        animationSpec = if (pressed) tween(MotionTokens.PressMs, easing = MotionTokens.EaseOut)
        else tween(MotionTokens.PressReleaseMs, easing = MotionTokens.EaseOut),
        label = "swipeActionPress"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(pressScale)
            .clickable(
                role = Role.Button,
                indication = null,
                interactionSource = interaction,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .border(1.dp, color.copy(alpha = 0.6f), CircleShape)
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            ShotsIcon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        ShotsText(
            text = label,
            style = ShotsTheme.typography.labelMedium,
            color = color
        )
    }
}

@Composable
private fun TimerSheet(
    accent: AppAccent,
    onClose: () -> Unit,
    onSetTimer: (Int) -> Unit
) {
    val scheme = ShotsTheme.colorScheme
    var sliderT by remember { mutableFloatStateOf(tForMinutes(60)) }
    val minutes = minutesForT(sliderT)
    PopupCardShell {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(
                            role = Role.Button,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onClose
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    ShotsIcon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close timer",
                        tint = scheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            ShotsIcon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = scheme.secondary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            ShotsText(
                text = "Auto-delete after",
                style = ShotsTheme.typography.titleLarge,
                color = scheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            ShotsText(
                text = "This screenshot will be deleted after the time ends.",
                style = ShotsTheme.typography.bodyMedium,
                color = scheme.secondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            ShotsText(
                text = formatDuration(minutes),
                style = ShotsTheme.typography.headlineMedium,
                color = scheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            AccentSlider(
                value = sliderT,
                onValueChange = { sliderT = it },
                accent = accent.bg,
                track = scheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimerPresets.forEach { (presetMinutes, label) ->
                    val selected = minutes == presetMinutes
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) accent.bg.copy(alpha = 0.2f)
                                else scheme.surfaceVariant
                            )
                            .border(
                                1.dp,
                                if (selected) accent.bg.copy(alpha = 0.6f)
                                else scheme.outline,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(
                                role = Role.Button,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { sliderT = tForMinutes(presetMinutes) }
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ShotsText(
                            text = label,
                            style = ShotsTheme.typography.labelMedium,
                            color = if (selected) accent.bg else scheme.secondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TimerSetButton(
                accent = accent,
                onClick = { onSetTimer(minutes) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TimerSetButton(
    accent: AppAccent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) MotionTokens.PressScale else 1f,
        animationSpec = if (pressed) tween(MotionTokens.PressMs, easing = MotionTokens.EaseOut)
        else tween(MotionTokens.PressReleaseMs, easing = MotionTokens.EaseOut),
        label = "timerSetPress"
    )
    Box(
        modifier = modifier
            .scale(pressScale)
            .clip(RoundedCornerShape(14.dp))
            .background(accent.bg)
            .clickable(
                role = Role.Button,
                indication = null,
                interactionSource = interaction,
                onClick = onClick
            )
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        ShotsText(
            text = "Set timer",
            style = ShotsTheme.typography.titleMedium,
            color = accent.ink
        )
    }
}

@Composable
private fun DeletedPanel(onUndo: () -> Unit) {
    val scheme = ShotsTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) MotionTokens.PressScale else 1f,
        animationSpec = if (pressed) tween(MotionTokens.PressMs, easing = MotionTokens.EaseOut)
        else tween(MotionTokens.PressReleaseMs, easing = MotionTokens.EaseOut),
        label = "undoPress"
    )
    PopupCardShell {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, PopupGreen.copy(alpha = 0.7f), CircleShape)
                    .background(PopupGreen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                ShotsIcon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = PopupGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            ShotsText(
                text = "Deleted!",
                style = ShotsTheme.typography.titleLarge,
                color = scheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            ShotsText(
                text = "Screenshot removed from your gallery and history.",
                style = ShotsTheme.typography.bodyMedium,
                color = scheme.secondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(pressScale)
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.surfaceVariant)
                    .border(1.dp, scheme.outline, RoundedCornerShape(14.dp))
                    .clickable(
                        role = Role.Button,
                        indication = null,
                        interactionSource = interaction,
                        onClick = onUndo
                    )
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                ShotsText(
                    text = "Undo",
                    style = ShotsTheme.typography.titleMedium,
                    color = scheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SnoozedPanel(
    minutes: Int,
    onCancel: () -> Unit
) {
    val scheme = ShotsTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) MotionTokens.PressScale else 1f,
        animationSpec = if (pressed) tween(MotionTokens.PressMs, easing = MotionTokens.EaseOut)
        else tween(MotionTokens.PressReleaseMs, easing = MotionTokens.EaseOut),
        label = "snoozeCancelPress"
    )
    PopupCardShell {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, scheme.secondary.copy(alpha = 0.5f), CircleShape)
                    .background(scheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                ShotsIcon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = scheme.onSurface,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            ShotsText(
                text = "Snoozed!",
                style = ShotsTheme.typography.titleLarge,
                color = scheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            ShotsText(
                text = "Reminds in ${formatDuration(minutes)}.",
                style = ShotsTheme.typography.bodyMedium,
                color = scheme.secondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(pressScale)
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.surfaceVariant)
                    .border(1.dp, scheme.outline, RoundedCornerShape(14.dp))
                    .clickable(
                        role = Role.Button,
                        indication = null,
                        interactionSource = interaction,
                        onClick = onCancel
                    )
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                ShotsText(
                    text = "Cancel snooze",
                    style = ShotsTheme.typography.titleMedium,
                    color = scheme.onSurface
                )
            }
        }
    }
}

private fun fileSize(path: String): Long = try { File(path).length() } catch (_: Exception) { 0L }

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "Unknown size"
    if (bytes < 1024 * 1024L) return "${bytes / 1024L} KB"
    val mb = bytes / (1024f * 1024f)
    return if (mb < 1024) "%.1f MB".format(mb) else "%.2f GB".format(mb / 1024)
}

private fun nowStamp(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
