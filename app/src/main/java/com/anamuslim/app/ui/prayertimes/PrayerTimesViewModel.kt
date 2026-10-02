package com.anamuslim.app.ui.prayertimes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anamuslim.app.core.ServiceLocator
import com.anamuslim.app.data.hijri.HijriDate
import com.anamuslim.app.data.prayertimes.NextPrayerInfo
import com.anamuslim.app.data.prayertimes.PrayerTimesDisplay
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

data class PrayerTimesUiState(
    val isLoading: Boolean = true,
    val times: PrayerTimesDisplay? = null,
    val next: NextPrayerInfo? = null,
    val hijriLabel: String = "",
    val hasLocationPermission: Boolean = false
)

class PrayerTimesViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PrayerTimesUiState())
    val uiState: StateFlow<PrayerTimesUiState> = _uiState.asStateFlow()

    private val locationHelper = com.anamuslim.app.data.prayertimes.LocationHelper(application)
    private val repo = ServiceLocator.prayerTimes(application)

    init {
        viewModelScope.launch {
            ensureLocationSavedIfPossible()
            refreshLoop()
        }
    }

    private suspend fun ensureLocationSavedIfPossible() {
        val settings = ServiceLocator.settings(getApplication())
        val existing = settings.savedLocation.first()
        if (existing == null && locationHelper.hasLocationPermission()) {
            val loc = locationHelper.getLastKnownOrCurrentLocation()
            if (loc != null) {
                settings.saveLocation(loc.latitude, loc.longitude, locationHelper.currentUtcOffsetHours(), "")
            }
        }
    }

    private suspend fun refreshLoop() {
        while (true) {
            val now = Calendar.getInstance()
            val times = repo.getTodayTimes(now)
            val next = repo.getNextPrayer(now)
            val hijri = HijriDate.fromCalendar(now)
            _uiState.value = PrayerTimesUiState(
                isLoading = false,
                times = times,
                next = next,
                hijriLabel = "${hijri.day} ${hijri.monthName} ${hijri.year}",
                hasLocationPermission = locationHelper.hasLocationPermission()
            )
            delay(30_000) // نحدّث العدّ التنازلي كل 30 ثانية
        }
    }

    fun refreshNow() {
        viewModelScope.launch { refreshOnce() }
    }

    private suspend fun refreshOnce() {
        val now = Calendar.getInstance()
        val times = repo.getTodayTimes(now)
        val next = repo.getNextPrayer(now)
        val hijri = HijriDate.fromCalendar(now)
        _uiState.value = _uiState.value.copy(
            isLoading = false, times = times, next = next,
            hijriLabel = "${hijri.day} ${hijri.monthName} ${hijri.year}",
            hasLocationPermission = locationHelper.hasLocationPermission()
        )
    }
}
