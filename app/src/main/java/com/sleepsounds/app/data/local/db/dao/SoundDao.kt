package com.sleepsounds.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sleepsounds.app.data.local.db.entity.SoundEntity
import com.sleepsounds.app.domain.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundDao {
    @Query("SELECT * FROM sounds ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<SoundEntity>>

    @Query("SELECT * FROM sounds WHERE category = :category ORDER BY createdAtEpochMs DESC")
    fun observeByCategory(category: Category): Flow<List<SoundEntity>>

    @Query("SELECT * FROM sounds WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SoundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sounds: List<SoundEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(sound: SoundEntity)

    @Query("DELETE FROM sounds WHERE id = :id")
    suspend fun delete(id: String)
}
