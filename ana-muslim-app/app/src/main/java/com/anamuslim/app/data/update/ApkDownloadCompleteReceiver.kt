package com.anamuslim.app.data.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * يستقبل بث النظام عند اكتمال تنزيل ملف APK عبر DownloadManager.
 *
 * الاعتماد الرئيسي لتشغيل شاشة التثبيت تلقائياً عند إعادة فتح التطبيق هو فحص
 * ApkDownloadManagerHelper.hasCompletedPendingInstall() داخل MainActivity —
 * لأن أندرويد 10 فصاعداً يمنع عموماً بدء نشاط (Activity) من الخلفية دون تفاعل
 * مستخدم مباشر، فأي محاولة هنا لفتح شاشة التثبيت فور اكتمال التنزيل والتطبيق
 * مغلق قد لا تنجح دوماً — وهذا متوقع وليس خطأً؛ الفتح التالي للتطبيق يتكفّل
 * بإتمام التثبيت تلقائياً كما هو مطلوب بالضبط.
 */
class ApkDownloadCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

        val helper = ApkDownloadManagerHelper(context.applicationContext)
        if (!helper.hasCompletedPendingInstall()) return

        // محاولة فورية (تنجح إن كان التطبيق ما يزال في الواجهة الأمامية أو حديث الخلفية)
        try {
            val installIntent = helper.buildInstallIntent() ?: return
            context.applicationContext.startActivity(installIntent)
        } catch (e: Exception) {
            // متوقع أحياناً بحسب قيود النظام؛ الفحص عند فتح التطبيق هو الشبكة الآمنة الموثوقة
        }
    }
}
