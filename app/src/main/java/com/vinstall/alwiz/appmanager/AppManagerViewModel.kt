package com.vinstall.alwiz.appmanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vinstall.alwiz.backup.BackupManager
import com.vinstall.alwiz.model.AppInfo
import com.vinstall.alwiz.util.DebugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppManagerViewModel(app: Application) : AndroidViewModel(app) {

    enum class SortOrder { NAME, SIZE, INSTALL_DATE, UPDATE_DATE }
    
    // --- NUEVO: Filtros avanzados ---
    enum class SizeFilter(val label: String, val minBytes: Long) {
        ALL("All sizes", 0L),
        OVER_10MB("> 10 MB", 10L * 1024 * 1024),
        OVER_50MB("> 50 MB", 50L * 1024 * 1024),
        OVER_100MB("> 100 MB", 100L * 1024 * 1024),
        OVER_500MB("> 500 MB", 500L * 1024 * 1024)
    }
    // -------------------------------

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _query = MutableStateFlow("")
    private val _includeSystem = MutableStateFlow(false)
    private val _sortOrder = MutableStateFlow(SortOrder.NAME)
    private val _isLoading = MutableStateFlow(false)
    private var loadJob: Job? = null

    // --- NUEVO: StateFlows para filtros avanzados ---
    private val _sizeFilter = MutableStateFlow(SizeFilter.ALL)
    private val _onlyDangerousPerms = MutableStateFlow(false)
    private val _onlySplitApps = MutableStateFlow(false)
    // -----------------------------------------------

    val isLoading: StateFlow<Boolean> = _isLoading

    val displayedApps: StateFlow<List<AppInfo>> = combine(
        _allApps, _query, _includeSystem, _sortOrder,
        _sizeFilter, _onlyDangerousPerms, _onlySplitApps
    ) { all, query, includeSystem, sort, sizeFilter, dangerousPerms, splitApps ->
        
        // Filtro 1: Sistema
        var result = if (includeSystem) all else all.filter { !it.isSystemApp }
        
        // Filtro 2: Búsqueda por texto
        if (query.isNotBlank()) {
            val q = query.lowercase()
            result = result.filter { 
                it.label.lowercase().contains(q) || 
                it.packageName.lowercase().contains(q) 
            }
        }
        
        // --- NUEVO: Filtro 3: Tamaño mínimo ---
        if (sizeFilter != SizeFilter.ALL) {
            result = result.filter { it.sizeBytes >= sizeFilter.minBytes }
        }
        // --------------------------------------
        
        // --- NUEVO: Filtro 4: Solo permisos peligrosos ---
        if (dangerousPerms) {
            result = result.filter { hasDangerousPermissions(it) }
        }
        // -------------------------------------------------
        
        // --- NUEVO: Filtro 5: Solo apps con splits ---
        if (splitApps) {
            result = result.filter { it.isSplitApp }
        }
        // ---------------------------------------------
        
        // Ordenamiento
        when (sort) {
            SortOrder.NAME -> result.sortedBy { it.label.lowercase() }
            SortOrder.SIZE -> result.sortedByDescending { it.sizeBytes }
            SortOrder.INSTALL_DATE -> result.sortedByDescending { it.installTimeMs }
            SortOrder.UPDATE_DATE -> result.sortedByDescending { it.updateTimeMs }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun loadApps() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            DebugLog.i("AppManagerVM", "Loading application list...")
            try {
                val apps = BackupManager.listInstalledApps(getApplication(), includeSystem = true)
                _allApps.value = apps
                DebugLog.i("AppManagerVM", "Loaded ${apps.size} application(s)")
            } catch (e: Exception) {
                DebugLog.e("AppManagerVM", "Failed to load applications: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filter(query: String) { _query.value = query }
    fun setIncludeSystem(include: Boolean) { _includeSystem.value = include }
    fun setSort(order: SortOrder) { _sortOrder.value = order }
    fun refresh() { loadApps() }
    
    // --- NUEVO: Métodos para filtros avanzados ---
    fun setSizeFilter(filter: SizeFilter) { _sizeFilter.value = filter }
    fun setOnlyDangerousPerms(only: Boolean) { _onlyDangerousPerms.value = only }
    fun setOnlySplitApps(only: Boolean) { _onlySplitApps.value = only }
    fun getSizeFilter(): SizeFilter = _sizeFilter.value
    // -------------------------------------------

    // --- NUEVO: Verificar permisos peligrosos ---
    private fun hasDangerousPermissions(app: AppInfo): Boolean {
        val dangerousPerms = setOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.SEND_SMS",
            "android.permission.READ_PHONE_STATE",
            "android.permission.CALL_PHONE",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.BODY_SENSORS",
            "android.permission.READ_CALENDAR",
            "android.permission.WRITE_CALENDAR"
        )
        return app.requestedPermissions.any { it in dangerousPerms }
    }
    // -------------------------------------------
}
