package com.substituemanagment.managment.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.substituemanagment.managment.data.FileStorageManager
import com.substituemanagment.managment.data.FileType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FileUploadViewModel : ViewModel() {
    private val TAG = "FileUploadViewModel"
    private val _uiState = MutableStateFlow<FileUploadUiState>(FileUploadUiState.Idle)
    val uiState: StateFlow<FileUploadUiState> = _uiState

    private lateinit var fileStorageManager: FileStorageManager

    fun initialize(context: Context) {
        fileStorageManager = FileStorageManager(context)
        Log.d(TAG, "FileUploadViewModel initialized")
        Log.d(TAG, "Storage Info:\n${fileStorageManager.getStorageInfo()}")
    }

    fun uploadFile(uri: Uri, fileType: FileType, context: Context) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting file upload for type: $fileType")
                _uiState.value = FileUploadUiState.Loading

                fileStorageManager.saveFile(uri, fileType).fold(
                    onSuccess = { message ->
                        Log.d(TAG, "File upload success: $message")
                        _uiState.value = FileUploadUiState.Success(message)
                    },
                    onFailure = { error ->
                        val errorMsg = error.message ?: "Unknown error occurred"
                        Log.e(TAG, "File upload failed: $errorMsg", error)
                        _uiState.value = FileUploadUiState.Error(errorMsg)
                    }
                )
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error occurred"
                Log.e(TAG, "File upload exception: $errorMsg", e)
                _uiState.value = FileUploadUiState.Error(errorMsg)
            }
        }
    }

    fun resetState() {
        _uiState.value = FileUploadUiState.Idle
    }

    fun checkFileExists(fileType: FileType): Boolean {
        return fileStorageManager.doesFileExist(fileType)
    }
}

sealed class FileUploadUiState {
    object Idle : FileUploadUiState()
    object Loading : FileUploadUiState()
    data class Success(val message: String) : FileUploadUiState()
    data class Error(val message: String) : FileUploadUiState()
} 