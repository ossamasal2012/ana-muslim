package com.anamuslim.app.data.hijri

import java.util.Calendar

data class ShiaOccasion(val hijriMonth: Int, val hijriDay: Int, val title: String, val isMourning: Boolean = false)

/**
 * قائمة بأبرز المناسبات الدينية عند الشيعة الإثني عشرية، مرتبطة بالشهر واليوم
 * الهجريين فقط (تعمل بالكامل دون إنترنت).
 *
 * تم بناء هذه النسخة بمطابقة كل تاريخ فعلياً مقابل مقالة ويكيبيديا المخصصة
 * "Twelver Shia holy days" (وليس من الذاكرة فقط)، وصُححت فيها أخطاء كانت موجودة
 * في مسودة أولى (كانت تضع استشهاد الإمام الرضا خطأً في ١ ربيع الأول).
 *
 * ⚠️ بعض التواريخ فيها خلاف روائي حقيقي موثّق حتى داخل المصدر نفسه (مثال:
 * استشهاد الإمام الرضا رُوي في ١٧ صفر و٢٩ صفر و٣٠ صفر في مصادر مختلفة؛ استشهاد
 * الإمام الهادي رُوي في ٢٦ جمادى الآخرة و٣ رجب) — تم اختيار الرواية الأكثر
 * شيوعاً في الاستخدام المعاصر مع توثيق البديل بالتعليق. يُنصح بمراجعة عالم دين
 * موثوق قبل اعتماد القائمة نهائياً في إصدار عام واسع الانتشار.
 */
class ShiaOccasionsRepository {

