// ملف البناء الجذري لمشروع "أنا مسلم".
// هذا الملف لا يضيف أي كود بناء فعلي، فقط يعلن نسخ الإضافات (Plugins) المستخدمة
// في المشروع بأكمله حتى تستطيع وحدة app (وأي وحدة مستقبلية) تطبيقها دون تكرار الأرقام.

plugins {
    id("com.android.application") version "8.10.1" apply false
    id("org.jetbrains.kotlin.android") version "2.3.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
