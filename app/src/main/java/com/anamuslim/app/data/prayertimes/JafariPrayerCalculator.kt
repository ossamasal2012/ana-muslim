package com.anamuslim.app.data.prayertimes

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * محرك حساب مواقيت الصلاة، يعمل بالكامل على الجهاز دون أي اتصال إنترنت.
 *
 * منقول إلى Kotlin عن مكتبة PrayTime الإصدار 3.2 (https://praytimes.org — github.com/zarrabi/praytime)
 *   Copyright (c) 2007-2025 Hamid Zarrabi-Zadeh
 *   رخصة MIT — النص الكامل في licenses/PrayTime-MIT.txt (يجب الإبقاء على هذا الإشعار).
 * وهي الخوارزمية نفسها التي تقوم عليها Aladhan API (بحسب صفحة الشروط لديهم)، فلا يضيف الاتصال
 * بها دقة إضافية، بل اعتماداً على الشبكة فقط.
 *
 * ما الذي تم التحقق منه (أكتوبر 2026):
 *  - مطابقة 100% (دقيقة بدقيقة) مع PrayTime v3.2 الأصلي (JavaScript) على إحداثيات كربلاء والعتبتين
 *    ومع المحرك السابق للتطبيق، وعلى مدن متعددة وخطوط عرض من -60° إلى +66° (انظر اختبارات الوحدة).
 *  - الفرق عن حساب فلكي مستقل (PyEphem/VSOP87) في كربلاء لا يتجاوز 7 ثوانٍ، فالدقة الفلكية ليست
 *    هي المحدِّد؛ المحدِّد هو المعيار المختار (زوايا الفجر والمغرب) — راجع CalcProfile.
 *
 * التعريفات المعتمدة:
 *  - الفجر/العشاء/المغرب: لحظة بلوغ مركز الشمس زاوية معيّنة تحت الأفق (تأتي الزوايا من CalcProfile).
 *  - الغروب والشروق: الارتفاع -0.833° (انكسار جوي + نصف قطر القرص) للمستوى البحري.
 *  - الظهر: عبور الشمس للزوال (منتصف النهار الفلكي).
 *  - العصر: ظل الشيء = طوله (اصطلاح PrayTime «Standard»)، وهو اصطلاحي وليس تحديداً فقهياً.
 *  - منتصف الليل الشرعي: منتصف ما بين غروب الشمس وفجر اليوم التالي (نفس تعريف PrayTime «Jafari»).
 *
 * خطوط العرض العالية: قاعدة «منتصف الليل» (NightMiddle) كما في PrayTime، فلا يتجاوز الفجر/العشاء/المغرب
 * نصف طول الليل. وفي المناطق التي لا شروق ولا غروب فيها تكون القيمة NaN (تُعرض «غير متوفر»).
 */
object JafariPrayerCalculator {

    private const val HORIZON_ANGLE = 0.833        // درجة: الشروق/الغروب الظاهريان
    private const val IMSAK_OFFSET_MIN = 10.0      // دقائق قبل الفجر (تقليدي، للاحتياط)
    private const val ASR_SHADOW_FACTOR = 1.0      // ظل الشيء = طوله (Standard)
    private const val NIGHT_MIDDLE_PORTION = 0.5   // قاعدة NightMiddle الافتراضية

    /** نتيجة الحساب لمواعيد يوم واحد، كدقائق من منتصف الليل (0..1440). NaN = غير متوفر. */
    data class DayTimesMinutes(
        val imsak: Double,
        val fajr: Double,
        val sunrise: Double,
        val dhuhr: Double,
        val asr: Double,
        val sunset: Double,
        val maghrib: Double,
        val isha: Double,
        val midnight: Double,
        /** الفجر عند زاوية الاحتياط في المعيار المختار، أو NaN إن لم يكن للمعيار زاوية احتياط. */
        val fajrPrecaution: Double = Double.NaN
    )

    /**
     * يحسب مواقيت يوم واحد.
     * @param year/month/day تاريخ ميلادي
     * @param latitude خط العرض (موجب شمالاً)
     * @param longitude خط الطول (موجب شرقاً)
     * @param timeZoneOffsetHours فارق التوقيت عن UTC بالساعات (مثال: العراق = 3.0 طوال العام)
     * @param elevationMeters ارتفاع الموقع (اختياري؛ التطبيق يمرّر 0 لأن المعايير المنشورة بمستوى البحر)
     * @param profile زوايا المعيار المختار (الافتراضي: معيار التطبيق الافتراضي)
     */
    fun calculate(
        year: Int,
        month: Int,
        day: Int,
        latitude: Double,
        longitude: Double,
        timeZoneOffsetHours: Double,
        elevationMeters: Double = 0.0,
        profile: CalcProfile = CalcProfile.DEFAULT
    ): DayTimesMinutes {
        val sun = SunModel(daysSinceJ2000(year, month, day), latitude, longitude)
        val horizon = HORIZON_ANGLE + 0.0347 * sqrt(max(elevationMeters, 0.0))

        // القيم الأولية بالساعات الشمسية المحلية (نقطة الانطلاق نفسها في PrayTime.processTimes)
        var fajr = sun.angleTime(profile.fajrAngle, 5.0, -1)
        val sunrise = sun.angleTime(horizon, 6.0, -1)
        val dhuhr = sun.midDay(12.0)
        val asr = sun.angleTime(sun.asrAngle(ASR_SHADOW_FACTOR, 13.0), 13.0, 1)
        val sunset = sun.angleTime(horizon, 18.0, 1)
        var maghrib = sun.angleTime(profile.maghribAngle, 18.0, 1)
        var isha = sun.angleTime(profile.ishaAngle, 18.0, 1)
        val precautionAngle = profile.fajrPrecautionAngle
        var fajrPrecaution = if (precautionAngle != null) sun.angleTime(precautionAngle, 5.0, -1) else Double.NaN

        // تعديل خطوط العرض العالية (NightMiddle) — PrayTime.adjustHighLats / adjustTime
        var adjusted = false
        val night = 24.0 + sunrise - sunset
        val portion = NIGHT_MIDDLE_PORTION * night
        fun needsClamp(time: Double, base: Double, direction: Int): Boolean =
            time.isNaN() || (time - base) * direction > portion
        fun adjustTracked(time: Double, base: Double, direction: Int): Double =
            if (needsClamp(time, base, direction)) {
                adjusted = true
                base + portion * direction
            } else time

        fajr = adjustTracked(fajr, sunrise, -1)
        isha = adjustTracked(isha, sunset, 1)
        maghrib = adjustTracked(maghrib, sunset, 1)
        if (precautionAngle != null && needsClamp(fajrPrecaution, sunrise, -1)) {
            fajrPrecaution = sunrise - portion // لا يؤثر على علَم adjusted الخاص بمنتصف الليل
        }

        // منتصف الليل الجعفري: منتصف ما بين الغروب وفجر اليوم التالي (فجر الغد الفعلي، كما في v3.2)
        val nextFajr = sun.angleTime(profile.fajrAngle, 29.0, -1) + 24.0
        val midnight = (sunset + (if (adjusted) fajr + 24.0 else nextFajr)) / 2.0

        // التحويل إلى التوقيت المحلي ثم إلى دقائق من منتصف الليل
        val tzAdjust = timeZoneOffsetHours - longitude / 15.0
        fun minutes(solarHours: Double): Double = mod(solarHours + tzAdjust, 24.0) * 60.0

        return DayTimesMinutes(
            imsak = minutes(fajr - IMSAK_OFFSET_MIN / 60.0),
            fajr = minutes(fajr),
            sunrise = minutes(sunrise),
            dhuhr = minutes(dhuhr),
            asr = minutes(asr),
            sunset = minutes(sunset),
            maghrib = minutes(maghrib),
            isha = minutes(isha),
            midnight = minutes(midnight),
            fajrPrecaution = minutes(fajrPrecaution)
        )
    }

    /**
     * يحوّل «دقيقة من اليوم» (كسرية) إلى رقم دقيقة صحيح للعرض/الجدولة بالتقريب لأقرب دقيقة
     * (نصف دقيقة فأكثر يُقرَّب للأعلى) — نفس تقريب PrayTime الافتراضي (nearest).
     * يعيد -1 إذا كانت القيمة غير متوفرة (مثل الشروق/الغروب في المناطق القطبية).
     */
    fun Double.roundToMinuteOfDay(): Int {
        if (this.isNaN()) return -1
        val rounded = mod(this / 60.0 + 0.5 / 60.0, 24.0) * 60.0
        return floor(rounded).toInt().coerceIn(0, 1439)
    }

    /** أيام منذ J2000.0 (2000-01-01 12:00 UTC) عند منتصف ليل UTC لليوم المطلوب. */
    private fun daysSinceJ2000(year: Int, month: Int, day: Int): Double {
        // days_from_civil (خوارزمية معروفة في الملكية العامة): أيام منذ 1970-01-01
        val y = if (month <= 2) year - 1 else year
        val era = if (y >= 0) y / 400 else (y - 399) / 400
        val yoe = y - era * 400
        val mp = (month + 9) % 12 // مارس = 0
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        val epochDays = era * 146097 + doe - 719468
        return epochDays - 10957.5 // 1970-01-01 → J2000.0
    }
}

// ---------------------------------------------------------------------------------------------
// نموذج موضع الشمس (معادلات فلكية مبسّطة، انظر PrayTime.sunPosition) — دقة أفضل من 0.01°
// ---------------------------------------------------------------------------------------------

private class SunModel(
    private val daysAtUtcMidnight: Double,
    private val lat: Double,
    private val lng: Double
) {
    /** يعيد (ميل الشمس بالدرجات، معادلة الزمن بالساعات) عند الساعة المحلية الشمسية `time`. */
    private fun sunPosition(time: Double): Pair<Double, Double> {
        val d = daysAtUtcMidnight + time / 24.0 - lng / 360.0
        val g = mod(357.529 + 0.98560028 * d, 360.0)
        val q = mod(280.459 + 0.98564736 * d, 360.0)
        val l = mod(q + 1.915 * sinD(g) + 0.020 * sinD(2 * g), 360.0)
        val e = 23.439 - 0.00000036 * d
        val ra = mod(atan2D(cosD(e) * sinD(l), cosD(l)) / 15.0, 24.0)
        return asinD(sinD(e) * sinD(l)) to (q / 15.0 - ra)
    }

    fun midDay(time: Double): Double = mod(12.0 - sunPosition(time).second, 24.0)

    /** لحظة بلوغ الشمس زاوية `angle` تحت الأفق؛ direction = -1 صباحاً، +1 مساءً. NaN إن لم تبلغها. */
    fun angleTime(angle: Double, time: Double, direction: Int): Double {
        val decl = sunPosition(time).first
        val ratio = (-sinD(angle) - sinD(lat) * sinD(decl)) / (cosD(lat) * cosD(decl))
        if (ratio.isNaN() || ratio < -1.0 || ratio > 1.0) return Double.NaN
        return midDay(time) + acosD(ratio) / 15.0 * direction
    }

    /** زاوية الشمس عندما يساوي الظل (shadowFactor × طول الشيء) + ظل الزوال. */
    fun asrAngle(shadowFactor: Double, time: Double): Double {
        val decl = sunPosition(time).first
        return -arccotD(shadowFactor + tanD(abs(lat - decl)))
    }
}

private fun mod(a: Double, b: Double): Double = ((a % b) + b) % b
private fun sinD(d: Double): Double = sin(d * PI / 180.0)
private fun cosD(d: Double): Double = cos(d * PI / 180.0)
private fun tanD(d: Double): Double = tan(d * PI / 180.0)
private fun asinD(x: Double): Double = asin(x) * 180.0 / PI
private fun acosD(x: Double): Double = acos(x) * 180.0 / PI
private fun arccotD(x: Double): Double = atan(1.0 / x) * 180.0 / PI
private fun atan2D(y: Double, x: Double): Double = atan2(y, x) * 180.0 / PI
