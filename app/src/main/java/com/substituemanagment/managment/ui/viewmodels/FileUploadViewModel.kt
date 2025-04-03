package com.substituemanagment.managment.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.substituemanagment.managment.data.FileStorageManager
import com.substituemanagment.managment.data.FileType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FileUploadViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<FileUploadUiState>(FileUploadUiState.Idle)
    val uiState: StateFlow<FileUploadUiState> = _uiState

    private lateinit var fileStorageManager: FileStorageManager

    fun initialize(context: Context) {
        fileStorageManager = FileStorageManager(context)
    }

    fun uploadFile(uri: Uri, fileType: FileType, context: Context) {
        viewModelScope.launch {
            try {
                _uiState.value = FileUploadUiState.Loading

                // Read file content
                val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().readText()
                } ?: throw Exception("Failed to read file")

                // Save file
                fileStorageManager.saveFile(fileType, content).fold(
                    onSuccess = { message ->
                        _uiState.value = FileUploadUiState.Success(message)
                    },
                    onFailure = { error ->
                        _uiState.value = FileUploadUiState.Error(error.message ?: "Unknown error occurred")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = FileUploadUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun resetState() {
        _uiState.value = FileUploadUiState.Idle
    }
}

sealed class FileUploadUiState {
    object Idle : FileUploadUiState()
    object Loading : FileUploadUiState()
    data class Success(val message: String) : FileUploadUiState()
    data class Error(val message: String) : FileUploadUiState()
} 