package com.example.financesimply.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financesimply.data.db.SpendSenseDatabase
import com.example.financesimply.data.db.entity.TransactionEntity
import com.example.financesimply.data.db.entity.TransactionStatus
import com.example.financesimply.data.db.entity.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TransactionFilter {
    ALL, UNTAGGED, SPLIT, DEBIT_ONLY, CREDIT_ONLY, EXEMPT, OWED
}

data class TransactionListUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val untaggedCount: Int = 0,
    val filter: TransactionFilter = TransactionFilter.ALL,
    val isLoading: Boolean = true
)

class TransactionListViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SpendSenseDatabase.getInstance(application)
    private val transactionDao = database.transactionDao()

    private val _uiState = MutableStateFlow(TransactionListUiState())
    val uiState: StateFlow<TransactionListUiState> = _uiState.asStateFlow()

    private val _currentFilter = MutableStateFlow(TransactionFilter.ALL)

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            transactionDao.getAll().collect { allTransactions ->
                val filtered = when (_currentFilter.value) {
                    TransactionFilter.ALL -> allTransactions
                    TransactionFilter.UNTAGGED -> allTransactions.filter { 
                        it.status == TransactionStatus.PENDING 
                    }
                    TransactionFilter.SPLIT -> allTransactions.filter { 
                        it.splitDetails != null 
                    }
                    TransactionFilter.DEBIT_ONLY -> allTransactions.filter { 
                        it.transactionType == TransactionType.DEBIT 
                    }
                    TransactionFilter.CREDIT_ONLY -> allTransactions.filter { 
                        it.transactionType == TransactionType.CREDIT 
                    }
                    TransactionFilter.EXEMPT -> allTransactions.filter { 
                        it.isExemptFromBudget || (it.budgetImpactAmount == 0.0 && it.amount > 0.0 && it.transactionType == TransactionType.DEBIT)
                    }
                    TransactionFilter.OWED -> allTransactions.filter { 
                        it.splitDetails?.any { p -> !p.isSettled } == true 
                    }
                }
                _uiState.value = TransactionListUiState(
                    transactions = filtered,
                    untaggedCount = allTransactions.count { it.status == TransactionStatus.PENDING },
                    filter = _currentFilter.value,
                    isLoading = false
                )
            }
        }
    }

    fun setFilter(filter: TransactionFilter) {
        _currentFilter.value = filter
        loadTransactions()
    }

    fun updateTransaction(
        transaction: TransactionEntity,
        category: String? = null,
        tag: String? = null,
        note: String? = null
    ) {
        viewModelScope.launch {
            val updated = transaction.copy(
                category = category ?: transaction.category,
                tag = tag ?: transaction.tag,
                note = note ?: transaction.note,
                status = TransactionStatus.TAGGED
            )
            transactionDao.update(updated)
        }
    }
}
