package com.dragon.fm.data.local

import android.content.Context
import android.content.SharedPreferences
import com.dragon.fm.data.model.RadioStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FavoritesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("dragon_fm_data", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _favoriteStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val favoriteStations: StateFlow<List<RadioStation>> = _favoriteStations.asStateFlow()

    private val _history = MutableStateFlow<List<RadioStation>>(emptyList())
    val history: StateFlow<List<RadioStation>> = _history.asStateFlow()

    private val _customStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val customStations: StateFlow<List<RadioStation>> = _customStations.asStateFlow()

    private val _customNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val customNames: StateFlow<Map<String, String>> = _customNames.asStateFlow()

    init {
        loadCustomNames()
        loadHistory()
        loadFavorites()
        loadCustomStations()
    }

    private fun loadCustomNames() {
        val raw = prefs.getString(KEY_CUSTOM_NAMES, null) ?: return
        try {
            _customNames.value = json.decodeFromString(raw)
        } catch (_: Exception) {}
    }

    fun setCustomStationName(stationId: String, newName: String?) {
        val current = _customNames.value.toMutableMap()
        if (newName.isNullOrBlank()) {
            current.remove(stationId)
        } else {
            current[stationId] = newName.trim()
        }
        _customNames.value = current
        repoScope.launch {
            try {
                val encoded = json.encodeToString(current)
                prefs.edit().putString(KEY_CUSTOM_NAMES, encoded).apply()
            } catch (_: Exception) {}
        }

        // Also update name in favorites and history lists if present
        loadFavorites()
        loadHistory()
    }

    fun getCustomStationName(stationId: String): String? {
        return _customNames.value[stationId]
    }

    fun applyCustomName(station: RadioStation): RadioStation {
        val custom = _customNames.value[station.id]
        return if (!custom.isNullOrBlank()) {
            station.copy(name = custom)
        } else {
            station
        }
    }

    private fun loadFavorites() {
        val rawSet = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        _favoriteIds.value = rawSet

        val currentList = mutableListOf<RadioStation>()
        val rawList = prefs.getString(KEY_FAVORITE_STATIONS, null)
        if (rawList != null) {
            try {
                val list = json.decodeFromString<List<RadioStation>>(rawList)
                currentList.addAll(list.map { applyCustomName(it) })
            } catch (_: Exception) {}
        }

        // Reconcile: If any ID in rawSet is missing from currentList, restore it from presets or history
        val presentIds = currentList.map { it.id }.toSet()
        val missingIds = rawSet - presentIds
        if (missingIds.isNotEmpty()) {
            val presetsMap = PresetsData.defaultStations.associateBy { it.id }
            val historyMap = _history.value.associateBy { it.id }
            for (id in missingIds) {
                val found = presetsMap[id] ?: historyMap[id]
                if (found != null) {
                    currentList.add(applyCustomName(found.copy(isFavorite = true)))
                }
            }
            saveStations(KEY_FAVORITE_STATIONS, currentList)
        }
        val refreshedList = DedicatedSourceManager.refreshStationList(currentList)
        _favoriteStations.value = refreshedList
        if (refreshedList != currentList) {
            saveStations(KEY_FAVORITE_STATIONS, refreshedList)
        }
    }

    /**
     * Re-scans favorite stations against DedicatedSourceManager cache to apply fresh streams.
     */
    fun refreshFavorites() {
        val currentList = _favoriteStations.value
        val refreshed = DedicatedSourceManager.refreshStationList(currentList)
        if (refreshed != currentList) {
            _favoriteStations.value = refreshed
            saveStations(KEY_FAVORITE_STATIONS, refreshed)
        }
    }

    /**
     * Updates an existing station across favorites and history with fresh stream lines.
     */
    fun updateStation(freshStation: RadioStation) {
        val currentFavs = _favoriteStations.value.toMutableList()
        val favIdx = currentFavs.indexOfFirst { it.id == freshStation.id }
        if (favIdx != -1) {
            val old = currentFavs[favIdx]
            if (old.playUrl != freshStation.playUrl || old.lines != freshStation.lines) {
                currentFavs[favIdx] = applyCustomName(freshStation.copy(isFavorite = true))
                _favoriteStations.value = currentFavs
                saveStations(KEY_FAVORITE_STATIONS, currentFavs)
            }
        }

        val currentHist = _history.value.toMutableList()
        val histIdx = currentHist.indexOfFirst { it.id == freshStation.id }
        if (histIdx != -1) {
            val old = currentHist[histIdx]
            if (old.playUrl != freshStation.playUrl || old.lines != freshStation.lines) {
                currentHist[histIdx] = applyCustomName(freshStation)
                _history.value = currentHist
                saveStations(KEY_HISTORY, currentHist)
            }
        }
    }

    fun isFavorite(stationId: String): Boolean {
        return _favoriteIds.value.contains(stationId)
    }

    fun toggleFavorite(station: RadioStation): Boolean {
        val currentIds = _favoriteIds.value.toMutableSet()
        val currentList = _favoriteStations.value.toMutableList()
        val isFav: Boolean

        if (currentIds.contains(station.id)) {
            currentIds.remove(station.id)
            currentList.removeAll { it.id == station.id }
            isFav = false
        } else {
            currentIds.add(station.id)
            currentList.removeAll { it.id == station.id }
            currentList.add(0, applyCustomName(station.copy(isFavorite = true)))
            isFav = true
        }

        _favoriteIds.value = currentIds
        _favoriteStations.value = currentList

        prefs.edit().putStringSet(KEY_FAVORITES, currentIds).apply()
        saveStations(KEY_FAVORITE_STATIONS, currentList)
        return isFav
    }

    /**
     * Persists a newly reordered list of favorite stations across memory and disk.
     */
    fun reorderFavorites(newList: List<RadioStation>) {
        val mappedList = newList.map { applyCustomName(it.copy(isFavorite = true)) }
        val idSet = mappedList.map { it.id }.toSet()
        _favoriteStations.value = mappedList
        _favoriteIds.value = idSet
        saveStations(KEY_FAVORITE_STATIONS, mappedList)
        prefs.edit().putStringSet(KEY_FAVORITES, idSet).apply()
    }

    // Overload for toggleFavorite by ID
    fun toggleFavorite(stationId: String, fallbackStation: RadioStation? = null): Boolean {
        val currentIds = _favoriteIds.value.toMutableSet()
        val currentList = _favoriteStations.value.toMutableList()
        val isFav: Boolean

        if (currentIds.contains(stationId)) {
            currentIds.remove(stationId)
            currentList.removeAll { it.id == stationId }
            isFav = false
        } else {
            currentIds.add(stationId)
            val station = fallbackStation
                ?: _history.value.find { it.id == stationId }
                ?: PresetsData.defaultStations.find { it.id == stationId }
            if (station != null) {
                currentList.removeAll { it.id == stationId }
                currentList.add(0, applyCustomName(station.copy(isFavorite = true)))
            }
            isFav = true
        }

        _favoriteIds.value = currentIds
        _favoriteStations.value = currentList
        prefs.edit().putStringSet(KEY_FAVORITES, currentIds).apply()
        saveStations(KEY_FAVORITE_STATIONS, currentList)
        return isFav
    }

    fun removeFavorite(stationId: String) {
        val currentIds = _favoriteIds.value.toMutableSet()
        currentIds.remove(stationId)
        val currentList = _favoriteStations.value.toMutableList()
        currentList.removeAll { it.id == stationId }

        _favoriteIds.value = currentIds
        _favoriteStations.value = currentList
        prefs.edit().putStringSet(KEY_FAVORITES, currentIds).apply()
        saveStations(KEY_FAVORITE_STATIONS, currentList)
    }

    fun pinFavorite(stationId: String) {
        val currentList = _favoriteStations.value.toMutableList()
        val target = currentList.find { it.id == stationId } ?: return
        currentList.remove(target)
        currentList.add(0, target)
        _favoriteStations.value = currentList
        saveStations(KEY_FAVORITE_STATIONS, currentList)
    }

    fun addToHistory(station: RadioStation) {
        val customStation = applyCustomName(station)
        val current = _history.value.toMutableList()
        current.removeAll { it.id == station.id }
        current.add(0, customStation)
        val trimmed = current.take(60)
        _history.value = trimmed
        saveStations(KEY_HISTORY, trimmed)
    }

    fun removeFromHistory(stationId: String) {
        val current = _history.value.toMutableList()
        current.removeAll { it.id == stationId }
        _history.value = current
        saveStations(KEY_HISTORY, current)
    }

    fun clearHistory() {
        _history.value = emptyList()
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun loadHistory() {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return
        try {
            val list = json.decodeFromString<List<RadioStation>>(raw)
            _history.value = list.map { applyCustomName(it) }
        } catch (_: Exception) {}
    }

    fun addCustomStation(station: RadioStation) {
        val current = _customStations.value.toMutableList()
        current.removeAll { it.id == station.id }
        current.add(0, station)
        _customStations.value = current
        saveStations(KEY_CUSTOM_STATIONS, current)
    }

    private fun loadCustomStations() {
        val raw = prefs.getString(KEY_CUSTOM_STATIONS, null) ?: return
        try {
            _customStations.value = json.decodeFromString(raw)
        } catch (_: Exception) {}
    }

    private fun saveStations(key: String, list: List<RadioStation>) {
        val copy = list.toList()
        repoScope.launch {
            try {
                val encoded = json.encodeToString(copy)
                prefs.edit().putString(key, encoded).commit()
            } catch (_: Exception) {}
        }
    }

    /**
     * 批量导入并持久化收藏列表、自定义音乐源、自定义重命名别名与历史记录
     */
    fun importBackupData(
        newFavorites: List<RadioStation>?,
        newCustomStations: List<RadioStation>?,
        newCustomNames: Map<String, String>?,
        newHistory: List<RadioStation>? = null
    ) {
        if (newCustomNames != null) {
            _customNames.value = newCustomNames
            try {
                prefs.edit().putString(KEY_CUSTOM_NAMES, json.encodeToString(newCustomNames)).commit()
            } catch (_: Exception) {}
        }

        if (newFavorites != null) {
            val mapped = newFavorites.map { applyCustomName(it.copy(isFavorite = true)) }
            val ids = mapped.map { it.id }.toSet()
            _favoriteStations.value = mapped
            _favoriteIds.value = ids
            try {
                prefs.edit()
                    .putString(KEY_FAVORITE_STATIONS, json.encodeToString(mapped))
                    .putStringSet(KEY_FAVORITES, ids)
                    .commit()
            } catch (_: Exception) {}
        }

        if (newCustomStations != null) {
            _customStations.value = newCustomStations
            try {
                prefs.edit().putString(KEY_CUSTOM_STATIONS, json.encodeToString(newCustomStations)).commit()
            } catch (_: Exception) {}
        }

        if (newHistory != null) {
            val mappedHistory = newHistory.map { applyCustomName(it) }.take(60)
            _history.value = mappedHistory
            try {
                prefs.edit().putString(KEY_HISTORY, json.encodeToString(mappedHistory)).commit()
            } catch (_: Exception) {}
        }
    }

    companion object {
        private const val KEY_FAVORITES = "fav_station_ids"
        private const val KEY_FAVORITE_STATIONS = "fav_station_objects"
        private const val KEY_HISTORY = "history_stations"
        private const val KEY_CUSTOM_STATIONS = "custom_stations"
        private const val KEY_CUSTOM_NAMES = "custom_station_names"
    }
}
