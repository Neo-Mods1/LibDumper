# Lib Dumper ProGuard Rules

# Use custom dictionary for obfuscation
-obfuscationdictionary proguard-dictionary.txt
-packageobfuscationdictionary proguard-dictionary.txt
-classobfuscationdictionary proguard-dictionary.txt

# Keep crash handler
-keep class com.neomods.libdumper.crash.** { *; }
-keep class com.neomods.libdumper.jni.** { *; }
-keep class com.neomods.libdumper.domain.** { *; }

# Keep JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# Keep Compose
-keep class androidx.compose.** { *; }

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
