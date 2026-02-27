# Street Soccer — Seasonal Variant Templates

**This document defines the schema and content for all five planned seasonal variants.** Each variant re-skins the Suburban Crossroads street with thematic visuals, target types, and audio while retaining the same collision geometry and core mechanics. For the design intent, see `game-design-document.md` Section 6.

---

## 1. Variant Architecture

### 1.1 What Changes Per Variant

| Layer | What Changes | What Stays the Same |
|-------|-------------|---------------------|
| **Background** | Sky, ground, and buildings layers are swapped for themed versions | Layer dimensions (1920x1080), layer count (3), fallback behavior |
| **Target visuals** | Static target sprites are reskinned (e.g., window becomes jack-o-lantern) | Target hitbox geometry, point values, z-layer positions |
| **Moving targets** | Spawn lane target types may change (e.g., vehicles become ice cream trucks) | Lane start/end coordinates, speed ranges, z-layers |
| **Catcher NPC** | Catcher sprite may be reskinned | Catcher position, catch radius, behavior |
| **Audio** | Ambient loop changes; some impact sounds may have themed overlays | Core impact sounds, UI sounds, volume model |
| **Special scoring** | Some variants may add bonus targets or modify point values | Base scoring formula, streak multiplier curve, Big Bomb mechanics |

### 1.2 What Never Changes

The following are **invariant across all variants** and must not be modified by variant data:

- Static collider geometry (house facades, fences) -- same Box2D bodies
- Ball physics (gravity, drag, Magnus, spin decay)
- Launch zone position and player origin
- Horizon Y and depth scaling formula
- Streak multiplier curve
- Big Bomb activation thresholds and scoring formula
- Input system behavior (slider, flick, steer)
- Cosmetic system (all cosmetics work on all variants)
- HUD layout and behavior

---

## 2. JSON Schema Extension

Each variant is defined by a JSON file that **extends** the base `suburban-crossroads.json` format. The variant file is a complete level definition -- not a diff or patch. This keeps loading logic simple: read one file, get a complete level.

### 2.1 Extended Schema

The variant JSON adds a `variant_meta` block to the existing `level_meta`, and may modify `target_sensors`, `spawn_lanes`, and `catcher_spawn_point`. The `static_colliders` block is **identical** to the base level (copied verbatim).

```json
{
  "level_meta": {
    "id": "suburban_crossroads_variant_id",
    "variant_id": "variant_id",
    "base_level": "suburban_crossroads_01",
    "background_image": "background_variant.jpg",
    "background_layers": {
      "sky": "backgrounds/variant/sky.png",
      "ground": "backgrounds/variant/ground.png",
      "buildings": "backgrounds/variant/buildings.png"
    },
    "base_resolution": { "width": 1920, "height": 1080 },
    "horizon_y": 540,
    "player_origin": { "x": 960, "y": 0 }
  },

  "variant_meta": {
    "display_name": "Variant Display Name",
    "description": "Brief description shown in variant selection UI",
    "unlock_condition": {
      "type": "total_score",
      "threshold": 50000
    },
    "ambient_audio": "sounds/variant/ambient_loop.ogg",
    "color_palette": {
      "sky_tint": "#RRGGBB",
      "ground_tint": "#RRGGBB",
      "ui_accent": "#RRGGBB"
    },
    "target_reskins": {
      "window_glass": "sprites/variant/window_reskin.svg",
      "door_metal": "sprites/variant/door_reskin.svg"
    },
    "catcher_reskin": "sprites/variant/catcher_reskin.svg",
    "special_sounds": {
      "impact_variant_target": "sounds/variant/impact_special.ogg"
    }
  },

  "static_colliders": [
    "... identical to base suburban-crossroads.json ..."
  ],

  "target_sensors": [
    "... may differ from base (reskinned targets, different point values) ..."
  ],

  "catcher_spawn_point": {
    "... same position, possibly reskinned sprite ..."
  },

  "spawn_lanes": [
    "... may differ from base (themed target types, adjusted speeds) ..."
  ]
}
```

### 2.2 New Fields Explained

