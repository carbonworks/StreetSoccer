# Street Soccer — Asset Registry

**This document is the master catalog of all known and planned assets for Street Soccer.** It covers sprites, backgrounds, sounds, particle effects, level data, and fonts. Each entry lists the asset ID, file path, format, dimensions or duration, current production status, and style notes. For detailed audio specifications, see `audio-spec.md`.

---

## 1. Status Definitions

| Status | Meaning |
|--------|---------|
| **exists** | File is present in the repository and functional |
| **placeholder** | File is present but is a temporary stand-in (wrong style, wrong size, or auto-generated) |
| **planned** | File does not exist yet; listed here for production tracking |

---

## 2. Sprites

All sprites live under `assets/sprites/`. The target art style is **Flat 2.0 vector aesthetic** -- clean geometry, bold colors, minimal shading (GDD Section 1). SVG source files are provided where available; PNG rasters are the runtime format loaded by LibGDX.

### 2.1 Ball

| Asset ID | File Path | Format | Dimensions | Status | Notes |
|----------|-----------|--------|:----------:|:------:|-------|
| `sprite_ball` | `assets/sprites/ball.svg` | SVG | 64x64 px (source) | exists | Default ball sprite (classic white). SVG source for vector rendering. |
| `sprite_ball_png` | `assets/sprites/ball.png` | PNG | 64x64 px | exists | Rasterized fallback of `ball.svg`. Used when SVG rendering is unavailable. |
| `sprite_ball_shadow` | `assets/sprites/ball_shadow.svg` | SVG | 80x40 px (source) | exists | Dark ellipse rendered on the ground plane below the ball. Scaled by depth formula, opacity fades with height. |

### 2.2 Ball Skins (Planned)

These are cosmetic ball variants unlocked through gameplay (see `cosmetic-unlock-spec.md`). Each skin replaces the default `sprite_ball` at runtime.

| Asset ID | File Path | Format | Dimensions | Status | Notes |
|----------|-----------|--------|:----------:|:------:|-------|
| `skin_classic_white` | `assets/sprites/ball.svg` | SVG | 64x64 px | exists | Default skin -- same as `sprite_ball`. Always unlocked. |
| `skin_orange_street` | `assets/sprites/skins/ball_orange.svg` | SVG | 64x64 px | planned | Orange street ball with black pentagon pattern. Unlock: 5,000 total score. |
| `skin_chrome` | `assets/sprites/skins/ball_chrome.svg` | SVG | 64x64 px | planned | Reflective chrome ball with subtle gradient. Unlock: 25,000 total score. |
| `skin_flame` | `assets/sprites/skins/ball_flame.svg` | SVG | 64x64 px | planned | Ball with flame decal pattern. Unlock: 50,000 total score. |
| `skin_pixel` | `assets/sprites/skins/ball_pixel.svg` | SVG | 64x64 px | planned | Retro pixel-art style ball. Unlock: 100,000 total score. |

### 2.3 Targets

| Asset ID | File Path | Format | Dimensions | Status | Notes |
|----------|-----------|--------|:----------:|:------:|-------|
| `sprite_window_intact` | `assets/sprites/window_intact.svg` | SVG | 50x70 px | exists | Intact window pane on house facades. Flat-style glass with frame. |
| `sprite_window_intact_png` | `assets/sprites/window_intact.png` | PNG | 50x70 px | exists | Rasterized fallback. |
| `sprite_window_broken` | `assets/sprites/window_broken.svg` | SVG | 50x70 px | exists | Broken window -- shattered glass pattern in frame. Shown after hit. |
| `sprite_window_broken_png` | `assets/sprites/window_broken.png` | PNG | 50x70 px | exists | Rasterized fallback. |
| `sprite_vehicle` | `assets/sprites/vehicle.svg` | SVG | 120x60 px | exists | Cross-street vehicle (car). Side-view silhouette, flat color. |
| `sprite_vehicle_png` | `assets/sprites/vehicle.png` | PNG | 120x60 px | exists | Rasterized fallback. |
| `sprite_runner` | `assets/sprites/runner.svg` | SVG | 40x80 px | exists | Deep-alley runner/jogger. Front-facing, scales up as it approaches camera. |
| `sprite_runner_png` | `assets/sprites/runner.png` | PNG | 40x80 px | exists | Rasterized fallback. |
| `sprite_drone` | `assets/sprites/drone.svg` | SVG | 48x24 px | exists | Sky-path delivery drone. Top-down view with rotors. |
| `sprite_drone_png` | `assets/sprites/drone.png` | PNG | 48x24 px | exists | Rasterized fallback. |
| `sprite_garage_door` | `assets/sprites/garage_door.svg` | SVG | 120x100 px | planned | Garage door on house facade. Large, flat target surface. |

