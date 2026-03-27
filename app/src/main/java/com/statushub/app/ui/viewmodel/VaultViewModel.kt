package com.statushub.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.statushub.app.data.model.SavedStatus
import com.statushub.app.data.repository.StatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultUiState(
    val isLoading: Boolean = true,
    val hiddenStatuses: List<SavedStatus> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: StatusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var correctPin: String? = null

    init {
        loadHiddenStatuses()
    }

    private fun loadHiddenStatuses() {
        viewModelScope.launch {
            repository.getHiddenStatuses().collect { statuses ->
                _uiState.value = VaultUiState(
                    isLoading = false,
                    hiddenStatuses = statuses
                )
            }
        }
    }

    fun checkVaultStatus() {
        viewModelScope.launch {
            correctPin = repository.vaultPin.first()
            if (correctPin == null) {
                // No PIN set, vault is unlocked
                _isLocked.value = false
            }
        }
    }

    fun unlockVault(pin: String): Boolean {
        return if (pin == correctPin) {
            _isLocked.value = false
            true
        } else {
            false
        }
    }

    fun lockVault() {
        _isLocked.value = true
    }

    fun setupPin(pin: String) {
        viewModelScope.launch {
            repository.setVaultPin(pin)
            correctPin = pin
            _isLocked.value = false
        }
    }

    fun deleteStatus(savedStatus: SavedStatus) {
        viewModelScope.launch {
            repository.deleteStatus(savedStatus)
        }
    }

    fun moveToRegular(savedStatus: SavedStatus) {
        viewModelScope.launch {
            repository.moveFromVault(savedStatus)
        }
    }
}