| Field | Type | Purpose |
|-------|------|---------|
| `variant_id` | string | Unique variant identifier. Matches IDs in `VariantState.unlockedVariants`. |
| `base_level` | string | ID of the base level this variant derives from. Used for validation. |
| `background_layers` | object | Paths to the three background layer PNGs for this variant. |
| `display_name` | string | Human-readable name shown in the variant selection carousel. |
| `description` | string | Brief flavor text for the variant selection card. |
| `unlock_condition` | object | Condition to unlock this variant (type + threshold). Types: `total_score`, `total_hits`, `best_streak`. |
| `ambient_audio` | string | Path to the variant's ambient audio loop (OGG format). |
| `color_palette` | object | Tint colors for sky/ground rendering and UI accent highlights. |
| `target_reskins` | object | Map of base target type IDs to variant-specific sprite paths. |
| `catcher_reskin` | string | Path to the variant's catcher sprite. |
| `special_sounds` | object | Map of variant-specific sound cue IDs to OGG file paths. |

### 2.3 Loading Logic

```
function loadVariant(variantId: String): LevelData {
    val filename = VARIANT_FILE_MAP[variantId]
    val json = readJsonFile(filename)
    val levelData = deserialize(json)

    // Validate: static_colliders must match base level
    assert(levelData.static_colliders == BASE_COLLIDERS)

    // Load variant background layers
    loadBackgroundLayers(levelData.level_meta.background_layers)

    // Apply target reskins
    for (target in levelData.target_sensors):
        target.sprite = levelData.variant_meta.target_reskins[target.type] ?: defaultSprite(target.type)

    // Load ambient audio
    loadAmbientAudio(levelData.variant_meta.ambient_audio)

    return levelData
}
```

---

## 3. Variant File Map

| Variant ID | JSON File | Status |
|------------|-----------|:------:|
| `suburban_crossroads` | `suburban-crossroads.json` | exists |
| `summer_block_party` | `variants/summer-block-party.json` | planned |
| `halloween_night` | `variants/halloween-night.json` | planned |
| `winter_holidays` | `variants/winter-holidays.json` | planned |
| `rainy_day` | `variants/rainy-day.json` | planned |
| `garage_sale` | `variants/garage-sale-saturday.json` | planned |

All variant JSON files live under `variants/` at the project root.

---

## 4. Variant Definitions

### 4.1 Summer Block Party

> *The neighborhood is throwing a summer bash. Inflatable targets dot the lawns, kids on bikes zip through the cross-street, and the ice cream truck is fair game.*

| Aspect | Detail |
|--------|--------|
| **Variant ID** | `summer_block_party` |
| **Unlock condition** | Total score >= 25,000 |
| **Display name** | Summer Block Party |

#### Background Swaps

| Layer | Change |
|-------|--------|
| `sky.png` | Bright blue summer sky with scattered white clouds and a high sun |
| `ground.png` | Same road layout; lawns have party decorations (streamers, blankets). Sprinklers running on left lawn. |
| `buildings.png` | Same building geometry; buildings draped with bunting and banners. Open garage on right side. |

#### Target Reskin List

| Base Target | Summer Variant | Visual Description | Points |
|-------------|---------------|-------------------|:------:|
| `window_glass` | `summer_balloon_target` | Inflatable pool toy propped against window frame. Pops on hit. | 250 (unchanged) |
| `door_metal` | `summer_grill_target` | Charcoal grill parked in front of garage door. Sends up sparks on hit. | 100 (unchanged) |

#### Spawn Lane Modifications

| Lane | Base Target | Summer Target | Speed Change | Notes |
|------|-------------|--------------|:------------:|-------|
| `cross_street_traffic` | `vehicle` | `summer_bike_kid` | Same range (100--250) | Kids on bicycles replace cars. Smaller hitbox, same point value (300). |
| `deep_alley_approach` | `runner` | `summer_ice_cream_truck` | Slower (40--60) | Ice cream truck approaches from distance. Larger hitbox, 200 points. |
| `sky_drone_path` | `drone` | `drone` | Same (80--150) | Unchanged -- drones deliver party supplies. |

