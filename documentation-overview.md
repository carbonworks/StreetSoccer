To move from a solid architecture to a **shippable game**, you need documents that act as "connective tissue." Since you are working with AI, these docs aren't just for you—they are for the AI to ensure it doesn't "hallucinate" or drift away from your original vision.

Here are the essential technical and non-technical documents you need to build next:

---

### 1. The Game Design Document (GDD) – "The North Star"

This is a non-technical doc that describes **what it feels like to play.** Without this, the AI will suggest generic mechanics that might not fit your specific "Big Bomb" vibe.

* **Core Loop:** Kick ball  Watch travel  Hit target  Get points/Damage  Repeat.
* **The "Feel":** Is it a high-speed arcade game or a "Zen" physics puzzler?
* **Scoring Logic:** How do "Big Bombs" differ from "Precision Chips" in terms of rewards?
* **Level Progression:** How does the suburban layout change as the player gets better? (e.g., more obstacles, tighter alleys).

### 2. The Technical Specifications (The "Blueprints")

These docs define the math and logic rules so the code remains consistent across different screens.

* **Physics & Coordinate Spec:** Since you're doing 2.5D, you need a standard for how "Depth" () affects "Size" () and "Screen Position" ().
> **The Formula:** 
> This tells the AI: "As an object moves into the background ( increases), shrink it by this specific ratio."


* **State Machine Diagram:** A map of game flow: `BOOT`  `MAIN_MENU`  `LEVEL_LOAD`  `PLAYING`  `PAUSED`  `GOAL_ANIMATION`  `SCORE_SUMMARY`.
* **Asset Pipeline Doc:** The "Recipe" for your art.
*Example:* `Recraft (Vector Art) -> Claude (SVG Optimization) -> Android Studio (AmanithSVG Rendering)`.

### 3. The "Asset Registry" (Non-Technical)

As you generate dozens of houses and ball types, things will get messy. You need a simple spreadsheet or Markdown table to track your "Game World Inventory."

| Asset ID | Type | Source | Style Notes |
| --- | --- | --- | --- |
| `house_blue_01` | Target | Recraft | 2-story, blue siding, slate roof. |
| `ball_football_01` | Player | Claude (SVG) | High-contrast orange, white laces. |
| `ground_sidewalk` | Environment | Recraft | Tiled, grey, 2.5D perspective edge. |

### 4. The AI "Style Guide" (The Context Bible)

This is arguably the most important doc for *your* specific workflow. It’s a set of "Golden Prompts" and constraints you give to Gemini and Claude every time you start a new session.

* **Coding Standards:** "Always use KTX DSLs for UI," "Keep functions under 20 lines," "No hardcoded magic numbers for physics."
* **Visual Style Tokens:** A list of hex codes and geometric rules (e.g., "Corner radius: 4px," "Shadow opacity: 20%").

---

### 5. The "Release & Testing" Plan

* **Performance Budget:** Since you're using SVGs, how many can be on screen before the frame rate drops on an older Android phone?
* **Feedback Loop:** How will you test the "kick feel"? (e.g., a "Debug Overlay" that shows the ball’s flight path and velocity in real-time).

---

### Summary Checklist

| Document | Category | AI Utility |
| --- | --- | --- |
| **GDD** | Non-Technical | Prevents "feature creep" and generic AI suggestions. |
| **State Diagram** | Technical | Helps Claude write the `Screen` switching logic without bugs. |
| **Physics Spec** | Technical | Ensures "Big Bombs" behave predictably across all levels. |
| **Asset Registry** | Management | Keeps your file naming clean and organized. |
| **Style Guide** | AI Context | Ensures new code and art match the existing "Flat 2.0" look. |

---

### Your Next Step

Before we write more code, **would you like me to draft a 1-page "Game Design Document" (GDD)** that specifically defines the "Big Bomb" mechanics and the scoring system for your suburban layout? This will give us a "Source of Truth" to feed into Gemini.
