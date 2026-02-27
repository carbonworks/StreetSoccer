package com.streetsoccer.screens

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.physics.box2d.World
import com.streetsoccer.ecs.components.CatcherComponent
import com.streetsoccer.ecs.components.TransformComponent
import com.streetsoccer.ecs.components.VisualComponent
import com.streetsoccer.ecs.systems.CatcherSystem
import com.streetsoccer.ecs.systems.CollisionSystem
import com.streetsoccer.ecs.systems.HudSystem
import com.streetsoccer.ecs.systems.InputSystem
import com.streetsoccer.ecs.systems.PhysicsSystem
import com.streetsoccer.ecs.systems.RenderSystem
import com.streetsoccer.ecs.systems.SpawnSystem
import com.streetsoccer.ecs.systems.TrajectorySystem
import com.streetsoccer.input.InputRouter
import com.streetsoccer.level.LevelData
import com.streetsoccer.physics.PhysicsContactListener
import com.streetsoccer.physics.TuningConstants
import com.streetsoccer.services.AudioService
import com.streetsoccer.services.SessionAccumulator
import com.streetsoccer.state.GameStateListener
import com.streetsoccer.state.GameStateManager
import com.streetsoccer.ui.PauseOverlay

/**
 * Creates and configures the ECS Engine: registers all systems, creates
 * initial entities (catcher), and builds the InputMultiplexer.
 *
 * Returns a [BootstrapResult] bundle containing references needed by
 * [GameLoop] and [LevelScreen] for lifecycle management.
 */