#### Additional Static Targets

| Target ID | Type | Position | Points | Description |
|-----------|------|----------|:------:|-------------|
| `summer_sprinkler` | bonus | Left lawn, Z-layer 1 | 150 | Lawn sprinkler. Hitting it makes it spray wildly (visual only). |

#### Ambient Audio

| Asset ID | Description |
|----------|-------------|
| `sfx_summer_ambient` | Summer party loop: crowd chatter, sizzling grill, kids laughing, sprinkler spray, distant music. |

#### Color Palette

| Element | Color |
|---------|-------|
| Sky tint | `#87CEEB` (light sky blue) |
| Ground tint | `#90EE90` (light green -- lush summer grass) |
| UI accent | `#FF6347` (tomato red -- party feel) |

---

### 4.2 Halloween Night

> *Night has fallen on the neighborhood. Jack-o-lanterns glow on porches, trick-or-treaters parade down the sidewalks, and bats swoop through the dark sky.*

| Aspect | Detail |
|--------|--------|
| **Variant ID** | `halloween_night` |
| **Unlock condition** | Total score >= 50,000 |
| **Display name** | Halloween Night |

#### Background Swaps

| Layer | Change |
|-------|--------|
| `sky.png` | Dark purple-black night sky with a large moon and wispy clouds. Stars visible. |
| `ground.png` | Same road layout; ground is darker (nighttime). Fallen leaves on lawns. Orange glow from porch jack-o-lanterns. |
| `buildings.png` | Same building geometry; windows glow yellow (interior lights). Cobwebs on fences. Spooky decorations on facades. |

#### Target Reskin List

| Base Target | Halloween Variant | Visual Description | Points |
|-------------|------------------|-------------------|:------:|
| `window_glass` | `halloween_pumpkin` | Carved jack-o-lantern sitting on windowsill. Smashes with a squelchy impact. | 250 (unchanged) |
| `door_metal` | `halloween_tombstone` | Foam tombstone decoration propped against garage. Cracks on hit. | 100 (unchanged) |

#### Spawn Lane Modifications

| Lane | Base Target | Halloween Target | Speed Change | Notes |
|------|-------------|-----------------|:------------:|-------|
| `cross_street_traffic` | `vehicle` | `halloween_trick_or_treaters` | Slower (80--150) | Group of costumed trick-or-treaters crossing the street. Medium hitbox. 300 points. |
| `deep_alley_approach` | `runner` | `halloween_ghost` | Same (50--80) | Translucent ghost figure floating toward camera. Same hitbox and points (350). |
| `sky_drone_path` | `drone` | `halloween_bat_swarm` | Faster (120--200) | Cluster of bats swooping across the sky. Smaller hitbox. 500 points. |

#### Ambient Audio

| Asset ID | Description |
|----------|-------------|
| `sfx_halloween_ambient` | Halloween night loop: eerie wind, distant owl hooting, creaking gate, faint spooky music. |

#### Special Impact Sounds

| Asset ID | Description | Trigger |
|----------|-------------|---------|
| `sfx_halloween_pumpkin_hit` | Wet, squelchy pumpkin smash sound. | Hitting a `halloween_pumpkin` target. |

#### Color Palette

| Element | Color |
|---------|-------|
| Sky tint | `#1A0A2E` (deep purple-black) |
| Ground tint | `#2D1F0E` (dark brown -- autumn night) |
| UI accent | `#FF8C00` (dark orange -- jack-o-lantern glow) |

---

### 4.3 Winter Holidays

> *Snow has blanketed the neighborhood. String lights twinkle on every roofline, snowmen stand in the yards, and a mail carrier trudges through with packages.*

| Aspect | Detail |
|--------|--------|
| **Variant ID** | `winter_holidays` |
| **Unlock condition** | Total score >= 75,000 |
| **Display name** | Winter Holidays |

#### Background Swaps