### 2.4 Effects & Visual Feedback

| Asset ID | File Path | Format | Dimensions | Status | Notes |
|----------|-----------|--------|:----------:|:------:|-------|
| `sprite_glass_particle` | `assets/sprites/glass_particle.svg` | SVG | 8x8 px | exists | Individual glass shard for shatter particle effect. Small, angular. |
| `sprite_glass_particle_png` | `assets/sprites/glass_particle.png` | PNG | 8x8 px | exists | Rasterized fallback. |
| `sprite_impact_confetti` | `assets/sprites/effects/confetti.svg` | SVG | 6x6 px | planned | Confetti piece for the confetti burst impact effect (cosmetic unlock). |
| `sprite_impact_spark` | `assets/sprites/effects/spark.svg` | SVG | 4x4 px | planned | Spark particle for drone hit and metallic impacts. |
| `sprite_impact_smoke` | `assets/sprites/effects/smoke_puff.svg` | SVG | 32x32 px | planned | Smoke puff for smoke-related impact effects and cosmetic trail. |
| `sprite_impact_splinter` | `assets/sprites/effects/splinter.svg` | SVG | 10x3 px | planned | Wood splinter for fence hit particle effect. |
| `sprite_impact_dust` | `assets/sprites/effects/dust.svg` | SVG | 16x16 px | planned | Dust puff for facade hit miss feedback. |
| `sprite_trail_streak` | `assets/sprites/effects/trail_streak.svg` | SVG | 16x4 px | planned | Light streak segment for the "light streak" trail effect (cosmetic). |
| `sprite_trail_sparkle` | `assets/sprites/effects/trail_sparkle.svg` | SVG | 8x8 px | planned | Sparkle particle for the "sparkle trail" effect (cosmetic). |
| `sprite_meteor_glow` | `assets/sprites/effects/meteor_glow.svg` | SVG | 96x96 px | planned | Glow overlay for Big Bomb at maximum depth (beta -- alpha uses color ramp). |

### 2.5 NPCs & Interactive Elements

| Asset ID | File Path | Format | Dimensions | Status | Notes |
|----------|-----------|--------|:----------:|:------:|-------|
| `sprite_catcher` | `assets/sprites/catcher.svg` | SVG | 48x80 px | exists | Intersection catcher NPC. Standing figure in the crossroads. Intercepts balls within `catch_radius`. |

---

## 3. Backgrounds

Background layers live under `assets/backgrounds/`. The multi-layer system is specified in `assets/backgrounds/LAYER_SPEC.txt`. The fallback single-image background is at the assets root.

### 3.1 Layer System

| Asset ID | File Path | Format | Dimensions | Status | Notes |
|----------|-----------|--------|:----------:|:------:|-------|
| `bg_sky` | `assets/backgrounds/sky.png` | PNG | 1920x1080 px | planned | Furthest-back layer. Solid sky gradient, clouds, atmospheric elements. Fully opaque. |
| `bg_ground` | `assets/backgrounds/ground.png` | PNG | 1920x1080 px | planned | Roads, sidewalks, grass, manhole cover, horizon. Upper portion transparent for sky layer behind. |
| `bg_buildings` | `assets/backgrounds/buildings.png` | PNG | 1920x1080 px | planned | Building facades, windows, fences. Areas outside buildings are transparent. |

### 3.2 Fallback

| Asset ID | File Path | Format | Dimensions | Status | Notes |
|----------|-----------|--------|:----------:|:------:|-------|
| `bg_fallback` | `assets/background.jpg` | JPEG | 1920x1080 px | exists | Single-image fallback. Full suburban intersection illustration. Used when layer PNGs are not available. Also serves as the attract screen background. |

