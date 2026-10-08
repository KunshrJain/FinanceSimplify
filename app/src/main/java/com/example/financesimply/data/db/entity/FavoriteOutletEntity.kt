package com.example.financesimply.data.db.entity


data class FavoriteOutletEntity(
    val id: Long = 0,
    val rawMerchantPattern: String,
    val friendlyName: String,
    val defaultCategory: String = ""
)
