# قواعد ProGuard / R8 الخاصة بتطبيق "أنا مسلم"
# الهدف: تصغير حجم APK وتشفير الكود قليلاً دون كسر أي من المكتبات المستخدمة.

# --- Kotlinx Serialization ---
# يحتاج R8 معرفة أي الأصناف (data classes) قابلة للتسلسل حتى لا يحذف حقولها
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.anamuslim.app.**$$serializer { *; }
-keepclassmembers class com.anamuslim.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.anamuslim.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Retrofit / OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, Exceptions
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# --- نماذج بيانات المشروع (حتى لا يتم حذف حقولها عند التسلسل من/إلى JSON) ---
-keep class com.anamuslim.app.data.**.model.** { *; }
-keep class com.anamuslim.app.data.**.**Dto { *; }
-keep class com.anamuslim.app.data.**.**Response { *; }

# --- عام: الحفاظ على أسماء الأصناف التي تُستخدم بالتفكير (Reflection) ---
-keepattributes SourceFile,LineNumberTable
-keepclassmembers class * extends android.app.Service
-keepclassmembers class * extends android.content.BroadcastReceiver
