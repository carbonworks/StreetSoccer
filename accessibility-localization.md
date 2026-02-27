# Street Soccer -- Accessibility & Localization

This document defines the accessibility considerations and localization approach for Street Soccer. It covers colorblind support, touch target sizing, font scaling, text localization, and screen reader compatibility. These guidelines apply to all current and future UI elements.

---

## 1. Colorblind Considerations

### Problem Areas

Street Soccer uses color to communicate gameplay state in several places. The following elements rely on color distinction and require alternative cues for players with color vision deficiencies.

#### Big Bomb Color Ramp

| Element | Current Design | Issue | Affected Conditions |
|---------|---------------|-------|-------------------|
| **Big Bomb depth feedback** | Progressive red overlay on ball sprite (alpha ramp from no color to full red as ball travels deeper) | Red tint is the sole depth indicator. Players with deuteranopia (red-green deficiency, ~8% of males) or protanopia (red deficiency, ~1% of males) may not perceive the color shift | Deuteranopia, protanopia |

**Recommended alternatives (implement at least one):**

| Alternative | Description | Implementation Effort |
|------------|-------------|----------------------|
| **Pulsing outline** | A white or yellow pulsing outline on the ball that increases in pulse frequency as depth increases. Frequency ramp: 0.5 Hz at start depth, 3 Hz at max depth | Low -- add an oscillating outline pass in RenderSystem |
| **Particle trail** | An expanding particle trail behind the ball that grows denser/longer with depth. Trail color uses yellow-to-white ramp (visible to all common deficiencies) | Medium -- requires particle system or trail sprite rendering |
| **Size pulsation** | Ball sprite scale oscillates with increasing amplitude as depth increases. Scale range: 1.0x at entry, 0.9x-1.1x pulse at max depth | Low -- modify the existing scale factor in RenderSystem |
| **Screen shake** | Subtle camera/viewport shake that intensifies with depth | Low -- offset the viewport projection matrix by a small oscillating value |

**Recommendation:** Implement the **pulsing outline** as the primary colorblind alternative. It is low-effort, clearly visible regardless of color vision, and does not require additional art assets. Enable it alongside the red tint (not as a replacement) so both cues are available simultaneously.

#### Steer Budget Meter

| Element | Current Design | Issue | Affected Conditions |
|---------|---------------|-------|-------------------|
| **Steer meter segments** | Green (full) -> yellow (reduced) -> orange (weak) -> dim red (residual) | Green-to-red gradient is the classic problematic spectrum for deuteranopia and protanopia | Deuteranopia, protanopia |

**Recommended alternative:** Use a **brightness ramp** instead of (or in addition to) a hue ramp. Full segments are bright white, reduced segments are medium gray, weak segments are dark gray, residual segments are near-black. The brightness distinction is perceivable by all color vision types. Additionally, each segment can use a distinct **fill pattern** (solid, diagonal lines, dots, hollow) for a shape-based cue.

#### Streak Badge Color Tiers

| Element | Current Design | Issue | Affected Conditions |
|---------|---------------|-------|-------------------|
| **Streak badge** | Gray -> white -> yellow -> orange -> red/gold at streak levels 1 through 5+ | Yellow/orange/red transitions may not be distinguishable | Deuteranopia, protanopia, tritanopia (rare) |

**Recommended alternative:** Pair color with **size and animation intensity**. The badge already scales and pulses at higher streaks -- ensure the scale increase is sufficient to communicate streak level without relying on color alone. Add a numeric display (already present as the multiplier text "x3") as the primary indicator.

#### Score Popups

| Element | Current Design | Issue | Affected Conditions |
|---------|---------------|-------|-------------------|
| **Score popup style** | White for x1 hits, gold/yellow for multiplied hits | Color alone distinguishes base from multiplied hits | Deuteranopia (gold vs white may be difficult) |

**Recommended alternative:** Multiplied score popups already use a **larger font size**. Ensure the size difference is prominent enough to serve as the primary distinguishing cue. Additionally, prepend a label for multiplied hits: display "x3 750" rather than relying on color to communicate the multiplier.

### Implementation Approach

**Phase 1 (alpha):** Ensure all color-coded elements have at least one non-color alternative cue (size, text label, animation, pattern). No settings toggle required -- the alternatives are always active alongside color.

**Phase 2 (beta):** Add a "High Contrast" toggle in Settings that activates enhanced alternatives: pulsing outlines on Big Bomb, brightness-only steer meter, and text labels on all popups. This is a single toggle, not a per-deficiency configuration -- simplicity for the player matters more than clinical precision.