| Layer | Change |
|-------|--------|
| `sky.png` | Pale gray-white overcast sky with soft snowfall. Gentle gradient from white (horizon) to light gray (top). |
| `ground.png` | Same road layout; roads have snow on edges. Lawns covered in white snow. Tire tracks on the road. |
| `buildings.png` | Same building geometry; rooftops covered in snow. Multicolored string lights along rooflines and fences. Wreaths on doors. |

#### Target Reskin List

| Base Target | Winter Variant | Visual Description | Points |
|-------------|---------------|-------------------|:------:|
| `window_glass` | `winter_snowman` | Snowman with top hat and carrot nose in front yard. Explodes into snow cloud on hit. | 250 (unchanged) |
| `door_metal` | `winter_light_reindeer` | Light-up wire reindeer decoration on roof. Sparks and goes dark on hit. | 150 (+50 over base garage door) |

#### Spawn Lane Modifications

| Lane | Base Target | Winter Target | Speed Change | Notes |
|------|-------------|--------------|:------------:|-------|
| `cross_street_traffic` | `vehicle` | `winter_mail_truck` | Slower (60--120) | Mail carrier's truck loaded with packages. Larger hitbox. 300 points. |
| `deep_alley_approach` | `runner` | `winter_carolers` | Same (50--80) | Group of carolers walking toward camera. Larger hitbox. 350 points. |
| `sky_drone_path` | `drone` | `winter_santa_drone` | Same (80--150) | Drone with a tiny Santa hat. Same hitbox and points (500). Visual reskin only. |

#### Special Scoring Rules

| Rule | Description |
|------|-------------|
| **Rooftop reindeer bonus** | `winter_light_reindeer` targets are worth 150 points instead of the base garage door's 100, reflecting their elevated position and smaller hitbox. |

#### Ambient Audio

| Asset ID | Description |
|----------|-------------|
| `sfx_winter_ambient` | Winter holiday loop: gentle snowfall ambience, distant jingle bells, wind chimes, crackling fireplace from inside a house. |

#### Special Impact Sounds

| Asset ID | Description | Trigger |
|----------|-------------|---------|
| `sfx_winter_snowman_hit` | Soft "poof" of compacted snow bursting apart. | Hitting a `winter_snowman` target. |

#### Color Palette

| Element | Color |
|---------|-------|
| Sky tint | `#D3D3D3` (light gray -- overcast) |
| Ground tint | `#F0F0F0` (near-white -- snow-covered) |
| UI accent | `#C41E3A` (Christmas red) |

---

### 4.4 Rainy Day

> *Overcast skies and a steady drizzle transform the street. Umbrellas dot the sidewalks, puddles form on the road, and delivery vans splash through.*

| Aspect | Detail |
|--------|--------|
| **Variant ID** | `rainy_day` |
| **Unlock condition** | Total score >= 100,000 |
| **Display name** | Rainy Day |

#### Background Swaps

| Layer | Change |
|-------|--------|
| `sky.png` | Dark gray overcast sky with rain streaks. No sun visible. Dramatic cloud formations. |
| `ground.png` | Same road layout; puddles on the road surface (reflective patches). Grass looks darker and wet. Gutter streams visible along curbs. |
| `buildings.png` | Same building geometry; buildings look slightly darker (wet surfaces). Some windows show interior lights glowing through the rain. Awnings over some windows. |

#### Target Reskin List

| Base Target | Rainy Day Variant | Visual Description | Points |
|-------------|------------------|-------------------|:------:|
| `window_glass` | `rainy_window_glass` | Same window but with rain streaks running down the glass. Standard glass-break effect. | 250 (unchanged) |
| `door_metal` | `rainy_garage_door` | Garage door with water stain and dripping gutter above. Same metallic clang. | 100 (unchanged) |

#### Spawn Lane Modifications

| Lane | Base Target | Rainy Day Target | Speed Change | Notes |
|------|-------------|-----------------|:------------:|-------|
| `cross_street_traffic` | `vehicle` | `rainy_delivery_van` | Same (100--250) | Delivery vans with headlights on, splashing through puddles. Same hitbox and points (300). |
| `deep_alley_approach` | `runner` | `rainy_umbrella_walker` | Slower (40--60) | Pedestrian with umbrella walking cautiously. Umbrella makes hitbox slightly wider. 350 points. |
| `sky_drone_path` | `drone` | `drone` | Slower (60--100) | Same drone but moving slower (rain conditions). 500 points. |

