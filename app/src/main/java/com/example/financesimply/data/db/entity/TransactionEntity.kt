package com.example.financesimply.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
    DEBIT,
    CREDIT
}

enum class TransactionStatus {
    PENDING,
    TAGGED,
    DISMISSED
}

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["notificationHash"]),
        Index(value = ["timestamp"]),
        Index(value = ["category"]),
        Index(value = ["status"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val merchantRaw: String = "",
    val merchantFriendly: String = "",
    val transactionType: TransactionType = TransactionType.DEBIT,
    val category: String = "",
    val tag: String = "",
    val note: String = "",
    val budgetImpactAmount: Double = amount,
    val splitDetails: List<SplitParticipant>? = null,
    val isExemptFromBudget: Boolean = false,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val sourceApp: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val notificationHash: String = ""
)
