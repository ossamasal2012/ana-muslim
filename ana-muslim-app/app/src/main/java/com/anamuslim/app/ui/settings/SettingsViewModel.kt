package com.anamuslim.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anamuslim.app.alarm.AlarmScheduler
import com.anamuslim.app.core.ServiceLocator
import com.anamuslim.app.data.settings.SettingsRepository
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
    val quranAutoSave: Boolean = true,
    val updateStatus: String? = null,
    val isCheckingUpdate: Boolean = false
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
        viewModelScope.launch {
            settings.quranAutoSaveEnabled.collect { _uiState.value = _uiState.value.copy(quranAutoSave = it) }
        }
    }

    fun setPrayerEnabled(prayer: SettingsRepository.Prayer, enabled: Boolean) {
        viewModelScope.launch {
            settings.setPrayerAdhanEnabled(prayer, enabled)
            AlarmScheduler.rescheduleAll(getApplication())
        }
    }

    fun setQuranAutoSave(enabled: Boolean) = viewModelScope.launch { settings.setQuranAutoSaveEnabled(enabled) }

    fun resetQuranSavedPage() = viewModelScope.launch { settings.resetQuranSavedPage() }

    fun checkForUpdate(onResult: (UpdateCheckResult) -> Unit) {
        _uiState.value = _uiState.value.copy(isCheckingUpdate = true)
        viewModelScope.launch {
            val result = updateRepo.checkForUpdate()
            _uiState.value = _uiState.value.copy(isCheckingUpdate = false)
            onResult(result)
        }
    }
}
