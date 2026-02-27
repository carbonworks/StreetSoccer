# Street Soccer -- Testing & Performance Plan

This document defines the target device profile, performance budgets, and testing strategy for Street Soccer. It establishes the measurable criteria that determine whether the game runs acceptably on production hardware. For the physics model and tuning constants referenced here, see `physics-and-tuning.md`. For the rendering pipeline, see `technical-architecture.md` Section 8.

---

## 1. Target Device Profile

### Minimum Supported Hardware

| Attribute | Minimum | Notes |
|-----------|---------|-------|
| **Android API level** | 26 (Android 8.0 Oreo) | LibGDX 1.14.x supports API 19+, but API 26 is the practical floor for modern OpenGL ES 3.0 features and 95%+ active device coverage |
| **RAM** | 3 GB | Ensures sufficient headroom for texture atlases, Box2D world, ECS entity pool, and Android OS overhead |
| **GPU** | Adreno 505 / Mali-G71 or better | Minimum GPU tier that handles the SpriteBatch draw call volume and ShapeRenderer trajectory preview at 60 FPS |
| **CPU** | Quad-core ARM Cortex-A53 @ 1.4 GHz | Minimum for fixed-timestep physics at 1/60 s with up to 5 catch-up steps per frame |
| **Screen density** | 320 dpi (xhdpi) | Base density for touch target sizing (48 dp = 96 px at xhdpi) |
| **Screen resolution** | 1280x720 minimum | Viewport scales from 1920x1080 reference; 720p is the lower bound for readable HUD text and target hitboxes |

### Recommended Hardware

| Attribute | Recommended |
|-----------|-------------|
| **Android API level** | 30+ (Android 11+) |
| **RAM** | 4+ GB |
| **GPU** | Adreno 612 / Mali-G57 or better |
| **CPU** | Octa-core with 2+ performance cores @ 2.0 GHz+ |
| **Screen** | 1920x1080 at 60 Hz |

---

## 2. Performance Budgets

### Frame Rate Target

| Metric | Target | Tolerance |
|--------|--------|-----------|
| **Target frame rate** | 60 FPS (16.67 ms per frame) | Sustained -- no single frame should exceed 33 ms (2x budget) except during scene transitions |
| **99th percentile frame time** | < 18 ms | Allows minor spikes without visible stutter |
| **Frame drop threshold** | < 1% dropped frames over a 60-second gameplay window | Measured via `Gdx.graphics.getFramesPerSecond()` and systrace |

### Input Latency Budget

| Metric | Target | Notes |
|--------|--------|-------|
| **Touch-to-visual-response** | < 16 ms (1 frame) | From `touchDown` event to the next rendered frame reflecting the input (slider thumb movement, flick drag visualization) |
| **Steer-to-spin-visible** | < 33 ms (2 frames) | From steer swipe to visible ball trajectory change -- 1 frame for input processing, 1 frame for physics + render |
| **Touch event polling** | Every frame | `InputRouter.processQueuedEvents()` runs at the top of each `render()` call |

### Physics Budget

| Metric | Budget | Notes |
|--------|--------|-------|
| **Fixed timestep** | 1/60 s (16.67 ms) | Per `physics-and-tuning.md` constant #13 |
| **Max steps per frame** | 5 | Spiral-of-death protection (implemented in WP-21). Caps accumulator at `FIXED_TIMESTEP * 5` |
| **Physics step cost** | < 2 ms per step | Includes gravity, drag, Magnus force, spin decay, and position update for 1 ball entity |
| **Box2D step cost** | < 1 ms per step | Zero-gravity world with ~20 static bodies, 0-1 dynamic bodies, 6 velocity iterations, 2 position iterations |
| **Total physics budget** | < 5 ms per frame | Even at max 5 catch-up steps, total physics time must stay within budget |

### Rendering Budget

