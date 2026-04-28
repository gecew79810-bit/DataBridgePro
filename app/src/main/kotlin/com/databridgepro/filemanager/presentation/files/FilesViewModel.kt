package com.databridgepro.filemanager.presentation.files

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.databridgepro.filemanager.data.model.FileItem
import com.databridgepro.filemanager.data.repository.FileRepository
import com.databridgepro.filemanager.data.repository.SettingsRepository
import com.databridgepro.filemanager.util.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortMode { NAME, SIZE, DATE, TYPE }

sealed class FilesUiState {
    data object Loading : FilesUiState()
    data class Success(val files: List<FileItem>) : FilesUiState()
    data class Error(val message: String) : FilesUiState()
}

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val application: Application,
    private val fileRepository: FileRepository,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<FilesUiState>(FilesUiState.Loading)
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    private val _currentPath = MutableStateFlow(PermissionUtils.getAndroidDataTreeUri())
    val currentPath: StateFlow<Uri> = _currentPath.asStateFlow()

    private val _pathStack = MutableStateFlow<List<Pair<String, Uri>>>(emptyList())
    val pathStack: StateFlow<List<Pair<String, Uri>>> = _pathStack.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<Uri>>(emptySet())
    val selectedFiles: StateFlow<Set<Uri>> = _selectedFiles.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.NAME)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    private val _operationProgress = MutableStateFlow<Int?>(null)
    val operationProgress: StateFlow<Int?> = _operationProgress.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()

    private var clipboardFiles: List<Uri> = emptyList()
    private var clipboardOperation: ClipboardOp = ClipboardOp.NONE

    enum class ClipboardOp { NONE, COPY, MOVE }

    init {
        viewModelScope.launch {
            settingsRepository.safUri.collect { uriStr ->
                if (uriStr.isNotEmpty()) {
                    val uri = Uri.parse(uriStr)
                    _currentPath.value = uri
                    _pathStack.value = listOf("Android/data" to uri)
                    loadFiles()
                }
            }
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun setupSearch() {
        _searchQuery
            .debounce(300)
            .flatMapLatest { query ->
                if (query.isBlank()) fileRepository.getFiles(_currentPath.value)
                else fileRepository.searchFiles(_currentPath.value, query)
            }
            .catch { _uiState.value = FilesUiState.Error(it.message ?: "Search failed") }
            .onEach { files -> _uiState.value = FilesUiState.Success(sortFiles(files)) }
            .launchIn(viewModelScope)
    }

    fun loadFiles() {
        viewModelScope.launch {
            _uiState.value = FilesUiState.Loading
            fileRepository.getFiles(_currentPath.value)
                .catch { _uiState.value = FilesUiState.Error(it.message ?: "Failed to load files") }
                .collect { files ->
                    _uiState.value = FilesUiState.Success(sortFiles(files))
                }
        }
    }

    fun navigateToFolder(fileItem: FileItem) {
        if (!fileItem.isDirectory) return
        val stack = _pathStack.value.toMutableList()
        stack.add(fileItem.name to fileItem.uri)
        _pathStack.value = stack
        _currentPath.value = fileItem.uri
        _selectedFiles.value = emptySet()
        loadFiles()
    }

    fun navigateBack(): Boolean {
        val stack = _pathStack.value
        if (stack.size <= 1) return false
        val newStack = stack.dropLast(1)
        _pathStack.value = newStack
        _currentPath.value = newStack.last().second
        _selectedFiles.value = emptySet()
        loadFiles()
        return true
    }

    fun navigateToBreadcrumb(index: Int) {
        val stack = _pathStack.value
        if (index >= stack.size - 1) return
        val newStack = stack.take(index + 1)
        _pathStack.value = newStack
        _currentPath.value = newStack.last().second
        _selectedFiles.value = emptySet()
        loadFiles()
    }

    fun toggleSelection(uri: Uri) {
        val current = _selectedFiles.value.toMutableSet()
        if (current.contains(uri)) current.remove(uri) else current.add(uri)
        _selectedFiles.value = current
    }

    fun clearSelection() {
        _selectedFiles.value = emptySet()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            loadFiles()
        } else {
            viewModelScope.launch {
                _uiState.value = FilesUiState.Loading
                fileRepository.searchFiles(_currentPath.value, query)
                    .catch { _uiState.value = FilesUiState.Error(it.message ?: "Search failed") }
                    .collect { files -> _uiState.value = FilesUiState.Success(sortFiles(files)) }
            }
        }
    }

    fun toggleSearch() {
        _isSearching.value = !_isSearching.value
        if (!_isSearching.value) {
            _searchQuery.value = ""
            loadFiles()
        }
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
        val state = _uiState.value
        if (state is FilesUiState.Success) {
            _uiState.value = FilesUiState.Success(sortFiles(state.files))
        }
    }

    fun deleteSelectedFiles() {
        viewModelScope.launch {
            val uris = _selectedFiles.value.toList()
            uris.forEach { uri ->
                val doc = DocumentFile.fromSingleUri(application, uri)
                if (doc != null) {
                    fileRepository.deleteFile(doc)
                }
            }
            _selectedFiles.value = emptySet()
            loadFiles()
        }
    }

    fun copySelectedToClipboard() {
        clipboardFiles = _selectedFiles.value.toList()
        clipboardOperation = ClipboardOp.COPY
        _selectedFiles.value = emptySet()
    }

    fun moveSelectedToClipboard() {
        clipboardFiles = _selectedFiles.value.toList()
        clipboardOperation = ClipboardOp.MOVE
        _selectedFiles.value = emptySet()
    }

    fun paste() {
        if (clipboardFiles.isEmpty() || clipboardOperation == ClipboardOp.NONE) return
        viewModelScope.launch {
            val destDoc = DocumentFile.fromTreeUri(application, _currentPath.value) ?: return@launch
            clipboardFiles.forEach { uri ->
                val sourceDoc = DocumentFile.fromSingleUri(application, uri) ?: return@forEach
                when (clipboardOperation) {
                    ClipboardOp.COPY -> fileRepository.copyFile(sourceDoc, destDoc)
                    ClipboardOp.MOVE -> fileRepository.moveFile(sourceDoc, destDoc)
                    ClipboardOp.NONE -> {}
                }
            }
            clipboardFiles = emptyList()
            clipboardOperation = ClipboardOp.NONE
            loadFiles()
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            fileRepository.createFolder(_currentPath.value, name)
            loadFiles()
        }
    }

    fun renameFile(uri: Uri, newName: String) {
        viewModelScope.launch {
            val doc = DocumentFile.fromSingleUri(application, uri) ?: return@launch
            fileRepository.renameFile(doc, newName)
            loadFiles()
        }
    }

    private fun sortFiles(files: List<FileItem>): List<FileItem> {
        val dirs = files.filter { it.isDirectory }
        val nonDirs = files.filter { !it.isDirectory }
        val sortedDirs = dirs.sortedBy { it.name.lowercase() }
        val sortedFiles = when (_sortMode.value) {
            SortMode.NAME -> nonDirs.sortedBy { it.name.lowercase() }
            SortMode.SIZE -> nonDirs.sortedByDescending { it.size }
            SortMode.DATE -> nonDirs.sortedByDescending { it.lastModified }
            SortMode.TYPE -> nonDirs.sortedBy { it.mimeType ?: "" }
        }
        return sortedDirs + sortedFiles
    }
}
