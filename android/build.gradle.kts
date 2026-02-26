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

    // ---- Signing config (commented out) ----
    // Uncomment and fill in your keystore details before building a signed release APK.
    // Store sensitive values in ~/.gradle/gradle.properties or environment variables
    // rather than checking them into version control.
    //
    // signingConfigs {
    //     create("release") {
    //         storeFile = file("path/to/your-release-key.jks")
    //         storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String? ?: ""
    //         keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String? ?: ""
    //         keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String? ?: ""
    //     }
    // }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // To use the signing config above, uncomment the following line:
            // signingConfig = signingConfigs.getByName("release")
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
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named("preBuild") {
    dependsOn("copyAndroidNatives")
}
