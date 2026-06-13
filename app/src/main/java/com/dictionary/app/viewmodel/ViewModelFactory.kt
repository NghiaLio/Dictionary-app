package com.dictionary.app.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dictionary.app.data.repository.DictionaryRepository
import com.dictionary.app.data.datastore.SettingsDataStore

class ViewModelFactory(
    private val application: Application,
    private val repository: DictionaryRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DictionaryViewModel::class.java) -> {
                DictionaryViewModel(application, repository) as T
            }
            modelClass.isAssignableFrom(SavedWordsViewModel::class.java) -> {
                SavedWordsViewModel(application, repository) as T
            }
            modelClass.isAssignableFrom(WordOfTheDayViewModel::class.java) -> {
                WordOfTheDayViewModel(application, repository) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(application, settingsDataStore) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
