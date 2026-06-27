package com.dictionary.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dictionary.app.data.local.entity.RecentSearchEntity
import com.dictionary.app.data.repository.DictionaryRepository
import com.dictionary.app.data.remote.mapper.toSavedWordEntity
import com.dictionary.app.domain.model.WordResult
import com.dictionary.app.util.AudioPlayerHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DictionaryUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val result: WordResult? = null,
    val error: String? = null,
    val suggestions: List<String> = emptyList(),
    val fuzzySuggestion: String? = null,
    val isAudioPlaying: Boolean = false
)

data class AiUiState(
    val isLoading: Boolean = false,
    val responseText: String? = null,
    val error: String? = null
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class DictionaryViewModel(
    application: Application,
    private val repository: DictionaryRepository
) : AndroidViewModel(application) {

    // Configure your Gemini API key here
    var geminiApiKey = ""

    private val _uiState = MutableStateFlow(DictionaryUiState())
    val uiState: StateFlow<DictionaryUiState> = _uiState.asStateFlow()

    private val _aiState = MutableStateFlow(AiUiState())
    val aiState: StateFlow<AiUiState> = _aiState.asStateFlow()

    private val audioPlayerHelper = AudioPlayerHelper(application)
    private val aiCache = mutableMapOf<String, String>()

    // Observe search history from database
    val recentSearches: StateFlow<List<RecentSearchEntity>> = repository.getRecentSearches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks bookmark status of the currently selected word
    private val currentWord = MutableStateFlow("")

    val isCurrentWordSaved: StateFlow<Boolean> = currentWord.flatMapLatest { word ->
        if (word.isBlank()) flowOf(false) else repository.checkWordSaved(word)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Debounced query flow for Datamuse autocomplete suggestions
    private val queryDebounceFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queryDebounceFlow
                .debounce(800) // Increase debounce to 800ms to save API quota
                .distinctUntilChanged()
                .collect { query ->
                    if (query.length >= 4) { // Only suggest for longer queries
                        val suggestionsList = repository.getSearchSuggestions(query)
                        _uiState.update { it.copy(suggestions = suggestionsList) }
                    } else {
                        _uiState.update { it.copy(suggestions = emptyList()) }
                    }
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        queryDebounceFlow.value = newQuery
    }

    fun clearSuggestions() {
        _uiState.update { it.copy(suggestions = emptyList()) }
    }

    fun searchWord(word: String, saveToHistory: Boolean = true) {
        val query = word.trim()
        if (query.isBlank()) return

        // If searching the same word again, don't re-trigger
        if (_uiState.value.result?.word?.equals(query, ignoreCase = true) == true) {
            clearSuggestions()
            return
        }

        // Clear suggestions dropdown
        clearSuggestions()

        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isLoading = true, 
                    error = null, 
                    result = null, 
                    fuzzySuggestion = null
                ) 
            }
            _aiState.update { AiUiState() } // Clear previous AI states

            val result = repository.getWordResult(query, saveToHistory)
            
            result.onSuccess { wordResult ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        result = wordResult,
                        error = null
                    )
                }
                currentWord.value = wordResult.word
            }.onFailure { exception ->
                // API 404/failure, attempt datamuse spelling fuzzy suggestion
                val fuzzyWord = repository.getFuzzySpelling(query)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = exception.message ?: "An error occurred.",
                        fuzzySuggestion = fuzzyWord
                    )
                }
            }
        }
    }

    // Call Gemini AI with memory caching
    fun explainWordWithAi(actionType: String, word: String? = null) {
        if (_aiState.value.isLoading) return
        val wordName = word ?: currentWord.value
        if (wordName.isBlank()) return

        if (geminiApiKey.isBlank()) {
            _aiState.update { 
                it.copy(
                    isLoading = false,
                    error = "LƯU Ý: Vui lòng cấu hình Gemini API Key trong Settings trước để sử dụng tính năng này."
                ) 
            }
            return
        }

        val cacheKey = "${wordName.trim().lowercase()}_$actionType"
        if (aiCache.containsKey(cacheKey)) {
            _aiState.update {
                it.copy(
                    isLoading = false,
                    responseText = aiCache[cacheKey],
                    error = null
                )
            }
            return
        }

        viewModelScope.launch {
            _aiState.update { it.copy(isLoading = true, error = null, responseText = null) }
            val result = repository.explainWithAi(wordName, actionType, geminiApiKey)
            result.onSuccess { text ->
                aiCache[cacheKey] = text
                _aiState.update { it.copy(isLoading = false, responseText = text) }
            }.onFailure { error ->
                _aiState.update { it.copy(isLoading = false, error = error.message ?: "AI request failed.") }
            }
        }
    }

    fun clearAiState() {
        _aiState.update { AiUiState() }
    }

    // Play pronunciation audio
    fun playAudio(url: String?) {
        if (url.isNullOrBlank()) return

        _uiState.update { it.copy(isAudioPlaying = true) }
        audioPlayerHelper.play(
            url = url,
            onPrepared = {},
            onComplete = {
                _uiState.update { it.copy(isAudioPlaying = false) }
            },
            onError = { _ ->
                _uiState.update { it.copy(isAudioPlaying = false) }
            }
        )
    }

    fun toggleSaveCurrentWord() {
        val wordResult = _uiState.value.result ?: return
        viewModelScope.launch {
            val existing = repository.getSavedWordByText(wordResult.word)
            if (existing != null) {
                repository.deleteSavedWord(existing)
            } else {
                repository.insertSavedWord(wordResult.toSavedWordEntity())
            }
        }
    }

    fun deleteHistoryItem(word: String) {
        viewModelScope.launch {
            repository.deleteRecentSearch(word)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearRecentSearches()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerHelper.stop()
    }
}
