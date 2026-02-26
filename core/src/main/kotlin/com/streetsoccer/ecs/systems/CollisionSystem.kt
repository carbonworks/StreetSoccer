package com.streetsoccer.ecs.systems

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.streetsoccer.ecs.catcherCmpMapper
import com.streetsoccer.ecs.colliderCmpMapper
import com.streetsoccer.ecs.targetCmpMapper
import com.streetsoccer.ecs.transformCmpMapper
import com.streetsoccer.ecs.velocityCmpMapper
import com.streetsoccer.physics.PhysicsContactListener
import com.streetsoccer.physics.TuningConstants
import com.streetsoccer.services.AudioService
import com.streetsoccer.services.SessionAccumulator
import com.streetsoccer.state.GameState
import com.streetsoccer.state.GameStateManager

/**
 * Processes queued physics contacts and out-of-bounds checks each frame.
 *
 * - Target hits: calculate depth-scaled score, record via [SessionAccumulator],
 *   transition to [GameState.Scoring], and remove the ball entity.
 * - Collider (wall/obstacle) hits: break streak, transition to
 *   [GameState.ImpactMissed], and remove the ball entity.
 * - Out-of-bounds: if the ball leaves the play area, treat as a miss.
 *
 * Only runs while the game is in [GameState.BallInFlight].
 */
