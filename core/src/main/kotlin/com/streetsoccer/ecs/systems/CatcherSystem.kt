package com.streetsoccer.ecs.systems

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.streetsoccer.ecs.components.CatcherComponent
import com.streetsoccer.ecs.components.TransformComponent
import com.streetsoccer.ecs.components.VelocityComponent
import com.streetsoccer.state.GameState
import com.streetsoccer.state.GameStateManager
import ktx.ashley.mapperFor

/**
 * Handles ball catcher NPC logic: proximity-based catch detection.
 *
 * Each frame during [GameState.BallInFlight], this system checks whether the
 * ball entity is within any catcher's [CatcherComponent.catchRadius]. If so,
 * it removes the ball and transitions to [GameState.Caught] -- a distinct
 * outcome from target hits (Scoring) and wall misses (ImpactMissed).
 *
 * The catcher is stationary for now (no movement AI). Future iterations may
 * add diving animations or lateral movement to increase difficulty.
 *
 * Catch detection uses game-space distance between the ball's ground-plane
 * position (x, y) and the catcher's position, ignoring ball height. This
 * means a high-arcing ball that passes directly over the catcher will be
 * caught as long as its ground shadow overlaps the catch radius, matching
 * the visual expectation of a goalkeeper catching a ball.
 */
class CatcherSystem(
    private val gameStateManager: GameStateManager,
    private val ecsEngine: Engine
) : EntitySystem() {

    // ---- Component mappers ----
    private val transformMapper = mapperFor<TransformComponent>()
    private val catcherMapper = mapperFor<CatcherComponent>()
    private val velocityMapper = mapperFor<VelocityComponent>()

    // ---- Entity families ----

    /** Catcher entities: have CatcherComponent + Transform. */
    private val catcherFamily: Family = Family
        .all(CatcherComponent::class.java, TransformComponent::class.java)
        .get()

    private lateinit var catcherEntities: ImmutableArray<Entity>

    // ---- Engine lifecycle ----

    override fun addedToEngine(engine: Engine) {
        super.addedToEngine(engine)
        catcherEntities = engine.getEntitiesFor(catcherFamily)
    }

    // ---- Update ----

    override fun update(deltaTime: Float) {
        // Only check for catches while the ball is in flight.
        if (gameStateManager.currentState !is GameState.BallInFlight) return
        if (catcherEntities.size() == 0) return

        // Find the ball entity (the one with a VelocityComponent).
        val ballEntity = findBallEntity() ?: return
        val ballTransform = transformMapper[ballEntity] ?: return

        // Check proximity to each catcher.
        for (i in 0 until catcherEntities.size()) {
            val catcherEntity = catcherEntities[i]
            val catcherTransform = transformMapper[catcherEntity] ?: continue
            val catcher = catcherMapper[catcherEntity] ?: continue

            val dx = ballTransform.x - catcherTransform.x
            val dy = ballTransform.y - catcherTransform.y
            val distSq = dx * dx + dy * dy
            val radiusSq = catcher.catchRadius * catcher.catchRadius

            if (distSq <= radiusSq) {
                // Ball caught! Transition to the Caught state and remove the ball.
                gameStateManager.transitionTo(GameState.Caught)
                ecsEngine.removeEntity(ballEntity)
                return
            }
        }
    }

    /**
     * Locate the ball entity in the engine. The ball is identified as the
     * entity with a [VelocityComponent] (only one ball exists at a time).
     */
    private fun findBallEntity(): Entity? {
        val entities = ecsEngine.entities
        for (i in 0 until entities.size()) {
            val entity = entities[i]
            if (velocityMapper.has(entity)) {
                return entity
            }
        }
        return null
    }
}
