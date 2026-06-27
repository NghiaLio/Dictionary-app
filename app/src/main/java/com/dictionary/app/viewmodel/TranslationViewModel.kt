package com.dictionary.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dictionary.app.data.repository.DictionaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TranslationUiState(
    val sourceText: String = "",
    val targetText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class TranslationViewModel(
    private val repository: DictionaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranslationUiState())
    val uiState: StateFlow<TranslationUiState> = _uiState.asStateFlow()

    fun onSourceTextChange(text: String) {
        _uiState.update { it.copy(sourceText = text) }
    }

    fun translate(from: String, to: String) {
        val currentText = _uiState.value.sourceText
        if (currentText.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.translateText(currentText, from, to)
            result.onSuccess { translated ->
                _uiState.update { it.copy(targetText = translated, isLoading = false) }
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message, isLoading = false) }
            }
        }
    }

    fun swapLanguages(sourceText: String, targetText: String) {
        _uiState.update { 
            it.copy(
                sourceText = targetText,
                targetText = sourceText
            )
        }
    }
}
