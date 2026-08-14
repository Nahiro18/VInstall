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

    // --- OPTIMIZATION: ArrayDeque is much more efficient (O(1)) for adding/removing elements ---
    private val logQueue = ArrayDeque<String>(MAX_ENTRIES)
    // -----------------------------------------------------------------------------

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

        // --- FIX: Synchronization to avoid crashes from concurrent access ---
        synchronized(logQueue) {
            logQueue.addLast(entry)
            
            // If we exceed the limit, remove the oldest one in O(1) time
            if (logQueue.size > MAX_ENTRIES) {
                logQueue.removeFirst()
            }
            
            // Emit a quick snapshot to the UI (DebugWindow)
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
