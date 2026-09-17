# 007 — Reduced motion support

- **Status**: DONE
- **Commit**: 795574f
- **Severity**: MEDIUM
- **Category**: Accessibility
- **Estimated scope**: 3 files, ~30 lines

## Problem

Movement with no `prefers-reduced-motion` handling. Grep for `prefers-reduced-motion|ReducedMotion|isEnabled` in `app/src` returns nothing. Every entrance (`SmoothDialog` fade+scale+slide, `OverlayScreen` fade+scale+slide), tab scale, button press scale, slider thumb scale, and onboarding hero scale moves unconditionally — eye-hurting for motion-sensitive users.

AUDIT rule: reduced motion means fewer and gentler animations, not zero — keep transitions that aid comprehension, remove position changes:

```css
@media (prefers-reduced-motion: reduce) {
  .element { animation: fade 0.2s ease; } /* keep opacity/color, drop movement */
}
```

In JS: `useReducedMotion()` and branch transform values. Ungated `:hover` motion N/A on touch (keep as-is).

## Target

Branch transform values, keep opacity/color at 0.2s (200ms) fade:

```kotlin
// target pattern (Compose equivalent of AUDIT snippet)
@Composable fun isReducedMotion(): Boolean {
  // Android: Settings.Global.TRANSITION_ANIMATION_SCALE == 0f || AccessibilityManager.isEnabled with reduced-motion API 33+
  // Minimal: ViewConfiguration / animatorDurationScale == 0f
  val context = LocalContext.current
  return remember { android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) == 0f }
}

// usage:
val reduced = isReducedMotion()
enter = fadeIn(tween(200)) + if (!reduced) scaleIn(...) + slideInVertically(...) else EnterTransition.None
// press scale: if (reduced) 1f else 0.97f; tab/slide/scale → 1f; keep animateColorAsState
```

Keep opacity/color (fade 0.2s), drop movement (scale/slide/offset). Do not nuke all feedback.

## Repo conventions to follow

- Tokens `MotionTokens.FadeMs = 180` (~0.2s), `EaseOut`, `EnterScale/PressScale` (`SmoothUI.kt:45-54`); exemplar `SmoothUI.kt:67-77` entrance composition.
- Add `app/src/main/java/com/shots/ui/components/ReducedMotion.kt` with `isReducedMotion()` + `reducedMotionScale(target: Float)` helpers; reuse in `SmoothUI`, `OverlayScreen`, `ShotsKit`, `OnboardingScreen`.

## Steps

1. Create `ReducedMotion.kt` with `isReducedMotion()` reading `TRANSITION_ANIMATION_SCALE == 0f` (plus `AccessibilityManager` reduced-motion check on API 33+ where available, default false).
2. `SmoothUI.kt:67-77` (`SmoothDialog`): if reduced → `enter = fadeIn(tween(200))` only; else current fade+scale+slide.
3. `OverlayScreen.kt:184-193`: same branching for popup `AnimatedVisibility`.
4. `ShotsKit.kt` + `SmoothUI.kt` tabs + `OnboardingScreen` heroes: `targetValue = if (reduced) 1f else <scale>`; keep `animateColorAsState` always.
5. Keep durations ≤200ms for reduced path (fade 0.2s).

## Boundaries

- Do NOT remove all feedback in reduced mode (keep fade/color).
- Do NOT gate click behavior or touch `:hover` (N/A). Motion-only. STOP on drift.

## Verification

- **Mechanical**: `./gradlew :app:assembleDebug` — `BUILD SUCCESSFUL`.
- **Feel check**: enable Animator duration scale 0x (Developer options) / Accessibility reduced motion → popup/dialog fades 200ms with no scale/slide; tabs change color only; buttons don't dip; disable → full motion returns. Confirm comprehension retained (fade signals entrance).
- **Done when**: reduced-motion drops position/scale, keeps opacity/color fade 0.2s, gated in all 4 sites.
