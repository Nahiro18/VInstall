package com.vinstall.alwiz.installer

import android.content.Context
import android.net.Uri
import java.io.File

object ApksInstaller {

    suspend fun install(
        context: Context,
        uri: Uri,
        onStep: (String) -> Unit,
        selectedSplits: List<String>? = null,
        onProgress: ((Float) -> Unit)? = null
    ): Result<Unit> {
        return try {
            onStep("Extracting splits...")
            val cacheDir = File(context.cacheDir, "apks_extract").also {
                it.deleteRecursively()
                it.mkdirs()
            }
            
            val apkFiles = ZipSplitExtractor.extractApks(context, uri, cacheDir, onStep, reportTotalProgress = true)
            
            if (apkFiles.isEmpty()) return Result.failure(Exception("No APK splits found in archive"))
            onStep("Installing splits...")
            SplitInstaller.installSplits(context, apkFiles, selectedSplits, onProgress)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listSplits(context: Context, uri: Uri): List<String> {
        return ZipSplitExtractor.listSplits(context, uri)
    }
}
