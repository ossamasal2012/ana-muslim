package com.anamuslim.app

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.anamuslim.app.alarm.AlarmScheduler
import com.anamuslim.app.data.update.ApkDownloadManagerHelper
import com.anamuslim.app.ui.navigation.AnaMuslimNavHost
import com.anamuslim.app.ui.theme.AnaMuslimTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // إن وُجد تحديث اكتمل تنزيله والتطبيق كان خارجاً وقتها، نثبّته تلقائياً الآن
        val downloadHelper = ApkDownloadManagerHelper(this)
        if (downloadHelper.hasCompletedPendingInstall()) {
            downloadHelper.buildInstallIntent()?.let { startActivity(it) }
        }

        setContent {
            AnaMuslimTheme {
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* لا حاجة لأي إجراء إضافي هنا */ }
                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { /* الواجهة تراقب الحالة تلقائياً عبر إعادة التركيب */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    locationPermissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                }

                AnaMuslimNavHost()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // نعيد جدولة الأذان عند كل عودة للتطبيق: يغطي حالة العودة من شاشة منح إذن
        // "المنبهات الدقيقة" أو تغيير الموقع، والعملية آمنة التكرار (نفس المعرّفات تُحدَّث لا تتضاعف)
        lifecycleScope.launch { AlarmScheduler.rescheduleAll(applicationContext) }
    }

    /** يفتح صفحة إعدادات النظام لمنح إذن "المنبهات الدقيقة" إن لم يكن ممنوحاً (أندرويد 12+). */
    fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}
