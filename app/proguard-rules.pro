# Lib Dumper ProGuard Rules

# Use custom dictionary for obfuscation
-obfuscationdictionary proguard-dictionary.txt
-packageobfuscationdictionary proguard-dictionary.txt
-classobfuscationdictionary proguard-dictionary.txt

# Don't optimize - prevents R8 from stripping Kotlin null-check intrinsics
-dontoptimize

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

# JNI native methods - must keep original names
-keepclasseswithmembernames class * {
    native <methods>;
}

# Domain models - keep field names for Gson @SerializedName
-keep class com.neomods.libdumper.domain.Symbol { *; }
-keep class com.neomods.libdumper.domain.ClassInfo { *; }
-keep class com.neomods.libdumper.domain.NamespaceInfo { *; }
-keep class com.neomods.libdumper.domain.Method { *; }
-keep class com.neomods.libdumper.domain.Field { *; }
-keep class com.neomods.libdumper.domain.DumpConfig { *; }
-keep class com.neomods.libdumper.domain.DumpResult { *; }
-keep class com.neomods.libdumper.domain.ElfInfo { *; }

# Crash handler - keep all (critical for crash reporting)
-keep class com.neomods.libdumper.crash.** { *; }

# JNI wrapper - keep native method signatures only, allow renaming internals
-keep class com.neomods.libdumper.jni.NativeLibWrapper {
    private static external <methods>;
    private static native <methods>;
}

# Settings/Storage - keep DataStore field names
-keep class com.neomods.libdumper.storage.SettingsManager { *; }

# Allow R8 to rename private/internal methods and fields in other classes
-allowaccessmodification
-repackageclasses ''

# Suppress warnings
-dontwarn javax.annotation.**
-dontwarn kotlinx.**
-dontwarn org.jetbrains.**
