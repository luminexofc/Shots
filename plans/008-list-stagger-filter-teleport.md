# 008 — List stagger and filter teleport

- **Status**: DONE
- **Commit**: 795574f
- **Severity**: LOW
- **Category**: Cohesion & tokens + Missed opportunities
- **Estimated scope**: 3 files, ~40 lines

## Problem

Everything-at-once group entrances where a 30–80ms stagger belongs, plus state changes that teleport. Stagger is decorative — must never block interaction.

1. `HistoryScreen.kt:111-166` — `LazyColumn { items(filteredScreenshots) { ... } }` with no `animateItemPlacement`, no entrance stagger. Switching `SmoothTabs` filters (All/Kept/Pending/Snoozed/Deleted, `HistoryScreen.kt:95-100`) swaps the whole list instantly — jarring crossfade that shows two overlapping states / teleport.
2. `MainScreen.kt:196-204` How-It-Works dialog steps, `OnboardingScreen.kt:352-356` steps, `PermissionsPage` items (`OnboardingScreen.kt:312-320`) — all `Column` children appear at once.

```kotlin
// app/src/main/java/com/shots/ui/history/HistoryScreen.kt:111-112 — current
LazyColumn {
    items(filteredScreenshots) { screenshot ->
```

## Target

Per AUDIT: 30–80ms stagger; jarring crossfade masked with subtle `filter: blur(2px)` during transition; use `translate` percentages / `clip-path: inset()` reveals (no hardcoded pixel offsets) for additive delight. In Compose:

```kotlin
// target History list
LazyColumn {
  items(filteredScreenshots, key = { it.id }) { screenshot ->
    var visible by remember(screenshot.id, selectedFilter) { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay((index * 40L).coerceAtMost(200L)); visible = true } // 30–80ms stagger (use 40ms)
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(200, easing = MotionTokens.EaseOut)) + slideInVertically(tween(200, easing = MotionTokens.EaseOut)) { it / 10 }) {
      ScreenshotItem(...)
    }
  }
}
// + Modifier.animateItemPlacement() on item container for filter reorders
```

Stagger 40ms (within 30–80ms), fade/slide 200ms `ease-out` (Tooltips 125–200ms / Dropdowns 150–250ms). Blur: Compose has no transition-time `filter: blur()` under 20px equivalent cheap — skip blur, use fade+slide (note why in code comment). Filter change keeps scroll position, never blocks tap.

## Repo conventions to follow

- `MotionTokens.EaseOut/FadeMs` (`SmoothUI.kt:45-54`); exemplar entrance `SmoothUI.kt:69-76`.
- `ScreenshotItem` key must be stable (`it.id`) for `animateItemPlacement` to work.

## Steps

1. `HistoryScreen.kt:111-166`: add `key = { it.id }` to `items`, add `Modifier.animateItemPlacement()` to item root, add per-item 40ms stagger entrance (`fadeIn + slideInVertically`, 200ms, `MotionTokens.EaseOut`); cap total stagger delay at ~200ms for long lists.
2. `MainScreen.kt:196-204` + `OnboardingScreen.kt:352-356,312-320`: add same 40ms stagger entrance to step/permission rows (fade+slide, non-blocking).
3. Keep `SmoothTabs` selection instant (tab feedback already 150ms) — stagger only list content, not the tab bar.
4. Document why no blur (Compose blur cost, keep under 20px rule satisfied by omission).

## Boundaries

- Do NOT change filtering logic, DB queries, or item layout.
- Do NOT make stagger block interaction (delay only entrance alpha/offset, not clickability). STOP on drift.

## Verification

- **Mechanical**: `./gradlew :app:assembleDebug` — `BUILD SUCCESSFUL`.
- **Feel check**: History → switch All→Pending→Deleted — list crossfades item-by-item (~40ms apart), no teleport flash; spam filter taps — retargets mid-animation; long list (>20) caps stagger so last item appears <500ms; slow-mo confirms slide `it/10` (subtle, not hardcoded px).
- **Done when**: filter changes animate (fade+slide, 40ms stagger, keyed + `animateItemPlacement`), static step groups stagger identically.
