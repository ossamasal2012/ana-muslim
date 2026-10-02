package com.anamuslim.app.ui.update

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anamuslim.app.R
import com.anamuslim.app.data.update.ApkDownloadManagerHelper
import com.anamuslim.app.data.update.VersionInfo
import kotlinx.coroutines.launch

/** تُعرض إلزامياً عند وجود تحديث: عنوان + زر تثبيت واحد فقط (بدون "لاحقاً"، لأن كل التحديثات إلزامية حالياً). */
@Composable
fun UpdateDialogHost(info: VersionInfo, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val helper = remember { ApkDownloadManagerHelper(context) }
    val scope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var percent by remember { mutableStateOf(0) }
    var downloadedBytes by remember { mutableStateOf(0L) }
    var totalBytes by remember { mutableStateOf(0L) }

    AlertDialog(
        onDismissRequest = { /* تحديث إلزامي: لا يُغلق الحوار بالضغط خارجه */ },
        title = { Text(stringResource(R.string.update_available_title)) },
        text = {
            Column {
                if (!isDownloading) {
                    Text(stringResource(R.string.update_available_message, info.versionName))
                    if (info.releaseNotes.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(info.releaseNotes, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Text(stringResource(R.string.update_downloading))
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(
                            R.string.update_download_progress,
                            percent,
                            formatBytes(downloadedBytes),
                            formatBytes(totalBytes)
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            if (!isDownloading) {
                TextButton(onClick = {
                    isDownloading = true
                    val id = helper.startDownload(info)
                    scope.launch {
                        helper.observeProgress(id).collect { progress ->
                            percent = progress.percent
                            downloadedBytes = progress.bytesDownloaded
                            totalBytes = progress.totalBytes
                            if (progress.isDone) {
                                // إن كان التطبيق لا يزال بالواجهة الأمامية أثناء اكتمال التحميل، نثبّت فوراً
                                helper.buildInstallIntent()?.let { context.startActivity(it) }
                                onDismissRequest()
                            }
                        }
                    }
                }) { Text(stringResource(R.string.update_install_button)) }
            }
        }
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / 1_048_576.0
    return "%.1f MB".format(mb)
}
