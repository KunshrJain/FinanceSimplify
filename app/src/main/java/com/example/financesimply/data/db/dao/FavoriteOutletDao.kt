package com.example.financesimply.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.financesimply.data.db.entity.FavoriteOutletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteOutletDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(outlet: FavoriteOutletEntity): Long

    @Update
    suspend fun update(outlet: FavoriteOutletEntity)

    @Delete
    suspend fun delete(outlet: FavoriteOutletEntity)

    @Query("SELECT * FROM favorite_outlets ORDER BY friendlyName ASC")
    fun getAll(): Flow<List<FavoriteOutletEntity>>

    @Query("SELECT * FROM favorite_outlets")
    suspend fun getAllSync(): List<FavoriteOutletEntity>

    @Query("SELECT * FROM favorite_outlets WHERE :merchantRaw LIKE '%' || rawMerchantPattern || '%' LIMIT 1")
    suspend fun findMatchingOutlet(merchantRaw: String): FavoriteOutletEntity?
}
