# 005 — Switch slider transform performance

- **Status**: DONE
- **Commit**: 795574f
- **Severity**: MEDIUM
- **Category**: Performance
- **Estimated scope**: 1 file, ~25 lines

## Problem

Animating layout properties triggers layout + paint + composite; target is transform and opacity only. Two violations:

```kotlin
// app/src/main/java/com/shots/ui/components/ShotsKit.kt:145-149 — current
val thumbOffset by animateDpAsState(
    targetValue = if (checked) 20.dp else 0.dp,
    animationSpec = tween(180, easing = EaseOutCubic),
    label = "switchThumb"
)
// ... Modifier.offset(x = thumbOffset) at line 172
```

`Modifier.offset` with animated Dp re-lays-out the thumb every frame.

```kotlin
// app/src/main/java/com/shots/ui/components/ShotsKit.kt:256-269 — current
androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(32.dp)) {
    val cx = fraction * size.width
    val r = 14.dp.toPx() / 2 * thumbScale
    drawCircle(color = colors.primary, radius = r, center = Offset(cx.coerceIn(r, size.width - r), size.height / 2))
}
```

Canvas redraws the slider thumb on every `thumbScale` frame (main-thread, drops frames under load). Keep transition-time `filter: blur()` under 20px — no blur here, keep it that way.

## Target

Animate `transform` and `opacity` only (AUDIT). In Compose that means `graphicsLayer { translationX / scaleX / scaleY }`, not `offset(x = animatedDp)` or Canvas radius:

```kotlin
// target switch (schematic)
val density = LocalDensity.current
val thumbPx = with(density) { 20.dp.toPx() }
val thumbOffsetPx by animateFloatAsState(if (checked) thumbPx else 0f, tween(180, easing = MotionTokens.EaseOut), label = "switchThumb")
Box(Modifier.graphicsLayer { translationX = thumbOffsetPx })
```

```kotlin
// target slider thumb (schematic)
Box(Modifier.fillMaxWidth().height(32.dp)) {
  // track Boxes unchanged
  Box(Modifier.align(Alignment.CenterStart).offset { IntOffset(((fraction * (maxWidthPx - thumbPx)).toInt()), 0) }.graphicsLayer { scaleX = thumbScale; scaleY = thumbScale }.size(14.dp).clip(CircleShape).background(colors.primary))
}
```

Or minimal fix: keep Canvas but drive `thumbScale` via `graphicsLayer` on a thumb Box instead of `drawCircle` radius. Either satisfies transform-only; prefer Box + graphicsLayer (CSS beats rAF-based JS under load — use CSS/transform for predetermined motion).

## Repo conventions to follow

- `MotionTokens.EaseOut` + durations (`ShotsKit.kt:147,152,205` currently `tween(180/150, easing = EaseOutCubic)`); exemplar `ShotsKit.kt:78-85` press scale via `animateFloatAsState` + `Modifier.scale`.
- Keep `Role.Switch`, `clickable`, `enabled` alpha behavior unchanged.

## Steps

1. `ShotsKit.kt:144-177` (`ShotsSwitch`): replace `animateDpAsState` + `Modifier.offset(x = thumbOffset)` with `animateFloatAsState` pixel translation + `Modifier.graphicsLayer { translationX = ... }`; convert 20.dp once via `LocalDensity`.
2. `ShotsKit.kt:203-269` (`ShotsSlider`): replace Canvas `drawCircle(radius = r * thumbScale)` with a thumb `Box` (`size(14.dp)`, `CircleShape`, `background(primary)`) positioned by fraction and scaled via `graphicsLayer { scaleX = thumbScale; scaleY = thumbScale }`; delete Canvas block.
3. Keep specs: switch 180ms, slider thumb 150ms, `MotionTokens.EaseOut`.
4. Ensure no `animateDpAsState` remains for thumb; no Canvas radius animation.

## Boundaries

- Do NOT change drag logic (`detectTapGestures`/`detectHorizontalDragGestures`), value math, track visuals, or DataStore persistence.
- Do NOT add blur or new deps. STOP on drift.

## Verification

- **Mechanical**: `./gradlew :app:assembleDebug` — `BUILD SUCCESSFUL`.
- **Feel check**: toggle switch 10x fast — thumb glides 180ms, never restarts from zero; drag slider full range — thumb tracks finger 1:1, grows 1.25x on press (150ms), no lag vs Settings komo Slider; profile with GPU rendering bars — no layout spikes on toggle.
- **Done when**: thumb motion is transform-only (`graphicsLayer`), no animated `offset(Dp)` or Canvas radius.
