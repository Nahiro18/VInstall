package com.vinstall.alwiz.history

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vinstall.alwiz.model.InstallHistoryEntry
import java.io.File

object InstallHistoryManager {

    private const val FILE_NAME = "install_history.json"
    private val gson = Gson()

    private fun historyFile(context: Context): File =
        File(context.filesDir, FILE_NAME)

    fun getAll(context: Context): List<InstallHistoryEntry> {
        val file = historyFile(context)
        if (!file.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<InstallHistoryEntry>>() {}.type
            gson.fromJson(file.readText(), type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun add(context: Context, entry: InstallHistoryEntry) {
        val current = getAll(context).toMutableList()
        current.add(0, entry)
        save(context, current)
    }

    fun remove(context: Context, id: String) {
        val updated = getAll(context).filter { it.id != id }
        save(context, updated)
    }

    fun clear(context: Context) {
        historyFile(context).delete()
    }

    fun count(context: Context): Int = getAll(context).size

    private fun save(context: Context, entries: List<InstallHistoryEntry>) {
        historyFile(context).writeText(gson.toJson(entries))
    }
}
