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

# --- عام: الحفاظ على أسماء الأصناف التي يحتاجها نظام أندرويد عبر AndroidManifest ---
# ملاحظة: Android Gradle Plugin يولّد تلقائياً قواعد مشابهة لكل مكوّن مُعلَن في
# AndroidManifest.xml، لكن هذا السطر صريح إضافي يضمن عدم تغيير اسم صنفَي الخدمة
# والمستقبِل الصوتي الحسّاسين (تشغيل الأذان) تحت أي ظرف.
-keep class * extends android.app.Service { public <init>(); }
-keep class * extends android.content.BroadcastReceiver { public <init>(); }

# --- تعزيز التعتيم على الكود المُجمَّع (Hardening) ---
# أسماء الملفات الأصلية (QuranScreen.kt وغيرها) تُستبدل بقيمة ثابتة عامة؛ أرقام
# الأسطر تبقى (مع keepattributes LineNumberTable أعلاه) لتبقى تقارير الأعطال
# قابلة لفك الترميز من جهتكم عبر ملف mapping.txt الذي ينتجه كل بناء على GitHub
# Actions (artifact خاص بالبناء، لا يُشحن أبداً داخل APK نفسه).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
# يسمح لـ R8 بتسطيح كل الأصناف المُعتَّمة داخل حزمة واحدة غير مسمّاة، فيصعب على
# أي أداة فكّ ترجمة استنتاج بنية الحزم/الوحدات الأصلية للمشروع.
-repackageclasses ''
# يسمح لـ R8 بتعديل مستويات الوصول (private/public) عند الحاجة لتفعيل تعتيم
# وتصغير أكثر فعالية؛ آمن تماماً هنا لأن كل شيء داخل APK واحد مُوقَّع (لا توجد
# مكتبة عامة يعتمد عليها كود خارجي على ظهور تعديل التوقيعات هذا).
-allowaccessmodification
# يسمح بإعادة استخدام نفس الاسم المُعتَّم القصير لعدة أعضاء مختلفين (تحميل
# زائد)، فيصبح الناتج المُفكَّكة ترجمته أكثر تشويشاً وأصغر حجماً.
-overloadaggressively
