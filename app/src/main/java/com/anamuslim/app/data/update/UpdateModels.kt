package com.anamuslim.app.data.update

import kotlinx.serialization.Serializable

/**
 * صيغة ملف version.json الذي يولّده GitHub Actions تلقائياً عند كل إصدار.
 * ⚠️ هذا الملف لا يُلمس يدوياً أبداً — فقط عدّل version.properties (راجع README).
 */
@Serializable
data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val apkSizeBytes: Long = 0L,
    val releaseNotes: String = ""
)