### 3.3 Art Notes

| Asset ID | File Path | Format | Status | Notes |
|----------|-----------|--------|:------:|-------|
| `art_note_flatten_hill` | `assets/art-notes/flatten-hill.md` | Markdown | exists | Art edit spec for flattening the front-left hill in `background.jpg`. |

---

## 4. Sound Effects

All sounds live under `assets/sounds/`. For full descriptions, durations, volume levels, mixing rules, and production notes, see `audio-spec.md`.

| Asset ID | File Path | Format | Duration | Status | Category |
|----------|-----------|--------|:--------:|:------:|----------|
| `sfx_kick_normal` | `assets/sounds/kick_normal.ogg` | OGG | 0.3 s | planned | kick |
| `sfx_kick_big_bomb` | `assets/sounds/kick_big_bomb.ogg` | OGG | 0.6 s | planned | kick |
| `sfx_impact_glass` | `assets/sounds/impact_glass.ogg` | OGG | 0.5 s | planned | impact |
| `sfx_impact_metal` | `assets/sounds/impact_metal.ogg` | OGG | 0.6 s | planned | impact |
| `sfx_impact_vehicle` | `assets/sounds/impact_vehicle.ogg` | OGG | 0.8 s | planned | impact |
| `sfx_impact_drone` | `assets/sounds/impact_drone.ogg` | OGG | 0.7 s | planned | impact |
| `sfx_miss_facade` | `assets/sounds/miss_facade.ogg` | OGG | 0.3 s | planned | impact |
| `sfx_miss_fence` | `assets/sounds/miss_fence.ogg` | OGG | 0.35 s | planned | impact |
| `sfx_miss_out_of_bounds` | `assets/sounds/miss_out_of_bounds.ogg` | OGG | 0.4 s | planned | impact |
| `sfx_score_popup` | `assets/sounds/score_popup.ogg` | OGG | 0.25 s | planned | score |
| `sfx_streak_chime` | `assets/sounds/streak_chime.ogg` | OGG | 0.4 s | planned | score |
| `sfx_streak_broken` | `assets/sounds/streak_broken.ogg` | OGG | 0.5 s | planned | score |
| `sfx_big_bomb_score` | `assets/sounds/big_bomb_score.ogg` | OGG | 0.6 s | planned | score |
| `sfx_steer_whoosh_full` | `assets/sounds/steer_whoosh_full.ogg` | OGG | 0.3 s | planned | feedback |
| `sfx_steer_whoosh_reduced` | `assets/sounds/steer_whoosh_reduced.ogg` | OGG | 0.25 s | planned | feedback |
| `sfx_steer_whoosh_weak` | `assets/sounds/steer_whoosh_weak.ogg` | OGG | 0.2 s | planned | feedback |
| `sfx_steer_whoosh_residual` | `assets/sounds/steer_whoosh_residual.ogg` | OGG | 0.15 s | planned | feedback |
| `sfx_catcher_catch` | `assets/sounds/catcher_catch.ogg` | OGG | 0.35 s | planned | feedback |
| `sfx_ui_tap` | `assets/sounds/ui_tap.ogg` | OGG | 0.1 s | planned | ui |
| `sfx_ui_slider_drag` | `assets/sounds/ui_slider_drag.ogg` | OGG | 0.05 s | planned | ui |
| `sfx_ui_pause` | `assets/sounds/ui_pause.ogg` | OGG | 0.15 s | planned | ui |
| `sfx_ambient_suburban` | `assets/sounds/ambient_suburban.ogg` | OGG | 10.0+ s | planned | ambient |

---

## 5. Particle Effects

Particle effects are implemented using LibGDX's `ParticleEffect` system or manually via per-frame sprite emission. Effect definitions will live under `assets/particles/` when the particle system is implemented.

