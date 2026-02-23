This technical specification defines how your flat 2D background image is translated into a **2.5D Physics and Rendering Environment** for LibGDX.

By hardcoding these layers and zones, your AI coding assistants (Claude/Gemini) will know exactly where to spawn targets, how to scale the ball, and where collisions should occur.

---

# Environment Z-Depth & Collision Specification

**Level Name:** Suburban Crossroads (Base Environment)
**Perspective:** 1-Point Perspective (Center Vanishing Point)
**Resolution Target:** 1920x1080 (16:9 Aspect Ratio Base)

## 1. The Z-Layer Architecture (Rendering Order)

To create the illusion of depth, the game engine will sort sprites and dynamic objects into distinct Z-layers. `Z=0` is closest to the camera.

| Layer Index | Name | Visual Elements (from image) | Purpose |
| --- | --- | --- | --- |
| **Layer 0** | **Launch Zone** | Bottom 20% of screen: Manhole cover, curved foreground sidewalks, lower asphalt. | The player's anchor point. No targets spawn here. |
| **Layer 1** | **Cross-Street** | Horizontal road cutting left to right across the mid-foreground. | Spawning lane for fast-moving horizontal targets (cars, bikers, dogs). |
| **Layer 2** | **Primary Properties** | Large tan house (Left), Large beige house (Right), white fences, front lawns. | High-value, massive target zones. Walls block horizontal ball travel. |
| **Layer 3** | **Deep Neighborhood** | The vertical road extending to the center vanishing point and the distant townhomes lining it. | The "Big Bomb" corridor. Infinite depth for long-distance skill shots. |
| **Layer 4** | **Skybox** | The solid blue upper ~45% of the image. | Background. Air-target spawn zone (drones, birds, rogue balloons). |

---

## 2. Collision Mapping (The Box2D World)

In a stationary 2.5D game, the physics engine actually calculates everything on a top-down 2D plane. We map the visual background to these physical zones:

### A. The "Floor" (Ground Zones)

These are areas where the ball's *shadow* is rendered. The shadow is always projected onto the ground plane at the ball's (x, y) position, scaled by the depth formula from Section 3, with opacity decreasing as the ball's height increases (see `physics-and-tuning.md` Section 5). If the visual ball's height drops to zero while over these zones, a "bounce" physics event occurs.

* **The Asphalt:** Covers the center inverted "T" shape (foreground curve, horizontal cross-street, and the deep center road).
* **The Lawns/Sidewalks:** Flanking the asphalt. Bounces here might have a lower restitution (less bouncy) than the street.

### B. The "Walls" (Vertical Colliders)

These are the static Box2D bodies. When the ball's shadow intersects these coordinates, the engine checks the ball's  height to see *where* it hit the building (e.g., ground floor vs. 2nd-story window).

* **Left Townhome Facade:** A massive polygon collider covering the front and right-facing side of the left building.
* **Right Townhome Facade:** A polygon collider covering the front and left-facing side of the right building.
* **White Fences:** Low-height colliders. If the ball's height is  when passing over it, it travels cleanly. If , it bounces off.

---

## 3. Depth & Scaling Mathematics

To make the ball look like it is traveling into the image, we must scale it based on its  position on the screen (since moving "up" the screen equals moving "away" from the camera).

Assuming the horizon line (vanishing point) is at exactly **50% of the screen height ( on a 1080p screen)**:

* **Launch Scale:** At , the ball scale is 1.0 (100%).
* **Horizon Scale:** At , the ball scale approaches 0.0 (0%).

**The Scaling Formula:**


*Note: We cap the minimum scale at 0.05 so the ball never completely vanishes until it passes behind a visual layer.*

---

## 4. Spawning Coordinates (X, Y Anchors)

When you ask the AI to spawn a "Duck" (target), use these reference guidelines based on a standard 1920x1080 grid (Origin 0,0 at bottom left):

* **Sidewalk Pedestrians:** Spawn at , moving horizontally from  to .
* **Left House Windows:** Center coordinates are roughly  and .
* **Right House Windows:** Center coordinates are roughly  and .
* **Deep Alley Runners:** Spawn at , moving directly "down" the screen toward the player (scaling up as they approach).

---

## Companion Documents

- **`physics-and-tuning.md`** — Restates the coordinate system (Section 1) and defines ball shadow rendering (Section 5) that uses the depth scaling formula from Section 3 of this document.
- **`suburban-crossroads.json`** — Level data containing the concrete collider geometry, target sensor coordinates, and spawn lane definitions for the Suburban Crossroads environment.
- **`technical-architecture.md`** — Rendering pipeline (Section 8) implements the Z-layer sort order defined here. Box2D integration (Section 5) builds static bodies from the collision mapping.
- **`game-design-document.md`** — Design intent for the 2.5D play field and target placement.
