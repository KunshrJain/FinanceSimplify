package com.example.financesimply.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financesimply.data.db.SpendSenseDatabase
import com.example.financesimply.data.db.entity.WatchedItemEntity
import com.example.financesimply.data.db.entity.WatchedItemType
import com.example.financesimply.data.preferences.UserPreferences
import com.example.financesimply.service.SpendSenseNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUiState(
    val budget: Long = 30000L,
    val whitelistedPackages: Set<String> = emptySet(),
    val categories: List<String> = emptyList(),
    val isNotificationAccessGranted: Boolean = false,
    val onboardingCompleted: Boolean = true,
    val isLoading: Boolean = true,
    val watchedItems: List<WatchedItemEntity> = emptyList(),
    val existingMerchants: List<String> = emptyList(),
    val llmApiKey: String = ""
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SpendSenseDatabase.getInstance(application)
    private val userPreferences = UserPreferences(application)
    private val watchedItemDao = database.watchedItemDao()
    private val transactionDao = database.transactionDao()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                userPreferences.monthlyBudget,
                userPreferences.whitelistedPackages,
                userPreferences.categories,
                userPreferences.onboardingCompleted,
                watchedItemDao.getAll(),
                transactionDao.getAllMerchantNames(),
                transactionDao.getAllCategories(),
                userPreferences.llmApiKey
            ) { args ->
                @Suppress("UNCHECKED_CAST")
                val budget = args[0] as Long
                @Suppress("UNCHECKED_CAST")
                val packages = args[1] as Set<String>
                @Suppress("UNCHECKED_CAST")
                val categories = args[2] as List<String>
                val onboarded = args[3] as Boolean
                @Suppress("UNCHECKED_CAST")
                val watchedItems = args[4] as List<WatchedItemEntity>
                @Suppress("UNCHECKED_CAST")
                val merchants = args[5] as List<String>
                @Suppress("UNCHECKED_CAST")
                val dbCategories = args[6] as List<String>
                val apiKey = args[7] as String

                val allCategories = (categories + dbCategories).distinct().sorted()

                SettingsUiState(
                    budget = budget,
                    whitelistedPackages = packages,
                    categories = allCategories,
                    isNotificationAccessGranted = checkNotificationAccess(),
                    onboardingCompleted = onboarded,
                    isLoading = false,
                    watchedItems = watchedItems,
                    existingMerchants = merchants,
                    llmApiKey = apiKey
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun updateBudget(amount: Long) {
        viewModelScope.launch {
            userPreferences.setMonthlyBudget(amount)
        }
    }

    fun addWhitelistedPackage(packageName: String) {
        viewModelScope.launch {
            userPreferences.addWhitelistedPackage(packageName)
        }
    }

    fun setWhitelistedPackages(packages: Set<String>) {
        viewModelScope.launch {
            userPreferences.setWhitelistedPackages(packages)
        }
    }

    fun setOnboardingCompleted() {
        viewModelScope.launch {
            userPreferences.setOnboardingCompleted(true)
        }
    }

    fun addWatchedItem(typeStr: String, value: String) {
        viewModelScope.launch {
            val type = when (typeStr.uppercase()) {
                "CATEGORY" -> WatchedItemType.CATEGORY
                "MERCHANT" -> WatchedItemType.MERCHANT
                else -> WatchedItemType.CATEGORY
            }
            watchedItemDao.insert(WatchedItemEntity(type = type, value = value))
        }
    }

    fun deleteWatchedItem(item: WatchedItemEntity) {
        viewModelScope.launch {
            watchedItemDao.delete(item)
        }
    }

    fun setLlmApiKey(key: String) {
        viewModelScope.launch {
            userPreferences.setLlmApiKey(key)
        }
    }

    fun refreshNotificationAccess() {
        _uiState.value = _uiState.value.copy(
            isNotificationAccessGranted = checkNotificationAccess()
        )
    }

    private fun checkNotificationAccess(): Boolean {
        val context = getApplication<Application>()
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        val componentName = ComponentName(context, SpendSenseNotificationListener::class.java)
        return enabledListeners.contains(componentName.flattenToString())
    }
}
