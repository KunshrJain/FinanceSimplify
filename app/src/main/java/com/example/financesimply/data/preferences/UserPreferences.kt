package com.example.financesimply.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "spendsense_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        private val MONTHLY_BUDGET = longPreferencesKey("monthly_budget")
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val WHITELISTED_PACKAGES = stringSetPreferencesKey("whitelisted_packages")
        private val CATEGORIES = stringPreferencesKey("categories_json")
        private val LAST_BUDGET_THRESHOLD_NOTIFIED = longPreferencesKey("last_budget_threshold_notified")

        val DEFAULT_PACKAGES = setOf(
            "com.google.android.apps.nbu.paisa.user", // GPay
            "com.phonepe.app",                         // PhonePe
            "net.one97.paytm",                         // Paytm
            "com.google.android.apps.messaging",       // Google Messages (SMS)
            "com.samsung.android.messaging",           // Samsung Messages (SMS)
            "com.android.mms"                          // Default SMS app
        )

        val DEFAULT_CATEGORIES = listOf(
            "Food", "Transport", "Shopping", "Bills",
            "Entertainment", "Health", "Education", "Other"
        )

        const val DEFAULT_BUDGET = 30000L
    }

    // Monthly budget
    val monthlyBudget: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[MONTHLY_BUDGET] ?: DEFAULT_BUDGET
    }

    suspend fun setMonthlyBudget(amount: Long) {
        context.dataStore.edit { prefs ->
            prefs[MONTHLY_BUDGET] = amount
        }
    }

    // Onboarding
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED] = completed
        }
    }

    // Whitelisted packages
    val whitelistedPackages: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[WHITELISTED_PACKAGES] ?: DEFAULT_PACKAGES
    }

    suspend fun setWhitelistedPackages(packages: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[WHITELISTED_PACKAGES] = packages
        }
    }

    suspend fun addWhitelistedPackage(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[WHITELISTED_PACKAGES] ?: DEFAULT_PACKAGES
            prefs[WHITELISTED_PACKAGES] = current + packageName
        }
    }

    // Categories
    val categories: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val json = prefs[CATEGORIES]
        if (json != null) {
            json.split(",").filter { it.isNotBlank() }
        } else {
            DEFAULT_CATEGORIES
        }
    }

    suspend fun setCategories(cats: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[CATEGORIES] = cats.joinToString(",")
        }
    }

    // Budget threshold tracking (to avoid spamming notifications)
    val lastBudgetThresholdNotified: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[LAST_BUDGET_THRESHOLD_NOTIFIED] ?: 0L
    }

    suspend fun setLastBudgetThresholdNotified(percentage: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_BUDGET_THRESHOLD_NOTIFIED] = percentage
        }
    }

    suspend fun getMonthlyBudgetSync(): Long {
        return context.dataStore.data.first()[MONTHLY_BUDGET] ?: DEFAULT_BUDGET
    }

    suspend fun getLastThresholdSync(): Long {
        return context.dataStore.data.first()[LAST_BUDGET_THRESHOLD_NOTIFIED] ?: 0L
    }

    suspend fun getWhitelistedPackagesSync(): Set<String> {
        return context.dataStore.data.first()[WHITELISTED_PACKAGES] ?: DEFAULT_PACKAGES
    }
}
