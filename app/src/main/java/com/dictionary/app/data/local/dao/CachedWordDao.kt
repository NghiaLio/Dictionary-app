package com.dictionary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dictionary.app.data.local.entity.CachedWordEntity

@Dao
interface CachedWordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedWord(cachedWord: CachedWordEntity)

    @Query("SELECT * FROM cached_words WHERE word = :word LIMIT 1")
    suspend fun getCachedWord(word: String): CachedWordEntity?

    @Query("DELETE FROM cached_words WHERE word = :word")
    suspend fun deleteCachedWord(word: String)

    @Query("DELETE FROM cached_words")
    suspend fun clearAllCache()
}
