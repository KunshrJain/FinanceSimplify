package com.example.financesimply.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financesimply.data.db.SpendSenseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class WatchedItemStats(
    val name: String,
    val type: String, // "Category" or "Merchant"
    val frequency7Day: Int = 0,
    val spend7Day: Double = 0.0,
    val frequency30Day: Int = 0,
    val spend30Day: Double = 0.0,
    val avgWeeklyFrequency: Double = 0.0,
    val avgWeeklySpend: Double = 0.0,
    val frequencyTrend: TrendDirection = TrendDirection.STABLE,
    val spendTrend: TrendDirection = TrendDirection.STABLE
)

enum class TrendDirection {
    UP, DOWN, STABLE
}

data class AddictionUiState(
    val watchedStats: List<WatchedItemStats> = emptyList(),
    val isLoading: Boolean = true
)

class AddictionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SpendSenseDatabase.getInstance(application)
    private val transactionDao = database.transactionDao()

    private val _uiState = MutableStateFlow(AddictionUiState())
    val uiState: StateFlow<AddictionUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val now = System.currentTimeMillis()
            val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000
            val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000

            // Auto-generate insights based on top spending in the last 30 days
            val topCategories = transactionDao.getCategoryBreakdown(thirtyDaysAgo, now).first().take(3)
            val topMerchants = transactionDao.getTopOutlets(thirtyDaysAgo, now).first().take(3)
            
            val itemsToAnalyze = mutableListOf<Pair<String, String>>()
            topCategories.forEach { itemsToAnalyze.add(Pair(it.category, "Category")) }
            topMerchants.forEach { itemsToAnalyze.add(Pair(it.merchant, "Merchant")) }

            val stats = itemsToAnalyze.map { (name, type) ->
                val freq7 = transactionDao.getFrequencyForItem(name, sevenDaysAgo, now)
                val spend7 = transactionDao.getSpendForItem(name, sevenDaysAgo, now)
                val freq30 = transactionDao.getFrequencyForItem(name, thirtyDaysAgo, now)
                val spend30 = transactionDao.getSpendForItem(name, thirtyDaysAgo, now)

                val avgWeeklyFreq = freq30 / 4.0
                val avgWeeklySpend = spend30 / 4.0

                val freqTrend = when {
                    avgWeeklyFreq <= 0 -> TrendDirection.STABLE
                    freq7 > avgWeeklyFreq * 1.3 -> TrendDirection.UP
                    freq7 < avgWeeklyFreq * 0.7 -> TrendDirection.DOWN
                    else -> TrendDirection.STABLE
                }

                val spendTrend = when {
                    avgWeeklySpend <= 0 -> TrendDirection.STABLE
                    spend7 > avgWeeklySpend * 1.3 -> TrendDirection.UP
                    spend7 < avgWeeklySpend * 0.7 -> TrendDirection.DOWN
                    else -> TrendDirection.STABLE
                }

                WatchedItemStats(
                    name = name,
                    type = type,
                    frequency7Day = freq7,
                    spend7Day = spend7,
                    frequency30Day = freq30,
                    spend30Day = spend30,
                    avgWeeklyFrequency = avgWeeklyFreq,
                    avgWeeklySpend = avgWeeklySpend,
                    frequencyTrend = freqTrend,
                    spendTrend = spendTrend
                )
            }.sortedByDescending { it.spend30Day }

            _uiState.value = AddictionUiState(
                watchedStats = stats,
                isLoading = false
            )
        }
    }
}
