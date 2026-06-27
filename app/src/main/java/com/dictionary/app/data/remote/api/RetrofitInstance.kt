package com.dictionary.app.data.remote.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Dedicated client for Gemini AI with longer timeouts to prevent SocketTimeoutExceptions
    private val geminiClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // 1. Dictionary API Client
    val dictionaryApi: DictionaryApi by lazy {
        Retrofit.Builder()
            .baseUrl(DictionaryApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(DictionaryApi::class.java)
    }

    // 2. Datamuse API Client (Suggestions)
    val datamuseApi: DatamuseApi by lazy {
        Retrofit.Builder()
            .baseUrl(DatamuseApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(DatamuseApi::class.java)
    }

    // 3. Gemini AI Client
    val geminiApi: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(GeminiApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(geminiClient)
            .build()
            .create(GeminiApi::class.java)
    }

    // 4. Api-Ninjas Client (Random Word)
    val apiNinjasApi: ApiNinjasApi by lazy {
        Retrofit.Builder()
            .baseUrl(ApiNinjasApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiNinjasApi::class.java)
    }

    // 5. Translation API Client
    val translationApi: TranslationApi by lazy {
        Retrofit.Builder()
            .baseUrl(TranslationApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(TranslationApi::class.java)
    }
}
