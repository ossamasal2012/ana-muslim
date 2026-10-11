package com.anamuslim.app.data.prayertimes

import com.anamuslim.app.data.prayertimes.JafariPrayerCalculator.roundToMinuteOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * اختبارات وحدة (JVM) لمحرك المواقيت ومعاييره — لا تحتاج أي مكتبة أندرويد.
 *
 * مرجعان مستقلان:
 *  1) karbala_reference_2026_2027.csv : جدول سنوي للعتبة الحسينية محسوب بمكتبة PyEphem (VSOP87) —
 *     مستقل تماماً عن خوارزمية PrayTime. أنشأه tools/verify_karbala_prayer_times.py
 *  2) legacy_engine_baseline.csv : مخرجات المحرك السابق للتطبيق (قبل الانتقال إلى PrayTime v3.2).
 *     يضمن أن معيار ليفا لم يتغيّر وأن الانتقال إلى المحرك الجديد لم يكسر شيئاً.
 */
class JafariPrayerCalculatorTest {

    private val husaynLat = 32.61639
    private val husaynLng = 44.03250
    private val iraqTz = 3.0 // Asia/Baghdad = UTC+3 طوال العام

    private fun minutes(hhmm: String): Int = hhmm.substring(0, 2).toInt() * 60 + hhmm.substring(3, 5).toInt()

    private fun readCsv(name: String): List<List<String>> {
        val stream = javaClass.classLoader!!.getResourceAsStream(name) ?: error("ملف اختبار مفقود: $name")
        return stream.bufferedReader(Charsets.UTF_8).readLines()
            .drop(1).filter { it.isNotBlank() }.map { it.trim().split(",") }
    }

    /** فرق دائري على ساعة 24 (حتى لا يفشل الاختبار لمنتصف ليل قرب 00:00). */
    private fun assertWithin(label: String, expected: Int, actual: Int, tolerance: Int) {
        val raw = abs(expected - actual)
        val diff = minOf(raw, 1440 - raw)
        assertTrue("$label: المتوقع=$expected الفعلي=$actual (التسامح ±$tolerance دقيقة)", diff <= tolerance)
    }

    @Test
    fun profilesUseTheDocumentedAngles() {
        assertEquals(18.0, CalcProfile.NAJAF_ARC.fajrAngle, 0.0)
        assertEquals(4.5, CalcProfile.NAJAF_ARC.maghribAngle, 0.0)
        assertEquals(16.0, CalcProfile.NAJAF_ARC.fajrPrecautionAngle!!, 0.0)
        assertEquals(16.0, CalcProfile.LEVA_QOM.fajrAngle, 0.0)
        assertEquals(4.0, CalcProfile.LEVA_QOM.maghribAngle, 0.0)
        assertEquals(14.0, CalcProfile.LEVA_QOM.ishaAngle, 0.0)
        assertEquals(17.7, CalcProfile.TEHRAN_IGUT.fajrAngle, 0.0)
        assertEquals(4.5, CalcProfile.TEHRAN_IGUT.maghribAngle, 0.0)
    }

    @Test
    fun defaultMethodIsNajafAndUnknownStoredValueFallsBack() {
        assertEquals(PrayerCalcMethod.NAJAF_ARC, PrayerCalcMethod.DEFAULT)
        assertEquals(PrayerCalcMethod.NAJAF_ARC, PrayerCalcMethod.fromStored(null))
        assertEquals(PrayerCalcMethod.NAJAF_ARC, PrayerCalcMethod.fromStored("قيمة-من-إصدار-مستقبلي"))
        assertEquals(PrayerCalcMethod.LEVA_QOM, PrayerCalcMethod.fromStored("LEVA_QOM"))
        assertEquals(PrayerCalcMethod.TEHRAN_IGUT, PrayerCalcMethod.fromStored("TEHRAN_IGUT"))
    }

