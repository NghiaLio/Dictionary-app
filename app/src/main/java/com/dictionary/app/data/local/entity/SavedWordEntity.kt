package com.dictionary.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_words")
data class SavedWordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val phonetic: String?,
    val audioUrl: String?,
    val shortDefinition: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
