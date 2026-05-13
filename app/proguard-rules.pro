###############################################
# ProGuard / R8 Rules for Expense Tracker App #
###############################################

##########  KEEP ANDROID ENTRY POINTS ##########
# Keep application, activities, services, receivers, content providers
-keepnames class * extends android.app.Application
-keepnames class * extends android.app.Activity
-keepnames class * extends android.app.Service
-keepnames class * extends android.content.BroadcastReceiver
-keepnames class * extends android.content.ContentProvider

# Explicitly keep your SMSReceiver (processes incoming bank messages)
-keep class com.expensetracker.receiver.SMSReceiver { *; }
-keep class com.expensetracker.utils.SMSProcessor { *; }

##########  KEEP COMPOSE & UI CLASSES ##########
# Keep Jetpack Compose runtime & UI functions
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep Material Icons & Material3 components
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.material3.** { *; }

# Keep Navigation Compose
-keep class androidx.navigation.** { *; }

##########  KOTLIN & COROUTINES ##########
-keep class kotlinx.coroutines.** { *; }
-keep class kotlin.Metadata { *; }

##########  JSON (Gson) & REFLECTION ##########
# Keep Gson TypeToken and reflective access classes
-keep class com.google.gson.reflect.TypeToken
-keep class com.google.gson.TypeAdapter
-keep class com.google.gson.TypeAdapterFactory
-keep class com.google.gson.JsonSerializer
-keep class com.google.gson.JsonDeserializer
-keep class com.google.gson.JsonElement

# Keep your model/data classes (used with Gson for storage)
-keep class com.expensetracker.data.models.** { *; }
-keep class com.expensetracker.data.repository.** { *; }
-keep class com.expensetracker.data.storage.** { *; }
-keep class com.expensetracker.data.ml.** { *; }

##########  VIEWMODEL & LIFECYCLE ##########
-keep class androidx.lifecycle.** { *; }

##########  ENUMS ##########
# Preserve enum values used by Gson/reflection/UI
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

##########  MISC ##########
# Keep all inner classes (needed for some Compose lambdas/ViewModels)
-keep class *$* { *; }

# Don't warn for Kotlin/JetBrains/Compose generated classes
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.compose.**
-dontwarn androidx.lifecycle.**
-dontwarn androidx.navigation.**
-dontwarn com.google.accompanist.permissions.**

##########  ACCOMPANIST PERMISSIONS ##########
-keep class com.google.accompanist.permissions.** { *; }

##########  OPTIONAL: DEBUG LOG KEEPING ##########
# If you want to keep Timber/Log calls in release for debugging,
# comment out the following (otherwise logging is stripped)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
