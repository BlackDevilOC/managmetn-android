package com.substituemanagment.managment.ui.viewmodels

sealed class ProcessUiState {
    object Ready : ProcessUiState()
    object Processing : ProcessUiState()
    object Completed : ProcessUiState()
    data class Error(val message: String) : ProcessUiState()
} 