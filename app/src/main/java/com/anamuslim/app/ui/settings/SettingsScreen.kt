@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.anamuslim.app.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anamuslim.app.BuildConfig
import com.anamuslim.app.R
import com.anamuslim.app.data.settings.QuranDisplayMode
import com.anamuslim.app.data.settings.SettingsRepository
import com.anamuslim.app.data.settings.TimeFormatPreference
import com.anamuslim.app.data.update.UpdateCheckResult
import com.anamuslim.app.ui.update.UpdateDialogHost

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var updateResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var lastCheckMessage by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.settings_title)) })

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionTitle(stringResource(R.string.settings_section_adhan))
            Text(stringResource(R.string.settings_adhan_enable_all), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            PrayerToggleRow(stringResource(R.string.prayer_name_fajr), state.fajrEnabled) { viewModel.setPrayerEnabled(SettingsRepository.Prayer.FAJR, it) }
            PrayerToggleRow(stringResource(R.string.prayer_name_dhuhr), state.dhuhrEnabled) { viewModel.setPrayerEnabled(SettingsRepository.Prayer.DHUHR, it) }
            PrayerToggleRow(stringResource(R.string.prayer_name_asr), state.asrEnabled) { viewModel.setPrayerEnabled(SettingsRepository.Prayer.ASR, it) }
            PrayerToggleRow(stringResource(R.string.prayer_name_maghrib), state.maghribEnabled) { viewModel.setPrayerEnabled(SettingsRepository.Prayer.MAGHRIB, it) }
            PrayerToggleRow(stringResource(R.string.prayer_name_isha), state.ishaEnabled) { viewModel.setPrayerEnabled(SettingsRepository.Prayer.ISHA, it) }

            // إذن "المنبهات الدقيقة" أضافته جوجل بدءاً من أندرويد 12 فقط كخطوة أمان
            // إضافية؛ هذا الإذن غير موجود إطلاقاً على أندرويد 8-11 (الأذان هناك
            // يعمل بدقة تلقائياً بلا أي إذن)، لذا نُخفي الزر بالكامل تحت أندرويد 12
            // حتى لا يظهر للمستخدم زر لا يفعل شيئاً على جهازه فيظنّ أن هناك نقصاً.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Spacer(Modifier.height(8.dp))
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.settings_exact_alarm_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.settings_exact_alarm_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            (context as? com.anamuslim.app.MainActivity)?.requestExactAlarmPermissionIfNeeded()
                        }) { Text(stringResource(R.string.settings_exact_alarm_grant)) }
                    }
                }
            }

            Divider(Modifier.padding(vertical = 12.dp))
            SectionTitle(stringResource(R.string.settings_section_quran_display))
            Text(
                stringResource(R.string.settings_quran_display_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            RadioOptionRow(
                label = stringResource(R.string.settings_quran_display_continuous),
                selected = state.quranDisplayMode == QuranDisplayMode.CONTINUOUS_SCROLL,
                onClick = { viewModel.setQuranDisplayMode(QuranDisplayMode.CONTINUOUS_SCROLL) }
            )
            RadioOptionRow(
                label = stringResource(R.string.settings_quran_display_tap_button),
                selected = state.quranDisplayMode == QuranDisplayMode.TAP_BUTTON,
                onClick = { viewModel.setQuranDisplayMode(QuranDisplayMode.TAP_BUTTON) }
            )

            Divider(Modifier.padding(vertical = 12.dp))
            SectionTitle(stringResource(R.string.settings_section_time_format))
            RadioOptionRow(
                label = stringResource(R.string.settings_time_format_12),
                selected = state.timeFormat == TimeFormatPreference.HOUR_12,
                onClick = { viewModel.setTimeFormat(TimeFormatPreference.HOUR_12) }
            )
            RadioOptionRow(
                label = stringResource(R.string.settings_time_format_24),
                selected = state.timeFormat == TimeFormatPreference.HOUR_24,
                onClick = { viewModel.setTimeFormat(TimeFormatPreference.HOUR_24) }
            )

            Divider(Modifier.padding(vertical = 12.dp))
            SectionTitle(stringResource(R.string.settings_section_about))
            Text(stringResource(R.string.settings_current_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.checkForUpdate { result ->
                        updateResult = result
                        lastCheckMessage = when (result) {
                            is UpdateCheckResult.UpToDate -> context.getString(R.string.settings_up_to_date)
                            is UpdateCheckResult.Failed -> context.getString(R.string.error_generic)
                            is UpdateCheckResult.UpdateAvailable -> null
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isCheckingUpdate
            ) {
                if (state.isCheckingUpdate) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.settings_check_update))
                }
            }
            lastCheckMessage?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }

    (updateResult as? UpdateCheckResult.UpdateAvailable)?.let { available ->
        UpdateDialogHost(info = available.info, onDismissRequest = { updateResult = null })
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
}

@Composable
private fun PrayerToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun RadioOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
