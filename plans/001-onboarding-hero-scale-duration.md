# 001 — Onboarding hero scale duration and origin

- **Status**: DONE
- **Commit**: 795574f
- **Severity**: HIGH
- **Category**: Easing & duration + Physicality & origin
- **Estimated scope**: 1 file, ~10 lines

## Problem

`WelcomePage` and `ReadyPage` scale from nothing with a 1000ms default tween, no easing. This breaks two rules: UI animations stay under 300ms (marketing/explanatory can be longer, but 1000ms is eye-hurting), and never `scale(0)` — target is `scale(0.9–0.97)` + `opacity: 0`. Starting at 0.5f reads as a pop from nothing and the slow linear-feel ramp is the stiffest motion in the app.

Locations:

```kotlin
// app/src/main/java/com/shots/ui/onboarding/OnboardingScreen.kt:273-275 — current
var scale by remember { mutableStateOf(0.5f) }
val animatedScale by animateFloatAsState(targetValue = scale, animationSpec = tween(durationMillis = 1000), label = "scale")
LaunchedEffect(Unit) { scale = 1f }
```

```kotlin
// app/src/main/java/com/shots/ui/onboarding/OnboardingScreen.kt:379-381 — current
var scale by remember { mutableStateOf(0.5f) }
val animatedScale by animateFloatAsState(targetValue = scale, animationSpec = tween(durationMillis = 1000), label = "scale")
LaunchedEffect(Unit) { scale = 1f }
```

Usage `Modifier.size(120.dp).scale(animatedScale)` (lines 285, 389) animates size via scale transform but from 0.5.

## Target

Rare/first-time onboarding may use delight budget, but keep it snappy with a strong ease-out:

```kotlin
// target
val EaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f) // --ease-out: cubic-bezier(0.23, 1, 0.32, 1)
var scale by remember { mutableStateOf(0.96f) }
val animatedScale by animateFloatAsState(
  targetValue = scale,
  animationSpec = tween(500, easing = EaseOut),
  label = "scale"
)
```

Why 500ms: Modals/drawers budget is 200–500ms; marketing/explanatory can be longer — 500ms is the max of that budget, down from 1000ms. Entering/exiting uses `ease-out` (starts fast, feels responsive). Physicality target `scale(0.9–0.97)` + `opacity: 0` — use 0.96 to match `MotionTokens.EnterScale = 0.96f`. Add `alpha` 0→1 alongside scale (animate transform and opacity only).

## Repo conventions to follow

- Easing tokens live in `app/src/main/java/com/shots/ui/components/SmoothUI.kt:45-54` (`MotionTokens`), e.g. `val EaseOut = EaseOutCubic`, `const val DialogMs = 240`, `const val EnterScale = 0.96f`.
- Exemplar that already does this correctly (`SmoothUI.kt:69-76`): `fadeIn(tween(MotionTokens.FadeMs, easing = MotionTokens.EaseOut)) + scaleIn(tween(MotionTokens.DialogMs, easing = MotionTokens.EaseOut), initialScale = MotionTokens.EnterScale)`.
- Add `OnboardingMs = 500` to `MotionTokens` and reuse it in both pages; do not hardcode 500 twice.

## Steps

1. In `SmoothUI.kt:45-54` add `const val OnboardingMs = 500` to `MotionTokens`.
2. In `OnboardingScreen.kt:271-275` (`WelcomePage`): change `mutableStateOf(0.5f)` to `mutableStateOf(0.96f)`, change `tween(durationMillis = 1000)` to `tween(MotionTokens.OnboardingMs, easing = MotionTokens.EaseOut)`, add `val alpha by animateFloatAsState(if (scale == 1f) 1f else 0f, tween(MotionTokens.OnboardingMs, easing = MotionTokens.EaseOut))` and apply `.graphicsLayer { scaleX = animatedScale; scaleY = animatedScale; alpha = alpha }` instead of `.scale(animatedScale)`.
3. Repeat step 2 in `OnboardingScreen.kt:378-381` (`ReadyPage`) identically.
4. Keep `LaunchedEffect(Unit) { scale = 1f }` unchanged.

## Boundaries

- Do NOT touch `HorizontalPager`, dot indicators, `PermissionsPage`, `HowItWorksPage`, or navigation logic.
- Do NOT change markup/structure — motion properties only.
- Do NOT add new dependencies.
- If a step doesn't match the code you find (drift since 795574f), STOP and report instead of improvising.

## Verification

- **Mechanical**: `./gradlew :app:assembleDebug` — expect `BUILD SUCCESSFUL`.
- **Feel check**: fresh install → onboarding, trigger Welcome and Ready pages, confirm:
  - Icon scales from its center from ~0.96 (not from nothing) with fade, lands in ~500ms, no slow start (`ease-in` on UI is always a finding).
  - Replay 3x — feels like delight, not lag; compare against dialog entrance (180/240ms) — onboarding slightly longer is intentional for rare moment.
  - In Android Studio slow animation (0.25x) confirm scale + opacity move together.
- **Done when**: both heroes enter in 500ms with `--ease-out: cubic-bezier(0.23, 1, 0.32, 1)` from 0.96 + opacity, no 1000ms tween remains.
