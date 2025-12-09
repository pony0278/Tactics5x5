# LibGDX ProGuard rules
-verbose

-dontwarn android.support.**
-dontwarn com.badlogic.gdx.backends.android.AndroidFragmentApplication

-keep class com.badlogic.gdx.** { *; }
-keep class com.tactics.client.** { *; }

# Required for reflection in LibGDX
-keepclassmembers class * implements com.badlogic.gdx.utils.Json$Serializable {
    *;
}
