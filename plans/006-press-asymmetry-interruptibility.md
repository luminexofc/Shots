# 006 — Press asymmetry and interruptibility

- **Status**: DONE
- **Commit**: 795574f
- **Severity**: MEDIUM
- **Category**: Interruptibility
- **Estimated scope**: 2 files, ~20 lines

## Problem

Symmetric timing on press-and-release is a finding. `ShotsButton` uses one spec both ways:

```kotlin
// app/src/main/java/com/shots/ui/components/ShotsKit.kt:78-82 — current
val pressScale by animateFloatAsState(
    targetValue = if (pressed) 0.97f else 1f,
    animationSpec = tween(120, easing = EaseOutCubic),
    label = "btnPress"
)
```

Deliberate phases (press, hold, destructive confirm) animate slower; the system's response snaps. Press should ease in slightly slower, release should snap faster. Also `SmoothTabs` tab scale/color (`SmoothUI.kt:127-140`, `tween(ColorMs)`) uses the same 150ms both directions — tab select should snap, deselect can linger.

Compose `animate*AsState` already retargets mid-animation (correct — CSS transitions retarget from current state; keyframes restart from zero — no keyframes in repo, good). Gesture-driven slider drag already carries velocity via direct `onValueChange` (good — gesture motion should use springs / direct drive, not fixed keyframes). This plan only fixes symmetry.

## Target

Asymmetric specs (both within Button 100–160ms and Dropdown 150–250ms budgets):

```kotlin
// target
val pressScale by animateFloatAsState(
  targetValue = if (pressed) MotionTokens.PressScale else 1f,
  animationSpec = if (pressed) tween(120, easing = MotionTokens.EaseOut) else tween(100, easing = MotionTokens.EaseOut),
  label = "btnPress"
)
```

For tabs (`SmoothUI.kt:127-140`): selected→snap 150ms, deselect→200ms? Keep both under 300ms UI budget; entering → `ease-out`. Spring alternative per AUDIT (Apple-style `{ type: "spring", duration: 0.5, bounce: 0.2 }`, bounce 0.1–0.3) is reserved for drag-to-dismiss/playful — do NOT spring buttons/tabs.

## Repo conventions to follow

- `MotionTokens`: `PressMs = 120`, `ColorMs = 150`, `EaseOut`, `PressScale = 0.97f` (`SmoothUI.kt:45-54`).
- Exemplar of retarget-safe API: all `animateFloatAsState`/`animateColorAsState` (transitions, interruptible) — keep them, do not introduce `@keyframes` equivalents.

## Steps

1. `ShotsKit.kt:78-82`: branch `animationSpec` on `pressed` (120ms press / 100ms release, both `MotionTokens.EaseOut`, `MotionTokens.PressScale`).
2. Apply same branching to plan 004's new press scales (`ShotsIconButton`, `SegmentedControl`) when implemented — or if 004 not yet done, note dependency in code comment.
3. `SmoothUI.kt:127-140`: branch tab `bg`/`fg`/`scale` specs: `if (selected) tween(MotionTokens.ColorMs, ...) else tween(200, ...)` (select snaps, deselect softens).
4. Keep `ShotsSwitch`/`ShotsSlider` symmetric (continuous drags need symmetric retarget).

## Boundaries

- Do NOT introduce springs for buttons/tabs; do NOT add keyframes; do NOT change drag-dismiss behavior (no velocity dismissal needed — overlay has no drag).
- Motion-only. STOP on drift.

## Verification

- **Mechanical**: `./gradlew :app:assembleDebug` — `BUILD SUCCESSFUL`.
- **Feel check**: press-hold-release button — press dips 120ms, release snaps ~100ms (response snaps); spam-tap 10x — never restarts from zero, always retargets mid-flight; tabs: selecting snaps, deselecting softens; slow-mo 0.25x confirms asymmetry.
- **Done when**: press and release use different durations (120/100), tabs use 150/200 select/deselect, all `ease-out`.
