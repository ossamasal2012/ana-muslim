package com.anamuslim.app.alarm

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.anamuslim.app.MainActivity
import com.anamuslim.app.R
import com.anamuslim.app.notifications.NotificationHelper

private const val ACTION_STOP = "com.anamuslim.app.action.STOP_ADHAN"
private const val NOTIFICATION_ID = 5501

/**
 * خدمة أمامية (Foreground Service من نوع mediaPlayback) تشغّل ملف الأذان كاملاً.
 *
 * ملاحظة موثّقة رسمياً من أندرويد: التنبيهات الدقيقة (setExactAndAllowWhileIdle)
 * "مسموح لها بتشغيل خدمة أمامية حتى والتطبيق في الخلفية"، ولا تخضع لقيود
 * بدء الخدمات الأمامية من الخلفية — وهذا بالضبط ما يعتمد عليه هذا التصميم.
 */
class AdhanPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPlaybackAndSelf()
            return START_NOT_STICKY
        }

        val prayerKey = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: "FAJR"
        val prayerLabel = prayerDisplayName(prayerKey)

        // نمرّر نوع الخدمة صراحةً (متطلب أندرويد 14+) بدل الاعتماد الضمني على المانيفست
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(prayerLabel), serviceType)

        acquireWakeLockBriefly()
        startPlayback()

        return START_NOT_STICKY
    }

    private fun prayerDisplayName(key: String): String = when (key) {
        "FAJR" -> getString(R.string.prayer_name_fajr)
        "DHUHR" -> getString(R.string.prayer_name_dhuhr)
        "ASR" -> getString(R.string.prayer_name_asr)
        "MAGHRIB" -> getString(R.string.prayer_name_maghrib)
        "ISHA" -> getString(R.string.prayer_name_isha)
        else -> key
    }

    private fun buildNotification(prayerLabel: String): Notification {
        val stopIntent = Intent(this, AdhanPlaybackService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationHelper.ADHAN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_adhan)
            .setContentTitle(getString(R.string.adhan_time_title, prayerLabel))
            .setContentText(getString(R.string.adhan_time_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.adhan_stop_action), stopPendingIntent)
            .build()
    }

    private fun startPlayback() {
        // إن وصل تنبيه ثانٍ أثناء تشغيل أذان سابق، نحرّر المشغّل القديم أولاً (منع صوتين متداخلين)
        releasePlayer()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                val afd = resources.openRawResourceFd(R.raw.adhan_sound)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = false
                setOnCompletionListener { stopPlaybackAndSelf() }
                setOnErrorListener { _, _, _ -> stopPlaybackAndSelf(); true }
                prepare()
                start()
            }
        } catch (e: Exception) {
            stopPlaybackAndSelf()
        }
    }

    private fun acquireWakeLockBriefly() {
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AnaMuslim:AdhanPlayback")
            wakeLock?.acquire(6 * 60 * 1000L) // 6 دقائق كحد أقصى احتياطي (مدة الأذان المرفق ~5.3 دقيقة)
        } catch (e: Exception) { /* ليست حرجة */ }
    }

    private fun releasePlayer() {
        try {
            mediaPlayer?.let { if (it.isPlaying) it.stop() }
        } catch (e: Exception) { /* تجاهل */ }
        try {
            mediaPlayer?.release()
        } catch (e: Exception) { /* تجاهل */ }
        mediaPlayer = null
    }

    /** تنظيف الموارد فقط (بدون stopSelf) — يُستدعى أيضاً من onDestroy بأمان. */
    private fun releaseResources() {
        releasePlayer()
        try { wakeLock?.let { if (it.isHeld) it.release() } } catch (e: Exception) { }
        wakeLock = null
    }

    private fun stopPlaybackAndSelf() {
        releaseResources()
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        releaseResources()
        super.onDestroy()
    }
}
