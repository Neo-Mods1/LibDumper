# Lib Dumper ProGuard Rules

# Use custom dictionary for obfuscation
-obfuscationdictionary proguard-dictionary.txt
-packageobfuscationdictionary proguard-dictionary.txt
-classobfuscationdictionary proguard-dictionary.txt

# Keep crash handler
-keep class com.neomods.libdumper.crash.** { *; }
-keep class com.neomods.libdumper.jni.** { *; }
-keep class com.neomods.libdumper.domain.** { *; }
-keep class com.neomods.libdumper.ui.screens.** { *; }
-keep class com.neomods.libdumper.ui.theme.** { *; }

# Keep JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keepattributes LocalVariableTable,LineNumberTable

# Keep Compose stable/Unstable markers
-keep class androidx.compose.runtime.internal.ComposableLambda { *; }
-keep class androidx.compose.runtime.internal.ComposableLambdaN { *; }
-keep class androidx.compose.runtime.internal.ComposableSingletons$* { *; }
-keep class * extends androidx.compose.runtime.internal.ComposableLambda { *; }

# Keep all Composable functions and their generated classes
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep class androidx.compose.runtime.CompositionKt { *; }
-keep class androidx.compose.runtime.RecomposeScopeImpl { *; }
-keep class androidx.compose.runtime.Recomposer { *; }
-keep class androidx.compose.runtime.internal.ComposableLambda { *; }

# Keep Compose UI internals that R8 may break
-keep class androidx.compose.ui.platform.AndroidUiFrameClock { *; }
-keep class androidx.compose.ui.platform.AndroidUiDispatcher { *; }

# Keep Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep data classes for Gson serialization
-keep class com.neomods.libdumper.domain.** { *; }

# Suppress warnings
-dontwarn javax.annotation.**
-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn org.jetbrains.**
