package com.anamuslim.app.core

import android.content.Context
import com.anamuslim.app.data.hijri.ShiaOccasionsRepository
import com.anamuslim.app.data.prayertimes.LocationHelper
import com.anamuslim.app.data.prayertimes.PrayerTimesRepository
import com.anamuslim.app.data.quran.NetworkModule
import com.anamuslim.app.data.quran.QuranRepository
import com.anamuslim.app.data.settings.SettingsRepository
import com.anamuslim.app.data.tasbih.TasbihRepository
import com.anamuslim.app.data.update.UpdateRepository

/**
 * موفر تبعيات بسيط جداً (Service Locator) بدل استخدام Hilt/Dagger.
 *
 * السبب: Hilt يحتاج معالج شيفرة (kapt/ksp) يضيف طبقة تعقيد إضافية أثناء البناء
 * الآلي عبر GitHub Actions قد تفشل لأسباب لا علاقة لها بمنطق التطبيق نفسه.
 * بما أن حجم التطبيق معقول ولا يحتاج حقن تبعيات معقد متعدد النطاقات، هذا
 * الأسلوب اليدوي البسيط أخف وأكثر ضماناً للنجاح دون أي فقدان حقيقي في التنظيم.
 */
object ServiceLocator {

    @Volatile private var settingsRepository: SettingsRepository? = null
    @Volatile private var prayerTimesRepository: PrayerTimesRepository? = null
    @Volatile private var quranRepository: QuranRepository? = null
    @Volatile private var tasbihRepository: TasbihRepository? = null
    @Volatile private var updateRepository: UpdateRepository? = null
    @Volatile private var shiaOccasionsRepository: ShiaOccasionsRepository? = null

    fun settings(context: Context): SettingsRepository =
        settingsRepository ?: synchronized(this) {
            settingsRepository ?: SettingsRepository(context.applicationContext).also { settingsRepository = it }
        }

    fun prayerTimes(context: Context): PrayerTimesRepository =
        prayerTimesRepository ?: synchronized(this) {
            prayerTimesRepository ?: PrayerTimesRepository(
                settings = settings(context),
                locationHelper = LocationHelper(context.applicationContext)
            ).also { prayerTimesRepository = it }
        }

    fun quran(context: Context): QuranRepository =
        quranRepository ?: synchronized(this) {
            quranRepository ?: QuranRepository(
                context = context.applicationContext,
                api = NetworkModule.quranApi,
                settings = settings(context)
            ).also { quranRepository = it }
        }

    fun tasbih(context: Context): TasbihRepository =
        tasbihRepository ?: synchronized(this) {
            tasbihRepository ?: TasbihRepository(context.applicationContext).also { tasbihRepository = it }
        }

    fun update(context: Context): UpdateRepository =
        updateRepository ?: synchronized(this) {
            updateRepository ?: UpdateRepository(context.applicationContext).also { updateRepository = it }
        }

    fun shiaOccasions(): ShiaOccasionsRepository =
        shiaOccasionsRepository ?: synchronized(this) {
            shiaOccasionsRepository ?: ShiaOccasionsRepository().also { shiaOccasionsRepository = it }
        }
}
