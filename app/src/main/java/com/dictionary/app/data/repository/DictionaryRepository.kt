package com.dictionary.app.data.repository

import com.dictionary.app.data.local.dao.SavedWordDao
import com.dictionary.app.data.local.dao.RecentSearchDao
import com.dictionary.app.data.local.dao.CachedWordDao
import com.dictionary.app.data.local.entity.SavedWordEntity
import com.dictionary.app.data.local.entity.RecentSearchEntity
import com.dictionary.app.data.local.entity.CachedWordEntity
import com.dictionary.app.data.remote.api.DictionaryApi
import com.dictionary.app.data.remote.api.DatamuseApi
import com.dictionary.app.data.remote.api.GeminiApi
import com.dictionary.app.data.remote.dto.WordDto
import com.dictionary.app.data.remote.dto.GeminiRequest
import com.dictionary.app.data.remote.dto.GeminiContent
import com.dictionary.app.data.remote.dto.GeminiPart
import com.dictionary.app.data.remote.mapper.toWordResult
import com.dictionary.app.data.remote.mapper.toSavedWordEntity
import com.dictionary.app.domain.model.WordResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import retrofit2.HttpException

class DictionaryRepository(
    private val dictionaryApi: DictionaryApi,
    private val datamuseApi: DatamuseApi,
    private val geminiApi: GeminiApi,
    private val savedWordDao: SavedWordDao,
    private val recentSearchDao: RecentSearchDao,
    private val cachedWordDao: CachedWordDao
) {
    private val gson = Gson()

    // 1. Fetch word definition: Checks offline cache first, falls back to API and caches the response.
    suspend fun getWordResult(word: String): Result<WordResult> {
        val query = word.trim().lowercase()
        if (query.isBlank()) return Result.failure(Exception("Query is empty."))

        // Check local cache
        try {
            val cached = cachedWordDao.getCachedWord(query)
            if (cached != null) {
                val listType = object : TypeToken<List<WordDto>>() {}.type
                val dtoList: List<WordDto> = gson.fromJson(cached.jsonData, listType)
                if (dtoList.isNotEmpty()) {
                    val wordResult = dtoList.first().toWordResult()
                    insertRecentSearch(wordResult.word)
                    return Result.success(wordResult)
                }
            }
        } catch (e: Exception) {
            // Fallback to network
        }

        // Call remote API
        return try {
            val response = dictionaryApi.getWordInfo(query)
            if (response.isNotEmpty()) {
                val wordResult = response.first().toWordResult()
                
                // Cache raw response JSON
                val jsonString = gson.toJson(response)
                cachedWordDao.insertCachedWord(
                    CachedWordEntity(word = query, jsonData = jsonString)
                )
                
                insertRecentSearch(wordResult.word)
                Result.success(wordResult)
            } else {
                Result.failure(Exception("Word not found."))
            }
        } catch (e: HttpException) {
            if (e.code() == 404) {
                Result.failure(Exception("Word not found. Check spelling."))
            } else {
                Result.failure(Exception("Server error (Code: ${e.code()})."))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please check your internet connection."))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Unknown error occurred."))
        }
    }

    // 2. Autocomplete suggestions from Datamuse
    suspend fun getSearchSuggestions(query: String): List<String> {
        val prefix = query.trim().lowercase()
        if (prefix.isBlank()) return emptyList()
        return try {
            val response = datamuseApi.getSuggestions(prefix)
            response.take(5).map { it.word }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 3. Fuzzy search helper from Datamuse
    suspend fun getFuzzySpelling(word: String): String? {
        val query = word.trim().lowercase()
        if (query.isBlank()) return null
        return try {
            val response = datamuseApi.getSpelledLike(query)
            // Return spelling suggestion if it exists and differs from current query
            val suggestion = response.firstOrNull()?.word
            if (suggestion != null && !suggestion.equals(query, ignoreCase = true)) {
                suggestion
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // 4. Gemini AI explanation with caching
    private var lastAiRequestTime = 0L

    suspend fun explainWithAi(word: String, actionType: String, apiKey: String): Result<String> {
        if (apiKey.isBlank()) return Result.failure(Exception("Gemini API key is not configured."))
        
        // Optimize cache key: if it's a daily word prompt, use a short date-based key
        val isDailyWord = actionType.startsWith("CUSTOM_PROMPT:")
        val cacheKey = if (isDailyWord) {
            "AI_DAILY_${actionType.substringAfter("date seed '").substringBefore("'")}"
        } else {
            "AI_${actionType}_${word.trim().lowercase()}"
        }
        
        // Try to get from local cache first
        try {
            val cached = cachedWordDao.getCachedWord(cacheKey)
            if (cached != null && cached.jsonData.isNotBlank()) {
                return Result.success(cached.jsonData)
            }
        } catch (e: Exception) {
            // Ignore cache error and proceed
        }

        // Rate limiting: Prevent calling AI more than once every 3 seconds
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAiRequestTime < 3000) {
            return Result.failure(Exception("Please wait a few seconds before another AI request."))
        }

        val prompt = when {
            isDailyWord -> actionType.removePrefix("CUSTOM_PROMPT:")
            actionType == "VIETNAMESE" -> "Giải thích từ tiếng Anh '$word' bằng tiếng Việt một cách dễ hiểu cho người học. Bao gồm định nghĩa, cách dùng, 3 ví dụ đơn giản và các lỗi thường gặp (Common Mistakes). Trả về kết quả định dạng Markdown ngắn gọn."
            actionType == "SIMPLE" -> "Explain the English word '$word' in very simple terms for an elementary English learner. Include definition, pronunciation tip, and 2 simple example sentences. Keep it short and formatted in Markdown."
            actionType == "MISTAKES" -> "What are the common grammar or usage mistakes English learners make when using the word '$word'? Explain clearly and show correct vs incorrect examples. Format in Markdown."
            actionType == "EXAMPLES" -> "Generate 4 realistic example sentences using the word '$word' for these specific contexts: 1. Daily Conversation, 2. Academic Writing, 3. Business English, 4. Informal chat. Label each clearly and format in Markdown."
            actionType == "RELATED" -> "Provide a list of Related Words (synonyms, idioms, derivatives) and the Word Family (e.g. success, succeed, successful, successfully) for the English word '$word'. Keep it concise and format in Markdown."
            actionType == "TRANSLATE" -> "Dịch từ tiếng Anh '$word' sang các nghĩa tiếng Việt thông dụng nhất. Trả về một danh sách ngắn gọn các từ loại và nghĩa tương ứng."
            else -> "Explain the word '$word' in detail."
        }

        return try {
            lastAiRequestTime = System.currentTimeMillis()
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = prompt)
                        )
                    )
                )
            )
            val response = geminiApi.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!responseText.isNullOrBlank()) {
                // Save to local cache
                cachedWordDao.insertCachedWord(
                    CachedWordEntity(word = cacheKey, jsonData = responseText)
                )
                Result.success(responseText)
            } else {
                Result.failure(Exception("Không nhận được câu trả lời từ Trợ lý AI."))
            }
        } catch (e: retrofit2.HttpException) {
            val errorMsg = when (e.code()) {
                404 -> "Không tìm thấy mô hình AI (404). Vui lòng kiểm tra lại cấu hình."
                401 -> "Khóa API không hợp lệ (401). Vui lòng cấu hình lại Gemini API Key trong phần Cài đặt."
                429 -> "Tần suất yêu cầu quá nhanh (429 - Too Many Requests). Bạn đã vượt quá giới hạn miễn phí của Gemini (15 yêu cầu/phút). Vui lòng đợi 1 phút rồi thử lại."
                else -> "Lỗi máy chủ AI (Mã: ${e.code()}): ${e.message()}"
            }
            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Yêu cầu AI thất bại."))
        }
    }

    // Saved Words DAO wrappers
    fun getSavedWords(): Flow<List<SavedWordEntity>> = savedWordDao.getSavedWords()

    suspend fun getSavedWordByText(word: String): SavedWordEntity? = savedWordDao.getWordByText(word)

    suspend fun insertSavedWord(word: SavedWordEntity) = savedWordDao.insertWord(word)

    suspend fun deleteSavedWord(word: SavedWordEntity) = savedWordDao.deleteWord(word)

    suspend fun deleteSavedWordByText(word: String) = savedWordDao.deleteWordByText(word)

    fun checkWordSaved(word: String): Flow<Boolean> = savedWordDao.checkWordSaved(word)

    suspend fun updateFavorite(id: Int, isFavorite: Boolean) = savedWordDao.updateFavorite(id, isFavorite)

    // Recent Searches DAO wrappers
    fun getRecentSearches(): Flow<List<RecentSearchEntity>> = recentSearchDao.getRecentSearches()

    suspend fun insertRecentSearch(word: String) {
        if (word.isNotBlank()) {
            recentSearchDao.insertSearch(RecentSearchEntity(word = word.trim().lowercase()))
        }
    }

    suspend fun deleteRecentSearch(word: String) {
        recentSearchDao.deleteSearch(word)
    }

    suspend fun clearRecentSearches() {
        recentSearchDao.clearRecentSearches()
    }
}
