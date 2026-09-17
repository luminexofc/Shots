# Animation plans

Recommended execution order (leverage = impact ÷ effort), dependencies, status.

| # | Title | Severity | Status | Depends on |
|---|-------|----------|--------|------------|
| 001 | Onboarding hero scale duration and origin | HIGH | TODO | — (adds `OnboardingMs` token used by others as example) |
| 002 | Onboarding dots easing and layout | MEDIUM | TODO | 003 (uses `MotionTokens.EaseOut/ColorMs`) |
| 003 | Motion token consolidation | MEDIUM | TODO | — (do first alongside 001; unblocks 002/004/006) |
| 004 | Press feedback gaps | MEDIUM | TODO | 003 (uses `PressScale/EaseOut`) |
| 005 | Switch slider transform performance | MEDIUM | TODO | 003 |
| 006 | Press asymmetry and interruptibility | MEDIUM | TODO | 004 (branches 004's new press scales) |
| 007 | Reduced motion support | MEDIUM | TODO | 003 (uses tokens); apply after 001–006 so reduced branches cover new motion |
| 008 | List stagger and filter teleport | LOW | TODO | 003 |

Order: 003 → 001 → 002 → 004 → 005 → 006 → 008 → 007 (007 last so it gates everything).

## Audit source

- Recon: Jetpack Compose (BOM 2025.04.00) + komo-ui 0.4.0 + custom `ShotsKit`/`SmoothUI`; `MotionTokens` in `SmoothUI.kt:45-54`; popup/dialog/tabs/buttons already 120–240ms `EaseOutCubic` (correct); onboardingft 1000ms bare tweens + 0.5 scale + Dp-size dots (stiff/eye-hurting); no reduced-motion; no stagger.
- Vetted: modal `transform-origin: center` is correct per AUDIT (exempt) — not reported. Compose `animate*AsState` retargeting correct — not reported. Slider direct-drive correct — not reported.
- Missed opportunities folded into 008 (filter teleport, step groups); ReadyPage delight covered by 001 (no extra celebration — utility personality stays crisp).

## How to execute

Pick a plan with any agent (weak executors OK — plans are self-contained with exact `cubic-bezier(0.23, 1, 0.32, 1)`, durations, file:line, excerpts). Then `improve-animations execute <plan>` or manual run. Verify with `./gradlew :app:assembleDebug` + feel checks in each plan. Mark status here DONE when verified on device.
