package com.substituemanagment.managment.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.substituemanagment.managment.data.TimetableProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProcessViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ProcessUiState>(ProcessUiState.Ready)
    val uiState: StateFlow<ProcessUiState> = _uiState.asStateFlow()

    private var timetableProcessor: TimetableProcessor? = null

    fun initialize(context: Context) {
        timetableProcessor = TimetableProcessor(context)
    }

    fun processTimetable() {
        viewModelScope.launch {
            _uiState.value = ProcessUiState.Processing
            
            try {
                val success = timetableProcessor?.processTimetable() ?: false
                if (success) {
                    _uiState.value = ProcessUiState.Completed
                } else {
                    _uiState.value = ProcessUiState.Error("Failed to process timetable")
                }
            } catch (e: Exception) {
                _uiState.value = ProcessUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
} 