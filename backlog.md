# Street Soccer — Backlog

Items are ordered by priority (top = do first). Tags indicate category. Wave assignments show which work package picks up each item.

## Open Items

| #  | Item | Tag | Wave |
|----|------|-----|------|
| 25 | Physics debug panel | dev-tool | Unassigned |
| 32 | Decompose LevelScreen god object | refactor | → WP-26 (Wave 6) |
| 36 | Cache ball entity reference in CollisionSystem | perf | → WP-27 (Wave 6) |
| 39 | Add conditional logging to reduce GC pressure | perf | → WP-28 (Wave 6) |
| 40 | Integrate level JSON parsing with LevelScreen | integration | → WP-26 (Wave 6) |
| 41 | Remove fragile shadow detection heuristic | refactor | → WP-27 (Wave 6) |
| 42 | Add AndroidLauncher lifecycle overrides | polish | → WP-29 (Wave 6) |
| 10 | Big Bomb meteor feedback | feature | Wave 5+ |
| 13 | Cosmetic & unlock system | feature | Wave 5+ |
| 14 | Audio Spec | doc | Wave 5+ |
| 15 | Asset Registry (doc + placeholders) | doc | Wave 5+ |
| 16 | Cosmetic & Unlock System Spec | doc | Wave 5+ |
| 17 | Seasonal Variant Templates | doc | Wave 5+ |
| 18 | Big Bomb Meteor Sprite Set | beta | Wave 5+ |
| 19 | Handedness Configuration | beta | Wave 5+ |
| 20 | Testing & Performance Plan | doc | Wave 5+ |
| 21 | Tips System Spec | doc | Wave 5+ |
| 22 | Trajectory Preview Spec | doc | Wave 5+ |
| 23 | Accessibility & Localization | doc | Wave 5+ |
| 24 | Update README.md | doc | Wave 5+ |

## Item Details

### 25. Physics debug panel `dev-tool`

In-game panel for live-tuning all ball flight constants (gravity, max kick speed, drag, Magnus coefficient, spin decay, steer sensitivity, etc.) without rebuilding.

**Settings integration:** A "Debug Panel" toggle in the Settings overlay. When enabled, a small button appears on the gameplay HUD. Tapping the button opens the debug panel overlay.

**Panel layout:**
- A master toggle at the top to enable/disable overrides. When off, the game uses `TuningConstants` defaults regardless of slider positions. When on, slider values replace the compiled defaults for the current session.
- Below the toggle, a horizontally scrolling container of vertical sliders — one per tuning constant. Each slider shows the constant name, current value, and min/max range.
- Each slider has a "?" icon that, when tapped, shows an alert dialog describing what the constant controls, its valid range, and gameplay effect.
- Values can also be set by tapping the current value label and typing a number directly.

**Persistence:** Override values are session-only by default (reset on app restart). The panel does not write to `TuningConstants.kt` or any save file.

### 32. Decompose LevelScreen god object `refactor`

LevelScreen is 410+ lines handling ECS setup, physics, input, pause overlay, save lifecycle, entity creation, and the render loop. Extract into: GameLoop (render/update coordination), ECSBootstrapper (engine + system setup), InputSetup (multiplexer wiring). Keep LevelScreen as a thin screen wrapper.

**Files:** LevelScreen.kt

### 36. Cache ball entity reference in CollisionSystem `perf`

CollisionSystem and CatcherSystem iterate all entities each frame to find the ball via linear search. InputSystem already tracks `activeBallEntity`. Expose it or maintain a cached reference.

**Files:** CollisionSystem.kt:180-196/207-226, CatcherSystem.kt:98-107

### 39. Add conditional logging to reduce GC pressure `perf`

Many Gdx.app.log() calls use string templates that allocate even when logging is disabled. Wrap in conditional checks or use lazy logging.

