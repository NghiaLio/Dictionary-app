package com.dictionary.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val word: String,
    val searchedAt: Long = System.currentTimeMillis()
)
