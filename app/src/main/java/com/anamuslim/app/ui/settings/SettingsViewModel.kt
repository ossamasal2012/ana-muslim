package com.anamuslim.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anamuslim.app.alarm.AlarmScheduler
import com.anamuslim.app.core.ServiceLocator
import com.anamuslim.app.data.settings.QuranDisplayMode
import com.anamuslim.app.data.settings.SettingsRepository
import com.anamuslim.app.data.settings.TimeFormatPreference
import com.anamuslim.app.data.update.UpdateCheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SettingsUiState(
    val fajrEnabled: Boolean = true,
    val dhuhrEnabled: Boolean = true,
    val asrEnabled: Boolean = true,
    val maghribEnabled: Boolean = true,
    val ishaEnabled: Boolean = true,
    val isCheckingUpdate: Boolean = false,
    val quranDisplayMode: QuranDisplayMode = QuranDisplayMode.CONTINUOUS_SCROLL,
    val timeFormat: TimeFormatPreference = TimeFormatPreference.HOUR_12
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = ServiceLocator.settings(application)
    private val updateRepo = ServiceLocator.update(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settings.isPrayerAdhanEnabled(SettingsRepository.Prayer.FAJR),
                settings.isPrayerAdhanEnabled(SettingsRepository.Prayer.DHUHR),
                settings.isPrayerAdhanEnabled(SettingsRepository.Prayer.ASR),
                settings.isPrayerAdhanEnabled(SettingsRepository.Prayer.MAGHRIB),
                settings.isPrayerAdhanEnabled(SettingsRepository.Prayer.ISHA)
            ) { values -> values }.collect { values ->
                _uiState.value = _uiState.value.copy(
                    fajrEnabled = values[0], dhuhrEnabled = values[1], asrEnabled = values[2],
                    maghribEnabled = values[3], ishaEnabled = values[4]
                )
            }
        }
        // تجميع منفصل عمداً عن الأعلى حتى لا يُلمس منطق تنبيهات الأذان العامل حالياً.
        viewModelScope.launch {
            combine(
                settings.quranDisplayMode,
                settings.timeFormatPreference
            ) { mode, format -> mode to format }.collect { (mode, format) ->
                _uiState.value = _uiState.value.copy(quranDisplayMode = mode, timeFormat = format)
            }
        }
    }

    fun setPrayerEnabled(prayer: SettingsRepository.Prayer, enabled: Boolean) {
        viewModelScope.launch {
            settings.setPrayerAdhanEnabled(prayer, enabled)
            AlarmScheduler.rescheduleAll(getApplication())
        }
    }

    fun checkForUpdate(onResult: (UpdateCheckResult) -> Unit) {
        _uiState.value = _uiState.value.copy(isCheckingUpdate = true)
        viewModelScope.launch {
            val result = updateRepo.checkForUpdate()
            _uiState.value = _uiState.value.copy(isCheckingUpdate = false)
            onResult(result)
        }
    }

    /** وضع عرض القرآن: التبديل فوري لأن الشاشة تقرأ هذا الإعداد مباشرة من DataStore. */
    fun setQuranDisplayMode(mode: QuranDisplayMode) {
        viewModelScope.launch { settings.setQuranDisplayMode(mode) }
    }

    /** نظام 12/24 ساعة: لا يغيّر حسابات الأوقات، فقط طريقة عرضها في كل الشاشات. */
    fun setTimeFormat(format: TimeFormatPreference) {
        viewModelScope.launch { settings.setTimeFormatPreference(format) }
    }
}
