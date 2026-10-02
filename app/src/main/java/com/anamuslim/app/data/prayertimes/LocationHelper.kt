package com.anamuslim.app.data.prayertimes

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.TimeZone
import kotlin.coroutines.resume

/**
 * تحديد الموقع باستخدام LocationManager القياسي في أندرويد (وليس
 * Google Play Services FusedLocationProvider) عمداً — حتى يعمل التطبيق على
 * كل الأجهزة بلا استثناء (بما فيها الأجهزة الخالية من خدمات جوجل)، ولتقليل
 * حجم التطبيق وعدد الاعتماديات، فدقة مدينة/حي تكفي تماماً لحساب مواقيت الصلاة.
 */
class LocationHelper(private val context: Context) {

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    /** فارق التوقيت المحلي الحالي عن UTC بالساعات (يراعي التوقيت الصيفي تلقائياً). */
    fun currentUtcOffsetHours(): Double {
        val tz = TimeZone.getDefault()
        return tz.getOffset(System.currentTimeMillis()) / 3_600_000.0
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownOrCurrentLocation(): Location? {
        if (!hasLocationPermission()) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        // أولاً: نجرب أي موقع معروف مسبقاً من أي مزوّد (فوري بلا انتظار)
        val providers = manager.getProviders(true)
        var best: Location? = null
        for (provider in providers) {
            val loc = try { manager.getLastKnownLocation(provider) } catch (e: SecurityException) { null } ?: continue
            if (best == null || loc.time > best!!.time) best = loc
        }
        if (best != null) return best

        // إن لم يوجد أي موقع محفوظ سابقاً، نطلب تحديثاً حياً لمرة واحدة (بمهلة زمنية)
        return requestSingleLocationUpdate(manager, providers)
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleLocationUpdate(
        manager: LocationManager,
        providers: List<String>
    ): Location? = suspendCancellableCoroutine { cont ->
        val provider = when {
            providers.contains(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            providers.contains(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            providers.isNotEmpty() -> providers.first()
            else -> null
        }
        if (provider == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(location: Location) {
                manager.removeUpdates(this)
                if (cont.isActive) cont.resume(location)
            }
        }
        try {
            manager.requestSingleUpdate(provider, listener, android.os.Looper.getMainLooper())
        } catch (e: Exception) {
            if (cont.isActive) cont.resume(null)
            return@suspendCancellableCoroutine
        }
        cont.invokeOnCancellation { manager.removeUpdates(listener) }
    }
}
