buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/releases/") }
        maven { url = uri("https://jitpack.io") }
    }
}

val gdxVersion = "1.14.0" // Ensure this matches 1.14.x as requested
val ktxVersion = "1.14.0-rc1" // Adjust according to available KTX releases mapped to GDX
val ashleyVersion = "1.7.4"

subprojects {
    version = "1.0"
    ext {
        set("gdxVersion", gdxVersion)
        set("ktxVersion", ktxVersion)
        set("ashleyVersion", ashleyVersion)
    }
}
