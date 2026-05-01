package com.sleepsounds.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sleepsounds.app.data.local.db.dao.FavoriteDao
import com.sleepsounds.app.data.local.db.dao.GenerationDao
import com.sleepsounds.app.data.local.db.dao.SoundDao
import com.sleepsounds.app.data.local.db.entity.FavoriteEntity
import com.sleepsounds.app.data.local.db.entity.GenerationEntity
import com.sleepsounds.app.data.local.db.entity.SoundEntity

@Database(
    entities = [SoundEntity::class, FavoriteEntity::class, GenerationEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class SleepSoundsDatabase : RoomDatabase() {
    abstract fun soundDao(): SoundDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun generationDao(): GenerationDao

    companion object {
        const val NAME = "sleep_sounds.db"
    }
}