    private val occasions = listOf(
        // محرم
        ShiaOccasion(1, 1, "رأس السنة الهجرية"),
        ShiaOccasion(1, 9, "تاسوعاء"),
        ShiaOccasion(1, 10, "عاشوراء - استشهاد الإمام الحسين (ع)", isMourning = true),
        ShiaOccasion(1, 25, "استشهاد الإمام زين العابدين (ع)", isMourning = true),

        // صفر
        ShiaOccasion(2, 5, "وفاة السيدة رقية بنت الحسين (ع)", isMourning = true),
        ShiaOccasion(2, 7, "استشهاد الإمام الحسن (ع)", isMourning = true),
        ShiaOccasion(2, 20, "أربعين الإمام الحسين (ع)", isMourning = true),
        ShiaOccasion(2, 28, "وفاة الرسول الأكرم (ص)", isMourning = true),
        // رواية الاستشهاد الأشهر في الاستخدام المعاصر (٢٩ صفر)؛ رُوي أيضاً ١٧ و٣٠ صفر في مصادر أخرى
        ShiaOccasion(2, 29, "استشهاد الإمام علي الرضا (ع)", isMourning = true),

        // ربيع الأول
        ShiaOccasion(3, 8, "استشهاد الإمام الحسن العسكري (ع)", isMourning = true),
        ShiaOccasion(3, 9, "عيد التمسك بولاية أهل البيت (ع)"),
        ShiaOccasion(3, 17, "مولد النبي محمد (ص) والإمام جعفر الصادق (ع)"),

        // ربيع الآخر
        ShiaOccasion(4, 8, "مولد الإمام الحسن العسكري (ع)"),
        ShiaOccasion(4, 10, "وفاة السيدة فاطمة المعصومة (ع)", isMourning = true),

        // جمادى الأولى
        ShiaOccasion(5, 5, "مولد السيدة زينب (ع)"),
        ShiaOccasion(5, 13, "بدء أيام فاطمية الأولى (استشهاد الزهراء ع)", isMourning = true),

        // جمادى الآخرة
        ShiaOccasion(6, 3, "استشهاد السيدة فاطمة الزهراء (ع)", isMourning = true),
        ShiaOccasion(6, 20, "مولد السيدة فاطمة الزهراء (ع) (رواية أخرى)"),

        // رجب
        ShiaOccasion(7, 1, "مولد الإمام محمد الباقر (ع)"),
        // رواية الاستشهاد الأشهر (٣ رجب)؛ رُوي أيضاً ٢٦ جمادى الآخرة في مصادر أخرى
        ShiaOccasion(7, 3, "استشهاد الإمام علي الهادي (ع)", isMourning = true),
        ShiaOccasion(7, 10, "مولد الإمام محمد الجواد (ع)"),
        ShiaOccasion(7, 13, "مولد الإمام علي (ع)"),
        ShiaOccasion(7, 15, "استشهاد السيدة زينب (ع) (رواية)", isMourning = true),
        ShiaOccasion(7, 25, "استشهاد الإمام موسى الكاظم (ع)", isMourning = true),
        ShiaOccasion(7, 27, "المبعث النبوي الشريف"),

        // شعبان
        ShiaOccasion(8, 3, "مولد الإمام الحسين (ع)"),
        ShiaOccasion(8, 4, "مولد أبي الفضل العباس (ع)"),
        ShiaOccasion(8, 5, "مولد الإمام زين العابدين (ع)"),
        ShiaOccasion(8, 15, "مولد الإمام المهدي المنتظر (عج)"),

        // رمضان
        ShiaOccasion(9, 15, "مولد الإمام الحسن (ع)"),
        ShiaOccasion(9, 19, "ليلة القدر الأولى - ضربة الإمام علي (ع)", isMourning = true),
        ShiaOccasion(9, 21, "استشهاد الإمام علي (ع)", isMourning = true),
        ShiaOccasion(9, 23, "ليلة القدر الثالثة"),

        // شوال
        ShiaOccasion(10, 1, "عيد الفطر"),
        ShiaOccasion(10, 25, "استشهاد الإمام جعفر الصادق (ع)", isMourning = true),

        // ذو القعدة
        ShiaOccasion(11, 1, "مولد السيدة فاطمة المعصومة (ع)"),
        ShiaOccasion(11, 11, "مولد الإمام علي الرضا (ع)"),
        ShiaOccasion(11, 25, "يوم دحو الأرض"),
        ShiaOccasion(11, 30, "استشهاد الإمام محمد الجواد (ع)", isMourning = true),

        // ذو الحجة
        ShiaOccasion(12, 1, "زواج الإمام علي والسيدة فاطمة الزهراء (ع)"),
        ShiaOccasion(12, 7, "استشهاد الإمام محمد الباقر (ع)", isMourning = true),
        ShiaOccasion(12, 9, "يوم عرفة"),
        ShiaOccasion(12, 10, "عيد الأضحى"),
        ShiaOccasion(12, 15, "مولد الإمام علي الهادي (ع)"),
        ShiaOccasion(12, 18, "عيد الغدير - نصب الإمام علي (ع) خليفة"),
        ShiaOccasion(12, 24, "يوم المباهلة")
    )

    data class UpcomingOccasion(val occasion: ShiaOccasion, val gregorianDate: Calendar, val daysRemaining: Int)

    /** يعيد قائمة أقرب المناسبات القادمة (افتراضياً 5) بدءاً من اليوم، محسوبة محلياً بلا إنترنت. */
    fun upcomingOccasions(count: Int = 5, from: Calendar = Calendar.getInstance()): List<UpcomingOccasion> {
        val todayHijri = HijriDate.fromCalendar(from)
        val results = mutableListOf<UpcomingOccasion>()

        // نبحث ضمن السنتين الهجريتين الحاليتين والتاليتين لضمان تغطية كل الحالات القريبة من نهاية السنة
        for (yearOffset in 0..2) {
            val hijriYear = todayHijri.year + yearOffset
            for (occ in occasions) {
                val (gy, gm, gd) = HijriDate.toGregorian(hijriYear, occ.hijriMonth, occ.hijriDay)
                val cal = Calendar.getInstance().apply {
                    clear()
                    set(gy, gm - 1, gd)
                }
                val diffDays = ((cal.timeInMillis - stripTime(from).timeInMillis) / 86_400_000L).toInt()
                if (diffDays >= 0) {
                    results.add(UpcomingOccasion(occ, cal, diffDays))
                }
            }
        }
        return results.sortedBy { it.daysRemaining }.take(count)
    }

    private fun stripTime(cal: Calendar): Calendar = (cal.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
}