#### Special Physics (Variant-Specific)

| Rule | Description |
|------|-------------|
| **Wet ground bounce** | Ground surface restitution is reduced from 0.3 to 0.15. Balls that hit the ground lose more energy and bounce lower. This is a subtle physics tweak that adds atmosphere without fundamentally changing gameplay. |

> **Note:** This is the only variant that modifies a physics parameter. The change is small (restitution only, not gravity or drag) and is encoded in the variant's `static_colliders` block as a different restitution value for the ground plane.

#### Ambient Audio

| Asset ID | Description |
|----------|-------------|
| `sfx_rainy_ambient` | Rainy day loop: steady rain patter, occasional distant thunder, car tire splashes, gutter drip. |

#### Special Impact Sounds

| Asset ID | Description | Trigger |
|----------|-------------|---------|
| `sfx_rainy_splash` | Wet splat layered on top of base impact sounds. Plays in addition to the standard impact. | Any target hit (layered). |

#### Color Palette

| Element | Color |
|---------|-------|
| Sky tint | `#708090` (slate gray) |
| Ground tint | `#4A4A4A` (dark gray -- wet asphalt) |
| UI accent | `#4682B4` (steel blue) |

---

### 4.5 Garage Sale Saturday

> *It is Saturday morning and the neighbors are clearing out their garages. Folding tables line the driveways, loaded with breakable junk. Browsers mill around on the sidewalks.*

| Aspect | Detail |
|--------|--------|
| **Variant ID** | `garage_sale` |
| **Unlock condition** | Total score >= 150,000 |
| **Display name** | Garage Sale Saturday |

#### Background Swaps

| Layer | Change |
|-------|--------|
| `sky.png` | Clear morning sky with a warm golden-hour tint. Sun low on the horizon. |
| `ground.png` | Same road layout; driveways have folding tables with items. Hand-painted "YARD SALE" signs stuck in the lawn. |
| `buildings.png` | Same building geometry; garage doors are open, revealing cluttered interiors. Tables and boxes visible in front of both houses. |

#### Target Reskin List

| Base Target | Garage Sale Variant | Visual Description | Points |
|-------------|-------------------|-------------------|:------:|
| `window_glass` | `garage_vase` | Ceramic vase on a folding table. Shatters with a ceramic crash. | 250 (unchanged) |
| `door_metal` | `garage_old_tv` | Old CRT television on a table. Screen cracks with a satisfying crunch. | 150 (+50 over base) |

#### Additional Static Targets

Garage Sale adds extra static targets on the folding tables, increasing the target density for this variant.

| Target ID | Type | Position (approx.) | Points | Z-Layer | Description |
|-----------|------|-------------------|:------:|:-------:|-------------|
| `garage_lamp` | breakable_junk | Left driveway table | 100 | 2 | Old table lamp. Bulb pops on hit. |
| `garage_fishbowl` | breakable_junk | Right driveway table | 200 | 2 | Glass fishbowl (no fish). Glass-break effect. |
| `garage_record_stack` | breakable_junk | Left driveway table | 150 | 2 | Stack of vinyl records. Scatter on hit. |

#### Spawn Lane Modifications

| Lane | Base Target | Garage Sale Target | Speed Change | Notes |
|------|-------------|-------------------|:------------:|-------|
| `cross_street_traffic` | `vehicle` | `garage_pickup_truck` | Slower (60--120) | Pickup trucks arriving to browse. Same hitbox. 300 points. |
| `deep_alley_approach` | `runner` | `garage_browser` | Same (50--80) | Casual browsers walking down the street examining items. 350 points. |
| `sky_drone_path` | `drone` | `drone` | Same (80--150) | Unchanged. |

#### Special Scoring Rules

