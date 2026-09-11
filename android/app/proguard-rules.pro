# Proguard rules for BRST DNS Controller
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.brst.dns.data.api.models.** { *; }
-keepclassmembers class * extends com.google.gson.TypeAdapter {
    public <init>(...);
}
