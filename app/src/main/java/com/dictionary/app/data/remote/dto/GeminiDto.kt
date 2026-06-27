package com.dictionary.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GeminiRequest(
    @SerializedName("model") val model: String,
    @SerializedName("input") val input: String
)

data class GeminiResponse(
    @SerializedName("text") val text: String?,
    @SerializedName("candidates") val candidates: List<GeminiCandidate>?,
    @SerializedName("error") val error: GeminiError?
)

data class GeminiCandidate(
    @SerializedName("content") val content: GeminiContent?
)

data class GeminiContent(
    @SerializedName("parts") val parts: List<GeminiPart>
)

data class GeminiPart(
    @SerializedName("text") val text: String
)

data class GeminiError(
    @SerializedName("message") val message: String?,
    @SerializedName("code") val code: Int?
)
