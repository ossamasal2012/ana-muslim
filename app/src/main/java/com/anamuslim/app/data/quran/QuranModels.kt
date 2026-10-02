package com.anamuslim.app.data.quran

import kotlinx.serialization.Serializable

@Serializable
data class QuranPageResponse(
    val code: Int,
    val status: String,
    val data: QuranPageData
)

@Serializable
data class QuranPageData(
    val number: Int, // رقم الصفحة
    val ayahs: List<QuranAyah>
)

@Serializable
data class QuranAyah(
    val number: Int, // الرقم التسلسلي للآية في كامل القرآن
    val text: String, // نص الآية بالرسم العثماني (من مشروع تنزيل)
    val numberInSurah: Int,
    val juz: Int,
    val page: Int,
    val surah: SurahInfo
)

@Serializable
data class SurahInfo(
    val number: Int,
    val name: String, // الاسم العربي
    val englishName: String,
    val numberOfAyahs: Int
)

/** نموذج صفحة قرآن مبسّط جاهز للعرض والتخزين المحلي (بعد تحويله من استجابة الشبكة). */
@Serializable
data class QuranPageCache(
    val pageNumber: Int,
    val juzNumber: Int,
    val surahName: String,
    val surahNumber: Int,
    val ayahs: List<CachedAyah>
)

@Serializable
data class CachedAyah(
    val numberInSurah: Int,
    val text: String,
    val surahName: String,
    val surahNumber: Int,
    val isNewSurahStart: Boolean = false
)
