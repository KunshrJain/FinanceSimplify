package com.example.financesimply.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financesimply.data.db.SpendSenseDatabase
import com.example.financesimply.data.db.dao.CategoryTotal
import com.example.financesimply.data.db.dao.DailySpend
import com.example.financesimply.data.db.dao.MerchantTotal
import com.example.financesimply.data.db.entity.TransactionEntity
import com.example.financesimply.data.preferences.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class PersonOwed(
    val name: String,
    val totalOwed: Double,
    val transactionCount: Int
)

data class DashboardUiState(
    val monthlyTotal: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val budget: Long = 30000L,
    val budgetRemaining: Double = 30000.0,
    val daysLeftInMonth: Int = 30,
    val daysPassed: Int = 1,
    val dailyAverage: Double = 0.0,
    val projectedOverspend: Double = 0.0,
    val categoryBreakdown: List<CategoryTotal> = emptyList(),
    val topOutlets: List<MerchantTotal> = emptyList(),
    val dailySpendTrend: List<DailySpend> = emptyList(),
    val owedToYou: Double = 0.0,
    val owedByPerson: List<PersonOwed> = emptyList(),
    val isLoading: Boolean = true
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SpendSenseDatabase.getInstance(application)
    private val userPreferences = UserPreferences(application)
    private val transactionDao = database.transactionDao()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // Month boundaries
    private val monthStart: Long
    private val monthEnd: Long
    private val totalDaysInMonth: Int
    private val daysLeft: Int
    private val daysPassed: Int

    init {
        val cal = Calendar.getInstance()
        totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        daysPassed = currentDay
        daysLeft = totalDaysInMonth - currentDay + 1

        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        monthStart = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        monthEnd = cal.timeInMillis

        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            combine(
                transactionDao.getMonthlyTotal(monthStart, monthEnd),
                transactionDao.getMonthlyIncome(monthStart, monthEnd),
                userPreferences.monthlyBudget,
                transactionDao.getCategoryBreakdown(monthStart, monthEnd),
                transactionDao.getTopOutlets(monthStart, monthEnd),
                transactionDao.getDailySpendTrend(monthStart, monthEnd),
                transactionDao.getTransactionsWithSplits()
            ) { args ->
                val total = args[0] as Double
                val income = args[1] as Double
                val budget = args[2] as Long
                @Suppress("UNCHECKED_CAST")
                val categories = args[3] as List<CategoryTotal>
                @Suppress("UNCHECKED_CAST")
                val outlets = args[4] as List<MerchantTotal>
                @Suppress("UNCHECKED_CAST")
                val trend = args[5] as List<DailySpend>
                @Suppress("UNCHECKED_CAST")
                val splits = args[6] as List<TransactionEntity>
                
                val remaining = budget - total
                val dailyAvg = if (daysPassed > 0) total / daysPassed else 0.0
                val projected = dailyAvg * totalDaysInMonth
                val projectedOverspend = (projected - budget).coerceAtLeast(0.0)

                // Compute per-person owed breakdown
                var totalOwed = 0.0
                val personMap = mutableMapOf<String, Pair<Double, Int>>()
                
                splits.forEach { txn ->
                    txn.splitDetails?.forEach { participant ->
                        if (!participant.isSettled) {
                            totalOwed += participant.amountOwed
                            val name = participant.name.ifBlank { "Unknown" }
                            val existing = personMap[name] ?: Pair(0.0, 0)
                            personMap[name] = Pair(
                                existing.first + participant.amountOwed,
                                existing.second + 1
                            )
                        }
                    }
                }

                val owedByPerson = personMap.map { (name, pair) ->
                    PersonOwed(
                        name = name,
                        totalOwed = pair.first,
                        transactionCount = pair.second
                    )
                }.sortedByDescending { it.totalOwed }

                DashboardUiState(
                    monthlyTotal = total,
                    monthlyIncome = income,
                    budget = budget,
                    budgetRemaining = remaining,
                    daysLeftInMonth = daysLeft,
                    daysPassed = daysPassed,
                    dailyAverage = dailyAvg,
                    projectedOverspend = projectedOverspend,
                    categoryBreakdown = categories,
                    topOutlets = outlets,
                    dailySpendTrend = trend,
                    owedToYou = totalOwed,
                    owedByPerson = owedByPerson,
                    isLoading = false
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                DashboardUiState()
            ).collect { state ->
                _uiState.value = state
            }
        }
    }

    /**
     * Settle a specific person's debt across all unsettled splits.
     */
    fun settlePersonDebt(personName: String) {
        viewModelScope.launch {
            val splits = transactionDao.getTransactionsWithSplits()
            splits.collect { transactions ->
                transactions.forEach { txn ->
                    val updatedParticipants = txn.splitDetails?.map { participant ->
                        if (participant.name == personName && !participant.isSettled) {
                            participant.copy(isSettled = true)
                        } else {
                            participant
                        }
                    }
                    if (updatedParticipants != txn.splitDetails) {
                        transactionDao.update(txn.copy(splitDetails = updatedParticipants))
                    }
                }
                return@collect
            }
        }
    }
}
