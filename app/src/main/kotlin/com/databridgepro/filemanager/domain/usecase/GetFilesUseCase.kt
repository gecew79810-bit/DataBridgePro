package com.databridgepro.filemanager.domain.usecase

import android.net.Uri
import com.databridgepro.filemanager.data.model.FileItem
import com.databridgepro.filemanager.data.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFilesUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    operator fun invoke(treeUri: Uri): Flow<List<FileItem>> =
        fileRepository.getFiles(treeUri)
}
