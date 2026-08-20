package com.example.financesimply.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financesimply.data.db.SpendSenseDatabase
import com.example.financesimply.data.db.entity.FavoriteOutletEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FavoriteOutletsUiState(
    val outlets: List<FavoriteOutletEntity> = emptyList(),
    val isLoading: Boolean = true
)

class FavoriteOutletsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SpendSenseDatabase.getInstance(application)
    private val outletDao = database.favoriteOutletDao()

    private val _uiState = MutableStateFlow(FavoriteOutletsUiState())
    val uiState: StateFlow<FavoriteOutletsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            outletDao.getAll().collect { outlets ->
                _uiState.value = FavoriteOutletsUiState(
                    outlets = outlets,
                    isLoading = false
                )
            }
        }
    }

    fun addOutlet(rawPattern: String, friendlyName: String, defaultCategory: String) {
        viewModelScope.launch {
            outletDao.insert(
                FavoriteOutletEntity(
                    rawMerchantPattern = rawPattern,
                    friendlyName = friendlyName,
                    defaultCategory = defaultCategory
                )
            )
        }
    }

    fun updateOutlet(outlet: FavoriteOutletEntity) {
        viewModelScope.launch {
            outletDao.update(outlet)
        }
    }

    fun deleteOutlet(outlet: FavoriteOutletEntity) {
        viewModelScope.launch {
            outletDao.delete(outlet)
        }
    }
}
