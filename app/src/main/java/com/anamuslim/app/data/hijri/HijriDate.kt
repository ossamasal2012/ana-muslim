package com.anamuslim.app.data.hijri

import java.util.Calendar

/**
 * تحويل التاريخ الميلادي ⇄ الهجري بالحساب الفلكي الجدولي القياسي (Tabular Islamic
 * Calendar)، يعمل بالكامل على الجهاز دون إنترنت.
 *
 * ⚠️ ملاحظة أمانة علمية مهمة: أي تقويم هجري "محسوب" رياضياً (وليس مبنياً على رؤية
 * هلال فعلية معلنة محلياً) قد يختلف يوماً واحداً عن الإعلان الرسمي لبداية الشهر في
 * بعض الأحيان، خصوصاً في المناسبات الدينية المهمة. لذلك يجب عرض هذا التنبيه للمستخدم
 * (راجع R.string.hijri_disclaimer) وعدم الجزم بأنه "المصدر الشرعي النهائي".
 */
data class HijriDate(val year: Int, val month: Int, val day: Int) {
    val monthName: String get() = HIJRI_MONTH_NAMES[month - 1]

    companion object {
        val HIJRI_MONTH_NAMES = listOf(
            "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
            "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
        )

        /** يحوّل تاريخاً ميلادياً إلى هجري وفق التقويم الجعفري المعتمد في التطبيق. */
        fun fromGregorian(year: Int, month: Int, day: Int): HijriDate {
            // التصحيح المعتمد للتقويم الشيعي: التاريخ المحسوب هنا متأخر يوماً واحداً.
            val jdn = gregorianToJdn(year, month, day) + 1
            return jdnToHijri(jdn)
        }

        fun fromCalendar(cal: Calendar): HijriDate =
            fromGregorian(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))

        /** يحوّل تاريخاً هجرياً إلى ميلادي (year, month, day) — مفيد لحساب "متى تأتي" مناسبة قادمة. */
        fun toGregorian(hYear: Int, hMonth: Int, hDay: Int): Triple<Int, Int, Int> {
            // نعكس تصحيح اليوم الواحد المطبق عند التحويل من الميلادي.
            val jdn = hijriToJdn(hYear, hMonth, hDay) - 1
            return jdnToGregorian(jdn)
        }

        private fun gregorianToJdn(year: Int, month: Int, day: Int): Long {
            val a = (14 - month) / 12
            val y = year + 4800 - a
            val m = month + 12 * a - 3
            return day + (153L * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045
        }

        private fun jdnToGregorian(jdnIn: Long): Triple<Int, Int, Int> {
            val jdn = jdnIn
            val a = jdn + 32044
            val b = (4 * a + 3) / 146097
            val c = a - (146097 * b) / 4
            val d = (4 * c + 3) / 1461
            val e = c - (1461 * d) / 4
            val m = (5 * e + 2) / 153
            val day = (e - (153 * m + 2) / 5 + 1).toInt()
            val month = (m + 3 - 12 * (m / 10)).toInt()
            val year = (100 * b + d - 4800 + m / 10).toInt()
            return Triple(year, month, day)
        }

        // معادلات التقويم الهجري الجدولي القياسي (Civil / Tabular Islamic calendar)
        private const val ISLAMIC_EPOCH_JDN = 1948440L

        private fun jdnToHijri(jdnIn: Long): HijriDate {
            var l = jdnIn - ISLAMIC_EPOCH_JDN + 10632
            val n = (l - 1) / 10631
            l = l - 10631 * n + 354
            val j = ((10985 - l) / 5316) * ((50 * l) / 17719) + (l / 5670) * ((43 * l) / 15238)
            l = l - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
            val month = ((24 * l) / 709).toInt()
            val day = (l - (709L * month) / 24).toInt()
            val year = (30 * n + j - 30).toInt()
            return HijriDate(year, month, day)
        }

        private fun hijriToJdn(year: Int, month: Int, day: Int): Long {
            return day + Math.ceil(29.5 * (month - 1)).toLong() + (year - 1).toLong() * 354 +
                Math.floor((3 + 11L * year) / 30.0).toLong() + ISLAMIC_EPOCH_JDN - 1
        }
    }
}
