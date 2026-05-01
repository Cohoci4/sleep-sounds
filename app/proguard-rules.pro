# Generic Android / Kotlin keeps -------------------------------------------
-dontobfuscate
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# Compose / Kotlinx Metadata
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }

# Hilt
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager
-keep class * extends androidx.lifecycle.ViewModel
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep class dagger.hilt.** { *; }
-keepnames class dagger.hilt.** { *; }
-keep,allowobfuscation,allowshrinking class dagger.hilt.android.lifecycle.HiltViewModel

# Retrofit / OkHttp / Gson --------------------------------------------------
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# Gson
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Room ---------------------------------------------------------------------
-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Firebase -----------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Crashlytics keeps line numbers visible
-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile

# Google Play Billing ------------------------------------------------------
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# Coil ---------------------------------------------------------------------
-keep class coil.** { *; }
-dontwarn coil.**

# Media3 / ExoPlayer -------------------------------------------------------
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Application data classes (kept so Gson serialization works after R8)
-keep class com.sleepsounds.app.data.remote.dto.** { *; }
-keep class com.sleepsounds.app.domain.model.** { *; }
-keepclassmembers class com.sleepsounds.app.** { *; }

# Generic enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Native methods
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Timber
-dontwarn org.jetbrains.annotations.**