    @Test
    fun najafProfileMatchesIndependentAstronomicalReference() {
        val rows = readCsv("karbala_reference_2026_2027.csv")
        assertTrue(rows.size >= 700)
        for (c in rows) {
            val (y, m, d) = c[0].split("-").map { it.toInt() }
            val t = JafariPrayerCalculator.calculate(y, m, d, husaynLat, husaynLng, iraqTz, profile = CalcProfile.NAJAF_ARC)
            assertWithin("${c[0]} الفجر 18°", minutes(c[2]), t.fajr.roundToMinuteOfDay(), 1)
            assertWithin("${c[0]} الشروق", minutes(c[3]), t.sunrise.roundToMinuteOfDay(), 1)
            assertWithin("${c[0]} الظهر", minutes(c[4]), t.dhuhr.roundToMinuteOfDay(), 1)
            assertWithin("${c[0]} الغروب", minutes(c[5]), t.sunset.roundToMinuteOfDay(), 1)
            assertWithin("${c[0]} المغرب 4.5°", minutes(c[7]), t.maghrib.roundToMinuteOfDay(), 1)
            assertWithin("${c[0]} منتصف الليل", minutes(c[8]), t.midnight.roundToMinuteOfDay(), 1)
        }
    }

    @Test
    fun levaProfileMatchesIndependentAstronomicalReference() {
        for (c in readCsv("karbala_reference_2026_2027.csv")) {
            val (y, m, d) = c[0].split("-").map { it.toInt() }
            val t = JafariPrayerCalculator.calculate(y, m, d, husaynLat, husaynLng, iraqTz, profile = CalcProfile.LEVA_QOM)
            assertWithin("${c[0]} الفجر 16°", minutes(c[1]), t.fajr.roundToMinuteOfDay(), 1)
            assertWithin("${c[0]} المغرب 4°", minutes(c[6]), t.maghrib.roundToMinuteOfDay(), 1)
        }
    }

    @Test
    fun newEngineReproducesPreviousEngineOutputsForKarbala() {
        val places = mapOf(
            "APPDEF" to (32.6160 to 44.0249),
            "HUSAYN" to (32.61639 to 44.03250),
            "ABBAS" to (32.61719 to 44.03622)
        )
        val profiles = mapOf(
            "A_app_16_4" to CalcProfile.LEVA_QOM,
            "B_arc_18_45" to CalcProfile.NAJAF_ARC,
            "C_tehran_177_45" to CalcProfile.TEHRAN_IGUT
        )
        val rows = readCsv("legacy_engine_baseline.csv")
        assertEquals(90, rows.size)
        for (c in rows) {
            val (lat, lng) = places.getValue(c[1])
            val (y, m, d) = c[2].split("-").map { it.toInt() }
            val t = JafariPrayerCalculator.calculate(y, m, d, lat, lng, iraqTz, profile = profiles.getValue(c[0]))
            val tag = "${c[0]} ${c[1]} ${c[2]}"
            // كل الأوقات متطابقة دقيقة بدقيقة؛ منتصف الليل فقط يسمح بفرق دقيقة لأن v3.2 يستخدم فجر الغد الفعلي.
            assertEquals("$tag الفجر", minutes(c[3]), t.fajr.roundToMinuteOfDay())
            assertEquals("$tag الشروق", minutes(c[4]), t.sunrise.roundToMinuteOfDay())
            assertEquals("$tag الظهر", minutes(c[5]), t.dhuhr.roundToMinuteOfDay())
            assertEquals("$tag العصر", minutes(c[6]), t.asr.roundToMinuteOfDay())
            assertEquals("$tag الغروب", minutes(c[7]), t.sunset.roundToMinuteOfDay())
            assertEquals("$tag المغرب", minutes(c[8]), t.maghrib.roundToMinuteOfDay())
            assertEquals("$tag العشاء", minutes(c[9]), t.isha.roundToMinuteOfDay())
            assertWithin("$tag منتصف الليل", minutes(c[10]), t.midnight.roundToMinuteOfDay(), 1)
        }
    }

