package com.anamuslim.app

import android.app.Application
import com.anamuslim.app.alarm.AlarmScheduler
import com.anamuslim.app.alarm.DailyRefreshWorker
import com.anamuslim.app.core.ServiceLocator
import com.anamuslim.app.data.update.ApkDownloadManagerHelper
import com.anamuslim.app.notifications.NotificationHelper
import com.anamuslim.app.security.IntegrityChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AnaMuslimApp : Application() {

    private val appScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        NotificationHelper.createChannels(this)

        // إصلاح مشكلة "التحديث ما زال يظهر بعد التثبيت": إن كانت هناك نسخة
        // تحديث كانت "معلّقة" التثبيت، وأصبح BuildConfig.VERSION_CODE الحالي
        // يساويها أو أكبر منها، فهذا يعني أن التثبيت نجح فعلاً والآن نشغّل هذه
        // النسخة الجديدة بالضبط — عندها ننظّف كل أثر لملف APK القديم فوراً.
        val downloadHelper = ApkDownloadManagerHelper(this)
        val prefs = getSharedPreferences("update_prefs", MODE_PRIVATE)
        val pendingVersionCode = prefs.getInt("pending_version_code", -1)
        if (pendingVersionCode in 1..BuildConfig.VERSION_CODE) {
            downloadHelper.cleanupAfterSuccessfulInstall()
        }

        appScope.launch {
            ServiceLocator.tasbih(this@AnaMuslimApp).ensureSeeded(defaultTasbihNames())
            AlarmScheduler.rescheduleAll(this@AnaMuslimApp)
        }

        DailyRefreshWorker.schedule(this)

        // فحص توقيع خفيف وغير معطِّل في الخلفية (راجع IntegrityChecker.kt لتفاصيل
        // حدوده المهمة) — لا يؤخر الإقلاع ولا يوقف التطبيق أبداً مهما كانت نتيجته.
        appScope.launch {
            IntegrityChecker.verifySigningCertificate(this@AnaMuslimApp)
        }
    }

    private fun defaultTasbihNames(): List<String> = listOf(
        getString(R.string.tasbih_default_1),
        getString(R.string.tasbih_default_2),
        getString(R.string.tasbih_default_3),
        getString(R.string.tasbih_default_4),
        getString(R.string.tasbih_default_5),
        getString(R.string.tasbih_default_6),
        getString(R.string.tasbih_default_7),
        getString(R.string.tasbih_default_8),
        getString(R.string.tasbih_default_9),
        getString(R.string.tasbih_default_10)
    )
}
