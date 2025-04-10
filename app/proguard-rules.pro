# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# SafeReach ProGuard Configuration
# Optimize for release while maintaining compatibility with libraries

#----------- General Configuration -----------
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# Prevent obfuscation of debug information for crash reports
-renamesourcefileattribute SourceFile

# Preserve stack traces for debugging
-keepattributes SourceFile,LineNumberTable

#----------- Kotlin Specific -----------
# Keep Kotlin Metadata for Reflection
-keepclassmembers class **.*$Companion {
    *;
}
-keepclasseswithmembers class kotlin.coroutines.Continuation

#----------- Jetpack Compose -----------
# Keep Compose components
-keep class androidx.compose.** { *; }
-keepclasseswithmembers class * {
    @androidx.compose.ui.tooling.preview.Preview *;
}

#----------- Hilt & Dagger -----------
-keepclassmembers,allowobfuscation class * {
    @javax.inject.* *;
    @dagger.* *;
    <init>();
}
-keep class dagger.* { *; }
-keep class javax.inject.* { *; }
-keep class * extends dagger.internal.Binding
-keep class * extends dagger.internal.ModuleAdapter
-keep class * extends dagger.internal.StaticInjection

#----------- Firebase -----------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep firebase auth classes
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

#----------- Firestore -----------
# Keep Firestore model classes
-keep class com.example.safereach.data.model.** { *; }
-keep class com.example.safereach.domain.model.** { *; }

# Serialize/deserialize support
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

#----------- Room Database -----------
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

#----------- Coroutines -----------
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

#----------- OkHttp and Retrofit -----------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

#----------- WorkManager -----------
-keep class androidx.work.** { *; }

#----------- App-specific classes -----------
# Keep ViewModel classes
-keep class com.example.safereach.presentation.viewmodel.** { *; }

# Keep Repository implementations
-keep class com.example.safereach.data.repository.** { *; }

# Keep SafeReach model classes for Firebase
-keep class com.example.safereach.data.model.** { *; }
-keep class com.example.safereach.domain.model.** { *; }

# Keep custom Views or composables if any
-keep class com.example.safereach.presentation.components.** { *; }