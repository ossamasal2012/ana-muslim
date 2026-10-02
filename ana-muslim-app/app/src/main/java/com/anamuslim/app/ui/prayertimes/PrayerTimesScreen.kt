package com.anamuslim.app.ui.prayertimes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anamuslim.app.R
import com.anamuslim.app.data.prayertimes.PrayerName
import com.anamuslim.app.data.prayertimes.PrayerTimesDisplay

@Composable
fun PrayerTimesScreen(viewModel: PrayerTimesViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.app_name)) })

        if (state.isLoading || state.times == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { HijriHeader(state.hijriLabel) }
            item { NextPrayerCard(state) }
            item {
                Text(
                    stringResource(R.string.calculation_method_jafari),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            state.times?.let { times ->
                items(prayerRows(times)) { row ->
                    PrayerRow(row, isNext = row.name == state.next?.prayer)
                }
            }
        }
    }
}

@Composable
private fun HijriHeader(hijriLabel: String) {
    Text(
        text = hijriLabel,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun NextPrayerCard(state: PrayerTimesUiState) {
    val next = state.next ?: return
    val hours = next.minutesRemaining / 60
    val minutes = next.minutesRemaining % 60

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.next_prayer_in, prayerLabel(next.prayer)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (hours > 0) "%dس %02dد".format(hours, minutes) else "%d دقيقة".format(minutes),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                PrayerTimesDisplay.formatMinute(next.atMinuteOfDay),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

private data class PrayerRowData(val name: PrayerName?, val label: String, val minute: Int)

@Composable
private fun prayerRows(times: PrayerTimesDisplay): List<PrayerRowData> = listOf(
    PrayerRowData(PrayerName.FAJR, stringResource(R.string.prayer_fajr), times.fajrMinute),
    PrayerRowData(null, stringResource(R.string.prayer_sunrise), times.sunriseMinute),
    PrayerRowData(PrayerName.DHUHR, stringResource(R.string.prayer_dhuhr), times.dhuhrMinute),
    PrayerRowData(PrayerName.ASR, stringResource(R.string.prayer_asr), times.asrMinute),
    PrayerRowData(PrayerName.MAGHRIB, stringResource(R.string.prayer_maghrib), times.maghribMinute),
    PrayerRowData(PrayerName.ISHA, stringResource(R.string.prayer_isha), times.ishaMinute)
)

@Composable
private fun prayerLabel(name: PrayerName): String = when (name) {
    PrayerName.FAJR -> stringResource(R.string.prayer_fajr)
    PrayerName.DHUHR -> stringResource(R.string.prayer_dhuhr)
    PrayerName.ASR -> stringResource(R.string.prayer_asr)
    PrayerName.MAGHRIB -> stringResource(R.string.prayer_maghrib)
    PrayerName.ISHA -> stringResource(R.string.prayer_isha)
}

@Composable
private fun PrayerRow(row: PrayerRowData, isNext: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isNext) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(row.label, style = MaterialTheme.typography.titleMedium)
            Text(
                PrayerTimesDisplay.formatMinute(row.minute),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                color = if (isNext) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