**Files:** Multiple (GameBootstrapper.kt, LoadingScreen.kt, BackgroundRenderer.kt, InputSystem.kt)

### 40. Integrate level JSON parsing with LevelScreen `integration`

LoadingScreen parses suburban-crossroads.json and stores the result in getLevelData(), but no screen ever calls it. LevelLoader (mentioned in comments) doesn't exist. Level data is parsed but unused.

**Files:** LoadingScreen.kt:131-142

### 41. Remove fragile shadow detection heuristic `refactor`

RenderSystem uses a fallback heuristic (renderLayer == 1 + black tint) to detect shadow entities when BallShadowComponent is missing. Should rely solely on BallShadowComponent tag and ensure InputSystem always adds it.

**Files:** RenderSystem.kt:150-166

### 42. Add AndroidLauncher lifecycle overrides `polish`

AndroidLauncher only implements onCreate(). Adding explicit onPause/onResume/onDestroy overrides (even if just for logging) makes lifecycle debugging easier on-device.

**Files:** AndroidLauncher.kt

### 10. Big Bomb meteor feedback `feature`

Replace the alpha-build red overlay with dedicated fireball sprite + flame trail for Big Bomb flights. See `ui-hud-layout.md` Section 11.

### 13. Cosmetic & unlock system `feature`

Ball skins, impact effects, and trail effects (GDD Section 7): selection UI, preview before unlock, unlock threshold calculations (score milestones, streak achievements, distance milestones), runtime application to ball rendering.

### 14. Audio Spec `doc`

Master list of all sound effects with asset IDs, format, duration, volume levels, and mixing/priority rules. GDD Section 9 describes ~12 cues by feel ("crisp glass-break", "deep metallic clang", etc.) but no implementable asset definitions exist.

### 15. Asset Registry (doc + placeholders) `doc`

Starter version listing all known and planned assets: sprites (ball skins, impact effects, trail effects), sounds (from Audio Spec), particle effects (glass-shatter, confetti, sparks, smoke, splinters), background images, and level JSONs. Include asset IDs, source tool, and style notes. Generate Flat 2.0 style placeholder SVGs for all visual elements that don't yet have assets.

### 16. Cosmetic & Unlock System Spec `doc`

Implementation details for ball skins, impact effects, and trail effects: selection UI, preview before unlock, unlock threshold calculations, runtime application to ball rendering.

### 17. Seasonal Variant Templates `doc`

JSON files or schema extensions for the 5 planned variants (Summer Block Party, Halloween, Winter Holidays, Rainy Day, Garage Sale). GDD Section 6 describes thematic changes but no level data or target/spawn-lane modifications are defined.

### 18. Big Bomb Meteor Sprite Set `beta`

Replace the alpha-build red overlay with a dedicated fireball sprite + flame trail for Big Bomb flights. See `ui-hud-layout.md` Section 11.

### 19. Handedness Configuration `beta`

Settings toggle to move the angle slider to the right edge and mirror the steer budget meter. See `ui-hud-layout.md` Section 11.

### 20. Testing & Performance Plan `doc`

Target devices (min API level, min RAM/GPU), frame rate target (60 FPS?), input latency requirements, SVG rendering budget, memory budget for assets.

### 21. Tips System Spec `doc`

Rendering/dismissal UI, trigger logic implementation, tip rotation/frequency, extensibility for new tips. GDD Section 10 defines 4 starter tips.

### 22. Trajectory Preview Rendering Spec `doc`

Dotted arc calculation, update frequency during AIMING vs. BALL_IN_FLIGHT, visual style (dot spacing, color, fade), performance considerations. Described as a toggleable setting in GDD Section 3.

### 23. Accessibility & Localization `doc`

Colorblind mode considerations, text localization approach, font sizing for screen densities, touch target sizing.

### 24. Update README.md `doc`

Reflect current mechanics (three-input model, 2-axis steer, diminishing returns, ball shadow), add references to all spec documents.

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
