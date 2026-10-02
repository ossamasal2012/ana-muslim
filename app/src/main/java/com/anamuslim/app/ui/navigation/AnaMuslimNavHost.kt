package com.anamuslim.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anamuslim.app.R
import com.anamuslim.app.ui.hijri.HijriCalendarScreen
import com.anamuslim.app.ui.prayertimes.PrayerTimesScreen
import com.anamuslim.app.ui.quran.QuranScreen
import com.anamuslim.app.ui.settings.SettingsScreen
import com.anamuslim.app.ui.tasbih.TasbihScreen

private sealed class Destination(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Prayer : Destination("prayer", R.string.nav_prayer_times, Icons.Filled.AccessTime)
    object Quran : Destination("quran", R.string.nav_quran, Icons.Filled.MenuBook)
    object Tasbih : Destination("tasbih", R.string.nav_tasbih, Icons.Filled.TouchApp)
    object Calendar : Destination("calendar", R.string.nav_calendar, Icons.Filled.CalendarMonth)
    object Settings : Destination("settings", R.string.nav_settings, Icons.Filled.Settings)
}

private val bottomDestinations = listOf(
    Destination.Prayer, Destination.Quran, Destination.Tasbih, Destination.Calendar, Destination.Settings
)

@Composable
fun AnaMuslimNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination

                bottomDestinations.forEach { dest ->
                    val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = null) },
                        label = { Text(stringResource(dest.labelRes)) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Destination.Prayer.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Destination.Prayer.route) { PrayerTimesScreen() }
            composable(Destination.Quran.route) { QuranScreen() }
            composable(Destination.Tasbih.route) { TasbihScreen() }
            composable(Destination.Calendar.route) { HijriCalendarScreen() }
            composable(Destination.Settings.route) { SettingsScreen() }
        }
    }
}