---

## 2. Touch Target Sizing

### Android Material Design Guidelines

All interactive touch targets must meet the **minimum 48 dp** size requirement per Android Material Design guidelines. At the reference resolution of 1920x1080 on a typical phone (~420 dpi, xxhdpi), 48 dp = approximately 96-144 px depending on exact density.

### Current HUD Element Audit

| Element | Current Size | Meets 48 dp? | Notes |
|---------|-------------|:------------:|-------|
| **Pause button** | 64x64 px touch target | Yes (at xhdpi+) | At xhdpi (320 dpi), 64 px = 32 dp -- below minimum. Increase to 96 px minimum |
| **Bomb mode button** | ~80x80 px | Yes | Adequate at all densities |
| **Angle slider rail** | 80 px wide touch zone | Yes | Touch zone is generous; the visual thumb may be narrower but the hit area is correct |
| **Angle slider thumb** | ~40x40 px visual, within 80 px wide touch zone | Yes | Touch target is the rail zone, not the thumb visual |
| **Settings gear icon** | ~48x48 px (attract screen) | Borderline | Increase touch target to 64x64 px minimum; icon can remain visually smaller |
| **Stats icon** | ~48x48 px (attract screen) | Borderline | Same as settings -- increase touch target |
| **Pause overlay buttons** (Resume, Quit) | ~200x60 px | Yes | Large buttons, well within guidelines |
| **Settings overlay toggles** | ~full width x 48 px | Yes | Standard list-item height |
| **Tip banner dismiss** | Full banner width x ~70 px | Yes | The entire banner is the tap target |

### Recommendations

1. **Pause button**: Increase touch target to 96x96 px (the visual icon can remain at 48-64 px). The extra hit area makes pause easily tappable during frantic gameplay
2. **Attract screen icons**: Increase touch target to at least 72x72 px. Add a transparent padding region around each icon that participates in hit detection
3. **General rule**: All new interactive elements must have a touch target of at least 96 px (48 dp at xxhdpi) in both width and height. The visual element can be smaller; the touch region is what matters

### Touch Target Implementation Pattern

```kotlin
// Visual icon is 48x48, but touch target extends to 96x96
val pauseButton = ImageButton(pauseDrawable).apply {
    // Pad the actor so the touchable area extends beyond the visual
    pad(24f)  // 24 px padding on each side: 48 + 24 + 24 = 96 px total
}
```

---

## 3. Font Sizing & Display Density

### Android System Font Scale

Android allows users to set a system-wide font scale (Settings -> Display -> Font size) ranging from 0.85x to 1.3x (or higher with accessibility settings). Street Soccer should respect this setting for all user-readable text.

### BitmapFont Scaling Strategy

LibGDX uses `BitmapFont` which is rasterized at a fixed size. Scaling approaches:

| Approach | Pros | Cons | Recommendation |
|----------|------|------|----------------|
| **Generate multiple font sizes** | Crisp text at all scales | Increases asset size; must detect density at load time | Use for alpha -- generate 3 sizes (small, medium, large) and select based on display density |
| **FreeType font generation at runtime** | Perfect scaling at any size; respects system font scale | Requires gdx-freetype extension; adds ~500 KB to APK | Use for beta -- cleanest solution |
| **Bitmap scaling** | Simple | Blurry text at non-native scales | Not recommended |

### Recommended Font Sizes

| Text Element | Base Size (at xhdpi/320 dpi) | Scale With | Notes |
|-------------|------------------------------|-----------|-------|
| **Session score** | 48 sp | Display density | Large, bold, always readable |
| **Streak multiplier** | 32 sp | Display density | Prominent but secondary to score |
| **Score popups** | 28 sp (base), 36 sp (multiplied) | Display density | Float-up text must be readable during brief display |
| **Angle label** | 18 sp | Display density | Small label near slider thumb |
| **Tip banner text** | 24 sp | Display density + system font scale | Instructional text should honor accessibility scale |
| **Pause overlay buttons** | 28 sp | Display density | Button text in overlays |
| **Settings labels** | 22 sp | Display density + system font scale | Settings text should honor accessibility scale |
| **Stats values** | 22 sp | Display density | Stats overlay content |

### Density Detection

At load time, determine the display density and select the appropriate font asset set:

```kotlin
val density = Gdx.graphics.density  // Returns a float: 1.0 = mdpi, 2.0 = xhdpi, 3.0 = xxhdpi
val fontScale = when {
    density <= 1.5f -> "mdpi"     // 160-240 dpi
    density <= 2.5f -> "xhdpi"    // 320 dpi
    else -> "xxhdpi"              // 480+ dpi
}
```

