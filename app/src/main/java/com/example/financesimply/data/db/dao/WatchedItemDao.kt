package com.example.financesimply.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.financesimply.data.db.entity.WatchedItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchedItemEntity): Long

    @Delete
    suspend fun delete(item: WatchedItemEntity)

    @Query("SELECT * FROM watched_items ORDER BY value ASC")
    fun getAll(): Flow<List<WatchedItemEntity>>

    @Query("SELECT * FROM watched_items")
    suspend fun getAllSync(): List<WatchedItemEntity>
}
