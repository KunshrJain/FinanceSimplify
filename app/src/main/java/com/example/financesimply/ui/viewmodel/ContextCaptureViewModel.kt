package com.example.financesimply.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financesimply.data.db.SpendSenseDatabase
import com.example.financesimply.data.db.entity.TransactionEntity
import com.example.financesimply.data.preferences.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ContextCaptureUiState(
    val transaction: TransactionEntity? = null,
    val categoryInput: String = "",
    val tagInput: String = "",
    val noteInput: String = "",
    val isSplit: Boolean = false,
    val splitParticipants: List<com.example.financesimply.data.db.entity.SplitParticipant> = emptyList(),
    val categorySuggestions: List<String> = emptyList(),
    val tagSuggestions: List<String> = emptyList(),
    val isVisible: Boolean = false
)

class ContextCaptureViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SpendSenseDatabase.getInstance(application)
    private val userPreferences = UserPreferences(application)
    private val transactionDao = database.transactionDao()

    private val _uiState = MutableStateFlow(ContextCaptureUiState())
    val uiState: StateFlow<ContextCaptureUiState> = _uiState.asStateFlow()

    /**
     * Show the context capture sheet for a given transaction id.
     * Pre-fills suggestions from previous tags and categories.
     */
    fun showForTransactionId(id: Long) {
        viewModelScope.launch {
            val transaction = transactionDao.getById(id) ?: return@launch
            showForTransaction(transaction)
        }
    }

    /**
     * Show the context capture sheet for a given transaction.
     * Pre-fills suggestions from previous tags and categories.
     */
    fun showForTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            val categories = (transactionDao.getAllCategories().first() +
                    userPreferences.categories.first()).distinct().sorted()
            val tags = transactionDao.getAllTags().first()

            _uiState.value = ContextCaptureUiState(
                transaction = transaction,
                categoryInput = transaction.category,
                tagInput = transaction.tag,
                isSplit = transaction.splitDetails != null,
                splitParticipants = transaction.splitDetails ?: emptyList(),
                categorySuggestions = categories,
                tagSuggestions = tags,
                isVisible = true
            )
        }
    }

    fun updateCategory(category: String) {
        _uiState.value = _uiState.value.copy(categoryInput = category)
    }

    fun updateTag(tag: String) {
        _uiState.value = _uiState.value.copy(tagInput = tag)
    }

    fun updateNote(note: String) {
        _uiState.value = _uiState.value.copy(noteInput = note)
    }

    fun updateIsSplit(value: Boolean) {
        _uiState.value = _uiState.value.copy(isSplit = value)
        if (value && _uiState.value.splitParticipants.isEmpty()) {
            addParticipant("") // Add an empty row by default
        }
    }

    fun addParticipant(name: String) {
        val current = _uiState.value.splitParticipants.toMutableList()
        current.add(com.example.financesimply.data.db.entity.SplitParticipant(name, 0.0))
        _uiState.value = _uiState.value.copy(splitParticipants = current)
    }

    fun updateParticipantName(index: Int, name: String) {
        val current = _uiState.value.splitParticipants.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(name = name)
            _uiState.value = _uiState.value.copy(splitParticipants = current)
        }
    }

    fun updateParticipantAmount(index: Int, amount: Double) {
        val current = _uiState.value.splitParticipants.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(amountOwed = amount)
            _uiState.value = _uiState.value.copy(splitParticipants = current)
        }
    }
    
    fun removeParticipant(index: Int) {
        val current = _uiState.value.splitParticipants.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _uiState.value = _uiState.value.copy(splitParticipants = current)
        }
    }

    fun autoDivideSplit() {
        val totalAmount = _uiState.value.transaction?.amount ?: 0.0
        val current = _uiState.value.splitParticipants.toMutableList()
        // Divide by participants + user
        val splitCount = current.size + 1
        if (splitCount > 1) {
            val share = totalAmount / splitCount
            for (i in current.indices) {
                current[i] = current[i].copy(amountOwed = share)
            }
            _uiState.value = _uiState.value.copy(splitParticipants = current)
        }
    }

    /**
     * Save the context and dismiss the sheet.
     */
    fun save() {
        val state = _uiState.value
        val transaction = state.transaction ?: return

        viewModelScope.launch {
            val budgetImpact = if (state.isSplit) {
                transaction.amount - state.splitParticipants.sumOf { it.amountOwed }
            } else {
                transaction.amount
            }

            val updated = transaction.copy(
                category = state.categoryInput,
                tag = state.tagInput,
                note = state.noteInput,
                budgetImpactAmount = budgetImpact,
                splitDetails = if (state.isSplit && state.splitParticipants.isNotEmpty()) state.splitParticipants else null,
                status = com.example.financesimply.data.db.entity.TransactionStatus.TAGGED
            )
            transactionDao.update(updated)
            dismiss()
        }
    }

    /**
     * Dismiss without saving — marks the transaction as "Untagged".
     */
    fun dismiss() {
        _uiState.value = ContextCaptureUiState()
    }
}
