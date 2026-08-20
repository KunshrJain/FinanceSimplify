package com.example.financesimply.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financesimply.data.db.SpendSenseDatabase
import com.example.financesimply.data.db.entity.WatchedItemEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WatchedItemStats(
    val item: WatchedItemEntity,
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
    private val watchedItemDao = database.watchedItemDao()

    private val _uiState = MutableStateFlow(AddictionUiState())
    val uiState: StateFlow<AddictionUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            watchedItemDao.getAll().collect { items ->
                val now = System.currentTimeMillis()
                val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000
                val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000

                val stats = items.map { item ->
                    val freq7 = transactionDao.getFrequencyForItem(item.value, sevenDaysAgo, now)
                    val spend7 = transactionDao.getSpendForItem(item.value, sevenDaysAgo, now)
                    val freq30 = transactionDao.getFrequencyForItem(item.value, thirtyDaysAgo, now)
                    val spend30 = transactionDao.getSpendForItem(item.value, thirtyDaysAgo, now)

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
                        item = item,
                        frequency7Day = freq7,
                        spend7Day = spend7,
                        frequency30Day = freq30,
                        spend30Day = spend30,
                        avgWeeklyFrequency = avgWeeklyFreq,
                        avgWeeklySpend = avgWeeklySpend,
                        frequencyTrend = freqTrend,
                        spendTrend = spendTrend
                    )
                }

                _uiState.value = AddictionUiState(
                    watchedStats = stats,
                    isLoading = false
                )
            }
        }
    }
}
