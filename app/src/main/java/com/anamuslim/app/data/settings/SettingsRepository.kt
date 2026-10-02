package com.anamuslim.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ana_muslim_settings")

/**
 * مستودع مركزي لكل الإعدادات التي يجب أن تبقى محفوظة حتى لو أُغلق التطبيق:
 * - تفعيل/إيقاف تنبيه الأذان لكل صلاة (افتراضياً الكل مفعّل)
 * - الموقع الجغرافي المحفوظ لحساب مواقيت الصلاة
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val FAJR_ENABLED = booleanPreferencesKey("adhan_fajr_enabled")
        val DHUHR_ENABLED = booleanPreferencesKey("adhan_dhuhr_enabled")
        val ASR_ENABLED = booleanPreferencesKey("adhan_asr_enabled")
        val MAGHRIB_ENABLED = booleanPreferencesKey("adhan_maghrib_enabled")
        val ISHA_ENABLED = booleanPreferencesKey("adhan_isha_enabled")

        val LOCATION_LAT = doublePreferencesKey("location_lat")
        val LOCATION_LNG = doublePreferencesKey("location_lng")
        val LOCATION_TZ_OFFSET = doublePreferencesKey("location_tz_offset")
        val LOCATION_CITY_NAME = stringPreferencesKey("location_city_name")

        val LAST_KNOWN_INSTALLED_VERSION_CODE = intPreferencesKey("last_known_installed_version_code")
    }

    enum class Prayer(val key: androidx.datastore.preferences.core.Preferences.Key<Boolean>) {
        FAJR(Keys.FAJR_ENABLED),
        DHUHR(Keys.DHUHR_ENABLED),
        ASR(Keys.ASR_ENABLED),
        MAGHRIB(Keys.MAGHRIB_ENABLED),
        ISHA(Keys.ISHA_ENABLED)
    }

    /** يعيد تدفّق حالة تفعيل الأذان لصلاة معينة (القيمة الافتراضية: مفعّل = true). */
    fun isPrayerAdhanEnabled(prayer: Prayer): Flow<Boolean> =
        context.dataStore.data.map { it[prayer.key] ?: true }

    suspend fun setPrayerAdhanEnabled(prayer: Prayer, enabled: Boolean) {
        context.dataStore.edit { it[prayer.key] = enabled }
    }

    data class SavedLocation(
        val latitude: Double,
        val longitude: Double,
        val timeZoneOffsetHours: Double,
        val cityName: String
    )

    val savedLocation: Flow<SavedLocation?> = context.dataStore.data.map { prefs ->
        val lat = prefs[Keys.LOCATION_LAT]
        val lng = prefs[Keys.LOCATION_LNG]
        val tz = prefs[Keys.LOCATION_TZ_OFFSET]
        val city = prefs[Keys.LOCATION_CITY_NAME]
        if (lat != null && lng != null && tz != null) {
            SavedLocation(lat, lng, tz, city ?: "")
        } else null
    }

    suspend fun saveLocation(lat: Double, lng: Double, tzOffsetHours: Double, cityName: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOCATION_LAT] = lat
            prefs[Keys.LOCATION_LNG] = lng
            prefs[Keys.LOCATION_TZ_OFFSET] = tzOffsetHours
            prefs[Keys.LOCATION_CITY_NAME] = cityName
        }
    }

    /**
     * تُستخدم فقط لإصلاح مشكلة ظهور زر التحديث من جديد بعد تثبيت التحديث فعلياً.
     * راجع UpdateChecker لتفاصيل كيف تُستخدم هذه القيمة.
     */
    val lastKnownInstalledVersionCode: Flow<Int> =
        context.dataStore.data.map { it[Keys.LAST_KNOWN_INSTALLED_VERSION_CODE] ?: 0 }

    suspend fun setLastKnownInstalledVersionCode(code: Int) {
        context.dataStore.edit { it[Keys.LAST_KNOWN_INSTALLED_VERSION_CODE] = code }
    }
}
