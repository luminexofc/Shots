# 002 — Onboarding dots easing and layout animation

- **Status**: DONE
- **Commit**: 795574f
- **Severity**: MEDIUM
- **Category**: Easing & duration + Performance
- **Estimated scope**: 1 file, ~15 lines

## Problem

Dot indicators animate with bare `tween(250)` (no easing) and animate layout size, which triggers layout + paint + composite instead of transform/opacity only:

```kotlin
// app/src/main/java/com/shots/ui/onboarding/OnboardingScreen.kt:207-211 — current
val dotSize by animateDpAsState(
    targetValue = if (isSelected) 10.dp else 8.dp,
    animationSpec = tween(250),
    label = "dotSize"
)
```

```kotlin
// app/src/main/java/com/shots/ui/onboarding/OnboardingScreen.kt:212-217 — current
val dotColor by animateColorAsState(
    targetValue = if (isSelected) ShotsTheme.colorScheme.primary
    else ShotsTheme.colorScheme.outline,
    animationSpec = tween(250),
    label = "dotColor"
)
```

```kotlin
// app/src/main/java/com/shots/ui/onboarding/OnboardingScreen.kt:218-223 — current
Box(
    modifier = Modifier
        .padding(horizontal = 4.dp)
        .size(dotSize)
        .background(dotColor, CircleShape)
)
```

Hunt hits from AUDIT: bare `ease`/`linear` on entrances, animated layout properties (`width`/`height`), durations over budget. Dots are hit on every swipe during onboarding — should be 125–200ms small-popover budget with `ease-out`.

## Target

```kotlin
// target
val dotScale by animateFloatAsState(
  targetValue = if (isSelected) 1.25f else 1f,
  animationSpec = tween(150, easing = MotionTokens.EaseOut),
  label = "dotScale"
)
val dotColor by animateColorAsState(
  targetValue = if (isSelected) ShotsTheme.colorScheme.primary else ShotsTheme.colorScheme.outline,
  animationSpec = tween(150, easing = MotionTokens.EaseOut),
  label = "dotColor"
)
Box(
  Modifier.padding(horizontal = 4.dp).size(8.dp).scale(dotScale).background(dotColor, CircleShape)
)
```

Exact values from AUDIT: entering → `ease-out`; Tooltips/small popovers 125–200ms (use 150ms, matches `MotionTokens.ColorMs = 150`); `--ease-out: cubic-bezier(0.23, 1, 0.32, 1)` via `MotionTokens.EaseOut`; animate transform and opacity only.

## Repo conventions to follow

- `MotionTokens` in `app/src/main/java/com/shots/ui/components/SmoothUI.kt:45-54`: `val EaseOut`, `const val ColorMs = 150`.
- Exemplar (`SmoothUI.kt:127-136`): `animateColorAsState(..., animationSpec = tween(MotionTokens.ColorMs, easing = MotionTokens.EaseOut), label = "tabBg")`.
- Keep `CircleShape` background + color animation; only size mechanism changes.

## Steps

1. In `OnboardingScreen.kt:207-211` replace `animateDpAsState` size animation with `animateFloatAsState` scale animation above (150ms + `MotionTokens.EaseOut`).
2. In `OnboardingScreen.kt:212-217` add `easing = MotionTokens.EaseOut` and change 250 → `MotionTokens.ColorMs` (150). Add import for `MotionTokens`.
3. In `OnboardingScreen.kt:218-223` change `.size(dotSize)` to `.size(8.dp).scale(dotScale)`; remove `animateDpAsState` import if unused.
4. Verify no `tween(250)` remains in this file for dots.

## Boundaries

- Do NOT touch hero scale (plan 001), pager logic, Skip/Next buttons.
- Do NOT change markup — motion properties only.
- Do NOT add dependencies. STOP and report on drift since 795574f.

## Verification

- **Mechanical**: `./gradlew :app:assembleDebug` — `BUILD SUCCESSFUL`.
- **Feel check**: swipe onboarding 0→3→0 rapidly, confirm dots pop via scale (not layout jump), color fades in 150ms, spamming swipes never restarts from zero (transitions retarget mid-animation).
- **Done when**: no `animateDpAsState` for dots, no bare `tween(250)`, dots use 150ms `ease-out` scale + color.