| Metric | Budget | Notes |
|--------|--------|-------|
| **SpriteBatch draw calls** | < 50 per frame | Background layers (3-5), ball (1), ball shadow (1), moving targets (5-10), static target visuals (10-15), score popups (1-3), HUD elements (5-8) |
| **Texture switches** | < 10 per frame | Use texture atlas packing to minimize switches. Background, sprite atlas, font atlas, and HUD atlas should cover all assets |
| **Shader changes** | < 3 per frame | Default SpriteBatch shader for world rendering, ShapeRenderer for trajectory preview (AIMING only), Stage shader for HUD |
| **ShapeRenderer usage** | AIMING state only | Trajectory preview dots. Max 20 circles per frame. Must not overlap with SpriteBatch begin/end |
| **Fill rate** | < 2x screen overdraw | Background is 1x; ball shadow, ball, and popups add partial overdraw. Big Bomb glow overlay adds ~0.5x at max depth |

### Memory Budget

| Resource | Budget | Notes |
|----------|--------|-------|
| **Total texture memory** | < 64 MB | Background layers (~20 MB at 1920x1080 RGBA), sprite atlas (~4 MB), font atlas (~2 MB), HUD atlas (~2 MB) |
| **Sound pool** | < 8 MB | ~12 OGG sound effects, each < 500 KB. `Sound` instances (not `Music`) for short effects |
| **ECS entity pool** | < 200 entities | Ball (1), shadow (1), catcher (1), static colliders (~15), target sensors (~10), spawn lane targets (~10 active at peak), score popups (~5 active) |
| **Box2D bodies** | < 30 | Static colliders (~15), target sensors (~10), ball (1), catcher (1) |
| **Java heap** | < 128 MB | Well within the 256-512 MB heap limit on minimum-spec devices |
| **Per-frame allocations** | 0 in hot paths | No `new` calls in `update()`, `render()`, `processEntity()`, or `touchDragged()` methods. Pre-allocate reusable objects |

---

## 3. Testing Strategy

### 3.1 Unit Tests

Unit tests validate deterministic, isolated subsystems. These run on JVM without LibGDX or Android dependencies.

| Test Area | What to Test | Priority |
|-----------|-------------|----------|
| **Trajectory calculation** | Given power, angle, and direction, verify initial velocity components match `physics-and-tuning.md` Section 2 equations | P0 |
| **Magnus effect** | Given spinX, spinY, and ball velocity, verify Magnus force magnitude and direction | P0 |
| **Spin decay** | Verify spin halves in ~0.35 s at SPIN_DECAY = 2.0 | P0 |
| **Drag model** | Verify ball retains ~86% speed after 1 second at DRAG = 0.15 | P0 |
| **Depth scaling** | Verify `max(0.05, (540 - y) / 540)` produces correct scale at boundary values (y=0, y=270, y=540, y=600) | P1 |
| **Shadow opacity** | Verify `max(0.1, 1.0 - height / SHADOW_FADE_HEIGHT)` at height=0, height=200, height=400, height=800 | P1 |
| **Flick power normalization** | Verify `clamp(swipeSpeed / MAX_FLICK_SPEED, 0.0, 1.0)` at below-min, mid-range, and above-max speeds | P1 |
| **Steer diminishing returns** | Verify swipe multipliers: swipe 1 at 1.0, swipe 2 at 0.6, swipe 3 at 0.25, swipe 4+ at 0.1 | P0 |
| **Big Bomb threshold** | Verify activation requires both power >= 0.9 AND slider >= 0.7 | P0 |
| **Streak multiplier** | Verify multiplier values at streak counts 1-5+ per GDD Section 5 | P1 |
| **Big Bomb distance scoring** | Verify score = maxY - playerOriginY | P1 |
| **State machine transitions** | Verify all valid transitions and reject invalid ones per `state-machine.md` | P0 |
| **Timer auto-advance** | Verify SCORING (1.0 s) and IMPACT_MISSED (0.75 s) timers advance to READY | P1 |
| **Pause/resume** | Verify Paused state preserves and restores previousState | P1 |
| **Save/load round-trip** | Verify ProfileData and SettingsData serialize/deserialize correctly with default values | P1 |
| **Schema migration** | Verify migration functions transform older JSON versions to current schema | P2 |
| **Corrupt file recovery** | Verify malformed JSON triggers backup + default reset, not crash | P1 |

### 3.2 Integration Tests

Integration tests verify multi-system pipelines. These require a LibGDX headless backend or instrumented test environment.

