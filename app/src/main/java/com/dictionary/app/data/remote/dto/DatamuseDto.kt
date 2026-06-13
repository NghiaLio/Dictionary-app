package com.dictionary.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DatamuseWordDto(
    @SerializedName("word") val word: String,
    @SerializedName("score") val score: Int?
)
