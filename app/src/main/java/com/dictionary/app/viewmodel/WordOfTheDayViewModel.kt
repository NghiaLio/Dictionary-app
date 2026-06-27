package com.dictionary.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dictionary.app.data.repository.DictionaryRepository
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WordOfTheDayUiState(
    val isLoading: Boolean = true, // Start with true to avoid flashing local data
    val word: String = "",
    val phonetic: String? = null,
    val audioUrl: String? = null,
    val meaning: String = "",
    val example: String = "",
    val relatedWords: List<String> = emptyList(),
    val error: String? = null
)

data class AiWordOfDay(
    @SerializedName("word") val word: String,
    @SerializedName("phonetic") val phonetic: String?,
    @SerializedName("meaning") val meaning: String,
    @SerializedName("example") val example: String,
    @SerializedName("relatedWords") val relatedWords: List<String>? = null
)

class WordOfTheDayViewModel(
    application: Application,
    private val repository: DictionaryRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(WordOfTheDayUiState())
    val uiState: StateFlow<WordOfTheDayUiState> = _uiState.asStateFlow()

    // Backup local word list in case Gemini API key is missing or offline
    private val localWords = listOf(
        AiWordOfDay("Resilient", "/rɪˈzɪl.jənt/", "Có khả năng phục hồi nhanh chóng sau khó khăn; kiên cường.", "She is a resilient girl who overcame many obstacles.", listOf("Tough", "Strong", "Hardy", "Adaptable")),
        AiWordOfDay("Serendipity", "/ˌser.ənˈdɪp.ə.ti/", "Sự tình cờ may mắn tìm ra những điều tốt đẹp.", "Finding my lost passport was a stroke of pure serendipity.", listOf("Chance", "Luck", "Providence", "Fluke", "Misfortune")),
        AiWordOfDay("Eloquent", "/ˈel.ə.kwənt/", "Có tài hùng biện, diễn đạt trôi chảy và đầy sức thuyết phục.", "The president made an eloquent speech at the opening ceremony.", listOf("Articulate", "Fluent", "Expressive", "Persuasive")),
        AiWordOfDay("Ephemeral", "/ɪˈfem.ər.əl/", "Chóng tàn, ngắn ngủi, phù du.", "The beauty of cherry blossoms is ephemeral, lasting only a few days.", listOf("Brief", "Fleeting", "Short-lived", "Transient")),
        AiWordOfDay("Pragmatic", "/præɡˈmæt.ɪk/", "Thực tế, thực dụng, giải quyết vấn đề dựa trên thực tế.", "We need to take a pragmatic approach to solve this budget issue.", listOf("Practical", "Realistic", "Logical", "Sensible")),
        AiWordOfDay("Benevolent", "/bəˈnev.əl.ənt/", "Tử tế, nhân từ, hay giúp đỡ mọi người.", "The benevolent old man donated all his savings to the hospital.", listOf("Kind", "Generous", "Caring", "Altruistic")),
        AiWordOfDay("Aesthetic", "/esˈθet.ɪk/", "Thuộc về thẩm mỹ, có óc thẩm mỹ hoặc vẻ đẹp nghệ thuật.", "The new library design is appreciated for both its safety and aesthetic value.", listOf("Artistic", "Beautiful", "Tasteful", "Style")),
        AiWordOfDay("Meticulous", "/məˈtɪk.jə.ləs/", "Tỉ mỉ, kỹ càng, quá chi tiết.", "She was meticulous about keeping her room clean and organized.", listOf("Careful", "Precise", "Detailed", "Thorough")),
        AiWordOfDay("Cognitive", "/ˈkɒɡ.nə.tɪv/", "Liên quan đến nhận thức, tư duy và trí tuệ.", "Playing chess helps in improving cognitive functions of children.", listOf("Mental", "Intellectual", "Rational", "Brainy")),
        AiWordOfDay("Nostalgia", "/nɒˈstældʒ.ə/", "Nỗi nhớ nhà, lòng hoài cổ về quá khứ.", "Listening to old songs filled her heart with a deep sense of nostalgia.", listOf("Longing", "Remembrance", "Yearning", "Homesickness"))
    )

    private var lastLoadedDate: String? = null
    private var loadJob: Job? = null

    fun loadWordOfTheDay() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // Skip if already loaded successfully today
        if (lastLoadedDate == today) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            var success = false
            var attempts = 0
            val maxAttempts = 3
            
            while (!success && attempts < maxAttempts && isActive) {
                attempts++
                try {
                    val randomWordResult = repository.getRandomWordFromApiNinjas()
                    if (randomWordResult.isSuccess) {
                        val word = randomWordResult.getOrNull()?.firstOrNull()?.trim()
                        if (!word.isNullOrBlank()) {
                            val wordDetails = repository.getWordResult(word, saveToHistory = false)
                            if (wordDetails.isSuccess) {
                                val detail = wordDetails.getOrThrow()
                                val firstMeaning = detail.meanings.firstOrNull()
                                val firstDef = firstMeaning?.definitions?.firstOrNull()
                                
                                val synonyms = detail.meanings.flatMap { m ->
                                    m.definitions.flatMap { d -> d.synonyms }
                                }.distinct().take(5)

                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        word = detail.word,
                                        phonetic = detail.phonetic,
                                        audioUrl = detail.audioUrl,
                                        meaning = firstDef?.definition ?: "No definition found",
                                        example = firstDef?.example ?: "",
                                        relatedWords = synonyms,
                                        error = null
                                    )
                                }
                                success = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Try next attempt
                }
            }

            // Fallback to local if all API attempts failed
            if (!success && isActive) {
                val seedIndex = Math.abs(today.hashCode()) % localWords.size
                val localWord = localWords[seedIndex]
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        word = localWord.word,
                        phonetic = localWord.phonetic,
                        audioUrl = null,
                        meaning = localWord.meaning,
                        example = localWord.example,
                        relatedWords = localWord.relatedWords ?: emptyList(),
                        error = null
                    )
                }
            }
            
            lastLoadedDate = today
        }
    }
}
