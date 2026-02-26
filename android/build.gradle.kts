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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
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

tasks.register("copyAndroidNatives") {
    doFirst {
        val jniLibsDir = file("src/main/jniLibs")
        jniLibsDir.deleteRecursively()
        configurations.getByName("natives").files.forEach { jar ->
            val abi = when {
                jar.name.contains("armeabi-v7a") -> "armeabi-v7a"
                jar.name.contains("arm64-v8a") -> "arm64-v8a"
                jar.name.contains("x86_64") -> "x86_64"
                jar.name.contains("x86") -> "x86"
                else -> return@forEach
            }
            val targetDir = jniLibsDir.resolve(abi)
            targetDir.mkdirs()
            zipTree(jar).matching { include("*.so") }.forEach { soFile ->
                soFile.copyTo(targetDir.resolve(soFile.name), overwrite = true)
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn("copyAndroidNatives")
}
