# Keep Moshi models
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class com.stockmarket.app.data.model.** { *; }
