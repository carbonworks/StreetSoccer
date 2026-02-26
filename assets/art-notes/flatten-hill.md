# Flatten Front-Left Hill — Art Edit Spec

## Summary

Remove the hill/mound in the front-left grass area of `background.jpg` and replace it with flat grass that matches the surrounding terrain level.

## Current State

The `assets/background.jpg` scene shows a suburban intersection viewed from the player's perspective (standing at the bottom center). Two roads curve away to the left and right, with grass medians/islands between them. The front-left grass area (bottom-left quadrant of the image) has a visible mound/hill where the grass surface rises above the adjacent curb line, creating a rounded hump shape.

## Area to Edit

### Approximate Pixel Coordinates (assuming 1920x1080 native resolution)

- **Bounding box**: roughly x: 0-480, y: 650-1080 (bottom-left quadrant)
- **Hill apex region**: approximately x: 100-380, y: 700-850
- **The hill** is the raised green mound between:
  - The left road curve (running from bottom-center toward upper-left)
  - The bottom edge of the image
  - The left edge of the image
- The grass mound visibly bulges upward above the curb/sidewalk line

### Reference: Front-Right Grass

The front-right grass area (approximately x: 1440-1920, y: 650-1080) is relatively flat and sits at curb level. The front-left should match this flatness.

## Required Edit

1. **Flatten the hill**: Lower the green grass surface in the front-left area so it sits at the same level as the curb/sidewalk edge, similar to the front-right grass area
2. **Maintain grass color and texture**: Use the same green tone and flat-style rendering as the existing grass. The dominant grass color is approximately `#6B8E3A` to `#7FA043` (muted flat-style green)
3. **Preserve the curb line**: The curved gray sidewalk/curb that borders the grass should remain intact; only the grass surface behind it changes
4. **Preserve the storm drain**: There is a small brown circle (storm drain/manhole) near the curb on the left side -- keep this element
5. **Edge blending**: Ensure the flattened grass area blends naturally with:
   - The sidewalk/curb edge along the road curve
   - The road surface where they meet
   - The left edge of the image (grass should extend flat to the edge)

## Visual Description of Desired Result

The front-left grass area should appear as a flat, level patch of green grass sitting flush with the curb/sidewalk, exactly like the front-right grass island. No mounding, no raised terrain -- just a flat green surface at road/curb grade level.

## Tool Suggestions

If editing programmatically:
- Use content-aware fill or clone stamp from the flat portions of the grass
- Sample the flat grass color from the front-right area or the lower portions of the front-left area
- The flat style of the illustration (no complex textures) makes simple color fills viable

If editing manually (recommended for best results):
- Open in GIMP, Photoshop, or similar
- Select the hill area above the curb line
- Paint over with flat grass color sampled from surrounding flat areas
- Ensure the curb/sidewalk edge remains crisp