### System Font Scale Integration

For text that should respect the Android system font scale (settings, tips, instructional text):

```kotlin
// Read Android system font scale
val systemFontScale = Gdx.app.getPreferences("system").getFloat("fontScale", 1.0f)
// Or via Android API: context.resources.configuration.fontScale

// Apply to label style
label.setFontScale(baseScale * systemFontScale)
```

**Gameplay HUD text** (score, streak, popups) should NOT scale with system font -- these are tuned for gameplay readability at specific sizes. **Menu/settings/tips text** SHOULD scale with system font.

---

## 4. Text Localization

### Approach: String Table (Key-Value Map)

All user-visible text is extracted to a string table loaded from JSON. Hardcoded strings in Kotlin code are replaced with key lookups.

### String Table Format

```json
{
  "locale": "en",
  "strings": {
    "hud.score": "Score",
    "hud.streak": "Streak",
    "hud.pause": "Pause",

    "menu.tap_to_play": "TAP TO PLAY",
    "menu.settings": "Settings",
    "menu.stats": "Stats",
    "menu.resume": "Resume",
    "menu.quit": "Quit",

    "settings.title": "Settings",
    "settings.trajectory_preview": "Trajectory Preview",
    "settings.slider_side": "Slider Side",
    "settings.slider_left": "Left",
    "settings.slider_right": "Right",
    "settings.music_volume": "Music Volume",
    "settings.sfx_volume": "SFX Volume",
    "settings.debug_panel": "Debug Panel",

    "stats.title": "Stats",
    "stats.career_score": "Career Score",
    "stats.best_streak": "Best Streak",
    "stats.best_session": "Best Session Score",
    "stats.total_kicks": "Total Kicks",
    "stats.total_hits": "Total Hits",
    "stats.hit_rate": "Hit Rate",

    "tips.trajectory_preview": "Enable **Trajectory Preview** in Settings to see your ball's predicted path.",
    "tips.steer_swipe": "Swipe while the ball is in the air to add **spin** and curve your shot!",
    "tips.streak_multiplier": "Hit targets in a row to build a **streak multiplier** -- up to x3!",
    "tips.big_bomb": "Aim a powerful flick straight up to send a **Big Bomb** down the central corridor for bonus points.",

    "feedback.nice": "Nice!",
    "feedback.streak_broken": "Streak broken"
  }
}
```

### File Location

```
assets/i18n/strings_en.json    (English -- default)
assets/i18n/strings_es.json    (Spanish -- future)
assets/i18n/strings_pt.json    (Portuguese -- future)
assets/i18n/strings_de.json    (German -- future)
```

### StringTable Implementation

```kotlin
object StringTable {
    private var strings: Map<String, String> = emptyMap()

    fun load(locale: String = "en") {
        val file = Gdx.files.internal("i18n/strings_$locale.json")
        if (file.exists()) {
            val data = Json.decodeFromString<StringTableData>(file.readString())
            strings = data.strings
        }
    }

    fun get(key: String): String {
        return strings[key] ?: key  // Fall back to key name if string is missing
    }

    fun get(key: String, vararg args: Any): String {
        val template = strings[key] ?: key
        return String.format(template, *args)
    }
}
```

### Usage in Code

```kotlin
// Before (hardcoded)
label.setText("TAP TO PLAY")

// After (localized)
label.setText(StringTable.get("menu.tap_to_play"))

// With parameters
label.setText(StringTable.get("stats.total_kicks_value", careerStats.totalKicks))
```

### Locale Detection

```kotlin
val deviceLocale = java.util.Locale.getDefault().language  // "en", "es", "pt", etc.
StringTable.load(deviceLocale)
```

If no string file exists for the device locale, fall back to English (`strings_en.json`).

### Language Support Scope

| Phase | Languages | Notes |
|-------|-----------|-------|
| **Alpha** | English only | All strings extracted to `strings_en.json`. No other locales shipped |
| **Beta** | English + 2-3 major languages | Spanish, Portuguese, German (high Android market share) |
| **Release** | English + 5-8 languages | Add French, Italian, Japanese, Korean based on market analysis |

### Text Direction

| Phase | Support | Notes |
|-------|---------|-------|
| **Alpha/Beta** | LTR only | English and target European/Latin American languages are all left-to-right |
| **Future** | RTL consideration | Arabic and Hebrew would require layout mirroring (HUD positions, slider rail, text alignment). This is a significant effort and should only be undertaken if the player base warrants it. LibGDX does not have built-in RTL support; mirroring would require custom layout logic |

