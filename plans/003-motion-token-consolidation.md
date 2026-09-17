# 003 — Motion token consolidation

- **Status**: DONE
- **Commit**: 795574f
- **Severity**: MEDIUM
- **Category**: Cohesion & tokens
- **Estimated scope**: 4 files, ~20 lines

## Problem

`MotionTokens` exists but only `SmoothUI.kt` uses it fully. Three other sites hand-type near-identical easings/durations — five hand-typed values that almost match is a consolidation finding. Mismatched personality (crisp 120–240ms vs floaty 1000ms) follows.

```kotlin
// app/src/main/java/com/shots/ui/overlay/OverlayScreen.kt:186-193 — current
enter = fadeIn(tween(180, easing = EaseOutCubic)) +
        scaleIn(tween(240, easing = EaseOutCubic), initialScale = 0.96f) +
        slideInVertically(tween(240, easing = EaseOutCubic)) { it / 20 }
```

```kotlin
// app/src/main/java/com/shots/ui/components/ShotsKit.kt:78-82 — current
val pressScale by animateFloatAsState(
    targetValue = if (pressed) 0.97f else 1f,
    animationSpec = tween(120, easing = EaseOutCubic),
    label = "btnPress"
)
```

```kotlin
// app/src/main/java/com/shots/ui/components/ShotsKit.kt:145-153 — current
val thumbOffset by animateDpAsState(targetValue = if (checked) 20.dp else 0.dp, animationSpec = tween(180, easing = EaseOutCubic), label = "switchThumb")
val trackColor by animateColorAsState(targetValue = ..., animationSpec = tween(150, easing = EaseOutCubic), label = "switchTrack")
```

```kotlin
// app/src/main/java/com/shots/ui/components/SmoothUI.kt:45-54 — tokens (keep)
object MotionTokens {
    val EaseOut = EaseOutCubic
    const val PressMs = 120
    const val FadeMs = 180
    const val DialogMs = 240
    const val ColorMs = 150
    const val SlideUpDp = 12
    const val EnterScale = 0.96f
    const val PressScale = 0.97f
}
```

## Target

All motion cites tokens; introduce the strong custom curve as the token value (do not invent parallel tokens):

```kotlin
// target — MotionTokens.EaseOut becomes:
val EaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f) // --ease-out: cubic-bezier(0.23, 1, 0.32, 1)
```

```kotlin
// target OverlayScreen:
enter = fadeIn(tween(MotionTokens.FadeMs, easing = MotionTokens.EaseOut)) +
        scaleIn(tween(MotionTokens.DialogMs, easing = MotionTokens.EaseOut), initialScale = MotionTokens.EnterScale) +
        slideInVertically(tween(MotionTokens.DialogMs, easing = MotionTokens.EaseOut)) { it / 20 }
```

Exact AUDIT values: `--ease-out: cubic-bezier(0.23, 1, 0.32, 1)`; `--ease-in-out: cubic-bezier(0.77, 0, 0.175, 1)`; `--ease-drawer: cubic-bezier(0.32, 0.72, 0, 1)`; UI under 300ms; Button 100–160ms; Tooltips/small popovers 125–200ms; Dropdowns/selects 150–250ms; Modals/drawers 200–500ms.

## Repo conventions to follow

- Tokens live in `SmoothUI.kt:45-54`; exemplar `SmoothUI.kt:69-76` already uses `MotionTokens.FadeMs/DialogMs/EaseOut/EnterScale`.
- Import `com.shots.ui.components.MotionTokens` + `androidx.compose.animation.core.CubicBezierEasing` where needed.

## Steps

1. In `SmoothUI.kt:46` change `val EaseOut = EaseOutCubic` to `val EaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)` with comment citing `--ease-out`. Remove now-unused `EaseOutCubic` import if unused elsewhere in file.
2. In `OverlayScreen.kt:186-193` replace `180` → `MotionTokens.FadeMs`, `240` → `MotionTokens.DialogMs`, `EaseOutCubic` → `MotionTokens.EaseOut`, `0.96f` → `MotionTokens.EnterScale`.
3. In `ShotsKit.kt:80,147,152,205` replace `tween(120, easing = EaseOutCubic)` → `tween(MotionTokens.PressMs, easing = MotionTokens.EaseOut)`, `tween(180, ...)` → `tween(MotionTokens.DialogMs - 60, ...)`? No — use semantic tokens: switch thumb 180 stays `tween(180, ...)` → add `const val SwitchMs = 180` to tokens and use it; track 150 → `MotionTokens.ColorMs`; slider thumb 150 → `MotionTokens.ColorMs`. Replace `0.97f` → `MotionTokens.PressScale`.
4. Grep `EaseOutCubic` and bare `tween(120|150|180|240|250|1000)` — none should remain outside `MotionTokens`.

## Boundaries

- Do NOT change durations' numeric values (only move them into tokens), except EaseOut curve swap and plan 001/002 changes which own their files.
- Do NOT change markup/structure; motion properties only. No new dependencies. STOP on drift.

## Verification

- **Mechanical**: `./gradlew :app:assembleDebug` — `BUILD SUCCESSFUL`; `grep -rn EaseOutCubic app/src` shows only token definition (or nothing if fully migrated).
- **Feel check**: popup, dialog, button, switch, slider, tabs feel identical to before (this plan is consolidation, not feel change) — token swap to `cubic-bezier(0.23, 1, 0.32, 1)` should feel slightly snappier start; confirm no regression at 0.25x slow-mo.
- **Done when**: all easings/durations reference `MotionTokens`; one `--ease-out` definition.
