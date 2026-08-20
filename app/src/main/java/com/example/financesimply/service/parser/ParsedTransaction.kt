package com.example.financesimply.service.parser

import com.example.financesimply.data.db.entity.TransactionType

data class ParsedTransaction(
    val amount: Double,
    val merchantName: String?,
    val transactionType: TransactionType,
    val timestamp: Long = System.currentTimeMillis()
)
