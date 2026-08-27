package com.example.financesimply.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financesimply.data.db.SpendSenseDatabase
import com.example.financesimply.data.db.entity.TransactionEntity
import com.example.financesimply.data.db.entity.TransactionStatus
import com.example.financesimply.data.db.entity.TransactionType
import com.example.financesimply.service.parser.CsvParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class InsightsUiState(
    val headers: List<String> = emptyList(),
    val transactions: List<CsvParser.CsvTransaction> = emptyList(),
    val totalDebit: Double = 0.0,
    val totalCredit: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasData: Boolean = false,
    val importedCount: Int = 0,
    val showImportSuccess: Boolean = false
)

class InsightsViewModel(application: Application) : AndroidViewModel(application) {

    private val csvParser = CsvParser(application)
    private val database = SpendSenseDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    fun importCsv(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, showImportSuccess = false)

            val result = csvParser.parse(uri)

            _uiState.value = _uiState.value.copy(
                headers = result.headers,
                transactions = result.transactions,
                totalDebit = result.totalDebit,
                totalCredit = result.totalCredit,
                isLoading = false,
                error = result.error,
                hasData = result.transactions.isNotEmpty()
            )
        }
    }

    /**
     * Import parsed CSV transactions into the Room database.
     */
    fun importToDatabase() {
        val transactions = _uiState.value.transactions
        if (transactions.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            var importedCount = 0
            val dao = database.transactionDao()

            // Common date formats used by Indian banks
            val dateFormats = listOf(
                SimpleDateFormat("dd/MM/yy", Locale.getDefault()),
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
                SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
                SimpleDateFormat("dd-MM-yy", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()),
                SimpleDateFormat("dd-MMM-yy", Locale.getDefault()),
                SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            )

            for (csvTxn in transactions) {
                val isDebit = csvTxn.debit != null && csvTxn.debit > 0
                val isCredit = csvTxn.credit != null && csvTxn.credit > 0
                if (!isDebit && !isCredit) continue

                val amount = if (isDebit) csvTxn.debit!! else csvTxn.credit!!
                val type = if (isDebit) TransactionType.DEBIT else TransactionType.CREDIT

                // Parse date
                var timestamp = System.currentTimeMillis()
                for (format in dateFormats) {
                    try {
                        format.isLenient = false
                        val date = format.parse(csvTxn.date)
                        if (date != null) {
                            timestamp = date.time
                            break
                        }
                    } catch (_: Exception) {
                        // Try next format
                    }
                }

                val entity = TransactionEntity(
                    amount = amount,
                    merchantRaw = csvTxn.description,
                    merchantFriendly = csvTxn.merchantName.ifBlank { csvTxn.description.take(40) },
                    transactionType = type,
                    category = csvTxn.category,
                    sourceApp = "csv_import",
                    timestamp = timestamp,
                    status = if (csvTxn.category.isNotBlank()) TransactionStatus.TAGGED else TransactionStatus.PENDING
                )

                dao.insert(entity)
                importedCount++
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                importedCount = importedCount,
                showImportSuccess = true
            )
        }
    }

    fun clearData() {
        _uiState.value = InsightsUiState()
    }

    fun dismissSuccess() {
        _uiState.value = _uiState.value.copy(showImportSuccess = false)
    }
}
