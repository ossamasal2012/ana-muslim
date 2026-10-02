@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.anamuslim.app.ui.hijri

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anamuslim.app.R
import com.anamuslim.app.core.ServiceLocator
import com.anamuslim.app.data.hijri.HijriDate
import com.anamuslim.app.data.hijri.ShiaOccasionsRepository
import com.anamuslim.app.ui.theme.MourningRed
import java.util.Calendar

@Composable
fun HijriCalendarScreen() {
    val today = remember { HijriDate.fromCalendar(Calendar.getInstance()) }
    val occasionsRepo = remember { ServiceLocator.shiaOccasions() }
    val upcoming = remember { occasionsRepo.upcomingOccasions(count = 10) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.nav_calendar)) })

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${today.day} ${today.monthName} ${today.year} هـ",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.hijri_disclaimer),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Text(
                    stringResource(R.string.hijri_upcoming_occasions),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(upcoming) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.occasion.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (item.occasion.isMourning) MourningRed else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${item.occasion.hijriDay} ${HijriDate.HIJRI_MONTH_NAMES[item.occasion.hijriMonth - 1]}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            if (item.daysRemaining == 0) "اليوم" else "بعد ${item.daysRemaining} يوم",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
