package com.streetsoccer.ecs

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.streetsoccer.ecs.components.*
import ktx.ashley.mapperFor

val transformCmpMapper = mapperFor<TransformComponent>()
val velocityCmpMapper = mapperFor<VelocityComponent>()
val spinCmpMapper = mapperFor<SpinComponent>()
val visualCmpMapper = mapperFor<VisualComponent>()
val colliderCmpMapper = mapperFor<ColliderComponent>()
val spawnLaneCmpMapper = mapperFor<SpawnLaneComponent>()
val targetCmpMapper = mapperFor<TargetComponent>()
val ballShadowCmpMapper = mapperFor<BallShadowComponent>()
val catcherCmpMapper = mapperFor<CatcherComponent>()

val Entity.transform: TransformComponent?
    get() = transformCmpMapper.get(this)

val Entity.velocity: VelocityComponent?
    get() = velocityCmpMapper.get(this)

val Entity.spin: SpinComponent?
    get() = spinCmpMapper.get(this)

val Entity.visual: VisualComponent?
    get() = visualCmpMapper.get(this)

val Entity.collider: ColliderComponent?
    get() = colliderCmpMapper.get(this)

val Entity.spawnLane: SpawnLaneComponent?
    get() = spawnLaneCmpMapper.get(this)

val Entity.target: TargetComponent?
    get() = targetCmpMapper.get(this)

val Entity.ballShadow: BallShadowComponent?
    get() = ballShadowCmpMapper.get(this)

val Entity.catcher: CatcherComponent?
    get() = catcherCmpMapper.get(this)
