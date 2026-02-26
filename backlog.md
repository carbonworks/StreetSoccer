# Street Soccer — Backlog

Items are ordered by priority (top = do first). Tags indicate category. Wave assignments show which work package picks up each item.

## Open Items

| #  | Item | Tag | Wave |
|----|------|-----|------|
| 25 | Physics debug panel | dev-tool | Unassigned |
| 26 | Fix TrajectorySystem rendering crash | bug/critical | Unassigned |
| 27 | Fix font memory leaks in HudSystem and PauseOverlay | bug/critical | Unassigned |
| 28 | Null-safe component accessors in EntityExtensions | bug/critical | Unassigned |
| 29 | Resolve double physics accumulation | bug/critical | Unassigned |
| 30 | Use cached component mappers instead of getComponent() | perf | Unassigned |
| 31 | Add spiral-of-death protection to physics loop | bug | Unassigned |
| 32 | Decompose LevelScreen god object | refactor | Unassigned |
| 33 | Wire slider side setting into InputRouter | bug | Unassigned |
| 34 | Centralize magic numbers into TuningConstants | refactor | Unassigned |
| 35 | Enable ProGuard/R8 and add signing config | build | Unassigned |
| 36 | Cache ball entity reference in CollisionSystem | perf | Unassigned |
| 37 | Reset batch state after Stage.draw() in HudSystem | bug | Unassigned |
| 38 | Reduce BitmapFont allocations in score popups | perf | Unassigned |
| 39 | Add conditional logging to reduce GC pressure | perf | Unassigned |
| 40 | Integrate level JSON parsing with LevelScreen | integration | Unassigned |
| 41 | Remove fragile shadow detection heuristic | refactor | Unassigned |
| 42 | Add AndroidLauncher lifecycle overrides | polish | Unassigned |
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

### 26. Fix TrajectorySystem rendering crash `bug/critical`

TrajectorySystem uses ShapeRenderer.begin() while RenderSystem's SpriteBatch is still active (between batch.begin/end). Mixing ShapeRenderer inside an active SpriteBatch block causes crashes or undefined behavior. Fix: move trajectory rendering to a separate phase in LevelScreen.render() after engine.update(), or have TrajectorySystem render independently outside the batch block.

**Files:** TrajectorySystem.kt:203-243, LevelScreen.kt render loop

### 27. Fix font memory leaks in HudSystem and PauseOverlay `bug/critical`

HudSystem.spawnScorePopup() creates a new BitmapFont() per popup and never disposes it — unbounded leak over a session. PauseOverlay creates ~3 BitmapFont instances without disposal tracking. Fix: pre-allocate shared fonts or add them to the managed disposal lists.

**Files:** HudSystem.kt:680, PauseOverlay.kt:78/119/120

### 28. Null-safe component accessors in EntityExtensions `bug/critical`

Extension properties like `Entity.transform` use `mapperFor<>().get()` without null checks. If an entity lacks the component, this crashes with NPE. Change to nullable return types or add assertions.

**Files:** EntityExtensions.kt:18-43

### 29. Resolve double physics accumulation `bug/critical`

Both LevelScreen (lines 205-212) and PhysicsSystem (internal accumulator) maintain independent fixed-timestep loops. This creates risk of double-stepping or desync. Clarify ownership: either LevelScreen or PhysicsSystem should own the accumulator, not both.

**Files:** LevelScreen.kt:197-221, PhysicsSystem.kt:18-24

### 30. Use cached component mappers instead of getComponent() `perf`

PhysicsSystem, CollisionSystem, and LevelScreen.syncBox2DPositions() use reflection-based `entity.getComponent()` per frame instead of cached `mapperFor<>()` accessors. Causes unnecessary overhead on every entity every frame.

**Files:** PhysicsSystem.kt:27-29, CollisionSystem.kt:119/185/211/215, LevelScreen.kt:322

### 31. Add spiral-of-death protection to physics loop `bug`

During BALL_IN_FLIGHT, if the game hitches the accumulator grows unbounded and the while loop iterates many times, causing further frame stutter. Add a max-iteration cap or clamp the accumulator.

**Files:** LevelScreen.kt:205-212

### 32. Decompose LevelScreen god object `refactor`

LevelScreen is 410+ lines handling ECS setup, physics, input, pause overlay, save lifecycle, entity creation, and the render loop. Extract into: GameLoop (render/update coordination), ECSBootstrapper (engine + system setup), InputSetup (multiplexer wiring). Keep LevelScreen as a thin screen wrapper.

**Files:** LevelScreen.kt

### 33. Wire slider side setting into InputRouter `bug`

InputRouter always assumes the angle slider is on the left side (SLIDER_ZONE_FRACTION = 10% of left edge). The SettingsData.sliderSide property is never read. When the user toggles to "right" in settings, nothing changes.

**Files:** InputRouter.kt:20, SettingsData

### 34. Centralize magic numbers into TuningConstants `refactor`

Scattered magic numbers: `yDiff < 40f` in PhysicsContactListener (collision tolerance), `SLIDER_ZONE_FRACTION` in InputRouter, bounds in CollisionSystem (MIN_X=-100, MAX_X=2020), shadow texture dimensions (64x32) in RenderSystem, duplicate HORIZON_Y=540f in InputSystem and CollisionSystem.

**Files:** PhysicsContactListener.kt:29, InputRouter.kt:20, CollisionSystem.kt:38-49, RenderSystem.kt:54-65

### 35. Enable ProGuard/R8 and add signing config `build`

`isMinifyEnabled = false` in release build type. No `signingConfigs` block defined. Release builds are unoptimized and cannot be created without manual signing.

**Files:** android/build.gradle.kts:27-31

### 36. Cache ball entity reference in CollisionSystem `perf`

CollisionSystem and CatcherSystem iterate all entities each frame to find the ball via linear search. InputSystem already tracks `activeBallEntity`. Expose it or maintain a cached reference.

**Files:** CollisionSystem.kt:180-196/207-226, CatcherSystem.kt:98-107

### 37. Reset batch state after Stage.draw() in HudSystem `bug`

After HudSystem's stage.draw(), the batch color and blend state may be left dirty for subsequent renders. Add explicit batch state reset after stage rendering.

**Files:** HudSystem.kt:416

### 38. Reduce BitmapFont allocations in score popups `perf`

HudSystem.spawnScorePopup() allocates a new BitmapFont per popup. If 50+ popups spawn per session, this stresses GC. Pre-allocate a shared font or use a font pool. (Related to #27 but focused on the performance angle.)

**Files:** HudSystem.kt:677-725

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