    @Test
    fun karbalaCoordinatesAreInterchangeableWithinOneMinute() {
        val abbasLat = 32.61719
        val abbasLng = 44.03622
        for ((y, m, d) in listOf(Triple(2026, 1, 15), Triple(2026, 6, 21), Triple(2026, 10, 10), Triple(2027, 2, 8))) {
            val a = JafariPrayerCalculator.calculate(y, m, d, husaynLat, husaynLng, iraqTz, profile = CalcProfile.NAJAF_ARC)
            val b = JafariPrayerCalculator.calculate(y, m, d, abbasLat, abbasLng, iraqTz, profile = CalcProfile.NAJAF_ARC)
            assertWithin("الفجر $y-$m-$d", a.fajr.roundToMinuteOfDay(), b.fajr.roundToMinuteOfDay(), 1)
            assertWithin("المغرب $y-$m-$d", a.maghrib.roundToMinuteOfDay(), b.maghrib.roundToMinuteOfDay(), 1)
        }
    }

    @Test
    fun fajrPrecautionIsLaterOnlyForNajafProfile() {
        for (month in 1..12) {
            val najaf = JafariPrayerCalculator.calculate(2026, month, 15, husaynLat, husaynLng, iraqTz, profile = CalcProfile.NAJAF_ARC)
            val gap = najaf.fajrPrecaution - najaf.fajr
            assertTrue("فرق الاحتياط في الشهر $month = $gap دقيقة", gap in 8.0..14.5)
            val leva = JafariPrayerCalculator.calculate(2026, month, 15, husaynLat, husaynLng, iraqTz, profile = CalcProfile.LEVA_QOM)
            assertEquals(-1, leva.fajrPrecaution.roundToMinuteOfDay())
        }
    }

    @Test
    fun timeZoneOffsetShiftsAllTimesByWholeHours() {
        val a = JafariPrayerCalculator.calculate(2026, 10, 10, husaynLat, husaynLng, 3.0, profile = CalcProfile.NAJAF_ARC)
        val b = JafariPrayerCalculator.calculate(2026, 10, 10, husaynLat, husaynLng, 4.0, profile = CalcProfile.NAJAF_ARC)
        assertEquals(a.fajr.roundToMinuteOfDay() + 60, b.fajr.roundToMinuteOfDay())
        assertEquals(a.maghrib.roundToMinuteOfDay() + 60, b.maghrib.roundToMinuteOfDay())
    }

    @Test
    fun highLatitudesAndPolarDaysDoNotCrash() {
        // ستوكهولم صيفاً: الفجر/العشاء لا يبلغان زواياهما، فيُطبَّق تعديل «منتصف الليل» ولا يتعطل شيء.
        val stockholm = JafariPrayerCalculator.calculate(2026, 6, 21, 59.3293, 18.0686, 2.0, profile = CalcProfile.LEVA_QOM)
        for (v in listOf(stockholm.fajr, stockholm.sunrise, stockholm.dhuhr, stockholm.sunset, stockholm.maghrib, stockholm.isha)) {
            assertTrue(v.roundToMinuteOfDay() in 0..1439)
        }
        // ترومسو في يونيو: شمس منتصف الليل — لا شروق ولا غروب → «غير متوفر» (-1) بلا استثناء.
        val tromso = JafariPrayerCalculator.calculate(2026, 6, 21, 69.6492, 18.9553, 2.0, profile = CalcProfile.LEVA_QOM)
        assertEquals(-1, tromso.sunrise.roundToMinuteOfDay())
        assertEquals(-1, tromso.sunset.roundToMinuteOfDay())
    }

    @Test
    fun roundingIsNearestMinute() {
        assertEquals(330, 330.4.roundToMinuteOfDay())
        assertEquals(331, 330.6.roundToMinuteOfDay())
        assertEquals(-1, Double.NaN.roundToMinuteOfDay())
    }
}
