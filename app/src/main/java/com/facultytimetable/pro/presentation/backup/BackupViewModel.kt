package com.facultytimetable.pro.presentation.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.backup.BackupManager
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
    val error: String? = null
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

    val state: StateFlow<BackupUiState> = combine(
        backupManager.history(),
        busy,
        message,
        error
    ) { history, isBusy, msg, err ->
        BackupUiState(history = history, isBusy = isBusy, message = msg, error = err)
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

    fun restoreBackup(uri: Uri) {
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
