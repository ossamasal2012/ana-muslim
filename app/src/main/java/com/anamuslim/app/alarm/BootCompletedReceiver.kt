package com.anamuslim.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val ACTION_EXACT_ALARM_PERMISSION_CHANGED =
    "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"

/**
 * يعيد جدولة كل تنبيهات الأذان عند:
 *  - إعادة تشغيل الجهاز (تُمسح منبهات AlarmManager تلقائياً عند كل ريستارت).
 *  - تحديث التطبيق نفسه (تُمسح منبهاته أيضاً عند استبدال الحزمة).
 *  - منح المستخدم إذن المنبهات الدقيقة لاحقاً (أندرويد يحذف كل المنبهات الدقيقة عند
 *    سحب الإذن، وتوصيته الرسمية إعادة الجدولة عند وصول هذا البث).
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != ACTION_EXACT_ALARM_PERMISSION_CHANGED
        ) return

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
