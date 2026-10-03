package com.anamuslim.app.data.prayertimes

import com.anamuslim.app.data.prayertimes.JafariPrayerCalculator.roundToMinuteOfDay
import com.anamuslim.app.data.settings.SettingsRepository
import com.anamuslim.app.data.settings.TimeFormatPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar

/** مواقيت يوم واحد بصيغة "ساعة:دقيقة" جاهزة للعرض + دقيقة اليوم كرقم صحيح لأغراض الجدولة. */
data class PrayerTimesDisplay(
    val dateLabel: String,
    val imsakMinute: Int,
    val fajrMinute: Int,
    val sunriseMinute: Int,
    val dhuhrMinute: Int,
    val asrMinute: Int,
    val maghribMinute: Int,
    val ishaMinute: Int,
    val midnightMinute: Int
) {
    companion object {
        /**
         * يحوّل دقيقة اليوم (0..1439 — رقم صحيح لا علاقة له بطريقة العرض) إلى نص
         * جاهز للعرض حسب تفضيل المستخدم. هذه الدالة هي نقطة العرض الوحيدة؛ لا تُغيَّر
         * الحسابات الفعلية (minuteOfDay) أبداً بسبب تغيير هذا التفضيل، فقط شكل النص.
         *
         * - 24 ساعة: نفس التنسيق الأصلي تماماً بلا أي تغيير 00:00..23:59.
         * - 12 ساعة: تنسيق عربي واضح "س:دد ص/م" (ص = صباحاً، م = مساءً)، الساعة 1..12.
         */
        fun formatMinute(
            minuteOfDay: Int,
            format: TimeFormatPreference = TimeFormatPreference.HOUR_12
        ): String {
            val h24 = (minuteOfDay / 60) % 24
            val m = minuteOfDay % 60
            return when (format) {
                TimeFormatPreference.HOUR_24 -> "%02d:%02d".format(h24, m)
                TimeFormatPreference.HOUR_12 -> {
                    val period = if (h24 < 12) "ص" else "م"
                    val h12 = when (val h = h24 % 12) {
                        0 -> 12
                        else -> h
                    }
                    "%d:%02d %s".format(h12, m, period)
                }
            }
        }
    }
}

enum class PrayerName { FAJR, DHUHR, ASR, MAGHRIB, ISHA }

data class NextPrayerInfo(val prayer: PrayerName, val minutesRemaining: Int, val atMinuteOfDay: Int)

class PrayerTimesRepository(
    private val settings: SettingsRepository,
    private val locationHelper: LocationHelper
) {

    /** يحسب مواقيت اليوم الحالي بناءً على الموقع المحفوظ (أو موقع افتراضي إن لم يوجد بعد). */
    suspend fun getTodayTimes(referenceCalendar: Calendar = Calendar.getInstance()): PrayerTimesDisplay =
        withContext(Dispatchers.Default) {
            val loc = settings.savedLocation.first()
            val lat = loc?.latitude ?: 32.6160 // كربلاء كموقع افتراضي منطقي إلى حين تحديد موقع المستخدم
            val lng = loc?.longitude ?: 44.0249
            val tz = loc?.timeZoneOffsetHours ?: locationHelper.currentUtcOffsetHours()

            val result = JafariPrayerCalculator.calculate(
                year = referenceCalendar.get(Calendar.YEAR),
                month = referenceCalendar.get(Calendar.MONTH) + 1,
                day = referenceCalendar.get(Calendar.DAY_OF_MONTH),
                latitude = lat,
                longitude = lng,
                timeZoneOffsetHours = tz
            )

            PrayerTimesDisplay(
                dateLabel = "%04d-%02d-%02d".format(
                    referenceCalendar.get(Calendar.YEAR),
                    referenceCalendar.get(Calendar.MONTH) + 1,
                    referenceCalendar.get(Calendar.DAY_OF_MONTH)
                ),
                imsakMinute = result.imsak.roundToMinuteOfDay(),
                fajrMinute = result.fajr.roundToMinuteOfDay(),
                sunriseMinute = result.sunrise.roundToMinuteOfDay(),
                dhuhrMinute = result.dhuhr.roundToMinuteOfDay(),
                asrMinute = result.asr.roundToMinuteOfDay(),
                maghribMinute = result.maghrib.roundToMinuteOfDay(),
                ishaMinute = result.isha.roundToMinuteOfDay(),
                midnightMinute = result.midnight.roundToMinuteOfDay()
            )
        }

    /** يحدد الصلاة القادمة والوقت المتبقي لها بالدقائق، بالاعتماد على وقت اليوم الحالي. */
    suspend fun getNextPrayer(now: Calendar = Calendar.getInstance()): NextPrayerInfo = withContext(Dispatchers.Default) {
        val today = getTodayTimes(now)
        val nowMinute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val ordered = listOf(
            PrayerName.FAJR to today.fajrMinute,
            PrayerName.DHUHR to today.dhuhrMinute,
            PrayerName.ASR to today.asrMinute,
            PrayerName.MAGHRIB to today.maghribMinute,
            PrayerName.ISHA to today.ishaMinute
        )

        val upcoming = ordered.firstOrNull { it.second > nowMinute }
        return@withContext if (upcoming != null) {
            NextPrayerInfo(upcoming.first, upcoming.second - nowMinute, upcoming.second)
        } else {
            // كل صلوات اليوم انتهت؛ القادمة هي فجر الغد
            val tomorrow = now.clone() as Calendar
            tomorrow.add(Calendar.DAY_OF_YEAR, 1)
            val tomorrowTimes = getTodayTimes(tomorrow)
            val minutesUntilMidnight = (24 * 60) - nowMinute
            NextPrayerInfo(PrayerName.FAJR, minutesUntilMidnight + tomorrowTimes.fajrMinute, tomorrowTimes.fajrMinute)
        }
    }
}
