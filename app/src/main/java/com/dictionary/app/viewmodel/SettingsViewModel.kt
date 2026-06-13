package com.dictionary.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dictionary.app.data.datastore.SettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.dictionary.app.data.local.database.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    application: Application,
    private val settingsDataStore: SettingsDataStore
) : AndroidViewModel(application) {

    val themeMode: StateFlow<String> = settingsDataStore.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val reminderEnabled: StateFlow<Boolean> = settingsDataStore.reminderEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val wordOfDayEnabled: StateFlow<Boolean> = settingsDataStore.wordOfDayEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val geminiApiKey: StateFlow<String> = settingsDataStore.geminiApiKeyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _cacheSize = MutableStateFlow("0.0 MB")
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    init {
        loadCacheSize()
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsDataStore.saveThemeMode(mode)
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveReminderEnabled(enabled)
        }
    }

    fun setWordOfDayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveWordOfDayEnabled(enabled)
        }
    }

    fun setGeminiApiKey(key: String) {
        viewModelScope.launch {
            settingsDataStore.saveGeminiApiKey(key)
        }
    }

    fun loadCacheSize() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val dbFile = context.getDatabasePath("dictionary_database")
            if (!dbFile.exists()) {
                _cacheSize.value = "0.0 MB"
                return@launch
            }
            var totalBytes = dbFile.length()
            val shmFile = context.getDatabasePath("dictionary_database-shm")
            if (shmFile.exists()) totalBytes += shmFile.length()
            val walFile = context.getDatabasePath("dictionary_database-wal")
            if (walFile.exists()) totalBytes += walFile.length()

            val sizeMb = totalBytes.toDouble() / (1024 * 1024)
            _cacheSize.value = String.format(java.util.Locale.US, "%.2f MB", sizeMb)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            val database = AppDatabase.getDatabase(getApplication())
            database.recentSearchDao.clearRecentSearches()
        }
    }

    fun clearOfflineCache() {
        viewModelScope.launch {
            val database = AppDatabase.getDatabase(getApplication())
            database.cachedWordDao.clearAllCache()
            loadCacheSize()
        }
    }
}
