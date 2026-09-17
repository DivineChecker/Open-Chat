# ProGuard / R8 rules for OpenChat AI

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class * {
    <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Ktor Client
-keep class io.ktor.** { *; }

# Coil 3
-keep class coil3.** { *; }
-dontwarn coil3.**

# PDFBox Android
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
