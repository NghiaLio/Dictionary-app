package com.dictionary.app.domain.model

data class WordResult(
    val word: String,
    val phonetic: String?,
    val audioUrl: String?,
    val meanings: List<MeaningResult>,
    val sourceUrls: List<String>
)

data class MeaningResult(
    val partOfSpeech: String,
    val definitions: List<DefinitionResult>
)

data class DefinitionResult(
    val definition: String,
    val example: String?,
    val synonyms: List<String>,
    val antonyms: List<String>
)
