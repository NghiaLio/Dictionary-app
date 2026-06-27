package com.dictionary.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Header

interface ApiNinjasApi {

    @GET("v2/randomword")
    suspend fun getRandomWord(
        @Header("X-Api-Key") apiKey: String = "WwNh7Qfeu8oHpcZcevLtbfecrfoAjrCu99b4YjtL"
    ): List<String>

    companion object {
        const val BASE_URL = "https://api.api-ninjas.com/"
    }
}
