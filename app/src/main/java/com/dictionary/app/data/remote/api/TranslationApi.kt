package com.dictionary.app.data.remote.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface TranslationApi {
    @POST("api/v1/translator/text")
    suspend fun translate(
        @Header("x-rapidapi-key") apiKey: String,
        @Header("x-rapidapi-host") host: String = "google-translate113.p.rapidapi.com",
        @Body request: TranslationRequest
    ): TranslationResponse

    companion object {
        const val BASE_URL = "https://google-translate113.p.rapidapi.com/"
    }
}

data class TranslationRequest(
    val from: String,
    val to: String,
    val text: String
)

data class TranslationResponse(
    val trans: String
)
