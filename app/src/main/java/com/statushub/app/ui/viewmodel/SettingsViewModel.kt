package com.statushub.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.statushub.app.data.model.SaveLocation
import com.statushub.app.data.model.ThemeMode
import com.statushub.app.data.repository.StatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: StatusRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = repository.themeMode
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ThemeMode.SYSTEM
        )

    val saveLocation: StateFlow<SaveLocation> = repository.saveLocation
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SaveLocation.APP_PRIVATE
        )

    val isPremium: Flow<Boolean> = repository.isPremium

    private val _cacheSize = MutableStateFlow("0 MB")
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    init {
        updateCacheSize()
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setSaveLocation(location: SaveLocation) {
        viewModelScope.launch {
            repository.setSaveLocation(location)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            updateCacheSize()
        }
    }

    private fun updateCacheSize() {
        val size = repository.getCacheSize()
        _cacheSize.value = repository.formatFileSize(size)
    }
}
