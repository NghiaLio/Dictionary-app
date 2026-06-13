package com.dictionary.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dictionary.app.data.local.dao.SavedWordDao
import com.dictionary.app.data.local.dao.RecentSearchDao
import com.dictionary.app.data.local.dao.CachedWordDao
import com.dictionary.app.data.local.entity.SavedWordEntity
import com.dictionary.app.data.local.entity.RecentSearchEntity
import com.dictionary.app.data.local.entity.CachedWordEntity

@Database(
    entities = [
        SavedWordEntity::class,
        RecentSearchEntity::class,
        CachedWordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract val savedWordDao: SavedWordDao
    abstract val recentSearchDao: RecentSearchDao
    abstract val cachedWordDao: CachedWordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dictionary_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
