buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.9.22")
    }
}

// Version constants shared across all subprojects via rootProject.extra
extra["gdxVersion"] = "1.12.1"
extra["ktxVersion"] = "1.12.1-rc2"
extra["ashleyVersion"] = "1.7.4"

subprojects {
    version = "1.0"
}
