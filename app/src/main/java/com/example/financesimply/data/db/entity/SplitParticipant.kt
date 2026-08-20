package com.example.financesimply.data.db.entity

data class SplitParticipant(
    val name: String,
    val amountOwed: Double,
    val isSettled: Boolean = false
)
