package com.example.financesimply.data.db.entity


enum class TransactionType {
    DEBIT,
    CREDIT
}

enum class TransactionStatus {
    PENDING,
    TAGGED,
    DISMISSED
}

data class TransactionEntity(
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
