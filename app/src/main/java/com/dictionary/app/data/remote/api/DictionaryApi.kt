package com.dictionary.app.data.remote.api

import com.dictionary.app.data.remote.dto.WordDto
import retrofit2.http.GET
import retrofit2.http.Path

interface DictionaryApi {
    
    @GET("api/v2/entries/en/{word}")
    suspend fun getWordInfo(
        @Path("word") word: String
    ): List<WordDto>

    companion object {
        const val BASE_URL = "https://api.dictionaryapi.dev/"
    }
}