| Test Pipeline | What to Test | Priority |
|---------------|-------------|----------|
| **Input -> Physics -> Position** | A simulated flick produces a FlickResult that creates a ball entity with correct initial velocity, and after N physics steps the ball position matches the expected trajectory | P0 |
| **Input -> Spin -> Magnus -> Curve** | A steer swipe during BALL_IN_FLIGHT modifies spinX/spinY, which produces Magnus force, which deflects the ball laterally or in depth | P0 |
| **Collision -> State -> Score** | Ball entity contacts a target sensor, triggering SCORING state, incrementing session score by base points * streak multiplier | P0 |
| **Collision -> State -> Miss** | Ball entity contacts a static collider (non-sensor), triggering IMPACT_MISSED state, resetting streak to 0 | P0 |
| **Session lifecycle** | Start session -> kick -> hit -> kick -> miss -> end session. Verify career stats merge correctly (totalKicks, totalHits, bestStreak) | P1 |
| **Settings persistence** | Change trajectory preview toggle -> save -> reload. Verify the setting persists and TrajectorySystem reads it | P1 |
| **Big Bomb pipeline** | Flick at power >= 0.9 with slider >= 0.7 -> verify Big Bomb activation flag -> verify distance scoring at session end | P1 |
| **Spawn system lifecycle** | Verify moving targets spawn at configured intervals, translate along lanes, and despawn off-screen. Verify spawning freezes during SCORING/IMPACT_MISSED | P2 |

### 3.3 Manual Test Matrix

#### Reference Devices

| Tier | Device | SoC | RAM | GPU | Screen | API | Notes |
|------|--------|-----|-----|-----|--------|-----|-------|
| **Budget** | Samsung Galaxy A13 | Exynos 850 | 3 GB | Mali-G52 | 1080x2408, 60 Hz | 33 | Minimum-tier validation |
| **Budget** | Xiaomi Redmi 10A | Helio G25 | 3 GB | PowerVR GE8320 | 720x1600, 60 Hz | 31 | Low-end GPU stress test |
| **Mid-range** | Samsung Galaxy A54 | Exynos 1380 | 6 GB | Mali-G68 | 1080x2340, 120 Hz | 34 | Popular mid-range reference |
| **Mid-range** | Google Pixel 6a | Tensor G1 | 6 GB | Mali-G78 | 1080x2400, 60 Hz | 33 | Stock Android behavior |
| **Mid-range** | OnePlus Nord CE 3 | Snapdragon 782G | 8 GB | Adreno 642L | 1080x2400, 120 Hz | 33 | Qualcomm mid-tier GPU |
| **Flagship** | Samsung Galaxy S23 | Snapdragon 8 Gen 2 | 8 GB | Adreno 740 | 1080x2340, 120 Hz | 34 | Flagship performance ceiling |
| **Flagship** | Google Pixel 8 Pro | Tensor G3 | 12 GB | Immortalis-G715 | 1344x2992, 120 Hz | 34 | High-DPI stress test |
| **Tablet** | Samsung Galaxy Tab S6 Lite | Exynos 1280 | 4 GB | Mali-G68 | 1200x2000 | 34 | Landscape orientation, larger screen |
| **Desktop (debug)** | Any desktop with OpenGL 3.0+ | -- | -- | -- | 1920x1080 | -- | Development iteration; not a production target |

#### Manual Test Checklist

For each reference device, verify:

**Input & Controls**
- [ ] Angle slider responds to drag with no perceptible lag
- [ ] Flick gesture registers correctly; ball launches in the expected direction
- [ ] Multi-touch works: slider + flick simultaneously on two fingers
- [ ] Steer swipes during flight visibly curve the ball
- [ ] Steer diminishing returns are perceptible (1st swipe curves more than 4th)
- [ ] Pause button tap target is reachable and responsive
- [ ] Bomb mode button activates and visually responds

**Rendering & Performance**
- [ ] Background renders without visible seams or scaling artifacts
- [ ] Ball shadow tracks ball position and fades with height
- [ ] Trajectory preview dots render during AIMING when enabled in settings
- [ ] Score popups appear at impact location and animate correctly
- [ ] Streak badge updates and animates on hit/miss
- [ ] Steer budget meter drains correctly during flight
- [ ] No visible frame drops during normal gameplay (steady 60 FPS)
- [ ] Big Bomb flights maintain 60 FPS even with color ramp overlay
- [ ] No Z-ordering artifacts (ball renders in front of shadow, behind HUD)

