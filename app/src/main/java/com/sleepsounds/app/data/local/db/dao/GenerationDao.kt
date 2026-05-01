package com.sleepsounds.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sleepsounds.app.data.local.db.entity.GenerationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GenerationDao {
    @Query("SELECT * FROM generations ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<GenerationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: GenerationEntity)

    @Query("DELETE FROM generations WHERE id = :id")
    suspend fun delete(id: String)
}
