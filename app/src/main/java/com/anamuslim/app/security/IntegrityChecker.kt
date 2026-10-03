package com.anamuslim.app.security

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Log
import com.anamuslim.app.BuildConfig
import java.security.MessageDigest

/**
 * فحص اختياري وغير معطِّل لسلامة توقيع التطبيق. الفكرة: بصمة شهادة التوقيع
 * الرسمية (SHA-256) تُحسب تلقائياً وقت البناء في GitHub Actions مباشرة من نفس
 * ملف مفتاح الإصدار (راجع app/build.gradle.kts)، وتُقارَن هنا وقت التشغيل
 * بالتوقيع الفعلي لنسخة APK قيد التشغيل حالياً على الجهاز.
 *
 * حدود هذا الفحص (مهم فهمها حتى لا يُفهَم بأكثر مما يقدّمه فعلياً):
 * - لا يمنع إطلاقاً استخراج الصور أو الأصوات أو أي ملف assets من الـ APK — ذلك
 *   ممكن تقنياً دائماً لأي تطبيق على أي نظام تشغيل، وليس خللاً قابلاً للإصلاح.
 * - لا يمنع فكّ ترجمة (decompile) الكود المُجمَّع، فقط يكشف إن كانت نسخة مُعدَّلة
 *   من الكود (بعد فكّ الترجمة والتعديل) قد أُعيد توقيعها بمفتاح غير مفتاحكم
 *   وأُعيد توزيعها كنسخة منافسة أو مزيّفة من "أنا مسلم".
 * - **لا يوقف تشغيل التطبيق مطلقاً** حتى لو اكتُشف عدم تطابق، تفادياً لأي احتمال
 *   — ولو ضئيلاً — لتعطيل التطبيق لدى مستخدمين حقيقيين شرعيين بسبب أي اختلاف
 *   غير متوقع بين الأجهزة أو إصدارات أندرويد لم تُختبر مسبقاً. يكتفي بتسجيل
 *   الأمر في Logcat لإتاحته لكم عند الحاجة.
 */
object IntegrityChecker {
    private const val TAG = "IntegrityChecker"

    /**
     * true: التوقيع مطابق للمتوقع، أو أن الفحص معطّل أصلاً لهذا البناء (مثل بناء
     * محلي للتطوير بلا بصمة متوقعة مُضمَّنة) — في الحالتين لا داعي لأي قلق.
     * false: توقيع هذه النسخة قيد التشغيل لا يطابق بصمة مفتاح الإصدار الرسمي.
     */
    fun verifySigningCertificate(context: Context): Boolean {
        val expected = BuildConfig.EXPECTED_SIGNING_CERT_SHA256
        if (expected.isBlank()) return true // الفحص معطّل لهذا البناء تحديداً

        val actualHashes = runCatching { currentSigningCertSha256Hashes(context) }.getOrNull()
        if (actualHashes.isNullOrEmpty()) {
            // تعذّر قراءة توقيع التطبيق نفسه لأي سبب غير متوقع: لا نفترض الأسوأ.
            return true
        }

        val matches = actualHashes.any { it.equals(expected, ignoreCase = true) }
        if (!matches) {
            Log.w(TAG, "توقيع هذه النسخة من تطبيق أنا مسلم لا يطابق بصمة مفتاح الإصدار الرسمي المعروفة")
        }
        return matches
    }

    private fun currentSigningCertSha256Hashes(context: Context): List<String> {
        val packageManager = context.packageManager
        val rawSignatures: Array<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            info.signingInfo?.apkContentsSigners ?: emptyArray()
        } else {
            @Suppress("DEPRECATION")
            val info = packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            info.signatures ?: emptyArray()
        }

        val digest = MessageDigest.getInstance("SHA-256")
        return rawSignatures.map { signature ->
            digest.digest(signature.toByteArray()).joinToString("") { b -> "%02X".format(b.toInt() and 0xFF) }
        }
    }
}