| Effect ID | Source Sprites | Particle Count | Duration | Status | GDD Reference |
|-----------|---------------|:--------------:|:--------:|:------:|---------------|
| `fx_glass_shatter` | `sprite_glass_particle` | 8--15 shards | 0.5 s | planned | Section 9: "Glass-shatter particle effect" on window hit |
| `fx_confetti_burst` | `sprite_impact_confetti` | 20--30 pieces | 0.8 s | planned | Section 7: Confetti burst cosmetic impact effect |
| `fx_sparks` | `sprite_impact_spark` | 6--10 sparks | 0.3 s | planned | Section 9: "Sparks + spiral fall" on drone hit |
| `fx_smoke_puff` | `sprite_impact_smoke` | 3--5 puffs | 0.6 s | planned | Section 7: Smoke puff cosmetic impact effect |
| `fx_splinters` | `sprite_impact_splinter` | 4--8 splinters | 0.4 s | planned | Section 9: "Wood splinter particles" on fence hit |
| `fx_dust_puff` | `sprite_impact_dust` | 3--5 puffs | 0.4 s | planned | Section 9: "Dust puff on impact" on facade hit |
| `fx_pixel_explosion` | (procedural) | 12--20 squares | 0.5 s | planned | Section 7: Pixel explosion cosmetic impact effect |
| `fx_trail_light_streak` | `sprite_trail_streak` | continuous | per-frame | planned | Section 7: Light streak cosmetic trail effect |
| `fx_trail_sparkle` | `sprite_trail_sparkle` | continuous | per-frame | planned | Section 7: Sparkle trail cosmetic trail effect |
| `fx_trail_smoke` | `sprite_impact_smoke` | continuous | per-frame | planned | Section 7: Smoke trail cosmetic trail effect |

---

## 6. Level Data

| Asset ID | File Path | Format | Status | Notes |
|----------|-----------|--------|:------:|-------|
| `level_suburban_crossroads` | `suburban-crossroads.json` | JSON | exists | Base level definition. Contains `level_meta`, `static_colliders`, `target_sensors`, `catcher_spawn_point`, and `spawn_lanes`. Located at project root. |

### Level Data Schema Summary

The JSON schema for level files includes these top-level keys:

```json
{
  "level_meta": { "id", "background_image", "base_resolution", "horizon_y", "player_origin" },
  "static_colliders": [ { "id", "type", "x", "y", "width", "height", "z_layer", "restitution" } ],
  "target_sensors": [ { "id", "type", "x", "y", "width", "height", "points", "z_layer" } ],
  "catcher_spawn_point": { "id", "x", "y", "catch_radius", "z_layer" },
  "spawn_lanes": [ { "id", "target_type", "start_x", "start_y", "end_x", "end_y", "z_layer", "speed_range" } ]
}
```

For the seasonal variant JSON extension schema, see `seasonal-variants.md`.

---

## 7. Fonts

| Asset ID | File Path | Format | Status | Notes |
|----------|-----------|--------|:------:|-------|
| `font_arcade_large` | `assets/fonts/arcade_large.fnt` | BitmapFont | planned | Large arcade-style font for session score display, menu titles, and "TAP TO PLAY" |
| `font_arcade_medium` | `assets/fonts/arcade_medium.fnt` | BitmapFont | planned | Medium arcade-style font for score popups, streak counter, stats display |
| `font_arcade_small` | `assets/fonts/arcade_small.fnt` | BitmapFont | planned | Small arcade-style font for angle label, settings text, tip text |

> **Note:** BitmapFont files (`.fnt` + `.png` atlas) are generated from a TTF source using tools like Hiero (LibGDX), BMFont, or gdx-freetype at runtime. The TTF source font should be identified during production and documented here.

---

## 8. Build Configuration

| File | Purpose | Status |
|------|---------|:------:|
| `assets/build.gradle.kts` | Gradle build script for the assets module. Defines the assets source set for LibGDX's asset pipeline. | exists |

---

## 9. Directory Structure Summary

