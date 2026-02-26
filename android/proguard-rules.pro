# ============================================================================
# Street Soccer — ProGuard / R8 rules
# ============================================================================
# This file is referenced by android/build.gradle.kts.  It keeps classes that
# are accessed reflectively or via JNI so that R8 minification does not strip
# or rename them.
# ============================================================================

# ----------------------------------------------------------------------------
# LibGDX core — uses JNI and reflection extensively
# ----------------------------------------------------------------------------
-keep class com.badlogic.gdx.** { *; }
-dontwarn com.badlogic.gdx.**

# ----------------------------------------------------------------------------
# LibGDX Box2D — native bindings via JNI
# ----------------------------------------------------------------------------
-keep class com.badlogic.gdx.physics.box2d.** { *; }

# ----------------------------------------------------------------------------
# Ashley ECS — component/system classes are looked up by Class token
# ----------------------------------------------------------------------------
-keep class com.badlogic.ashley.** { *; }

# ----------------------------------------------------------------------------
# KTX (LibKTX) — Kotlin extensions for LibGDX
# ----------------------------------------------------------------------------
-keep class ktx.** { *; }
-dontwarn ktx.**

# ----------------------------------------------------------------------------
# Street Soccer game classes
# ----------------------------------------------------------------------------
-keep class com.streetsoccer.** { *; }

# ----------------------------------------------------------------------------
# Kotlin standard library and reflection
# ----------------------------------------------------------------------------
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

# ----------------------------------------------------------------------------
# Kotlin serialization (kotlinx.serialization)
# ----------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.streetsoccer.**$$serializer { *; }
-keepclassmembers class com.streetsoccer.** {
    *** Companion;
}
-keepclasseswithmembers class com.streetsoccer.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# ----------------------------------------------------------------------------
# General Android / JNI safety
# ----------------------------------------------------------------------------
-keepclassmembers class * {
    native <methods>;
}

# Keep enums (used by LibGDX and game code)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
