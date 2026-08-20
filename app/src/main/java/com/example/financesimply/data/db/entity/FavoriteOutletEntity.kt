package com.example.financesimply.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_outlets")
data class FavoriteOutletEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawMerchantPattern: String,
    val friendlyName: String,
    val defaultCategory: String = ""
)
