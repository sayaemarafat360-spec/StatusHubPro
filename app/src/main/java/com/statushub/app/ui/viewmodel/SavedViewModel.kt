package com.statushub.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.statushub.app.data.local.preferences.PreferencesManager
import com.statushub.app.data.model.SavedStatus
import com.statushub.app.data.repository.StatusRepository
import com.statushub.app.ui.screens.saved.SavedTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedUiState(
    val isLoading: Boolean = true,
    val savedStatuses: List<SavedStatus> = emptyList(),
    val favoriteStatuses: List<SavedStatus> = emptyList(),
    val hiddenStatuses: List<SavedStatus> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val repository: StatusRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedUiState())
    val uiState: StateFlow<SavedUiState> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(SavedTab.ALL)
    val selectedTab: StateFlow<SavedTab> = _selectedTab.asStateFlow()

    val isPremium = preferencesManager.isPremium

    private var vaultUnlocked = false

    init {
        loadSavedStatuses()
    }

    private fun loadSavedStatuses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                repository.getSavedStatuses(),
                repository.getFavoriteStatuses(),
                repository.getHiddenStatuses()
            ) { saved, favorites, hidden ->
                SavedUiState(
                    isLoading = false,
                    savedStatuses = saved,
                    favoriteStatuses = favorites,
                    hiddenStatuses = hidden
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun selectTab(tab: SavedTab) {
        _selectedTab.value = tab
    }

    fun toggleFavorite(savedStatus: SavedStatus) {
        viewModelScope.launch {
            repository.toggleFavorite(savedStatus)
        }
    }

    fun deleteStatus(savedStatus: SavedStatus) {
        viewModelScope.launch {
            repository.deleteStatus(savedStatus)
        }
    }

    fun isVaultUnlocked(): Boolean {
        return vaultUnlocked
    }

    fun unlockVault(pin: String): Boolean {
        viewModelScope.launch {
            val savedPin = preferencesManager.vaultPin.first()
            if (pin == savedPin) {
                vaultUnlocked = true
            }
        }
        return vaultUnlocked
    }
}