### Strings That Are NOT Localized

| String Type | Reason |
|------------|--------|
| **Score numbers** (4250, 750) | Universal numeric format; no localization needed |
| **Multiplier display** (x1, x1.5, x3) | Mathematical notation is universal |
| **Angle display** (42 deg) | Numeric with universal degree symbol |
| **Asset IDs** (classic_white, glass_break) | Internal identifiers, never shown to the player |
| **Debug panel labels** | Developer tool, English only |

---

## 5. Screen Reader Compatibility

### Gameplay Scope

Street Soccer is a real-time action game. The core gameplay loop (aiming, kicking, steering) is inherently visual and cannot be meaningfully driven by a screen reader. No attempt is made to provide screen reader narration for in-flight gameplay.

### Menu & Settings Accessibility

Menus, settings, and stats overlays are static UI screens where screen reader support is feasible and valuable. All interactive elements in these screens should have **content descriptions**.

| Screen | Element | Content Description |
|--------|---------|-------------------|
| **Attract screen** | Tap-to-play zone | "Play game. Double-tap to start." |
| **Attract screen** | Settings icon | "Settings. Double-tap to open settings." |
| **Attract screen** | Stats icon | "Statistics. Double-tap to view career stats." |
| **Settings overlay** | Trajectory preview toggle | "Trajectory preview, currently [on/off]. Double-tap to toggle." |
| **Settings overlay** | Slider side toggle | "Slider side, currently [left/right]. Double-tap to toggle." |
| **Settings overlay** | Music volume slider | "Music volume, [value]%. Drag to adjust." |
| **Settings overlay** | SFX volume slider | "Sound effects volume, [value]%. Drag to adjust." |
| **Settings overlay** | Debug panel toggle | "Debug panel, currently [on/off]. Double-tap to toggle." |
| **Pause overlay** | Resume button | "Resume game. Double-tap to resume." |
| **Pause overlay** | Quit button | "Quit to menu. Double-tap to quit." |
| **Stats overlay** | Career score | "Career score: [value]." |
| **Stats overlay** | Best streak | "Best streak: [value] consecutive hits." |
| **Stats overlay** | Total kicks | "Total kicks: [value]." |

### TalkBack Integration

LibGDX's Scene2D UI does not natively integrate with Android's TalkBack service. To enable TalkBack for overlays:

1. **Android View overlay**: Render menu/settings as a transparent Android `View` overlay on top of the LibGDX `GLSurfaceView`. Android native views support TalkBack natively
2. **AccessibilityDelegate**: Attach accessibility delegates to Scene2D actors that bridge to the Android accessibility framework

**Recommendation:** Defer TalkBack integration to beta. For alpha, ensure all buttons have visible text labels (not icon-only) so sighted players with motor impairments can identify controls. This is the most impactful accessibility improvement with zero technical complexity.

---

## 6. Summary of Recommendations by Priority

| Priority | Area | Recommendation |
|----------|------|----------------|
| P0 | Touch targets | Increase all interactive elements to 96 px minimum (48 dp at xxhdpi) |
| P0 | Color alternatives | Ensure every color-coded element has a non-color cue (size, text, animation) |
| P1 | String extraction | Extract all user-visible strings to string table; ship `strings_en.json` |
| P1 | Font density | Generate font assets at 3 density tiers; select at load time |
| P1 | Score popup labels | Add text multiplier label (e.g., "x3") alongside color change |
| P2 | Steer meter patterns | Add brightness/pattern cues to supplement color gradient |
| P2 | Big Bomb outline | Add pulsing outline as colorblind-friendly depth indicator |
| P2 | System font scale | Honor Android system font scale for menu/settings text |
| P3 | High Contrast setting | Add a settings toggle for enhanced colorblind alternatives |
| P3 | TalkBack | Add content descriptions to menu/settings elements |
| P3 | RTL text support | Layout mirroring for Arabic/Hebrew (future, if market warrants) |

---

## Companion Documents

| Document | Relevance |
|----------|-----------|
| `game-design-document.md` Section 9 | Visual feedback elements that use color coding |
| `ui-hud-layout.md` | HUD element positions and sizes to audit for touch targets |
| `save-and-persistence.md` Section 3 | `SettingsData` where accessibility toggles would be stored |
| `tips-system-spec.md` | Tip text that must be localized |
| `menu-and-navigation-flow.md` | Menu screens where screen reader support applies |
