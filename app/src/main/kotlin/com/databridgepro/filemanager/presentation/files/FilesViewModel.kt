package com.databridgepro.filemanager.presentation.files

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.databridgepro.filemanager.data.model.FileItem
import com.databridgepro.filemanager.data.repository.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class SortMode { NAME, SIZE, DATE, TYPE }
enum class ViewMode { LIST, GRID }

sealed class FilesUiState {
    data object Loading : FilesUiState()
    data class Success(val files: List<FileItem>) : FilesUiState()
    data class Error(val message: String) : FilesUiState()
}

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val application: Application,
    private val storageRepository: StorageRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<FilesUiState>(FilesUiState.Loading)
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    private val _currentPath = MutableStateFlow(storageRepository.getRootPath())
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _pathStack = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val pathStack: StateFlow<List<Pair<String, String>>> = _pathStack.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
    val selectedFiles: StateFlow<Set<String>> = _selectedFiles.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.NAME)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _clipboardCount = MutableStateFlow(0)
    val clipboardCount: StateFlow<Int> = _clipboardCount.asStateFlow()

    private var clipboardPaths: List<String> = emptyList()
    private var clipboardOperation: ClipboardOp = ClipboardOp.NONE
    private var loadJob: Job? = null

    enum class ClipboardOp { NONE, COPY, MOVE }

    init {
        val root = storageRepository.getRootPath()
        _pathStack.value = listOf("Internal Storage" to root)
        _currentPath.value = root
        loadFiles()
    }

    fun loadFiles() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = FilesUiState.Loading
            storageRepository.listFiles(_currentPath.value)
                .catch { _uiState.value = FilesUiState.Error(it.message ?: "Failed to load files") }
                .collect { files ->
                    _uiState.value = FilesUiState.Success(sortFiles(files))
                }
        }
    }

    fun navigateToPath(path: String, name: String) {
        val stack = _pathStack.value.toMutableList()
        stack.add(name to path)
        _pathStack.value = stack
        _currentPath.value = path
        _selectedFiles.value = emptySet()
        loadFiles()
    }

    fun navigateToFolder(fileItem: FileItem) {
        if (!fileItem.isDirectory) return
        navigateToPath(fileItem.path, fileItem.name)
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

    fun toggleSelection(path: String) {
        val current = _selectedFiles.value.toMutableSet()
        if (current.contains(path)) current.remove(path) else current.add(path)
        _selectedFiles.value = current
    }

    fun selectAll() {
        val state = _uiState.value
        if (state is FilesUiState.Success) {
            _selectedFiles.value = state.files.map { it.path }.toSet()
        }
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
                storageRepository.searchFiles(_currentPath.value, query)
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

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
    }

    fun deleteSelectedFiles() {
        viewModelScope.launch {
            val paths = _selectedFiles.value.toList()
            paths.forEach { path ->
                storageRepository.deleteFile(File(path))
            }
            _selectedFiles.value = emptySet()
            loadFiles()
        }
    }

    fun deleteFile(path: String) {
        viewModelScope.launch {
            storageRepository.deleteFile(File(path))
            loadFiles()
        }
    }

    fun copySelectedToClipboard() {
        clipboardPaths = _selectedFiles.value.toList()
        clipboardOperation = ClipboardOp.COPY
        _clipboardCount.value = clipboardPaths.size
        _selectedFiles.value = emptySet()
    }

    fun moveSelectedToClipboard() {
        clipboardPaths = _selectedFiles.value.toList()
        clipboardOperation = ClipboardOp.MOVE
        _clipboardCount.value = clipboardPaths.size
        _selectedFiles.value = emptySet()
    }

    fun paste() {
        if (clipboardPaths.isEmpty() || clipboardOperation == ClipboardOp.NONE) return
        viewModelScope.launch {
            val destDir = File(_currentPath.value)
            clipboardPaths.forEach { path ->
                val source = File(path)
                when (clipboardOperation) {
                    ClipboardOp.COPY -> storageRepository.copyFile(source, destDir)
                    ClipboardOp.MOVE -> storageRepository.moveFile(source, destDir)
                    ClipboardOp.NONE -> {}
                }
            }
            clipboardPaths = emptyList()
            clipboardOperation = ClipboardOp.NONE
            _clipboardCount.value = 0
            loadFiles()
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            storageRepository.createFolder(_currentPath.value, name)
            loadFiles()
        }
    }

    fun renameFile(path: String, newName: String) {
        viewModelScope.launch {
            storageRepository.renameFile(File(path), newName)
            loadFiles()
        }
    }

    fun shareFile(fileItem: FileItem) {
        try {
            val file = File(fileItem.path)
            val uri = FileProvider.getUriForFile(
                application, "${application.packageName}.provider", file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = fileItem.mimeType ?: "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            application.startActivity(Intent.createChooser(intent, "Share ${fileItem.name}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) { }
    }

    fun openFile(fileItem: FileItem) {
        try {
            val file = File(fileItem.path)
            val uri = FileProvider.getUriForFile(
                application, "${application.packageName}.provider", file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, fileItem.mimeType ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            application.startActivity(intent)
        } catch (_: Exception) { }
    }

    fun installApk(fileItem: FileItem) {
        try {
            val file = File(fileItem.path)
            val uri = FileProvider.getUriForFile(
                application, "${application.packageName}.provider", file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            application.startActivity(intent)
        } catch (_: Exception) { }
    }

    private fun sortFiles(files: List<FileItem>): List<FileItem> {
        val dirs = files.filter { it.isDirectory }
        val nonDirs = files.filter { !it.isDirectory }
        val sortedDirs = dirs.sortedBy { it.name.lowercase() }
        val sortedFiles = when (_sortMode.value) {
            SortMode.NAME -> nonDirs.sortedBy { it.name.lowercase() }
            SortMode.SIZE -> nonDirs.sortedByDescending { it.size }
            SortMode.DATE -> nonDirs.sortedByDescending { it.lastModified }
            SortMode.TYPE -> nonDirs.sortedBy { it.extension }
        }
        return sortedDirs + sortedFiles
    }
}
