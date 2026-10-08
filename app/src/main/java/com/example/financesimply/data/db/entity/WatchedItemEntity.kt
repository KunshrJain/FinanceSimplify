package com.example.financesimply.data.db.entity


enum class WatchedItemType {
    CATEGORY,
    MERCHANT
}

data class WatchedItemEntity(
    val id: Long = 0,
    val type: WatchedItemType,
    val value: String
)