class CollisionSystem(
    private val contactListener: PhysicsContactListener,
    private val gameStateManager: GameStateManager,
    private val sessionAccumulator: SessionAccumulator,
    private val ecsEngine: Engine,
    private val audioService: AudioService
) : EntitySystem() {

    companion object {
        /** Minimum X boundary (game-space pixels). Ball left of this is out-of-bounds. */
        private const val OOB_MIN_X = -100f
        /** Maximum X boundary (game-space pixels). Ball right of this is out-of-bounds. */
        private const val OOB_MAX_X = 2020f
        /** Minimum Y boundary (game-space pixels). Ball below this is out-of-bounds. */
        private const val OOB_MIN_Y = -100f
        /** Maximum Y boundary (game-space pixels). Ball above this is out-of-bounds. */
        private const val OOB_MAX_Y = 1180f

        // Ball is considered stopped when total speed drops below this (px/s).
        private const val STOPPED_SPEED_THRESHOLD = 15f
        // Ball must be near the ground to count as stopped.
        private const val STOPPED_HEIGHT_THRESHOLD = 5f
    }

    override fun update(deltaTime: Float) {
        // Only process collisions while the ball is in flight.
        if (gameStateManager.currentState !is GameState.BallInFlight) {
            // Still drain the queue to prevent stale contacts from accumulating.
            contactListener.collisionQueue.clear()
            return
        }

        // Snapshot and drain the queue atomically.
        val impacts = contactListener.collisionQueue.toList()
        contactListener.collisionQueue.clear()

        // Process queued collision pairs.
        for ((entityA, entityB) in impacts) {
            if (processCollisionPair(entityA, entityB)) {
                // A decisive collision was handled; ball has been removed.
                return
            }
        }

        // No collision resolved — check for out-of-bounds or stopped ball.
        checkOutOfBounds()
        checkBallStopped()
    }

    /**
     * Inspect a collision pair and handle target hits or wall impacts.
     *
     * @return `true` if the collision was decisive (ball removed), `false` otherwise.
     */
    private fun processCollisionPair(entityA: Entity, entityB: Entity): Boolean {
        // --- Catcher catch check (before target/wall checks) ---
        val catcherA = catcherCmpMapper.get(entityA)
        val catcherB = catcherCmpMapper.get(entityB)

        if (catcherA != null || catcherB != null) {
            // Determine which entity is the ball (has VelocityComponent).
            val ballEntity = when {
                velocityCmpMapper.has(entityA) -> entityA
                velocityCmpMapper.has(entityB) -> entityB
                else -> return false
            }

            // Ball caught by catcher NPC — distinct from target hit and wall miss.
            // No points, no streak break — the catcher simply intercepts the ball.
            gameStateManager.transitionTo(GameState.Caught)
            ecsEngine.removeEntity(ballEntity)
            return true
        }

        // --- Target hit check ---
        val targetA = targetCmpMapper.get(entityA)
        val targetB = targetCmpMapper.get(entityB)

        if (targetA != null || targetB != null) {
            // Determine which entity is the target and which is the ball.
            val targetEntity: Entity
            val ballEntity: Entity
            if (targetA != null) {
                targetEntity = entityA
                ballEntity = entityB
            } else {
                targetEntity = entityB
                ballEntity = entityA
            }

            val target = targetCmpMapper.get(targetEntity)
            val ballTransform = transformCmpMapper.get(ballEntity)

            if (target != null && ballTransform != null) {
                val depthMultiplier = 1.0f + (ballTransform.y / TuningConstants.HORIZON_Y)
                val score = (target.basePoints * depthMultiplier).toLong()

                sessionAccumulator.recordHit(target.targetTypeId, score)

                // Play target-type-specific impact sound (GDD Section 9)
                playTargetHitSound(target.targetTypeId)
                audioService.playScorePopup()

                gameStateManager.transitionTo(GameState.Scoring)
                ecsEngine.removeEntity(ballEntity)
                return true
            }
        }

        // --- Non-sensor collider (wall/obstacle) hit check ---
        val colliderA = colliderCmpMapper.get(entityA)
        val colliderB = colliderCmpMapper.get(entityB)

        if ((colliderA != null && !colliderA.isSensor) || (colliderB != null && !colliderB.isSensor)) {
            // Identify the ball entity (the one with a VelocityComponent).
            val ballEntity = when {
                velocityCmpMapper.has(entityA) -> entityA
                velocityCmpMapper.has(entityB) -> entityB
                else -> return false
            }

            sessionAccumulator.breakStreak()
            audioService.playBounce()
            gameStateManager.transitionTo(GameState.ImpactMissed)
            ecsEngine.removeEntity(ballEntity)
            return true
        }

        return false
    }

    /**
     * Play the appropriate impact sound for a target hit based on target type.
     *
     * Maps target type IDs (from `suburban-crossroads.json`) to the sound cues
     * defined in GDD Section 9.
     */
    private fun playTargetHitSound(targetTypeId: String) {
        when (targetTypeId) {
            "window" -> audioService.playGlassBreak()
            "garage_door" -> audioService.playMetallicClang()
            "vehicle" -> audioService.playCarAlarm()
            "drone" -> audioService.playElectronicFizz()
            else -> audioService.playMetallicClang() // Fallback for unknown types
        }
    }

    /**
     * Check whether the ball entity has left the play area.
     * If so, treat it as a miss (same as hitting a wall).
     */
    private fun checkOutOfBounds() {
        // Find the ball entity: the one with a VelocityComponent.
        val entities = ecsEngine.entities
        for (i in 0 until entities.size()) {
            val entity = entities[i]
            if (velocityCmpMapper.has(entity)) {
                val transform = transformCmpMapper.get(entity) ?: continue
                if (transform.x < OOB_MIN_X || transform.x > OOB_MAX_X ||
                    transform.y < OOB_MIN_Y || transform.y > OOB_MAX_Y
                ) {
                    sessionAccumulator.breakStreak()
                    audioService.playMiss()
                    gameStateManager.transitionTo(GameState.ImpactMissed)
                    ecsEngine.removeEntity(entity)
                }
                // Only one ball at a time; stop after finding it.
                return
            }
        }
    }

    /**
     * Check whether the ball has come to rest on the ground.
     * If total speed is negligible and the ball is near ground level, treat as a miss.
     */
    private fun checkBallStopped() {
        if (gameStateManager.currentState !is GameState.BallInFlight) return

        val entities = ecsEngine.entities
        for (i in 0 until entities.size()) {
            val entity = entities[i]
            if (velocityCmpMapper.has(entity)) {
                val transform = transformCmpMapper.get(entity) ?: continue
                val velocity = velocityCmpMapper.get(entity) ?: continue

                if (transform.height <= STOPPED_HEIGHT_THRESHOLD) {
                    val speed = kotlin.math.sqrt(
                        (velocity.vx * velocity.vx + velocity.vy * velocity.vy + velocity.vz * velocity.vz).toDouble()
                    ).toFloat()
                    if (speed < STOPPED_SPEED_THRESHOLD) {
                        sessionAccumulator.breakStreak()
                        gameStateManager.transitionTo(GameState.ImpactMissed)
                        ecsEngine.removeEntity(entity)
                    }
                }
                return
            }
        }
    }
}
