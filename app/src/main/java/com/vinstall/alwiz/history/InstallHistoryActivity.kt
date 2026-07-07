package com.vinstall.alwiz.history

import android.os.Bundle
import android.view.MenuItem
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.vinstall.alwiz.R
import com.vinstall.alwiz.databinding.ActivityInstallHistoryBinding
import com.vinstall.alwiz.model.HistoryStatus
import com.vinstall.alwiz.model.InstallHistoryEntry
import com.vinstall.alwiz.settings.AppSettings
import com.vinstall.alwiz.settings.DialogHelper
import com.vinstall.alwiz.settings.DialogStyle
import com.vinstall.alwiz.util.FileUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InstallHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInstallHistoryBinding
    private lateinit var adapter: InstallHistoryAdapter
    private val entries = mutableListOf<InstallHistoryEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstallHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.history_title)

        adapter = InstallHistoryAdapter(
            entries = entries,
            onItemClick = { entry -> showDetailDialog(entry) },
            onDeleteClick = { entry, position -> deleteEntry(entry, position) }
        )

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
                    entries.clear()
                    adapter.notifyDataSetChanged()
                    updateEmptyState()
                    Toast.makeText(this, getString(R.string.history_cleared), Toast.LENGTH_SHORT).show()
                }
            )
        }

        loadHistory()
    }

    private fun loadHistory() {
        entries.clear()
        entries.addAll(InstallHistoryManager.getAll(this))
        adapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val isEmpty = entries.isEmpty()
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
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())

        val statusLabel = when (entry.status) {
            HistoryStatus.SUCCESS -> getString(R.string.history_status_success)
            HistoryStatus.FAILED -> getString(R.string.history_status_failed)
            HistoryStatus.CANCELLED -> getString(R.string.history_status_cancelled)
        }

        val durationText = when {
            entry.durationMs < 1000 -> "${entry.durationMs}ms"
            else -> String.format("%.1fs", entry.durationMs / 1000.0)
        }

        val detail = buildString {
            appendLine("${getString(R.string.history_detail_status)}: $statusLabel")
            appendLine("${getString(R.string.history_detail_package)}: ${entry.packageName.ifEmpty { "-" }}")
            appendLine("${getString(R.string.history_detail_version)}: ${entry.versionName.ifEmpty { "-" }}")
            appendLine("${getString(R.string.history_detail_format)}: ${entry.format}")
            appendLine("${getString(R.string.history_detail_size)}: ${FileUtil.formatSize(entry.fileSize)}")
            appendLine("${getString(R.string.history_detail_mode)}: ${entry.installMode}")
            appendLine("${getString(R.string.history_detail_duration)}: $durationText")
            appendLine("${getString(R.string.history_detail_time)}: ${dateFormat.format(Date(entry.timestamp))}")
            if (entry.detail.isNotEmpty()) {
                appendLine()
                appendLine("${getString(R.string.history_detail_info)}:")
                append(entry.detail)
            }
        }

        val tv = TextView(this).apply {
            text = detail
            textSize = 13f
            setPadding(48, 32, 48, 32)
            setTextIsSelectable(true)
        }

        val title = entry.appLabel.ifEmpty { entry.packageName }

        if (AppSettings.getDialogStyle(this) == DialogStyle.BOTTOM_SHEET) {
            val sheet = BottomSheetDialog(this)
            val scroll = ScrollView(this).apply { addView(tv) }
            val container = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                val titleView = TextView(this@InstallHistoryActivity).apply {
                    text = title
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(48, 40, 48, 0)
                }
                addView(titleView)
                addView(scroll)
                val closeBtn = android.widget.Button(this@InstallHistoryActivity).apply {
                    text = getString(R.string.crash_log_close)
                    isAllCaps = false
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setOnClickListener { sheet.dismiss() }
                }
                val btnLayout = android.widget.LinearLayout(this@InstallHistoryActivity).apply {
                    gravity = android.view.Gravity.END
                    setPadding(16, 0, 16, 16)
                    addView(closeBtn)
                }
                addView(btnLayout)
            }
            sheet.setContentView(container)
            sheet.setOnShowListener {
                val bottomSheet = sheet.findViewById<android.view.View>(
                    com.google.android.material.R.id.design_bottom_sheet
                )
                if (bottomSheet != null) {
                    val behavior = BottomSheetBehavior.from(bottomSheet)
                    behavior.skipCollapsed = true
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
            sheet.show()
        } else {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setView(tv)
                .setPositiveButton(getString(R.string.crash_log_close), null)
                .show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