**State & Lifecycle**
- [ ] Pause/resume returns to correct gameplay state
- [ ] Backgrounding the app (home button) triggers save
- [ ] Returning from background resumes correctly
- [ ] Force-killing the app does not corrupt save data
- [ ] Session stats merge into career stats on quit to menu

**Device-Specific**
- [ ] Touch targets are at least 48 dp on this device's density
- [ ] HUD text is readable at this device's screen size and density
- [ ] No letterboxing or cropping on non-16:9 aspect ratios
- [ ] Sound effects play at correct volume levels
- [ ] Android back button pauses during gameplay, dismisses overlays in menus

### 3.4 Performance Profiling

#### Tools

| Tool | Purpose | When to Use |
|------|---------|-------------|
| **Android GPU Profiler (AGI)** | GPU rendering analysis: draw call count, shader time, overdraw visualization | After major rendering changes; quarterly on minimum-spec device |
| **systrace / Perfetto** | System-level trace: frame timing, CPU scheduling, GC pauses, I/O stalls | When frame drops are reported; after physics or rendering pipeline changes |
| **LibGDX FPSLogger** | In-game FPS counter using `Gdx.graphics.getFramesPerSecond()` | Always enabled in debug builds; logs per-second average |
| **Android Studio Profiler** | Memory allocation tracking, heap dumps, CPU method traces | When investigating memory leaks or per-frame allocation violations |
| **LeakCanary** | Automated memory leak detection | Enabled in debug builds to catch Activity/Context leaks |

#### Profiling Scenarios

| Scenario | What to Measure | Pass Criteria |
|----------|----------------|---------------|
| **60-second free play** | Average FPS, 99th percentile frame time, total GC pauses | Avg FPS >= 58, P99 < 18 ms, GC < 100 ms total |
| **Rapid-fire kicks** (20 kicks in 30 seconds) | Entity creation/destruction overhead, heap growth | No heap growth trend; entity pool stays < 200 |
| **Big Bomb flight** (max power, max angle) | Frame time during 2-3 second flight with color ramp + trajectory preview | All frames < 18 ms |
| **Multiple moving targets** (all 3 spawn lanes active) | Draw call count, texture switches, entity count | Draw calls < 50, entity count < 100 |
| **Settings toggle rapid** | Memory stability after toggling trajectory preview on/off 20 times | No ShapeRenderer leak; heap stable |
| **Pause/resume cycle** (10 times) | Overlay creation/destruction, Stage disposal | No actor or texture leak |
| **App background/foreground** (5 times) | Save I/O latency, texture reload time, heap stability | Save < 50 ms, reload < 2 s, heap stable |

---

## 4. Build Verification

### Automated Build Checks

| Check | Command | Pass Criteria |
|-------|---------|---------------|
| **Compile** | `./gradlew build` | Zero errors, zero new warnings |
| **Desktop run** | `./gradlew :desktop:run` | App launches, reaches attract screen, enters gameplay |
| **Android debug APK** | `./gradlew :android:assembleDebug` | APK builds successfully |
| **Android release APK** | `./gradlew :android:assembleRelease` | Minified APK builds with R8; no missing keep rules |
| **APK size** | Check `android/build/outputs/apk/` | Debug APK < 50 MB, Release APK < 30 MB |

### Pre-Release Checklist

Before any release build:

- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Manual test matrix completed on at least 3 devices (budget, mid-range, flagship)
- [ ] Performance profiling scenarios pass on minimum-spec device
- [ ] No P0 or P1 backlog items remain
- [ ] `./gradlew build` passes with no errors or new warnings
- [ ] ProGuard/R8 does not strip required classes (verified by running release APK on device)
- [ ] Save data from previous version loads correctly (schema migration test)

---

## Companion Documents

| Document | Relevance |
|----------|-----------|
| `physics-and-tuning.md` | Physics equations and tuning constants that define the performance-critical hot path |
| `technical-architecture.md` | Rendering pipeline, game loop structure, and system architecture |
| `ui-hud-layout.md` | HUD element count and rendering requirements |
| `no-forward-debt.md` | Quality gate requirements that overlap with this testing plan |
| `backlog.md` | Outstanding work items that may affect test scope |
