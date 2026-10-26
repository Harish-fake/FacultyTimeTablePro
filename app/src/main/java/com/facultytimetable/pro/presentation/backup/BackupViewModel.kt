package com.facultytimetable.pro.presentation.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.backup.BackupManager
import com.facultytimetable.pro.data.local.backup.BackupPreview
import com.facultytimetable.pro.data.local.db.entity.BackupHistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val history: List<BackupHistoryEntity> = emptyList(),
    val isBusy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val preview: BackupPreview? = null
) {
    val lastBackup: BackupHistoryEntity? get() = history.firstOrNull()
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager
) : ViewModel() {

    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)
    private val preview = MutableStateFlow<BackupPreview?>(null)
    private var pendingRestoreUri: Uri? = null

    val state: StateFlow<BackupUiState> = combine(
        backupManager.history(),
        busy,
        message,
        error,
        preview
    ) { history, isBusy, msg, err, inspected ->
        BackupUiState(history = history, isBusy = isBusy, message = msg, error = err, preview = inspected)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackupUiState())

    fun consumeMessage() { message.value = null }
    fun consumeError() { error.value = null }

    fun createBackup(uri: Uri) {
        viewModelScope.launch {
            busy.value = true
            val result = backupManager.createBackup(uri)
            busy.value = false
            if (result.success) message.value = result.message else error.value = result.message
        }
    }

    /** Reads and verifies the chosen file before anything is replaced. */
    fun inspectBackup(uri: Uri) {
        viewModelScope.launch {
            busy.value = true
            val result = backupManager.inspectBackup(uri)
            busy.value = false
            if (!result.valid) {
                pendingRestoreUri = null
                preview.value = null
                error.value = result.message
            } else {
                pendingRestoreUri = uri
                preview.value = result
            }
        }
    }

    fun cancelRestore() {
        pendingRestoreUri = null
        preview.value = null
    }

    fun confirmRestore() {
        val uri = pendingRestoreUri ?: return
        pendingRestoreUri = null
        preview.value = null
        restoreBackup(uri)
    }

    private fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            busy.value = true
            val result = backupManager.restoreBackup(uri)
            busy.value = false
            if (result.success) message.value = result.message else error.value = result.message
        }
    }

    fun deleteHistory(item: BackupHistoryEntity) {
        viewModelScope.launch {
            try {
                backupManager.deleteHistoryItem(item.id)
                message.value = "Backup history entry removed"
            } catch (e: Exception) {
                error.value = e.message ?: "Could not remove entry"
            }
        }
    }
}
