package com.databridgepro.filemanager.presentation.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.databridgepro.filemanager.data.model.FileItem
import com.databridgepro.filemanager.data.repository.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CategoryUiState {
    data object Loading : CategoryUiState()
    data class Success(val files: List<FileItem>) : CategoryUiState()
    data class Error(val message: String) : CategoryUiState()
}

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val storageRepository: StorageRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryUiState>(CategoryUiState.Loading)
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    fun loadCategory(type: String) {
        viewModelScope.launch {
            _uiState.value = CategoryUiState.Loading
            storageRepository.searchByType(type)
                .catch { e -> _uiState.value = CategoryUiState.Error(e.message ?: "Failed to load files") }
                .collect { files -> _uiState.value = CategoryUiState.Success(files) }
        }
    }
}