```
assets/
  background.jpg                 # Fallback background (exists)
  build.gradle.kts               # Gradle config (exists)
  art-notes/
    flatten-hill.md              # Art edit spec (exists)
  backgrounds/
    LAYER_SPEC.txt               # Layer system documentation (exists)
    sky.png                      # Sky layer (planned)
    ground.png                   # Ground layer (planned)
    buildings.png                # Buildings layer (planned)
  sounds/
    .gitkeep                     # Directory placeholder (exists)
    kick_normal.ogg              # (planned)
    kick_big_bomb.ogg            # (planned)
    impact_glass.ogg             # (planned)
    impact_metal.ogg             # (planned)
    impact_vehicle.ogg           # (planned)
    impact_drone.ogg             # (planned)
    miss_facade.ogg              # (planned)
    miss_fence.ogg               # (planned)
    miss_out_of_bounds.ogg       # (planned)
    score_popup.ogg              # (planned)
    streak_chime.ogg             # (planned)
    streak_broken.ogg            # (planned)
    big_bomb_score.ogg           # (planned)
    steer_whoosh_full.ogg        # (planned)
    steer_whoosh_reduced.ogg     # (planned)
    steer_whoosh_weak.ogg        # (planned)
    steer_whoosh_residual.ogg    # (planned)
    catcher_catch.ogg            # (planned)
    ui_tap.ogg                   # (planned)
    ui_slider_drag.ogg           # (planned)
    ui_pause.ogg                 # (planned)
    ambient_suburban.ogg         # (planned)
  sprites/
    ball.svg                     # Default ball (exists)
    ball.png                     # Rasterized ball (exists)
    ball_shadow.svg              # Ball shadow (exists)
    catcher.svg                  # Catcher NPC (exists)
    drone.svg                    # Drone target (exists)
    drone.png                    # Rasterized drone (exists)
    glass_particle.svg           # Glass shard (exists)
    glass_particle.png           # Rasterized glass shard (exists)
    runner.svg                   # Runner target (exists)
    runner.png                   # Rasterized runner (exists)
    vehicle.svg                  # Vehicle target (exists)
    vehicle.png                  # Rasterized vehicle (exists)
    window_broken.svg            # Broken window (exists)
    window_broken.png            # Rasterized broken window (exists)
    window_intact.svg            # Intact window (exists)
    window_intact.png            # Rasterized intact window (exists)
    skins/                       # Ball skin variants (planned)
      ball_orange.svg            # (planned)
      ball_chrome.svg            # (planned)
      ball_flame.svg             # (planned)
      ball_pixel.svg             # (planned)
    effects/                     # Effect sprites (planned)
      confetti.svg               # (planned)
      spark.svg                  # (planned)
      smoke_puff.svg             # (planned)
      splinter.svg               # (planned)
      dust.svg                   # (planned)
      trail_streak.svg           # (planned)
      trail_sparkle.svg          # (planned)
      meteor_glow.svg            # (planned)
  fonts/                         # BitmapFont files (planned)
    arcade_large.fnt             # (planned)
    arcade_large.png             # (planned)
    arcade_medium.fnt            # (planned)
    arcade_medium.png            # (planned)
    arcade_small.fnt             # (planned)
    arcade_small.png             # (planned)
  particles/                     # ParticleEffect definitions (planned)
```

---

## 10. Asset Production Status Summary

| Category | Exists | Placeholder | Planned | Total |
|----------|:------:|:-----------:|:-------:|:-----:|
| Sprites (core) | 14 | 0 | 1 | 15 |
| Sprites (skins) | 0 | 0 | 4 | 4 |
| Sprites (effects) | 0 | 0 | 8 | 8 |
| Backgrounds | 1 | 0 | 3 | 4 |
| Sounds | 0 | 0 | 22 | 22 |
| Particle effects | 0 | 0 | 10 | 10 |
| Level data | 1 | 0 | 0 | 1 |
| Fonts | 0 | 0 | 3 | 3 |
| **Totals** | **16** | **0** | **51** | **67** |

---

## Companion Documents

| Document | Relevance |
|----------|-----------|
| `audio-spec.md` | Full audio cue catalog with descriptions, volumes, mixing rules, and production notes |
| `cosmetic-unlock-spec.md` | Ball skins, impact effects, and trail effects -- defines unlock thresholds and runtime application |
| `seasonal-variants.md` | Per-variant asset swaps (backgrounds, target reskins, ambient audio) |
| `game-design-document.md` Section 9 | Design intent for all visual and audio feedback |
| `environment-z-depth-and-collosion.md` | Z-layer architecture and depth scaling that governs sprite rendering |
| `assets/backgrounds/LAYER_SPEC.txt` | Technical requirements for background layer PNGs |
