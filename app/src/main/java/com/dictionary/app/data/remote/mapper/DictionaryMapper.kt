package com.dictionary.app.data.remote.mapper

import com.dictionary.app.data.local.entity.SavedWordEntity
import com.dictionary.app.data.remote.dto.WordDto
import com.dictionary.app.domain.model.DefinitionResult
import com.dictionary.app.domain.model.MeaningResult
import com.dictionary.app.domain.model.WordResult

// Map raw API DTO to clean UI Domain Model
fun WordDto.toWordResult(): WordResult {
    // 1. Filter out empty/null audio strings
    val validPhonetics = this.phonetics?.filter { !it.audio.isNullOrBlank() } ?: emptyList()
    
    // 2. Prioritize US, then UK, then first valid audio url
    val usAudio = validPhonetics.find { it.audio?.lowercase()?.contains("-us") == true }
    val ukAudio = validPhonetics.find { it.audio?.lowercase()?.contains("-uk") == true }
    val selectedAudioUrl = usAudio?.audio ?: ukAudio?.audio ?: validPhonetics.firstOrNull()?.audio

    // 3. Map meanings & definitions
    val uiMeanings = this.meanings?.map { meaningDto ->
        MeaningResult(
            partOfSpeech = meaningDto.partOfSpeech ?: "",
            definitions = meaningDto.definitions?.map { defDto ->
                DefinitionResult(
                    definition = defDto.definition ?: "",
                    example = defDto.example,
                    synonyms = defDto.synonyms ?: emptyList(),
                    antonyms = defDto.antonyms ?: emptyList()
                )
            } ?: emptyList()
        )
    } ?: emptyList()

    // Grab fallback phonetic text
    val phoneticText = this.phonetic ?: this.phonetics?.firstOrNull { !it.text.isNullOrBlank() }?.text

    return WordResult(
        word = this.word ?: "",
        phonetic = phoneticText,
        audioUrl = selectedAudioUrl,
        meanings = uiMeanings,
        sourceUrls = this.sourceUrls ?: emptyList()
    )
}

// Map Room SavedWordEntity to UI WordResult (used for viewing saved bookmarks offline)
fun SavedWordEntity.toWordResult(): WordResult {
    return WordResult(
        word = this.word,
        phonetic = this.phonetic,
        audioUrl = this.audioUrl,
        meanings = listOf(
            MeaningResult(
                partOfSpeech = "saved",
                definitions = listOf(
                    DefinitionResult(
                        definition = this.shortDefinition,
                        example = null,
                        synonyms = emptyList(),
                        antonyms = emptyList()
                    )
                )
            )
        ),
        sourceUrls = emptyList()
    )
}

// Convert UI WordResult to Room SavedWordEntity for database storage
fun WordResult.toSavedWordEntity(): SavedWordEntity {
    // Extract first meaning's definition as the fallback summary definition
    val firstMeaning = this.meanings.firstOrNull()
    val firstDefinition = firstMeaning?.definitions?.firstOrNull()

    return SavedWordEntity(
        word = this.word,
        phonetic = this.phonetic,
        audioUrl = this.audioUrl,
        shortDefinition = firstDefinition?.definition ?: ""
    )
}
