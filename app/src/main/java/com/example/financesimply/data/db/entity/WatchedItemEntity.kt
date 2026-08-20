package com.example.financesimply.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WatchedItemType {
    CATEGORY,
    MERCHANT
}

@Entity(tableName = "watched_items")
data class WatchedItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: WatchedItemType,
    val value: String
)
