package com.dictionary.app.data.remote.api

import com.dictionary.app.data.remote.dto.DatamuseWordDto
import retrofit2.http.GET
import retrofit2.http.Query

interface DatamuseApi {

    // Autocomplete suggestions
    @GET("sug")
    suspend fun getSuggestions(
        @Query("s") prefix: String
    ): List<DatamuseWordDto>

    // Fuzzy spelling corrections
    @GET("words")
    suspend fun getSpelledLike(
        @Query("sp") word: String,
        @Query("max") limit: Int = 3
    ): List<DatamuseWordDto>

    companion object {
        const val BASE_URL = "https://api.datamuse.com/"
    }
}
