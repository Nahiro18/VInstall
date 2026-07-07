package com.vinstall.alwiz.model

import android.graphics.Bitmap
import android.net.Uri

sealed class InstallState {
    object Idle : InstallState()

    object FileLoading : InstallState()

    data class PasswordRequired(
        val uri: Uri,
        val fileName: String,
        val packageName: String,
        val versionName: String,
        val label: String
    ) : InstallState()

    data class FileSelected(
        val uri: Uri,
        val name: String,
        val size: Long,
        val format: PackageFormat,
        val splits: List<String> = emptyList(),
        val hasSplits: Boolean = false,
        val packageName: String = "",
        val versionName: String = "",
        val versionCode: Long = 0L,
        val appLabel: String = "",
        val appIcon: Bitmap? = null,
        val hash: String = "",
        val isEncryptedApkv: Boolean = false,
        val apkvPassword: String? = null,
        val minSdk: Int = 0,
        val targetSdk: Int = 0
    ) : InstallState()

    object Analyzing : InstallState()

    data class Installing(val step: String, val progress: Float = -1f) : InstallState()

    data class Success(val packageName: String = "") : InstallState()

    data class Error(val message: String) : InstallState()

    data class Cancelled(val reason: String) : InstallState()
}
