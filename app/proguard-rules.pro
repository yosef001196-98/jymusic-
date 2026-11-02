# ProGuard Rules for JY Music

# Keep Media3 / ExoPlayer classes
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Room Database entities and DAOs
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Keep Coil image loader
-keep class coil.** { *; }
-dontwarn coil.**

# Keep Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep App Data Models
-keep class com.example.data.model.** { *; }
-keep class com.example.data.local.** { *; }
