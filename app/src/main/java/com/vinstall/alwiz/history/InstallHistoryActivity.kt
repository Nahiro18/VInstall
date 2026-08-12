package com.vinstall.alwiz.history

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.vinstall.alwiz.R
import com.vinstall.alwiz.databinding.ActivityInstallHistoryBinding
import com.vinstall.alwiz.model.HistoryStatus
import com.vinstall.alwiz.model.InstallHistoryEntry
import com.vinstall.alwiz.settings.DialogHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InstallHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInstallHistoryBinding
    private lateinit var adapter: InstallHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        com.vinstall.alwiz.settings.AppSettings.applyActivityTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityInstallHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.history_title)

        // --- MEJORA: El adaptador ya no recibe una lista externa ---
        adapter = InstallHistoryAdapter(
            onItemClick = { entry -> showDetailDialog(entry) },
            onDeleteClick = { entry, position -> deleteEntry(entry, position) }
        )
        // ------------------------------------------------------------

        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistory.adapter = adapter

        binding.btnClearHistory.setOnClickListener {
            DialogHelper.showConfirmation(
                activity = this,
                title = getString(R.string.history_clear_title),
                message = getString(R.string.history_clear_confirm),
                positiveLabel = getString(R.string.history_clear_yes),
                negativeLabel = getString(R.string.cancel),
                isDangerous = true,
                onConfirm = {
                    InstallHistoryManager.clear(this)
                    adapter.clear()
                    updateEmptyState()
                    Toast.makeText(this, getString(R.string.history_cleared), Toast.LENGTH_SHORT).show()
                }
            )
        }

        loadHistory()
    }

    private fun loadHistory() {
        val entries = InstallHistoryManager.getAll(this)
        adapter.submitList(entries)
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val isEmpty = adapter.isEmpty()
        binding.textEmpty.isVisible = isEmpty
        binding.recyclerHistory.isVisible = !isEmpty
        binding.btnClearHistory.isEnabled = !isEmpty
    }

    private fun deleteEntry(entry: InstallHistoryEntry, position: Int) {
        InstallHistoryManager.remove(this, entry.id)
        adapter.removeAt(position)
        updateEmptyState()
    }

    private fun showDetailDialog(entry: InstallHistoryEntry) {
        val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
        
        val statusText = when (entry.status) {
            HistoryStatus.SUCCESS -> getString(R.string.history_status_success)
            HistoryStatus.FAILED -> getString(R.string.history_status_failed)
            HistoryStatus.CANCELLED -> getString(R.string.history_status_cancelled)
        }
        
        val message = buildString {
            appendLine("${getString(R.string.history_detail_status)}: $statusText")
            appendLine("${getString(R.string.history_detail_package)}: ${entry.packageName}")
            appendLine("${getString(R.string.history_detail_version)}: v${entry.versionName}")
            appendLine("${getString(R.string.history_detail_format)}: ${entry.format}")
            appendLine("${getString(R.string.history_detail_size)}: ${entry.fileSize} bytes")
            appendLine("${getString(R.string.history_detail_mode)}: ${entry.installMode}")
            appendLine("${getString(R.string.history_detail_duration)}: ${entry.durationMs}ms")
            appendLine("${getString(R.string.history_detail_time)}: ${fmt.format(Date(entry.timestamp))}")
            if (entry.detail.isNotEmpty()) {
                appendLine("${getString(R.string.history_detail_info)}: ${entry.detail}")
            }
        }

        AlertDialog.Builder(this)
            .setTitle(entry.appLabel.ifBlank { entry.packageName })
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
