package com.example.financesimply.service

import com.example.financesimply.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Singleton event bus for relaying newly captured transactions
 * from the NotificationListenerService to the UI layer.
 * Uses SharedFlow so events are not lost even if no collector is active.
 */
object TransactionEventBus {

    private val _newTransaction = MutableSharedFlow<TransactionEntity>(
        replay = 1,
        extraBufferCapacity = 10
    )
    val newTransaction: SharedFlow<TransactionEntity> = _newTransaction.asSharedFlow()

    suspend fun emit(transaction: TransactionEntity) {
        _newTransaction.emit(transaction)
    }
}
