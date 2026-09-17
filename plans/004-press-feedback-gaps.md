# 004 — Press feedback gaps

- **Status**: DONE
- **Commit**: 795574f
- **Severity**: MEDIUM
- **Category**: Physicality & origin
- **Estimated scope**: 3 files, ~40 lines

## Problem

Pressable elements with no press feedback (AUDIT: hunt for pressable elements with no press feedback). `ShotsButton` is correct (`scale 0.97`, 120ms), but these are not:

```kotlin
// app/src/main/java/com/shots/ui/components/ShotsKit.kt:343-362 — current
fun ShotsIconButton(onClick: () -> Unit, ...) {
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = enabled, role = Role.Button, indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick).padding(8.dp), ...)
}
```

```kotlin
// app/src/main/java/com/shots/ui/components/SegmentedControl.kt:37-52 — current
Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (isSelected) ... else ...).clickable(role = Role.Tab, indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = { onSelected(index) }).padding(vertical = 10.dp), ...)
```

Komo `Button` usages in `OverlayScreen.kt:222-334` (Keep/Delete/presets/Custom) and `TimerPickerDialog` options `OverlayScreen.kt:489-493` (`.clickable { onTimerSelected(minutes) }`) also have no scale feedback — tapping feels dead/stiff.

## Target

Per AUDIT: press feedback `transform: scale(0.97)` on `:active` with `transition: transform 160ms ease-out`, subtle (0.95–0.98). In Compose:

```kotlin
// target pattern (matches ShotsButton ShotsKit.kt:76-85)
val interactionSource = remember { MutableInteractionSource() }
val pressed by interactionSource.collectIsPressedAsState()
val pressScale by animateFloatAsState(
  targetValue = if (pressed) MotionTokens.PressScale else 1f, // 0.97f
  animationSpec = tween(160, easing = MotionTokens.EaseOut),
  label = "press"
)
Box(Modifier.scale(pressScale).clickable(interactionSource = interactionSource, indication = null, ...) { ... })
```

Use 160ms (`transition: transform 160ms ease-out`) for new press feedback; keep existing `ShotsButton` 120ms as-is (within Button 100–160ms budget).

## Repo conventions to follow

- Exemplar `ShotsKit.kt:76-85`: `collectIsPressedAsState` + `animateFloatAsState(tween(120, easing = EaseOutCubic))` + `Modifier.scale(pressScale)` + `indication = null`.
- Tokens `MotionTokens.PressScale = 0.97f`, `MotionTokens.EaseOut` (`--ease-out: cubic-bezier(0.23, 1, 0.32, 1)` after plan 003).

## Steps

1. `ShotsKit.kt:343-362` (`ShotsIconButton`): add `interactionSource` + `pressed` + `pressScale` (160ms, `MotionTokens.EaseOut`, `MotionTokens.PressScale`) and `.scale(pressScale)` before `.clip`; reuse the hoisted `interactionSource` in `clickable`.
2. `SegmentedControl.kt:37-52`: same pattern per option (hoist `interactionSource` inside `forEachIndexed` item, not shared across options); scale the option `Box`.
3. `OverlayScreen.kt:489-493` timer rows: wrap `ShotsText` row in press-scale Box or add `Modifier.scale` on press via same pattern (per-row interactionSource); keep `fillMaxWidth` + `padding(vertical = 12.dp)`.
4. Komo `Button`s in `OverlayScreen` cannot take scale internally — wrap each `Button(modifier = ...)` content's outer `Modifier` with a parent `Box(Modifier.scale(...))`? Simpler: wrap Keep/Delete/preset rows in a press-scale container following the same pattern; do not fork komo.
5. Verify `collectIsPressedAsState` import added where needed.

## Boundaries

- Do NOT change colors, layout, click behavior, or komo library code.
- Do NOT retune `ShotsButton` (plan 006 owns asymmetry). Motion-only.
- STOP on drift since 795574f.

## Verification

- **Mechanical**: `./gradlew :app:assembleDebug` — `BUILD SUCCESSFUL`.
- **Feel check**: tap every button/tab/icon/ ഓtimer row — confirm 0.97 dip on press, snap back on release; at 0.25x slow-mo scale hits 0.97, never below 0.95; rapid taps retarget mid-animation (transitions, not keyframes restarting from zero).
- **Done when**: every pressable scales 0.95–0.98 on press with 160ms `ease-out`; no pressable with `clickable(indication = null)` lacks press scale (except scrim dismiss).