| Rule | Description |
|------|-------------|
| **Junk table sweep** | Hitting all three additional junk table targets (`garage_lamp`, `garage_fishbowl`, `garage_record_stack`) in a single streak awards a 500-point "Clean Sweep" bonus on top of normal scoring. Resets when targets respawn. |

#### Ambient Audio

| Asset ID | Description |
|----------|-------------|
| `sfx_garage_ambient` | Saturday morning loop: birds chirping, distant lawn mower, portable radio playing oldies, casual conversation murmur. |

#### Special Impact Sounds

| Asset ID | Description | Trigger |
|----------|-------------|---------|
| `sfx_garage_ceramic_break` | Ceramic shatter -- heavier and duller than glass. | Hitting `garage_vase` or `garage_fishbowl`. |
| `sfx_garage_tv_crack` | CRT screen crack with electrical pop. | Hitting `garage_old_tv`. |
| `sfx_garage_record_scatter` | Vinyl records sliding and clattering. | Hitting `garage_record_stack`. |

#### Color Palette

| Element | Color |
|---------|-------|
| Sky tint | `#FFE4B5` (moccasin -- warm morning light) |
| Ground tint | `#C2B280` (sand -- sunlit concrete) |
| UI accent | `#DAA520` (goldenrod -- yard sale vibe) |

---

## 5. Unlock Progression

Variants unlock based on **cumulative total score** across all play sessions (GDD Section 6: "Unlocking Variants"). This mirrors the cosmetic unlock model (see `cosmetic-unlock-spec.md`) and uses the same `CareerStats.totalScore` field.

| Unlock Order | Variant | Threshold | Approx. Play Time |
|:------------:|---------|:---------:|:-----------------:|
| 1 | Suburban Crossroads | Always unlocked | Immediate |
| 2 | Summer Block Party | 25,000 | 1--2 hours |
| 3 | Halloween Night | 50,000 | 3--5 hours |
| 4 | Winter Holidays | 75,000 | 5--8 hours |
| 5 | Rainy Day | 100,000 | 8--12 hours |
| 6 | Garage Sale Saturday | 150,000 | 12--20 hours |

### Progression Design Intent

- **Gradual disclosure**: Each variant introduces new visual themes and, in some cases, scoring twists (junk table sweep, rooftop reindeer bonus, wet ground bounce). Spacing unlocks across the progression curve ensures the player always has something new ahead.
- **No skill gate**: Unlock thresholds use `totalScore`, which only increases. Even a player who misses frequently will eventually unlock every variant through sustained play.
- **Endgame reward**: Garage Sale Saturday at 150,000 is the final variant -- a reward for dedicated long-term players.

### Unlock Persistence

Variant unlock state is stored in `ProfileData.variants.unlockedVariants` (a `Set<String>`). When `CareerStats.totalScore` crosses a threshold:

1. Add the variant ID to `unlockedVariants`.
2. Save `ProfileData` immediately (unlock event trigger per `save-and-persistence.md` Section 6).
3. Display an unlock notification toast (same mechanism as cosmetic unlocks).

The selected variant is stored in `ProfileData.variants.selectedVariant` and is changed via the variant selection overlay (see `menu-and-navigation-flow.md` Section 3).

---

## 6. Asset Requirements Per Variant

### Summary Table

| Variant | Background Layers | Target Reskins | New Targets | Spawn Reskins | Ambient Audio | Special Sounds | Total New Assets |
|---------|:-----------------:|:--------------:|:-----------:|:-------------:|:-------------:|:--------------:|:----------------:|
| Summer Block Party | 3 | 2 | 1 | 2 | 1 | 0 | 9 |
| Halloween Night | 3 | 2 | 0 | 3 | 1 | 1 | 10 |
| Winter Holidays | 3 | 2 | 0 | 3 | 1 | 1 | 10 |
| Rainy Day | 3 | 2 | 0 | 2 | 1 | 1 | 9 |
| Garage Sale Saturday | 3 | 2 | 3 | 2 | 1 | 3 | 14 |
| **Totals** | **15** | **10** | **4** | **12** | **5** | **6** | **52** |

### Directory Structure

