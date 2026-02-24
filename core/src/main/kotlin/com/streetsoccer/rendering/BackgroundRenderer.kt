package com.streetsoccer.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable

/**
 * Renders the level background as either a stack of layered images or a single
 * composite fallback (background.jpg). Layers are drawn back-to-front:
 *
 *   1. Sky        — solid sky gradient / clouds (furthest back)
 *   2. Ground     — roads, sidewalks, grass, horizon
 *   3. Buildings  — building facades rendered at their depth position
 *
 * Each layer is a full-screen (1920x1080) PNG with transparency so the layers
 * behind show through. If none of the layer files exist, the renderer falls
 * back to the monolithic background.jpg. If that also doesn't exist, the
 * renderer simply does nothing (the clear color shows through).
 *
 * Layer file paths (relative to internal assets root):
 *   - backgrounds/sky.png
 *   - backgrounds/ground.png
 *   - backgrounds/buildings.png
 *
 * Fallback path:
 *   - background.jpg
 *
 * This design supports future parallax scrolling by letting each layer move
 * at a different rate, and sky replacement by swapping the sky layer per
 * seasonal variant.
 */
class BackgroundRenderer(
    private val worldWidth: Float = 1920f,
    private val worldHeight: Float = 1080f
) : Disposable {

    companion object {
        private const val TAG = "BackgroundRenderer"

        /** Layer asset paths in draw order (back to front). */
        val LAYER_PATHS = listOf(
            "backgrounds/sky.png",
            "backgrounds/ground.png",
            "backgrounds/buildings.png"
        )

        /** Monolithic fallback when layers are not available. */
        const val FALLBACK_PATH = "background.jpg"
    }

    /**
     * Enum identifying each background layer for external reference
     * (e.g., parallax offsets, seasonal swaps).
     */
    enum class Layer(val assetPath: String) {
        SKY("backgrounds/sky.png"),
        GROUND("backgrounds/ground.png"),
        BUILDINGS("backgrounds/buildings.png")
    }

    /** Loaded layer textures in draw order, or null if that layer was not found. */
    private val layerTextures = arrayOfNulls<Texture>(LAYER_PATHS.size)

    /** Monolithic fallback texture. Loaded only when no layers exist. */
    private var fallbackTexture: Texture? = null

    /** True if at least one layered asset was loaded. */
    private var usingLayers = false

    /** True if the fallback background.jpg was loaded. */
    private var usingFallback = false

    /** True once [load] has been called. Prevents double-loading. */
    private var loaded = false

    /**
     * Attempt to load layered background assets. If none exist, load the
     * monolithic fallback. Call this once from show() or after asset manager
     * finishes loading.
     *
     * This method loads textures synchronously via [Texture] constructor.
     * For AssetManager-based loading, use [getAssetPaths] and [setTextures].
     */
    fun load() {
        if (loaded) return
        loaded = true

        // Try layered assets first
        var anyLayerFound = false
        for (i in LAYER_PATHS.indices) {
            val path = LAYER_PATHS[i]
            try {
                val file = Gdx.files.internal(path)
                if (file.exists()) {
                    layerTextures[i] = Texture(file).apply {
                        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                    }
                    anyLayerFound = true
                    Gdx.app.log(TAG, "Loaded layer: $path")
                } else {
                    Gdx.app.log(TAG, "Layer not found: $path")
                }
            } catch (e: Exception) {
                Gdx.app.error(TAG, "Failed to load layer $path: ${e.message}")
            }
        }

        if (anyLayerFound) {
            usingLayers = true
            Gdx.app.log(TAG, "Using layered background rendering")
            return
        }

        // Fall back to monolithic background.jpg
        try {
            val file = Gdx.files.internal(FALLBACK_PATH)
            if (file.exists()) {
                fallbackTexture = Texture(file).apply {
                    setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                }
                usingFallback = true
                Gdx.app.log(TAG, "Loaded fallback: $FALLBACK_PATH")
            } else {
                Gdx.app.log(TAG, "No background assets found (neither layers nor fallback)")
            }
        } catch (e: Exception) {
            Gdx.app.error(TAG, "Failed to load fallback $FALLBACK_PATH: ${e.message}")
        }
    }

    /**
     * Provide pre-loaded textures from an AssetManager instead of loading
     * them synchronously. Pass null for layers that were not available.
     *
     * @param layers Map of layer index (0=sky, 1=ground, 2=buildings) to Texture
     * @param fallback The monolithic background.jpg texture, or null
     */
    fun setTextures(layers: Map<Int, Texture>, fallback: Texture?) {
        loaded = true
        var anyLayerFound = false
        for ((index, texture) in layers) {
            if (index in layerTextures.indices) {
                layerTextures[index] = texture
                anyLayerFound = true
            }
        }

        if (anyLayerFound) {
            usingLayers = true
            Gdx.app.log(TAG, "Using layered background (AssetManager)")
        } else if (fallback != null) {
            fallbackTexture = fallback
            usingFallback = true
            Gdx.app.log(TAG, "Using fallback background (AssetManager)")
        }
    }

    /**
     * Return all asset paths that should be queued into an AssetManager.
     * Includes both layered paths and the fallback. The caller should check
     * which files actually exist before queuing.
     */
    fun getAssetPaths(): List<String> = LAYER_PATHS + FALLBACK_PATH

    /**
     * Draw all background layers (or the fallback) using the provided batch.
     * The batch must already be configured with the correct projection matrix
     * (typically the game viewport camera). The batch should NOT be in a
     * begin/end block -- this method calls begin/end itself, or if the batch
     * is already drawing, it draws directly.
     *
     * @param batch SpriteBatch to draw with. Must have projection set.
     * @param batchAlreadyDrawing If true, skip begin()/end() calls.
     */
    fun render(batch: SpriteBatch, batchAlreadyDrawing: Boolean = false) {
        if (!loaded) return

        if (!batchAlreadyDrawing) batch.begin()

        if (usingLayers) {
            // Draw layers back-to-front: sky, ground, buildings
            for (texture in layerTextures) {
                texture?.let {
                    batch.draw(it, 0f, 0f, worldWidth, worldHeight)
                }
            }
        } else if (usingFallback) {
            fallbackTexture?.let {
                batch.draw(it, 0f, 0f, worldWidth, worldHeight)
            }
        }

        if (!batchAlreadyDrawing) batch.end()
    }

    /** True if any renderable background is available (layers or fallback). */
    fun hasBackground(): Boolean = usingLayers || usingFallback

    /** True if using layered rendering mode. */
    fun isLayered(): Boolean = usingLayers

    override fun dispose() {
        // Only dispose textures that we loaded ourselves (synchronous path).
        // If textures came from AssetManager, the AssetManager owns them.
        if (usingLayers) {
            for (i in layerTextures.indices) {
                layerTextures[i]?.dispose()
                layerTextures[i] = null
            }
        }
        if (usingFallback) {
            fallbackTexture?.dispose()
            fallbackTexture = null
        }
        loaded = false
        usingLayers = false
        usingFallback = false
    }
}
