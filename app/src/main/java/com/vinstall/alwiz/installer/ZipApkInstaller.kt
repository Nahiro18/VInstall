package com.vinstall.alwiz.installer

import android.content.Context
import android.net.Uri
import com.vinstall.alwiz.util.DebugLog
import java.io.File

object ZipApkInstaller {

    suspend fun install(
        context: Context,
        uri: Uri,
        onStep: (String) -> Unit,
        selectedSplits: List<String>? = null,
        onProgress: ((Float) -> Unit)? = null
    ): Result<Unit> {
        return try {
            onStep("Inspecting ZIP contents...")
            val cacheDir = File(context.cacheDir, "zip_extract").also {
                it.deleteRecursively()
                it.mkdirs()
            }
            
            val apkFiles = ZipSplitExtractor.extractApks(context, uri, cacheDir, onStep, reportTotalProgress = false)
            
            if (apkFiles.isEmpty()) {
                return Result.failure(Exception("No APK files found inside the ZIP archive"))
            }
            
            DebugLog.i("ZipApkInstaller", "Found ${apkFiles.size} APK(s) in ZIP")
            onStep("Installing APK(s)...")
            SplitInstaller.installSplits(context, apkFiles, selectedSplits, onProgress)
        } catch (e: Exception) {
            DebugLog.e("ZipApkInstaller", "Install failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun listSplits(context: Context, uri: Uri): List<String> {
        return ZipSplitExtractor.listSplits(context, uri)
    }
}
