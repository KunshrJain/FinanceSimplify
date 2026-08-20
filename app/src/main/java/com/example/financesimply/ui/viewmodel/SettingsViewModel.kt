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
import kotlinx.coroutines.launch

data class SettingsUiState(
    val budget: Long = 30000L,
    val whitelistedPackages: Set<String> = emptySet(),
    val watchedItems: List<WatchedItemEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val isNotificationAccessGranted: Boolean = false,
    val onboardingCompleted: Boolean = true,
    val isLoading: Boolean = true
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SpendSenseDatabase.getInstance(application)
    private val userPreferences = UserPreferences(application)
    private val watchedItemDao = database.watchedItemDao()

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
                watchedItemDao.getAll(),
                userPreferences.categories,
                userPreferences.onboardingCompleted
            ) { budget, packages, watched, categories, onboarded ->
                SettingsUiState(
                    budget = budget,
                    whitelistedPackages = packages,
                    watchedItems = watched,
                    categories = categories,
                    isNotificationAccessGranted = checkNotificationAccess(),
                    onboardingCompleted = onboarded,
                    isLoading = false
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

    fun addWatchedItem(type: WatchedItemType, value: String) {
        viewModelScope.launch {
            watchedItemDao.insert(WatchedItemEntity(type = type, value = value))
        }
    }

    fun removeWatchedItem(item: WatchedItemEntity) {
        viewModelScope.launch {
            watchedItemDao.delete(item)
        }
    }

    fun setOnboardingCompleted() {
        viewModelScope.launch {
            userPreferences.setOnboardingCompleted(true)
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
