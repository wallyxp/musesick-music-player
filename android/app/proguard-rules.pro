# Proguard rules for Musesick
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep models
-keep class com.wally.musesick.model.** { *; }

# Coil image loader
-keep class coil.** { *; }
-dontwarn coil.**

