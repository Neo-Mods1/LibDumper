# Lib Dumper ProGuard Rules

# Use custom dictionary for obfuscation
-obfuscationdictionary proguard-dictionary.txt
-packageobfuscationdictionary proguard-dictionary.txt
-classobfuscationdictionary proguard-dictionary.txt

# Don't optimize - prevents R8 from stripping Kotlin null-check intrinsics
-dontoptimize

# Keep crash handler
-keep class com.neomods.libdumper.crash.** { *; }
-keep class com.neomods.libdumper.jni.** { *; }
-keep class com.neomods.libdumper.domain.** { *; }
-keep class com.neomods.libdumper.ui.screens.** { *; }
-keep class com.neomods.libdumper.ui.theme.** { *; }
-keep class com.neomods.libdumper.utils.** { *; }
-keep class com.neomods.libdumper.storage.** { *; }
-keep class com.neomods.libdumper.ui.navigation.** { *; }

# Keep JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Kotlin null-check intrinsics
-keep class kotlin.jvm.internal.Intrinsics { *; }
-keep class kotlin.jvm.internal.SourceDebugExtension { *; }
-keepattributes *Annotation*,RuntimeVisibleAnnotations

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-keep class dagger.hilt.android.lifecycle.** { *; }

# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.animation.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.**
-keepattributes LocalVariableTable,LineNumberTable

# Keep Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Suppress warnings
-dontwarn javax.annotation.**
-dontwarn kotlinx.**
-dontwarn org.jetbrains.**
