package com.sleepsounds.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sleepsounds.app.data.local.db.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT soundId FROM favorites")
    fun observeIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE soundId = :soundId")
    suspend fun remove(soundId: String)
}
