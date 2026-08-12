package com.vinstall.alwiz.history

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vinstall.alwiz.model.InstallHistoryEntry
import java.io.File

object InstallHistoryManager {

    private const val FILE_NAME = "install_history.json"
    private val gson = Gson()
    
    // --- NUEVO: Caché en memoria para evitar I/O excesivo (O(N^2) en lotes) ---
    @Volatile
    private var cachedHistory: MutableList<InstallHistoryEntry>? = null
    // ------------------------------------------------------------------------

    private fun historyFile(context: Context): File =
        File(context.filesDir, FILE_NAME)

    @Synchronized
    fun getAll(context: Context): List<InstallHistoryEntry> {
        // Si ya lo tenemos en memoria, lo devolvemos directamente (¡Súper rápido!)
        cachedHistory?.let { return it.toList() }
        
        val file = historyFile(context)
        val loaded: MutableList<InstallHistoryEntry> = if (!file.exists()) {
            mutableListOf()
        } else {
            try {
                val type = object : TypeToken<MutableList<InstallHistoryEntry>>() {}.type
                gson.fromJson(file.readText(), type) ?: mutableListOf()
            } catch (_: Exception) {
                mutableListOf()
            }
        }
        cachedHistory = loaded
        return loaded.toList()
    }

    @Synchronized
    fun add(context: Context, entry: InstallHistoryEntry) {
        val current = cachedHistory ?: getAll(context).toMutableList()
        current.add(0, entry)
        cachedHistory = current
        save(context, current)
    }

    @Synchronized
    fun remove(context: Context, id: String) {
        val current = cachedHistory ?: getAll(context).toMutableList()
        val updated = current.filter { it.id != id }.toMutableList()
        cachedHistory = updated
        save(context, updated)
    }

    @Synchronized
    fun clear(context: Context) {
        historyFile(context).delete()
        cachedHistory = mutableListOf()
    }

    @Synchronized
    fun count(context: Context): Int {
        return (cachedHistory ?: getAll(context)).size
    }

    @Synchronized
    private fun save(context: Context, entries: List<InstallHistoryEntry>) {
        historyFile(context).writeText(gson.toJson(entries))
    }
}
