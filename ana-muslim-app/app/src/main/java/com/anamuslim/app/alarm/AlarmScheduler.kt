package com.anamuslim.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.anamuslim.app.core.ServiceLocator
import com.anamuslim.app.data.prayertimes.JafariPrayerCalculator
import com.anamuslim.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar

const val EXTRA_PRAYER_NAME = "extra_prayer_name"

/**
 * يجدول تنبيهات دقيقة (Exact Alarms) لكل صلاة مفعّلة، لليوم الحالي والغد معاً
 * (نافذة يومين تكفي مع إعادة الجدولة اليومية التلقائية أدناه). يُعاد استدعاء
 * هذا عند: فتح التطبيق، بعد كل أذان يُطلَق (يعيد جدولة الغد)، بعد إعادة تشغيل
 * الجهاز (BootCompletedReceiver)، وبعمل دوري احتياطي (DailyRefreshWorker).
 */
object AlarmScheduler {

    suspend fun rescheduleAll(context: Context) {
        val settings = ServiceLocator.settings(context)
        val prayerRepo = ServiceLocator.prayerTimes(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (dayOffset in 0..1) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
            val today = prayerRepo.getTodayTimes(cal)

            scheduleOne(context, alarmManager, cal, today.fajrMinute, "FAJR", settings.isPrayerAdhanEnabled(SettingsRepository.Prayer.FAJR).first())
            scheduleOne(context, alarmManager, cal, today.dhuhrMinute, "DHUHR", settings.isPrayerAdhanEnabled(SettingsRepository.Prayer.DHUHR).first())
            scheduleOne(context, alarmManager, cal, today.asrMinute, "ASR", settings.isPrayerAdhanEnabled(SettingsRepository.Prayer.ASR).first())
            scheduleOne(context, alarmManager, cal, today.maghribMinute, "MAGHRIB", settings.isPrayerAdhanEnabled(SettingsRepository.Prayer.MAGHRIB).first())
            scheduleOne(context, alarmManager, cal, today.ishaMinute, "ISHA", settings.isPrayerAdhanEnabled(SettingsRepository.Prayer.ISHA).first())
        }
    }

    private fun scheduleOne(
        context: Context,
        alarmManager: AlarmManager,
        dayCal: Calendar,
        minuteOfDay: Int,
        prayerKey: String,
        enabled: Boolean
    ) {
        val triggerCal = (dayCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // مُعرّف فريد لكل (يوم + صلاة) حتى لا تتصادم تنبيهات أيام مختلفة مع بعضها
        val requestCode = (triggerCal.get(Calendar.DAY_OF_YEAR) * 1000) +
            (triggerCal.get(Calendar.YEAR) % 100) * 400 + prayerKey.hashCode().mod(100)

        val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            putExtra(EXTRA_PRAYER_NAME, prayerKey)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!enabled || triggerCal.timeInMillis <= System.currentTimeMillis()) {
            alarmManager.cancel(pendingIntent)
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                // لا إذن للمنبهات الدقيقة بعد؛ نتجنّب استثناء SecurityException.
                // الواجهة (شاشة الإعدادات) تطلب من المستخدم منح هذا الإذن بوضوح.
                return
            }
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerCal.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // احتياط إضافي على بعض الأجهزة المخصّصة (custom ROMs) التي قد تفرض قيوداً إضافية
        }
    }
}
