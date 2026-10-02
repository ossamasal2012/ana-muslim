package com.anamuslim.app.data.quran

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * واجهة Al Quran Cloud API (بلا مفتاح، بلا مصادقة) — النص عثماني مصدره
 * مشروع تنزيل (Tanzil Project)، مرخّص Creative Commons Attribution 3.0.
 * يُستخدم فقط لأول تحميل لكل صفحة، ثم تُخزَّن محلياً للأبد (راجع QuranRepository).
 */
interface QuranApiService {
    @GET("page/{page}/quran-uthmani")
    suspend fun getPage(@Path("page") pageNumber: Int): QuranPageResponse
}
