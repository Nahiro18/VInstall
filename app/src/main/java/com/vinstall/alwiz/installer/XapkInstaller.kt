package com.vinstall.alwiz.installer

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.google.gson.GsonBuilder
import com.vinstall.alwiz.parser.XapkManifest
import com.vinstall.alwiz.parser.XapkManifestDeserializer
import com.vinstall.alwiz.util.DebugLog
import com.vinstall.alwiz.util.FileUtil
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

object XapkInstaller {

    private val gson = GsonBuilder()
        .registerTypeAdapter(XapkManifest::class.java, XapkManifestDeserializer())
        .create()

    suspend fun install(
        context: Context,
        uri: Uri,
        onStep: (String) -> Unit,
        selectedSplits: List<String>? = null,
        onProgress: ((Float) -> Unit)? = null
    ): Result<Unit> {
        return try {
            onStep("Extracting package...")
            val cacheDir = File(context.cacheDir, "xapk_extract").also {
                it.deleteRecursively()
                it.mkdirs()
            }

            val manifest = extractWithZipInputStream(context, uri, cacheDir, onStep)
                ?: return Result.failure(Exception("Invalid XAPK: manifest.json not found in archive"))

            DebugLog.i("XapkInstaller", "Manifest parsed: pkg=${manifest.packageName} splitBundle=${manifest.isSplitApkBundle()} expansions=${manifest.hasExpansions()}")

            if (manifest.isSplitApkBundle()) {
                onStep("Installing split APKs...")
                val apkFiles = manifest.splitApks!!.mapNotNull { entry ->
                    val f = File(cacheDir, File(entry.file).name)
                    if (f.exists()) f else {
                        DebugLog.e("XapkInstaller", "Split APK not found in cache: ${entry.file}")
                        null
                    }
                }
                if (apkFiles.isEmpty()) {
                    return Result.failure(Exception("No split APK files could be extracted from XAPK"))
                }
                SplitInstaller.installSplits(context, apkFiles, selectedSplits, onProgress)
            } else {
                val mainApk = cacheDir.listFiles { f -> f.name.endsWith(".apk") }?.firstOrNull()
                    ?: return Result.failure(Exception("No APK found inside XAPK"))

                if (manifest.hasExpansions()) {
                    onStep("Copying expansion data...")
                    manifest.expansions?.forEach { exp ->
                        val src = File(cacheDir, File(exp.file).name)
                        if (src.exists()) {
                            val dest = File(Environment.getExternalStorageDirectory(), exp.installPath)
                            dest.parentFile?.mkdirs()
                            src.copyTo(dest, overwrite = true)
                            DebugLog.d("XapkInstaller", "OBB copied to ${dest.absolutePath}")
                        }
                    }
                }

                onStep("Installing APK...")
                SplitInstaller.installSplits(context, listOf(mainApk), onProgress = onProgress)
            }
        } catch (e: Exception) {
            DebugLog.e("XapkInstaller", "Install failed: ${e.message}")
            Result.failure(e)
        }
    }

    // --- MODIFICATION: Use ZipInputStream instead of copying entire file to disk ---
    fun listSplits(context: Context, uri: Uri): List<String> {
        val splits = mutableListOf<String>()
        val stream = FileUtil.openStream(context, uri) ?: return splits
        
        try {
            ZipInputStream(stream.buffered(FileUtil.BUFFER_SIZE)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".apk")) {
                        splits.add(File(entry.name).name)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            DebugLog.e("XapkInstaller", "listSplits error: ${e.message}")
        }
        return splits
    }
    // ---------------------------------------------------------------------------------------

    private fun extractWithZipInputStream(
        context: Context,
        uri: Uri,
        outDir: File,
        onStep: (String) -> Unit
    ): XapkManifest? {
        val stream = FileUtil.openStream(context, uri) ?: return null
        var manifest: XapkManifest? = null
        try {
            ZipInputStream(stream.buffered(FileUtil.BUFFER_SIZE)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        !entry.isDirectory && isManifestEntry(name) -> {
                            val text = zip.readBytes().toString(Charsets.UTF_8)
                                .trimStart('\uFEFF')
                                .trim()
                            DebugLog.d("XapkInstaller", "manifest.json found in stream, raw: ${text.take(512)}")
                            manifest = gson.fromJson(text, XapkManifest::class.java)
                        }
                        !entry.isDirectory && (name.endsWith(".apk") || name.endsWith(".obb")) -> {
                            val fileName = File(name).name
                            val outFile = File(outDir, fileName)
                            onStep("Extracting $fileName...")
                            outFile.outputStream().buffered(FileUtil.BUFFER_SIZE).use { out ->
                                zip.copyTo(out, FileUtil.BUFFER_SIZE)
                            }
                            DebugLog.d("XapkInstaller", "Extracted: $fileName (${outFile.length()} bytes)")
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            DebugLog.e("XapkInstaller", "extractWithZipInputStream error: ${e.message}")
        }
        return manifest
    }

    private fun isManifestEntry(name: String): Boolean {
        val normalized = name.replace('\\', '/').trimStart('/')
        return normalized.equals("manifest.json", ignoreCase = true)
            || normalized.endsWith("/manifest.json", ignoreCase = true)
    }
}
