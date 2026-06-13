package com.dictionary.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dictionary.app.data.local.entity.SavedWordEntity
import com.dictionary.app.data.repository.DictionaryRepository
import com.dictionary.app.util.AudioPlayerHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SavedWordSort {
    DATE_DESC,
    ALPHABET_ASC
}

enum class SavedWordFilter {
    ALL,
    FAVORITES,
    NEW,
    LEARNING,
    REVIEWING,
    MASTERED
}

data class SavedWordsUiState(
    val searchQuery: String = "",
    val sort: SavedWordSort = SavedWordSort.DATE_DESC,
    val filter: SavedWordFilter = SavedWordFilter.ALL,
    val isAudioPlaying: Boolean = false,
    val audioError: String? = null
)

class SavedWordsViewModel(
    application: Application,
    private val repository: DictionaryRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SavedWordsUiState())
    val uiState: StateFlow<SavedWordsUiState> = _uiState.asStateFlow()

    private val audioPlayerHelper = AudioPlayerHelper(application)

    // Flow of bookmarked words from Room database, combined with query, filter, sort
    val savedWords: StateFlow<List<SavedWordEntity>> = combine(
        repository.getSavedWords(),
        _uiState
    ) { words, state ->
        var filteredList = words

        // 1. Filter by search query
        if (state.searchQuery.isNotBlank()) {
            filteredList = filteredList.filter {
                it.word.contains(state.searchQuery, ignoreCase = true) ||
                        it.shortDefinition.contains(state.searchQuery, ignoreCase = true)
            }
        }

        // 2. Filter by category
        val now = System.currentTimeMillis()
        filteredList = when (state.filter) {
            SavedWordFilter.ALL -> filteredList
            SavedWordFilter.FAVORITES -> filteredList.filter { it.isFavorite }
            SavedWordFilter.NEW -> filteredList.filter { now - it.createdAt < 24 * 60 * 60 * 1000 } // last 24h
            SavedWordFilter.LEARNING -> filteredList.filter { (now - it.createdAt >= 24 * 60 * 60 * 1000) && (now - it.createdAt < 7 * 24 * 60 * 60 * 1000) } // 1-7 days
            SavedWordFilter.REVIEWING -> filteredList.filter { now - it.createdAt >= 7 * 24 * 60 * 60 * 1000 } // > 7 days
            SavedWordFilter.MASTERED -> filteredList.filter { it.isFavorite && (now - it.createdAt >= 3 * 24 * 60 * 60 * 1000) } // favorite & older than 3 days
        }

        // 3. Sort
        when (state.sort) {
            SavedWordSort.DATE_DESC -> filteredList.sortedByDescending { it.createdAt }
            SavedWordSort.ALPHABET_ASC -> filteredList.sortedBy { it.word.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSortChange(sort: SavedWordSort) {
        _uiState.update { it.copy(sort = sort) }
    }

    fun onFilterChange(filter: SavedWordFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun toggleFavorite(word: SavedWordEntity) {
        viewModelScope.launch {
            repository.updateFavorite(word.id, !word.isFavorite)
        }
    }

    fun deleteWord(word: SavedWordEntity) {
        viewModelScope.launch {
            repository.deleteSavedWord(word)
        }
    }

    fun playAudio(url: String?) {
        if (url.isNullOrBlank()) {
            _uiState.update { it.copy(audioError = "No audio pronunciation available.") }
            return
        }

        _uiState.update { it.copy(isAudioPlaying = true, audioError = null) }
        audioPlayerHelper.play(
            url = url,
            onPrepared = {},
            onComplete = {
                _uiState.update { it.copy(isAudioPlaying = false) }
            },
            onError = { error ->
                _uiState.update {
                    it.copy(
                        isAudioPlaying = false,
                        audioError = "Failed to play: $error"
                    )
                }
            }
        )
    }

    fun clearAudioError() {
        _uiState.update { it.copy(audioError = null) }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerHelper.stop()
    }
}