```
variants/
  summer-block-party.json
  halloween-night.json
  winter-holidays.json
  rainy-day.json
  garage-sale-saturday.json

assets/
  backgrounds/
    summer/
      sky.png
      ground.png
      buildings.png
    halloween/
      sky.png
      ground.png
      buildings.png
    winter/
      sky.png
      ground.png
      buildings.png
    rainy/
      sky.png
      ground.png
      buildings.png
    garage-sale/
      sky.png
      ground.png
      buildings.png
  sprites/
    variants/
      summer/
        balloon_target.svg
        grill_target.svg
        bike_kid.svg
        ice_cream_truck.svg
        sprinkler.svg
      halloween/
        pumpkin.svg
        tombstone.svg
        trick_or_treaters.svg
        ghost.svg
        bat_swarm.svg
      winter/
        snowman.svg
        light_reindeer.svg
        mail_truck.svg
        carolers.svg
        santa_drone.svg
      rainy/
        delivery_van.svg
        umbrella_walker.svg
      garage-sale/
        vase.svg
        old_tv.svg
        pickup_truck.svg
        browser.svg
        lamp.svg
        fishbowl.svg
        record_stack.svg
  sounds/
    variants/
      summer/
        ambient_summer.ogg
      halloween/
        ambient_halloween.ogg
        pumpkin_hit.ogg
      winter/
        ambient_winter.ogg
        snowman_hit.ogg
      rainy/
        ambient_rainy.ogg
        splash.ogg
      garage-sale/
        ambient_garage_sale.ogg
        ceramic_break.ogg
        tv_crack.ogg
        record_scatter.ogg
```

---

## 7. Implementation Considerations

### 7.1 Level Loader Extension

The existing level loader (which reads `suburban-crossroads.json`) needs to be extended to:

1. Accept a variant ID parameter.
2. Look up the corresponding JSON file from the variant file map (Section 3).
3. Parse the extended schema (Section 2) including `variant_meta`.
4. Apply target reskins by mapping base target types to variant sprite paths.
5. Load variant background layers instead of the base layers.
6. Initialize variant ambient audio.

### 7.2 Memory Management

Each variant's assets (backgrounds, reskinned sprites, ambient audio) should be loaded on variant selection and disposed when switching variants. Since only one variant is active at a time, memory usage stays bounded.

### 7.3 Variant Selection Flow

```
1. Player selects variant in Variant Selection Overlay
2. selectedVariant is updated in ProfileData and saved
3. On next "TAP TO PLAY" (MAIN_MENU -> READY):
   a. Dispose current variant assets
   b. Load selected variant's JSON and assets
   c. Initialize level with variant data
4. Gameplay proceeds with variant-specific visuals and audio
```

### 7.4 Testing Strategy

Each variant should be tested against this checklist:

- [ ] All base collider geometry is preserved (ball physics unchanged)
- [ ] All target reskins display correctly at their specified positions
- [ ] Moving targets spawn at correct lane positions with correct speeds
- [ ] Ambient audio loops seamlessly
- [ ] Special impact sounds play on the correct target types
- [ ] Variant-specific scoring rules work correctly (if any)
- [ ] Background layers render in correct order with correct transparency
- [ ] Cosmetic items (ball skins, trails, impact effects) render correctly on top of variant visuals
- [ ] Variant unlock notification displays when threshold is reached
- [ ] Memory is properly disposed when switching between variants

---

## Companion Documents

| Document | Relevance |
|----------|-----------|
| `game-design-document.md` Section 6 | Design intent for seasonal variants -- the source for all variant concepts |
| `suburban-crossroads.json` | Base level data that all variants extend |
| `environment-z-depth-and-collosion.md` | Z-layer architecture that governs target placement in all variants |
| `save-and-persistence.md` Section 3 | `VariantState` data model and persistence structure |
| `menu-and-navigation-flow.md` Section 3 | Variant selection overlay layout and navigation |
| `audio-spec.md` Section 6 | Per-variant audio changes and naming conventions |
| `asset-registry.md` | Cross-references all variant assets with production status |
| `cosmetic-unlock-spec.md` | Cosmetics work identically across all variants |
