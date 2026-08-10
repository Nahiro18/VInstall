package com.vinstall.alwiz.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vinstall.alwiz.databinding.ItemHistoryBinding
import com.vinstall.alwiz.model.HistoryStatus
import com.vinstall.alwiz.model.InstallHistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InstallHistoryAdapter(
    private val onItemClick: (InstallHistoryEntry) -> Unit,
    private val onDeleteClick: (InstallHistoryEntry, Int) -> Unit
) : RecyclerView.Adapter<InstallHistoryAdapter.ViewHolder>() {

    private val entries = mutableListOf<InstallHistoryEntry>()

    inner class ViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: InstallHistoryEntry) {
            val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            
            // Nombre de la app
            binding.textAppLabel.text = entry.appLabel.ifBlank { entry.packageName }
            
            // Package name
            binding.textPackageName.text = entry.packageName
            
            // Versión y formato combinados (según tu XML real)
            binding.textVersionFormat.text = "v${entry.versionName} · ${entry.format}"
            
            // Fecha
            binding.textDate.text = fmt.format(Date(entry.timestamp))
            
            // Configurar texto y color del indicador de estado
            val (statusText, statusColor) = when (entry.status) {
                HistoryStatus.SUCCESS -> "Success" to 0xFF4CAF50.toInt()   // Verde
                HistoryStatus.FAILED -> "Failed" to 0xFFF44336.toInt()     // Rojo
                HistoryStatus.CANCELLED -> "Cancelled" to 0xFFFF9800.toInt() // Naranja
            }
            
            binding.textStatus.text = statusText
            binding.textStatus.setTextColor(statusColor)
            
            // Cambiar el color de la barra lateral (status_indicator)
            binding.statusIndicator.setBackgroundColor(statusColor)
            
            // Listeners
            binding.root.setOnClickListener { onItemClick(entry) }
            binding.btnDelete.setOnClickListener { 
                onDeleteClick(entry, adapterPosition) 
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int = entries.size

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
}
