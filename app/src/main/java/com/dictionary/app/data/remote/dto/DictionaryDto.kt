package com.dictionary.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WordDto(
    @SerializedName("word") val word: String?,
    @SerializedName("phonetic") val phonetic: String?,
    @SerializedName("phonetics") val phonetics: List<PhoneticDto>?,
    @SerializedName("meanings") val meanings: List<MeaningDto>?,
    @SerializedName("sourceUrls") val sourceUrls: List<String>?
)

data class PhoneticDto(
    @SerializedName("text") val text: String?,
    @SerializedName("audio") val audio: String?
)

data class MeaningDto(
    @SerializedName("partOfSpeech") val partOfSpeech: String?,
    @SerializedName("definitions") val definitions: List<DefinitionDto>?
)

data class DefinitionDto(
    @SerializedName("definition") val definition: String?,
    @SerializedName("example") val example: String?,
    @SerializedName("synonyms") val synonyms: List<String>?,
    @SerializedName("antonyms") val antonyms: List<String>?
)
