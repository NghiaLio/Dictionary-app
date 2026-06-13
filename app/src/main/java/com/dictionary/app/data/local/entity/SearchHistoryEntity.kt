package com.dictionary.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val word: String,
    val searchedAt: Long = System.currentTimeMillis()
)
