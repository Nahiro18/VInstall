package com.vinstall.alwiz.installer

import android.content.Context
import android.net.Uri
import com.vinstall.alwiz.util.DebugLog
import com.vinstall.alwiz.util.FileUtil
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Utilidad centralizada para extraer archivos APK de archivos ZIP.
 * Elimina duplicación de código entre ApksInstaller, ApkmInstaller y ZipApkInstaller.
 */
object ZipSplitExtractor {

    /**
     * Lista todos los archivos .apk dentro de un archivo ZIP sin extraerlos.
     * 
     * @param context Contexto de Android
     * @param uri URI del archivo ZIP
     * @return Lista de nombres de archivos APK encontrados
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
     * Extrae todos los archivos .apk de un ZIP a un directorio de salida.
     * 
     * @param context Contexto de Android
     * @param uri URI del archivo ZIP
     * @param outDir Directorio donde extraer los archivos
     * @param onStep Callback para reportar progreso de extracción
     * @param reportTotalProgress Si es true, reporta progreso total basado en bytes extraídos
     * @return Lista de archivos APK extraídos
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
            // Con progreso total (usado por ApksInstaller y ApkmInstaller)
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
            // Sin progreso total detallado (usado por ZipApkInstaller)
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
