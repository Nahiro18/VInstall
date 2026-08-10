package com.vinstall.alwiz.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.vinstall.alwiz.R
import com.vinstall.alwiz.databinding.ItemHistoryEntryBinding
import com.vinstall.alwiz.model.HistoryStatus
import com.vinstall.alwiz.model.InstallHistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InstallHistoryAdapter(
    private val onItemClick: (InstallHistoryEntry) -> Unit,
    private val onDeleteClick: (InstallHistoryEntry, Int) -> Unit
) : RecyclerView.Adapter<InstallHistoryAdapter.ViewHolder>() {

    // --- MEJORA: El adaptador maneja su propia lista interna ---
    private val entries = mutableListOf<InstallHistoryEntry>()
    // ----------------------------------------------------------

    inner class ViewHolder(val binding: ItemHistoryEntryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: InstallHistoryEntry) {
            val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            
            binding.textAppName.text = entry.appLabel.ifBlank { entry.packageName }
            binding.textPackageName.text = entry.packageName
            binding.textVersion.text = "v${entry.versionName}"
            binding.textDate.text = fmt.format(Date(entry.timestamp))
            binding.textFormat.text = entry.format
            binding.textMode.text = entry.installMode.lowercase().replaceFirstChar { it.uppercase() }
            
            // Configurar color según estado
            val (statusText, statusColor) = when (entry.status) {
                HistoryStatus.SUCCESS -> "Success" to 0xFF4CAF50.toInt()
                HistoryStatus.FAILED -> "Failed" to 0xFFF44336.toInt()
                HistoryStatus.CANCELLED -> "Cancelled" to 0xFFFF9800.toInt()
            }
            binding.textStatus.text = statusText
            binding.textStatus.setTextColor(statusColor)
            
            binding.root.setOnClickListener { onItemClick(entry) }
            binding.btnDelete.setOnClickListener { 
                onDeleteClick(entry, adapterPosition) 
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int = entries.size

    // --- MEJORA: Métodos claros para actualizar datos ---
    fun submitList(newEntries: List<InstallHistoryEntry>) {
        entries.clear()
        entries.addAll(newEntries)
        notifyDataSetChanged()
    }

    fun removeAt(position: Int) {
        if (position < 0 || position >= entries.size) return
        entries.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, entries.size - position)
    }

    fun clear() {
        val size = entries.size
        entries.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun isEmpty(): Boolean = entries.isEmpty()
    // ----------------------------------------------------
}
