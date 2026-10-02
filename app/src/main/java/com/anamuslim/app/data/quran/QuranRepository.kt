package com.anamuslim.app.data.quran

import android.content.Context
import com.anamuslim.app.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

const val QURAN_TOTAL_PAGES = 604

sealed interface QuranPageResult {
    data class Success(val page: QuranPageCache) : QuranPageResult
    data class Error(val message: String) : QuranPageResult
}

class QuranRepository(
    private val context: Context,
    private val api: QuranApiService,
    private val settings: SettingsRepository
) {
    private val cacheDir: File by lazy {
        File(context.filesDir, "quran_cache").apply { mkdirs() }
    }

    private fun cacheFile(page: Int) = File(cacheDir, "page_$page.json")

    /** يجلب صفحة قرآن: من التخزين المحلي إن وُجدت، وإلا من الإنترنت لمرة واحدة فقط. */
    suspend fun getPage(pageNumber: Int): QuranPageResult = withContext(Dispatchers.IO) {
        val safePage = pageNumber.coerceIn(1, QURAN_TOTAL_PAGES)
        val file = cacheFile(safePage)

        if (file.exists()) {
            try {
                val cached = NetworkModule.json.decodeFromString(QuranPageCache.serializer(), file.readText())
                return@withContext QuranPageResult.Success(cached)
            } catch (e: Exception) {
                // ملف تالف بشكل ما؛ سنعيد تحميله من الإنترنت أدناه بدل الفشل
                file.delete()
            }
        }

        return@withContext try {
            val response = api.getPage(safePage)
            val mapped = mapToCache(response.data)
            file.writeText(NetworkModule.json.encodeToString(QuranPageCache.serializer(), mapped))
            QuranPageResult.Success(mapped)
        } catch (e: Exception) {
            QuranPageResult.Error(e.message ?: "network_error")
        }
    }

    private fun mapToCache(data: QuranPageData): QuranPageCache {
        // تحقق آلي دفاعي: كل آية بالاستجابة يجب أن تحمل نفس رقم الصفحة المطلوبة
        // (data.number) بحقلها page، وإلا فهذا يعني تناقضاً حقيقياً في بيانات
        // المصدر يستحق التسجيل بدل تمريره بصمت لعرضه للمستخدم كأنه سليم.
        val mismatched = data.ayahs.filter { it.page != data.number }
        if (mismatched.isNotEmpty()) {
            android.util.Log.w(
                "QuranRepository",
                "تعارض ترقيم صفحة عند الصفحة ${data.number}: ${mismatched.size} آية تحمل رقم صفحة مختلف"
            )
        }

        var lastSurah = -1
        val ayahs = data.ayahs.map { ayah ->
            val isNewSurah = ayah.surah.number != lastSurah
            lastSurah = ayah.surah.number
            CachedAyah(
                numberInSurah = ayah.numberInSurah,
                text = ayah.text,
                surahName = ayah.surah.name,
                surahNumber = ayah.surah.number,
                isNewSurahStart = isNewSurah
            )
        }
        val juz = data.ayahs.firstOrNull()?.juz ?: 1
        val firstSurah = data.ayahs.firstOrNull()?.surah
        return QuranPageCache(
            pageNumber = data.number,
            juzNumber = juz,
            surahName = firstSurah?.name ?: "",
            surahNumber = firstSurah?.number ?: 1,
            ayahs = ayahs
        )
    }

    /** يهيّئ التحميل المسبق لصفحات مجاورة في الخلفية لتحسين سلاسة التصفح (اختياري، لا يعطّل شيئاً عند الفشل). */
    suspend fun prefetchAround(pageNumber: Int) = withContext(Dispatchers.IO) {
        for (p in (pageNumber - 1)..(pageNumber + 1)) {
            if (p in 1..QURAN_TOTAL_PAGES && !cacheFile(p).exists()) {
                try { getPage(p) } catch (e: Exception) { /* تجاهل: مجرد تحميل مسبق اختياري */ }
            }
        }
    }

    val lastSavedPage = settings.quranLastPage
    val autoSaveEnabled = settings.quranAutoSaveEnabled

    suspend fun saveCurrentPage(page: Int) = settings.saveQuranLastPageIfEnabled(page)

    suspend fun setAutoSaveEnabled(enabled: Boolean) = settings.setQuranAutoSaveEnabled(enabled)

    /** "تصفير سجل حفظ جديد": يمسح آخر صفحة محفوظة ويعيدها لصفحة البداية. */
    suspend fun resetSavedPage() = settings.resetQuranSavedPage()
}
