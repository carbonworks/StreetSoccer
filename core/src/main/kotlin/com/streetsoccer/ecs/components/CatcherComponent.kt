package com.streetsoccer.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.utils.Pool.Poolable

/**
 * Marks an entity as a ball catcher NPC.
 *
 * The catcher stands at a fixed position in the intersection and can
 * intercept the ball if it passes within [catchRadius] game-space pixels.
 * A caught ball produces a distinct outcome from both target hits and
 * wall misses: the ball is removed and the game transitions to the
 * Caught state (streak is preserved but no points are awarded).
 *
 * Catch detection is performed by [com.streetsoccer.ecs.systems.CollisionSystem]
 * using distance-based proximity rather than Box2D fixtures, since the
 * catcher occupies a soft radius rather than a hard collision shape.
 */
class CatcherComponent : Component, Poolable {

    /** Catch radius in game-space pixels. Ball within this distance is caught. */
    var catchRadius: Float = DEFAULT_CATCH_RADIUS

    companion object {
        /** Default catch radius in game-space pixels. */
        const val DEFAULT_CATCH_RADIUS = 60f
    }

    override fun reset() {
        catchRadius = DEFAULT_CATCH_RADIUS
    }
}
