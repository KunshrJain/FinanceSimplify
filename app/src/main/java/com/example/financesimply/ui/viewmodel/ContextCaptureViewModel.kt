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
    val isForSomeoneElse: Boolean = false,
    val someoneElseName: String = "",
    val isExemptFromBudget: Boolean = false,
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
                isForSomeoneElse = transaction.isForSomeoneElse,
                someoneElseName = transaction.someoneElseName,
                isExemptFromBudget = transaction.isExemptFromBudget,
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

    fun updateIsForSomeoneElse(value: Boolean) {
        _uiState.value = _uiState.value.copy(isForSomeoneElse = value)
    }

    fun updateSomeoneElseName(name: String) {
        _uiState.value = _uiState.value.copy(someoneElseName = name)
    }

    fun updateIsExempt(value: Boolean) {
        _uiState.value = _uiState.value.copy(isExemptFromBudget = value)
    }

    /**
     * Save the context and dismiss the sheet.
     */
    fun save() {
        val state = _uiState.value
        val transaction = state.transaction ?: return

        viewModelScope.launch {
            val updated = transaction.copy(
                category = state.categoryInput,
                tag = state.tagInput,
                note = state.noteInput,
                isForSomeoneElse = state.isForSomeoneElse,
                someoneElseName = state.someoneElseName,
                isExemptFromBudget = state.isExemptFromBudget,
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
