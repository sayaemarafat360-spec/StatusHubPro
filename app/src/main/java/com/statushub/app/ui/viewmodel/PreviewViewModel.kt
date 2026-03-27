package com.statushub.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.statushub.app.data.model.SavedStatus
import com.statushub.app.data.model.Status
import com.statushub.app.data.repository.StatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PreviewUiState(
    val isLoading: Boolean = true,
    val items: List<Any> = emptyList(), // Can be Status or SavedStatus
    val currentIndex: Int = 0,
    val error: String? = null
)

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val repository: StatusRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState.asStateFlow()

    fun loadItems(statusId: String, isSavedStatus: Boolean) {
        viewModelScope.launch {
            _uiState.value = PreviewUiState(isLoading = true)
            
            if (isSavedStatus) {
                // Load saved statuses
                repository.getSavedStatuses().first().let { savedStatuses ->
                    val index = savedStatuses.indexOfFirst { it.id == statusId }
                    _uiState.value = PreviewUiState(
                        isLoading = false,
                        items = savedStatuses,
                        currentIndex = if (index >= 0) index else 0
                    )
                }
            } else {
                // Load regular statuses
                repository.getStatuses()
                    .onSuccess { statuses ->
                        val index = statuses.indexOfFirst { it.id == statusId }
                        _uiState.value = PreviewUiState(
                            isLoading = false,
                            items = statuses,
                            currentIndex = if (index >= 0) index else 0
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = PreviewUiState(
                            isLoading = false,
                            error = error.message
                        )
                    }
            }
        }
    }

    fun saveStatus(status: Status) {
        viewModelScope.launch {
            repository.saveStatus(status)
        }
    }

    fun toggleFavorite(savedStatus: SavedStatus) {
        viewModelScope.launch {
            repository.toggleFavorite(savedStatus)
        }
    }

    fun shareItem(item: Any) {
        when (item) {
            is Status -> repository.shareStatus(item)
            is SavedStatus -> repository.shareSavedStatus(item)
        }
    }
}
