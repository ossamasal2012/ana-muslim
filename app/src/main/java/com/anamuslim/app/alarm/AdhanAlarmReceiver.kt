package com.anamuslim.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdhanAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerKey = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "FAJR"

        val serviceIntent = Intent(context, AdhanPlaybackService::class.java).apply {
            putExtra(EXTRA_PRAYER_NAME, prayerKey)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        // إعادة جدولة الغد بعد إطلاق كل أذان (سلسلة ذاتية الاستمرار)، بعمل غير متزامن
        // آمن داخل BroadcastReceiver عبر goAsync()
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                AlarmScheduler.rescheduleAll(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