class ECSBootstrapper(
    private val gameStateManager: GameStateManager,
    private val sessionAccumulator: SessionAccumulator,
    private val contactListener: PhysicsContactListener,
    private val world: World,
    private val batch: SpriteBatch,
    private val inputRouter: InputRouter,
    private val audioService: AudioService,
    private val assets: AssetManager,
    private val trajectoryPreviewEnabled: Boolean
) {

    /**
     * Bundle of references produced by [bootstrap] for use by GameLoop
     * and LevelScreen lifecycle methods.
     */
    data class BootstrapResult(
        val engine: Engine,
        val physicsSystem: PhysicsSystem,
        val collisionSystem: CollisionSystem,
        val renderSystem: RenderSystem,
        val spawnSystem: SpawnSystem,
        val inputSystem: InputSystem,
        val hudSystem: HudSystem,
        val catcherSystem: CatcherSystem,
        val trajectorySystem: TrajectorySystem,
        val inputMultiplexer: InputMultiplexer,
        val pauseOverlay: PauseOverlay,
        /** Catcher placeholder texture, owned by the caller for disposal. */
        val catcherTexture: Texture?
    )

    /**
     * Bootstrap the entire ECS: create engine, register systems, create
     * entities from level data, and wire up input.
     *
     * @param levelData Parsed level JSON driving entity positions
     * @param onResume Callback when the user taps Resume in the pause overlay
     * @param onQuit Callback when the user taps Quit in the pause overlay
     * @param pauseStateListener Listener that shows/hides the pause overlay on state changes
     * @param backButtonProcessor InputAdapter handling BACK/ESCAPE keys
     */
    fun bootstrap(
        levelData: LevelData?,
        onResume: () -> Unit,
        onQuit: () -> Unit,
        pauseStateListener: GameStateListener,
        backButtonProcessor: InputAdapter
    ): BootstrapResult {
        val engine = Engine()

        // --- Create ECS Systems ---
        // InputSystem must be created first — CollisionSystem and CatcherSystem
        // use its cached ball reference to avoid per-frame entity searches (#36).
        val inputSystem = InputSystem(inputRouter, gameStateManager, world, audioService, assets)
        val physicsSystem = PhysicsSystem()
        val collisionSystem = CollisionSystem(
            contactListener, gameStateManager, sessionAccumulator, engine, audioService, inputSystem
        )
        val renderSystem = RenderSystem(batch, inputSystem)
        val spawnSystem = SpawnSystem(gameStateManager)
        val hudSystem = HudSystem(gameStateManager, sessionAccumulator).apply {
            sliderSide = inputRouter.sliderSide
        }
        val catcherSystem = CatcherSystem(gameStateManager, engine, inputSystem)
        val trajectorySystem = TrajectorySystem(gameStateManager, inputRouter).apply {
            trajectoryPreviewEnabled = this@ECSBootstrapper.trajectoryPreviewEnabled
        }

        // --- Register all systems with the engine ---
        engine.addSystem(physicsSystem)
        engine.addSystem(collisionSystem)
        engine.addSystem(renderSystem)
        engine.addSystem(spawnSystem)
        engine.addSystem(inputSystem)
        engine.addSystem(hudSystem)
        engine.addSystem(catcherSystem)
        engine.addSystem(trajectorySystem)

        // Disable PhysicsSystem from engine.update() -- the fixed-timestep
        // loop in GameLoop is the sole authority for physics ticking (issue #29).
        physicsSystem.setProcessing(false)

        // --- Create catcher entity from level data ---
        val catcherTexture = createCatcherEntity(engine, levelData)

        // --- Create pause overlay ---
        val pauseOverlay = PauseOverlay(
            onResume = onResume,
            onQuit = onQuit
        )
        pauseOverlay.hide()

        // --- Register pause state listener ---
        gameStateManager.addListener(pauseStateListener)

        // --- Build input multiplexer with priority order ---
        // 1. Back button handler (highest priority)
        // 2. Pause overlay stage (when visible, consumes all taps)
        // 3. HUD stage (for pause icon taps)
        // 4. InputRouter (for gameplay gestures)
        val inputMultiplexer = InputMultiplexer()
        inputMultiplexer.addProcessor(backButtonProcessor)
        inputMultiplexer.addProcessor(pauseOverlay.stage)
        inputMultiplexer.addProcessor(hudSystem.getStage())
        inputMultiplexer.addProcessor(inputRouter)

        return BootstrapResult(
            engine = engine,
            physicsSystem = physicsSystem,
            collisionSystem = collisionSystem,
            renderSystem = renderSystem,
            spawnSystem = spawnSystem,
            inputSystem = inputSystem,
            hudSystem = hudSystem,
            catcherSystem = catcherSystem,
            trajectorySystem = trajectorySystem,
            inputMultiplexer = inputMultiplexer,
            pauseOverlay = pauseOverlay,
            catcherTexture = catcherTexture
        )
    }

    /**
     * Create the ball catcher NPC entity at the position defined in level data.
     *
     * Falls back to center-intersection defaults if level data is unavailable.
     *
     * @return The placeholder texture created for the catcher, or null if a real
     *         sprite was loaded. Caller is responsible for disposing.
     */
    private fun createCatcherEntity(engine: Engine, levelData: LevelData?): Texture? {
        val catcherX = levelData?.catcherX ?: DEFAULT_CATCHER_X
        val catcherY = levelData?.catcherY ?: DEFAULT_CATCHER_Y
        val catchRadius = levelData?.catcherRadius ?: CatcherComponent.DEFAULT_CATCH_RADIUS

        val entity = engine.createEntity()

        val transform = engine.createComponent(TransformComponent::class.java).apply {
            x = catcherX
            y = catcherY
            height = 0f
            screenScale = maxOf(MINIMUM_SCALE, (TuningConstants.HORIZON_Y - catcherY) / TuningConstants.HORIZON_Y)
        }

        val catcher = engine.createComponent(CatcherComponent::class.java).apply {
            this.catchRadius = catchRadius
        }

        var placeholderTexture: Texture? = null
        val region = if (assets.isLoaded("sprites/catcher.png")) {
            TextureRegion(assets.get("sprites/catcher.png", Texture::class.java))
        } else {
            val size = 64
            val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
            pixmap.setColor(0f, 0f, 0f, 0f)
            pixmap.fill()
            pixmap.setColor(1f, 0.5f, 0.1f, 1f)
            pixmap.fillCircle(size / 2, size / 2, size / 2 - 2)
            placeholderTexture = Texture(pixmap)
            pixmap.dispose()
            TextureRegion(placeholderTexture)
        }

        val visual = engine.createComponent(VisualComponent::class.java).apply {
            this.region = region
            renderLayer = 1
        }

        entity.add(transform)
        entity.add(catcher)
        entity.add(visual)
        engine.addEntity(entity)

        return placeholderTexture
    }

    companion object {
        /** Fallback catcher X when level data is unavailable. */
        private const val DEFAULT_CATCHER_X = 960f
        /** Fallback catcher Y when level data is unavailable. */
        private const val DEFAULT_CATCHER_Y = 400f
        /** Minimum entity scale to prevent vanishing at the horizon. */
        private const val MINIMUM_SCALE = 0.05f
    }
}
