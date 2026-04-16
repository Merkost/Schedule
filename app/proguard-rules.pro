-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep public class * extends java.lang.Exception

-keep class ru.dvfu.appliances.model.repository.entity.** { *; }
-keep class ru.dvfu.appliances.compose.home.SelectedDate { *; }

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-dontwarn sun.misc.**

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

-keep class org.koin.** { *; }
-keepnames class * extends org.koin.core.module.Module

-keep class com.google.firebase.** { *; }
-keep class com.crashlytics.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.crashlytics.**

-keep class androidx.compose.runtime.** { *; }

-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
