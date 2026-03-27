package com.statushub.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.statushub.app.data.model.Status
import com.statushub.app.data.model.StatusUiState
import com.statushub.app.data.repository.StatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: StatusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatusUiState())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedItems: StateFlow<Set<String>> = _selectedItems.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _lastSavedCount = MutableStateFlow(0)
    val lastSavedCount: StateFlow<Int> = _lastSavedCount.asStateFlow()

    init {
        loadStatuses()
        checkOnboarding()
    }

    private fun checkOnboarding() {
        viewModelScope.launch {
            val hasShown = repository.hasShownOnboarding.first()
            if (!hasShown) {
                // Onboarding will be shown by the navigation
            }
        }
    }

    fun loadStatuses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            repository.getStatuses()
                .onSuccess { statuses ->
                    val lastViewed = repository.lastViewedTimestamp.first()
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            statuses = statuses,
                            hasPermission = true,
                            isNewStatusAvailable = statuses.any { s -> s.timestamp > lastViewed }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
        }
    }

    fun refreshStatuses() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadStatuses()
            _isRefreshing.value = false
        }
    }

    fun markAllAsViewed() {
        viewModelScope.launch {
            repository.setLastViewedTimestamp(System.currentTimeMillis())
            _uiState.update { it.copy(isNewStatusAvailable = false) }
        }
    }

    fun toggleSelection(statusId: String) {
        _selectedItems.update { current ->
            if (current.contains(statusId)) {
                current - statusId
            } else {
                current + statusId
            }
        }
    }

    fun selectAll() {
        _selectedItems.update { 
            _uiState.value.statuses.map { it.id }.toSet()
        }
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
    }

    fun saveStatus(status: Status, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.saveStatus(status)
                .onSuccess {
                    onSaved()
                }
        }
    }

    fun saveSelectedStatuses() {
        viewModelScope.launch {
            val selectedStatuses = _uiState.value.statuses
                .filter { _selectedItems.value.contains(it.id) }
            
            repository.saveStatuses(selectedStatuses)
                .onSuccess { saved ->
                    _lastSavedCount.value = saved.size
                    clearSelection()
                }
        }
    }

    fun openWhatsApp(): Boolean {
        return repository.openWhatsApp()
    }

    fun shareStatus(status: Status) {
        repository.shareStatus(status)
    }
}
