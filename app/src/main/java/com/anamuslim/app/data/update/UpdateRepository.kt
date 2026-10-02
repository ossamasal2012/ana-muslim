package com.anamuslim.app.data.update

import android.content.Context
import com.anamuslim.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val info: VersionInfo) : UpdateCheckResult
    object UpToDate : UpdateCheckResult
    data class Failed(val reason: String) : UpdateCheckResult
}

class UpdateRepository(private val context: Context) {

    /**
     * يفحص وجود تحديث بمقارنة **صحيحة** لتفادي المشكلة الشائعة: "التطبيق يقول محدّث
     * غير محدّث بعد التثبيت فعلاً". القاعدتان الحاسمتان هنا:
     *
     * 1) نقارن دائماً مقابل BuildConfig.VERSION_CODE — وهو رقم مُدمج داخل ملف APK
     *    نفسه وقت البناء، يعكس **بالضبط** النسخة المثبتة فعلياً على الجهاز الآن.
     *    لا نخزّن أو نقارن مقابل أي رقم محفوظ سابقاً في SharedPreferences، لأن أي
     *    قيمة محفوظة كهذه يمكن أن تُصبح "قديمة" (stale) بمجرد تحديث التطبيق دون أن
     *    يُحدَّث معها، وهذا بالضبط سبب المشكلة التي تم تفاديها هنا عمداً.
     * 2) نقارن الأرقام كـ Int وليس كنص (String)، لأن "9" > "10" نصياً وهذا خطأ.
     * 3) نجلب version.json بطلب لا يقبل أي تخزين مؤقت (no-cache headers + رابط
     *    فريد بإضافة الوقت الحالي) حتى لا نحصل أبداً على نسخة قديمة مخزّنة من الملف.
     */
    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val bustedUrl = BuildConfig.UPDATE_VERSION_JSON_URL +
                (if (BuildConfig.UPDATE_VERSION_JSON_URL.contains("?")) "&" else "?") +
                "t=" + System.currentTimeMillis()

            val request = Request.Builder()
                .url(bustedUrl)
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .build()

            val response = UpdateNetwork.noCacheHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext UpdateCheckResult.Failed("http_${response.code}")
            }
            val bodyText = response.body?.string() ?: return@withContext UpdateCheckResult.Failed("empty_body")
            val info = UpdateNetwork.json.decodeFromString(VersionInfo.serializer(), bodyText)

            val installedVersionCode = BuildConfig.VERSION_CODE // القيمة الحقيقية الوحيدة الموثوقة

            return@withContext if (info.versionCode > installedVersionCode) {
                UpdateCheckResult.UpdateAvailable(info)
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateCheckResult.Failed(e.message ?: "unknown_error")
        }
    }
}
