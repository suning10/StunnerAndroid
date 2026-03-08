package com.seclass.stunner.viewmodel

import androidx.lifecycle.ViewModel
import com.seclass.stunner.model.MainUiState
import com.seclass.stunner.repository.MainRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(
    private val repository: MainRepository = MainRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        _uiState.value = MainUiState(message = repository.getMessage())
    }

    fun updateMessage(newMessage: String) {
        _uiState.value = _uiState.value.copy(message = newMessage)
    }
}
