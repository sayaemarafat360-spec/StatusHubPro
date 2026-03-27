package com.statushub.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.statushub.app.data.repository.StatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: StatusRepository
) : ViewModel() {

    fun setOnboardingComplete() {
        viewModelScope.launch {
            repository.setOnboardingShown(true)
        }
    }
}
