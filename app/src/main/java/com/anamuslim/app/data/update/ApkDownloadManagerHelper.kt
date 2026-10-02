package com.anamuslim.app.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

data class DownloadProgress(val bytesDownloaded: Long, val totalBytes: Long, val percent: Int, val isDone: Boolean)

/**
 * يستخدم DownloadManager النظامي في أندرويد (وليس تنزيلاً يدوياً داخل التطبيق) عمداً،
 * لأنه الوحيد الذي:
 *  - يستمر بالتنزيل حتى لو أُغلق التطبيق كلياً (خدمة نظام مستقلة).
 *  - يُظهر تقدّماً حقيقياً بشريط الإشعارات تلقائياً دون أي كود إضافي.
 *  - يتعامل مع انقطاع الشبكة/الاستئناف بشكل موثوق.
 */
class ApkDownloadManagerHelper(private val context: Context) {

    companion object {
        private const val APK_FILE_NAME = "ana-muslim-update.apk"
    }

    private val downloadManager get() = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private fun targetFile(): File =
        File(context.getExternalFilesDir("updates"), APK_FILE_NAME)

    /** يبدأ تنزيلاً جديداً، بعد حذف أي ملف تحديث قديم متبقٍ من محاولة سابقة (لمنع تقديم نسخة قديمة خطأً). */
    fun startDownload(info: VersionInfo): Long {
        targetFile().let { if (it.exists()) it.delete() }
        targetFile().parentFile?.mkdirs()

        val request = DownloadManager.Request(Uri.parse(info.apkUrl))
            .setTitle(context.getString(com.anamuslim.app.R.string.app_name))
            .setDescription(context.getString(com.anamuslim.app.R.string.update_downloading))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(targetFile()))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val id = downloadManager.enqueue(request)
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("active_download_id", id).putInt("pending_version_code", info.versionCode).apply()
        return id
    }

    /** تدفّق يستطلع تقدّم التنزيل دورياً (DownloadManager لا يوفّر بثّاً مباشراً للتقدّم). */
    fun observeProgress(downloadId: Long): Flow<DownloadProgress> = flow {
        while (true) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            if (cursor == null || !cursor.moveToFirst()) {
                cursor?.close()
                emit(DownloadProgress(0, 0, 0, isDone = true))
                break
            }
            val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val downloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val status = cursor.getInt(statusIdx)
            val downloaded = cursor.getLong(downloadedIdx)
            val total = cursor.getLong(totalIdx)
            cursor.close()

            val percent = if (total > 0) ((downloaded * 100) / total).toInt() else 0
            val done = status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED
            emit(DownloadProgress(downloaded, total, percent, isDone = done))
            if (done) break
            delay(400)
        }
    }

    /** يبني نية تثبيت APK آمنة عبر FileProvider (مطلوب من أندرويد 7 فصاعداً). */
    fun buildInstallIntent(): Intent? {
        val file = targetFile()
        if (!file.exists()) return null
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

    /** هل يوجد تنزيل مكتمل لم يُثبَّت بعد؟ (حالة: المستخدم كان خارج التطبيق عند اكتمال التحميل). */
    fun hasCompletedPendingInstall(): Boolean {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        val pendingCode = prefs.getInt("pending_version_code", -1)
        return pendingCode > 0 && targetFile().exists() && targetFile().length() > 0
    }

    fun clearPendingInstallFlag() {
        context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE).edit()
            .remove("pending_version_code").remove("active_download_id").apply()
    }

    /**
     * ينظّف أي ملفات تحديث متبقية بعد نجاح التثبيت فعلياً (يُستدعى عند بدء التطبيق
     * إذا لاحظنا أن BuildConfig.VERSION_CODE الحالي أصبح مطابقاً أو أكبر من
     * pending_version_code المحفوظ — أي أن التحديث نجح ولم يعد الملف مطلوباً).
     */
    fun cleanupAfterSuccessfulInstall() {
        targetFile().let { if (it.exists()) it.delete() }
        clearPendingInstallFlag()
    }
}
