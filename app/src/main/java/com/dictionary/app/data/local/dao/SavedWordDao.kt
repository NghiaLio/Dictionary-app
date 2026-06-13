package com.dictionary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dictionary.app.data.local.entity.SavedWordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedWordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: SavedWordEntity)

    @Delete
    suspend fun deleteWord(word: SavedWordEntity)

    @Query("DELETE FROM saved_words WHERE word = :word")
    suspend fun deleteWordByText(word: String)

    @Query("SELECT * FROM saved_words ORDER BY createdAt DESC")
    fun getSavedWords(): Flow<List<SavedWordEntity>>

    @Query("SELECT * FROM saved_words WHERE word = :word LIMIT 1")
    suspend fun getWordByText(word: String): SavedWordEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM saved_words WHERE word = :word LIMIT 1)")
    fun checkWordSaved(word: String): Flow<Boolean>

    @Query("UPDATE saved_words SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFavorite: Boolean)
}
