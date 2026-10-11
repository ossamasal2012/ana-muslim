package com.anamuslim.app.data.prayertimes

/**
 * معايير حساب مواقيت الصلاة الجعفرية التي يختار بينها المستخدم من الإعدادات.
 *
 * هذا الملف لا يعتمد على أي مكتبة أندرويد (Kotlin صرف) حتى يمكن اختباره على الـ JVM مباشرة.
 *
 * مصادر القيم (تم فتحها والتحقق منها وقت كتابة هذا الملف، أكتوبر 2026):
 *  - NAJAF_ARC  : الصفحة الرسمية لحاسبة «مركز الدراسات والبحوث الفلكية» (قم)، المرتبط بمكتب السيد
 *                 السيستاني — https://arabic.nojumi.org/prayertimes — القيمتان الافتراضيتان الظاهرتان
 *                 في الحاسبة: زاوية الفجر 18°، وزاوية المغرب 4.5°. الحاسبة لا تعرض عصراً ولا عشاءً
 *                 مستقلَّين؛ وتنبّه إلى استحسان الاحتياط بدقائق في أذان الفجر.
 *  - LEVA_QOM   : https://praytimes.org/docs/methods (Jafari — Leva Research Institute, Qom)
 *                 وهي كذلك method=0 في https://api.aladhan.com/v1/methods : الفجر 16°، العشاء 14°، المغرب 4°.
 *  - TEHRAN_IGUT: https://praytimes.org/docs/methods (Tehran — Institute of Geophysics, University of Tehran)
 *                 وهي كذلك method=7 في Aladhan: الفجر 17.7°، العشاء 14°، المغرب 4.5°.
 *
 * ملاحظات مهمة:
 *  - منتصف الليل الشرعي في المعايير الثلاثة = منتصف ما بين الغروب والفجر (يحسبه المحرك).
 *  - زاوية العشاء (14°) تُستخدم فقط لعرض وقت «العشاء» الاصطلاحي كما كان في التطبيق من قبل؛
 *    فمعيار النجف (مركز الدراسات الفلكية) لا يعرض وقتاً مستقلاً للعشاء.
 *  - لا يحسم هذا الملف أي مسألة فقهية نيابةً عن المستخدم: الاختيار النهائي للمعيار هو للمستخدم ومرجعه.
 */
enum class PrayerCalcMethod {
    /** مركز الدراسات والبحوث الفلكية (المرتبط بمكتب السيد السيستاني): الفجر 18° / المغرب 4.5°. */
    NAJAF_ARC,

    /** معهد ليفا — قُم (Jafari في PrayTimes): الفجر 16° / المغرب 4° / العشاء 14°. */
    LEVA_QOM,

    /** معهد الجيوفيزياء — جامعة طهران: الفجر 17.7° / المغرب 4.5° / العشاء 14°. */
    TEHRAN_IGUT;

    companion object {
        /** المعيار الافتراضي عند أول استخدام (تغييره هنا يغيّر الافتراضي في كل التطبيق). */
        val DEFAULT: PrayerCalcMethod = NAJAF_ARC

        /** يقرأ القيمة المحفوظة؛ أي قيمة مجهولة (مثلاً من إصدار مستقبلي) تعود بأمان إلى الافتراضي. */
        fun fromStored(stored: String?): PrayerCalcMethod =
            entries.firstOrNull { it.name == stored } ?: DEFAULT
    }
}

/**
 * القيم العددية التي يحتاجها المحرك لمعيار معيّن (زوايا الشمس تحت الأفق بالدرجات).
 *
 * @property fajrPrecautionAngle زاوية أقل للفجر تُعرض كوقت «احتياط» إضافي (null = لا يوجد).
 */
data class CalcProfile(
    val fajrAngle: Double,
    val maghribAngle: Double,
    val ishaAngle: Double,
    val fajrPrecautionAngle: Double? = null
) {
    companion object {
        val NAJAF_ARC = CalcProfile(
            fajrAngle = 18.0,
            maghribAngle = 4.5,
            ishaAngle = 14.0,
            fajrPrecautionAngle = 16.0 // نظير معيار ليفا؛ يتأخر عن 18° بنحو 9.5–13 دقيقة في كربلاء
        )
        val LEVA_QOM = CalcProfile(fajrAngle = 16.0, maghribAngle = 4.0, ishaAngle = 14.0)
        val TEHRAN_IGUT = CalcProfile(fajrAngle = 17.7, maghribAngle = 4.5, ishaAngle = 14.0)

        val DEFAULT: CalcProfile get() = forMethod(PrayerCalcMethod.DEFAULT)

        fun forMethod(method: PrayerCalcMethod): CalcProfile = when (method) {
            PrayerCalcMethod.NAJAF_ARC -> NAJAF_ARC
            PrayerCalcMethod.LEVA_QOM -> LEVA_QOM
            PrayerCalcMethod.TEHRAN_IGUT -> TEHRAN_IGUT
        }
    }
}
