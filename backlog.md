# Street Soccer — Backlog

Items are ordered by priority (top = do first). Tags indicate category. Wave assignments show which work package picks up each item.

## Open Items

| #  | Item | Tag | Wave |
|----|------|-----|------|
| 13 | Cosmetic & unlock system | feature | Unassigned |

## Item Details

### 13. Cosmetic & unlock system `feature`

Ball skins, impact effects, and trail effects (GDD Section 7): selection UI, preview before unlock, unlock threshold calculations (score milestones, streak achievements, distance milestones), runtime application to ball rendering. See `cosmetic-unlock-spec.md` for implementation spec.

## Completed

- [x] #1 — Straight kick hits invisible wall — Fixed swapped sin/cos in velocity calc (WP-20)
- [x] #2 — Pause overlay menu — PauseOverlay with Resume/Quit, back button support (WP-10, Wave 3)
- [x] #3 — Save session integration — Lifecycle hooks in GameBootstrapper + LevelScreen (WP-11, Wave 3)
- [x] #4 — Settings overlay (functional) — Trajectory toggle, slider side, volume sliders (WP-12, Wave 3)
- [x] #5 — Stats overlay (live data) — Career score, best streak, kicks, targets by type (WP-15, Wave 4)
- [x] #6 — Audio implementation — AudioServiceImpl with 12 cues, placeholder sounds (WP-13, Wave 3)
- [x] #7 — Bomb Mode Button — Red HUD button, pulse animation, auto-reset (WP-16, Wave 4)
- [x] #8 — Ball Catcher NPC — CatcherComponent/System, proximity catch, Caught state (WP-14, Wave 3)
- [x] #9 — Trajectory preview rendering — Dotted arc during AIMING, respects settings (WP-17, Wave 4)
- [x] #11 — Separate buildings from background — BackgroundRenderer with layered loading (WP-18, Wave 4)
- [x] #12 — Flatten front-left hill — Art documentation delivered (WP-19, Wave 4)
- [x] #26 — Fix TrajectorySystem rendering crash — Deferred rendering after engine.update() (WP-21, Wave 5)
- [x] #27 — Fix font memory leaks in HudSystem and PauseOverlay — Managed font lists with disposal (WP-22, Wave 5)
- [x] #28 — Null-safe component accessors in EntityExtensions — All 9 accessors return nullable (WP-23, Wave 5)
- [x] #29 — Resolve double physics accumulation — Removed PhysicsSystem internal accumulator (WP-21, Wave 5)
- [x] #30 — Use cached component mappers — Replaced all getComponent() with mapperFor<>() (WP-23, Wave 5)
- [x] #31 — Spiral-of-death protection — MAX_STEPS_PER_FRAME cap in LevelScreen (WP-21, Wave 5)
- [x] #33 — Wire slider side setting into InputRouter — isInSliderZone() with left/right support (WP-24, Wave 5)
- [x] #34 — Centralize magic numbers — HORIZON_Y, DEPTH_COLLISION_TOLERANCE in TuningConstants (WP-24, Wave 5)
- [x] #35 — Enable ProGuard/R8 and add signing config — isMinifyEnabled=true, proguard-rules.pro (WP-25, Wave 5)
- [x] #37 — Reset batch state after Stage.draw() — drawStageAndResetBatch() helper in HudSystem (WP-22, Wave 5)
- [x] #38 — Reduce BitmapFont allocations in score popups — Shared popupFont instance (WP-22, Wave 5)
- [x] Update technical-architecture.md — Add input handling architecture and physics constants — Delivered as standalone specs: `input-system.md` and `physics-and-tuning.md`
- [x] Physics tuning guide — Define actual constants for gravity, drag, Magnus scaling, spin decay rate — Delivered as `physics-and-tuning.md`
- [x] Update game-mechanics-overview.md — Sync with the new flick+steer+angle slider mechanics, 2-axis steer, diminishing returns, and ball shadow
- [x] Fix project name in `state-machine.md` line 7 — Still says "Suburban Striker" instead of "Street Soccer" (committed as `0c6a645`)
- [x] UI/HUD Layout Spec — Delivered as `ui-hud-layout.md`
- [x] Save & Persistence Spec — Delivered as `save-and-persistence.md`
- [x] Menu & Navigation Flow Spec — Delivered as `menu-and-navigation-flow.md`
- [x] Detailed State Behavior Spec — Delivered in `state-machine.md`
- [x] Update technical-architecture.md — Delivered as 12-section architecture spec
- [x] Audio Spec implementation notes — AudioService interface with no-op placeholders wired into states (done in WP-0/WP-8)
- [x] #32 — Decompose LevelScreen god object — Split into GameLoop.kt, ECSBootstrapper.kt, LevelData.kt + thin LevelScreen wrapper (WP-26, Wave 6)
- [x] #36 — Cache ball entity reference — InputSystem.getActiveBall() used by CollisionSystem and CatcherSystem (WP-27, Wave 6)
- [x] #39 — Conditional logging to reduce GC pressure — Wrapped template-based log calls in logLevel guards (WP-28, Wave 6)
- [x] #40 — Integrate level JSON parsing — LevelData.fromJson() drives ECSBootstrapper entity creation (WP-26, Wave 6)
- [x] #41 — Remove fragile shadow heuristic — RenderSystem uses only BallShadowComponent (WP-27, Wave 6)
- [x] #42 — AndroidLauncher lifecycle overrides — onPause/onResume/onDestroy with Log.d() (WP-29, Wave 6)
- [x] #25 — Physics debug panel — DebugPanelOverlay with sliders for all tuning constants, DebugOverrides singleton (WP-30, Wave 7)
- [x] #10 — Big Bomb meteor feedback — Progressive red tint, screen flash, procedural meteor sprite (WP-32, Wave 7)
- [x] #18 — Big Bomb Meteor Sprite Set — Procedural fireball texture swapped at max depth (WP-32, Wave 7)
- [x] #19 — Handedness Configuration — HUD mirrors slider, steer meter, bomb button based on sliderSide (WP-31, Wave 7)
- [x] #14 — Audio Spec — 22 sound cues with asset IDs, mixing rules, volume model (WP-33, Wave 7)
- [x] #15 — Asset Registry — 67 assets cataloged across all categories (WP-33, Wave 7)
- [x] #16 — Cosmetic & Unlock System Spec — Data model, 13 items, unlock thresholds (WP-33, Wave 7)
- [x] #17 — Seasonal Variant Templates — 5 variant definitions with JSON schema (WP-33, Wave 7)
- [x] #20 — Testing & Performance Plan — Device targets, budgets, test strategy (WP-34, Wave 7)
- [x] #21 — Tips System Spec — TipSystem design, 4 starter tips, trigger logic (WP-34, Wave 7)
- [x] #22 — Trajectory Preview Spec — Arc calculation, visual style, performance budget (WP-34, Wave 7)
- [x] #23 — Accessibility & Localization — Colorblind, touch targets, font scaling, string tables (WP-34, Wave 7)
- [x] #24 — Update README.md — Full rewrite with current mechanics, build instructions, doc index (WP-34, Wave 7)
