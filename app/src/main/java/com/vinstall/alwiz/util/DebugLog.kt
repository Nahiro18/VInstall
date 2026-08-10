package com.vinstall.alwiz.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

object DebugLog {

    private const val MAX_ENTRIES = 300
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    // --- OPTIMIZACIÓN: ArrayDeque es mucho más eficiente (O(1)) para agregar/quitar elementos ---
    private val logQueue = ArrayDeque<String>(MAX_ENTRIES)
    // --------------------------------------------------------------------------------------------

    private val _entries = MutableStateFlow<List<String>>(emptyList())
    val entries: StateFlow<List<String>> = _entries

    fun d(tag: String, msg: String) {
        Log.d("VInstall/$tag", msg)
        append("D/$tag: $msg")
    }

    fun e(tag: String, msg: String) {
        Log.e("VInstall/$tag", msg)
        append("E/$tag: $msg")
    }

    fun i(tag: String, msg: String) {
        Log.i("VInstall/$tag", msg)
        append("I/$tag: $msg")
    }

    fun w(tag: String, msg: String) {
        Log.w("VInstall/$tag", msg)
        append("W/$tag: $msg")
    }

    private fun append(line: String) {
        val ts = fmt.format(Date())
        val entry = "[$ts] $line"

        // --- CORRECCIÓN: Sincronización para evitar crashes por concurrencia ---
        synchronized(logQueue) {
            logQueue.addLast(entry)
            
            // Si excedemos el límite, eliminamos el más antiguo en O(1) tiempo
            if (logQueue.size > MAX_ENTRIES) {
                logQueue.removeFirst()
            }
            
            // Emitimos una snapshot rápida a la UI (DebugWindow)
            _entries.value = logQueue.toList()
        }
        // -----------------------------------------------------------------------
    }

    fun clear() {
        synchronized(logQueue) {
            logQueue.clear()
            _entries.value = emptyList()
        }
    }

    fun getAll(): String {
        return synchronized(logQueue) {
            logQueue.joinToString("\n")
        }
    }
}
