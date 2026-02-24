plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

val gdxVersion: String by rootProject.extra
val ktxVersion: String by rootProject.extra
val ashleyVersion: String by rootProject.extra

dependencies {
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
    api("com.badlogicgames.gdx:gdx-box2d:$gdxVersion")
    
    api("io.github.libktx:ktx-app:$ktxVersion")
    api("io.github.libktx:ktx-graphics:$ktxVersion")
    api("io.github.libktx:ktx-math:$ktxVersion")
    api("io.github.libktx:ktx-scene2d:$ktxVersion")
    api("io.github.libktx:ktx-ashley:$ktxVersion")
    api("io.github.libktx:ktx-box2d:$ktxVersion")
    api("io.github.libktx:ktx-async:$ktxVersion")
    
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    testImplementation("junit:junit:4.13.2")
}
