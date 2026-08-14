package com.vinstall.alwiz.installer

import android.content.Context
import android.net.Uri
import com.vinstall.alwiz.util.DebugLog
import com.vinstall.alwiz.util.FileUtil
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Centralized utility for extracting APK files from ZIP archives.
 * Reduces code duplication between ApksInstaller, ApkmInstaller, and ZipApkInstaller.
 */
object ZipSplitExtractor {

    /**
     * Lists all .apk files within a ZIP archive without extracting them.
     * 
     * @param context Android context
     * @param uri URI of the ZIP file
     * @return List of APK file names found
     */
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
            DebugLog.e("ZipSplitExtractor", "listSplits error: ${e.message}")
        }
        
        return splits
    }

    /**
     * Extracts all .apk files from a ZIP to an output directory.
     * 
     * @param context Android context
     * @param uri URI of the ZIP file
     * @param outDir Directory to extract files to
     * @param onStep Callback for reporting extraction progress
     * @param reportTotalProgress If true, reports total progress based on bytes extracted
     * @return List of extracted APK files
     */
    fun extractApks(
        context: Context,
        uri: Uri,
        outDir: File,
        onStep: (String) -> Unit,
        reportTotalProgress: Boolean = true
    ): List<File> {
        val extractedFiles = mutableListOf<File>()
        
        if (reportTotalProgress) {
            // With total progress (used by ApksInstaller and ApkmInstaller)
            val totalSize = FileUtil.getFileSize(context, uri).coerceAtLeast(1L)
            var extractedBytes = 0L
            
            val stream = FileUtil.openStream(context, uri) ?: return extractedFiles
            ZipInputStream(stream.buffered(FileUtil.BUFFER_SIZE)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".apk")) {
                        val fileName = File(entry.name).name
                        val outFile = File(outDir, fileName)
                        
                        outFile.outputStream().buffered(FileUtil.BUFFER_SIZE).use { outStream ->
                            FileUtil.copyWithProgress(zip, outStream, entry.size.coerceAtLeast(1L)) { pct ->
                                onStep("Extracting $fileName… $pct%")
                            }
                        }
                        
                        extractedFiles.add(outFile)
                        extractedBytes += outFile.length()
                        
                        if (reportTotalProgress) {
                            val totalPct = ((extractedBytes * 100) / totalSize).coerceIn(0, 100)
                            onStep("Extracting splits… $totalPct%")
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } else {
            // Without detailed total progress (used by ZipApkInstaller)
            val stream = FileUtil.openStream(context, uri) ?: return extractedFiles
            ZipInputStream(stream.buffered(FileUtil.BUFFER_SIZE)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".apk")) {
                        val fileName = File(entry.name).name
                        val outFile = File(outDir, fileName)
                        
                        onStep("Extracting $fileName...")
                        outFile.outputStream().buffered(FileUtil.BUFFER_SIZE).use { outStream ->
                            zip.copyTo(outStream, FileUtil.BUFFER_SIZE)
                        }
                        
                        extractedFiles.add(outFile)
                        DebugLog.d("ZipSplitExtractor", "Extracted: $fileName")
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        
        return extractedFiles
    }
}
