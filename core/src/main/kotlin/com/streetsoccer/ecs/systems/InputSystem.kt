package com.streetsoccer.ecs.systems

import com.badlogic.ashley.core.EntitySystem

class InputSystem : EntitySystem() {
    
    override fun update(deltaTime: Float) {
        // Delegates to InputRouter, reads gesture results and applies to ball entities
    }
}
