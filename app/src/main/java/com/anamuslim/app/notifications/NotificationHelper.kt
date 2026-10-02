package com.anamuslim.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.anamuslim.app.R

object NotificationHelper {
    const val ADHAN_CHANNEL_ID = "adhan_alerts"
    const val UPDATE_CHANNEL_ID = "update_downloads"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // قناة الأذان: أهمية عالية (تنبيه واضح)، بلا صوت إشعار افتراضي لأن الصوت
        // الفعلي يأتي من الخدمة الأمامية التي تشغّل ملف الأذان كاملاً بنفسها
        val adhanChannel = NotificationChannel(
            ADHAN_CHANNEL_ID,
            context.getString(R.string.adhan_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)
            enableVibration(true)
        }

        val updateChannel = NotificationChannel(
            UPDATE_CHANNEL_ID,
            context.getString(R.string.update_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )

        manager.createNotificationChannel(adhanChannel)
        manager.createNotificationChannel(updateChannel)
    }
}
