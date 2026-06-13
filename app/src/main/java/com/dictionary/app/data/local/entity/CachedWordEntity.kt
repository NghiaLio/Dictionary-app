package com.dictionary.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_words")
data class CachedWordEntity(
    @PrimaryKey val word: String,
    val jsonData: String,           // Raw JSON String payload of the API response
    val cachedAt: Long = System.currentTimeMillis()
)
