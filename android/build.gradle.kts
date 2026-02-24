plugins {
    id("com.android.application")
    kotlin("android")
}

val gdxVersion: String by rootProject.extra

val natives: Configuration by configurations.creating

android {
    namespace = "com.streetsoccer.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.streetsoccer"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    
    sourceSets.getByName("main") {
        assets.srcDirs("src/main/assets", "../assets")
        jniLibs.srcDirs("libs")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-x86_64")
}

tasks.register<Copy>("copyAndroidNatives") {
    from(configurations.getByName("natives").files.map { zipTree(it) })
    into("src/main/jniLibs/armeabi-v7a")
    include("*.so")
    exclude("META-INF/")
}

tasks.named("preBuild") {
    dependsOn("copyAndroidNatives")
}
