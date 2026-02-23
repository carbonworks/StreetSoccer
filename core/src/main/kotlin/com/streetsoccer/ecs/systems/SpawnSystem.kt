package com.streetsoccer.ecs.systems

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.math.MathUtils
import com.streetsoccer.ecs.components.*
import com.streetsoccer.state.GameState
import com.streetsoccer.state.GameStateManager
import ktx.ashley.mapperFor

/**
 * Manages the lifecycle of moving targets (vehicles, drones, runners).
 *
 * Each spawn lane is represented by a "lane definition" entity that has a
 * [SpawnLaneComponent] but no [TargetComponent]. The system tracks per-lane
 * spawn timers and, when a timer fires, creates a new moving target entity
 * with Transform, Visual, Collider, Target, and SpawnLane components.
 *
 * Every frame the system:
 * 1. Ticks spawn timers for each lane (frozen during SCORING / IMPACT_MISSED)
 * 2. Spawns new targets when timers expire
 * 3. Translates all spawned targets along their lane direction
 * 4. Despawns targets that leave the play area
 *
 * Active during READY, AIMING, and BALL_IN_FLIGHT states.
 * Movement continues during SCORING and IMPACT_MISSED but new spawns are frozen.
 */
class SpawnSystem(
    private val gameStateManager: GameStateManager
) : EntitySystem() {

    // ---- Component mappers ----

    private val transformMapper = mapperFor<TransformComponent>()
    private val spawnLaneMapper = mapperFor<SpawnLaneComponent>()

    // ---- Entity families ----

    /** Lane definition entities: have SpawnLane but no Target. */
    private val laneFamily: Family = Family
        .all(SpawnLaneComponent::class.java, TransformComponent::class.java)
        .exclude(TargetComponent::class.java)
        .get()

    /** Spawned target entities: have SpawnLane AND Target. */
    private val targetFamily: Family = Family
        .all(SpawnLaneComponent::class.java, TransformComponent::class.java, TargetComponent::class.java)
        .get()

    private lateinit var laneEntities: ImmutableArray<Entity>
    private lateinit var targetEntities: ImmutableArray<Entity>

    // ---- Per-lane timers (keyed by lane entity) ----

    private val spawnTimers = mutableMapOf<Entity, Float>()

    // ---- Off-screen bounds ----

    companion object {
        private const val DESPAWN_MIN_X = -100f
        private const val DESPAWN_MAX_X = 2020f
        private const val DESPAWN_MIN_Y = -100f
        private const val DESPAWN_MAX_Y = 1180f

        /** Default spawn interval (seconds) when not specified in JSON. */
        private const val DEFAULT_SPAWN_INTERVAL = 5.0f
    }

    // ---- Engine lifecycle ----

    override fun addedToEngine(engine: Engine) {
        super.addedToEngine(engine)
        laneEntities = engine.getEntitiesFor(laneFamily)
        targetEntities = engine.getEntitiesFor(targetFamily)
    }

    override fun removedFromEngine(engine: Engine) {
        super.removedFromEngine(engine)
        spawnTimers.clear()
    }

    // ---- Update ----

    override fun update(deltaTime: Float) {
        // Only process during active gameplay states
        if (!gameStateManager.isInGameplay) return
        if (gameStateManager.isPaused) return

        val currentState = gameStateManager.currentState
        val spawningFrozen = currentState is GameState.Scoring ||
            currentState is GameState.ImpactMissed

        // 1. Tick spawn timers and spawn new targets (unless frozen)
        if (!spawningFrozen) {
            updateSpawnTimers(deltaTime)
        }

        // 2. Move all spawned targets along their lane direction
        moveTargets(deltaTime)

        // 3. Despawn off-screen targets
        despawnOffScreen()
    }

    // ---- Spawning ----

    /**
     * Tick the spawn timer for each lane definition entity and create a new
     * target entity when the timer expires.
     */
    private fun updateSpawnTimers(deltaTime: Float) {
        for (i in 0 until laneEntities.size()) {
            val laneEntity = laneEntities[i]
            val lane = spawnLaneMapper[laneEntity] ?: continue

            // Initialize timer on first encounter
            val interval = if (lane.spawnIntervalSeconds > 0f) {
                lane.spawnIntervalSeconds
            } else {
                DEFAULT_SPAWN_INTERVAL
            }

            val remaining = spawnTimers.getOrPut(laneEntity) {
                // Stagger initial spawn with a random offset so all lanes don't fire at once
                MathUtils.random(0f, interval)
            }

            val newRemaining = remaining - deltaTime
            if (newRemaining <= 0f) {
                spawnTarget(laneEntity, lane)
                // Reset timer for next spawn
                spawnTimers[laneEntity] = interval + newRemaining // carry over overshoot
            } else {
                spawnTimers[laneEntity] = newRemaining
            }
        }
    }

    /**
     * Create a new target entity at the lane's start position, configured
     * with a random speed from the lane's speed range.
     */
    private fun spawnTarget(laneEntity: Entity, lane: SpawnLaneComponent) {
        val laneTransform = transformMapper[laneEntity] ?: return

        val entity = engine.createEntity()

        // Transform: position at lane start
        val transform = engine.createComponent(TransformComponent::class.java).apply {
            x = laneTransform.x
            y = laneTransform.y
        }

        // Visual: rendering data (texture assignment deferred to asset loading)
        val visual = engine.createComponent(VisualComponent::class.java).apply {
            renderLayer = 0 // Will be set based on z_layer if available
        }

        // Collider: sensor for ball collision detection
        val collider = engine.createComponent(ColliderComponent::class.java).apply {
            isSensor = true
            // Box2D sensor body creation deferred — will be created when
            // the physics world integration picks up new collider entities
        }

        // Target: marks this as a scoreable target
        val target = engine.createComponent(TargetComponent::class.java).apply {
            targetTypeId = lane.laneId
            basePoints = resolveBasePoints(lane.laneId)
        }

        // SpawnLane: copy lane direction and assign a random speed from the range
        val spawnLane = engine.createComponent(SpawnLaneComponent::class.java).apply {
            laneId = lane.laneId
            directionX = lane.directionX
            directionY = lane.directionY
            // For spawned targets, minSpeed stores the actual movement speed
            val chosenSpeed = MathUtils.random(lane.minSpeed, lane.maxSpeed)
            minSpeed = chosenSpeed
            maxSpeed = chosenSpeed
        }

        entity.add(transform)
        entity.add(visual)
        entity.add(collider)
        entity.add(target)
        entity.add(spawnLane)
        engine.addEntity(entity)
    }

    /**
     * Resolve base points for a target based on its lane ID / target type.
     * Vehicles and drones have different base scoring values.
     */
    private fun resolveBasePoints(laneId: String): Int {
        return when {
            laneId.contains("drone", ignoreCase = true) -> 500
            laneId.contains("vehicle", ignoreCase = true) ||
                laneId.contains("traffic", ignoreCase = true) -> 300
            laneId.contains("runner", ignoreCase = true) ||
                laneId.contains("alley", ignoreCase = true) -> 400
            else -> 200
        }
    }

    // ---- Movement ----

    /**
     * Translate each spawned target entity along its lane direction at its
     * assigned speed. Speed is stored in the target's SpawnLaneComponent.minSpeed.
     */
    private fun moveTargets(deltaTime: Float) {
        for (i in 0 until targetEntities.size()) {
            val entity = targetEntities[i]
            val transform = transformMapper[entity] ?: continue
            val lane = spawnLaneMapper[entity] ?: continue

            // minSpeed holds the chosen movement speed for spawned targets
            val speed = lane.minSpeed
            transform.x += lane.directionX * speed * deltaTime
            transform.y += lane.directionY * speed * deltaTime
        }
    }

    // ---- Despawning ----

    /**
     * Remove target entities that have moved outside the play area bounds.
     */
    private fun despawnOffScreen() {
        // Iterate backwards to safely remove during iteration
        val entitiesToRemove = mutableListOf<Entity>()

        for (i in 0 until targetEntities.size()) {
            val entity = targetEntities[i]
            val transform = transformMapper[entity] ?: continue

            if (transform.x < DESPAWN_MIN_X || transform.x > DESPAWN_MAX_X ||
                transform.y < DESPAWN_MIN_Y || transform.y > DESPAWN_MAX_Y
            ) {
                entitiesToRemove.add(entity)
            }
        }

        for (entity in entitiesToRemove) {
            engine.removeEntity(entity)
        }
    }

    /**
     * Clean up timer references for lane entities that have been removed.
     * Called periodically to prevent memory leaks.
     */
    fun cleanUpTimers() {
        val keysToRemove = spawnTimers.keys.filter { !laneEntities.contains(it) }
        keysToRemove.forEach { spawnTimers.remove(it) }
    }
}
