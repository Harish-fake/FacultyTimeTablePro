package com.facultytimetable.pro.presentation.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.datastore.AppPreferences
import com.facultytimetable.pro.data.local.db.AppDatabase
import com.facultytimetable.pro.data.local.db.dao.BackupHistoryDao
import com.facultytimetable.pro.data.local.db.entity.BackupHistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class BackupUiState(
    val backupHistory: List<BackupHistoryEntity> = emptyList(),
    val isCreatingBackup: Boolean = false,
    val isRestoring: Boolean = false,
    val lastBackupTime: Long = 0L,
    val backupReminderInterval: Int = 7,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupHistoryDao: BackupHistoryDao,
    private val appPreferences: AppPreferences,
    private val database: AppDatabase
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                backupHistoryDao.getAllBackups(),
                appPreferences.lastBackupReminder,
                appPreferences.backupReminderInterval
            ) { history, lastTime, interval ->
                _state.update {
                    it.copy(
                        backupHistory = history,
                        lastBackupTime = lastTime,
                        backupReminderInterval = interval
                    )
                }
            }.collect()
        }
    }

    fun createBackup(notes: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isCreatingBackup = true, error = null) }
            try {
                val sourceDb = context.getDatabasePath(AppDatabase.DATABASE_NAME)
                if (!sourceDb.exists()) {
                    _state.update { it.copy(isCreatingBackup = false, error = "Database file not found") }
                    return@launch
                }

                val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "backup_$timestamp.db"
                val backupFile = File(backupDir, fileName)

                sourceDb.inputStream().use { input ->
                    backupFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val entity = BackupHistoryEntity(
                    fileName = fileName,
                    filePath = backupFile.absolutePath,
                    fileSize = backupFile.length(),
                    notes = notes,
                    isRestorePoint = false
                )
                backupHistoryDao.insert(entity)
                appPreferences.setLastBackupReminder(System.currentTimeMillis())

                _state.update { it.copy(isCreatingBackup = false, message = "Backup created successfully") }
            } catch (e: Exception) {
                _state.update { it.copy(isCreatingBackup = false, error = "Failed to create backup: ${e.message}") }
            }
        }
    }

    fun restoreBackup(backup: BackupHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isRestoring = true, error = null) }
            try {
                val backupFile = File(backup.filePath)
                if (!backupFile.exists()) {
                    _state.update { it.copy(isRestoring = false, error = "Backup file not found") }
                    return@launch
                }

                database.close()

                val destDb = context.getDatabasePath(AppDatabase.DATABASE_NAME)
                destDb.parentFile?.mkdirs()

                backupFile.inputStream().use { input ->
                    destDb.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                _state.update { it.copy(isRestoring = false, message = "Backup restored. Please restart the app for changes to take effect.") }
            } catch (e: Exception) {
                _state.update { it.copy(isRestoring = false, error = "Failed to restore: ${e.message}") }
            }
        }
    }

    fun deleteBackup(backup: BackupHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                File(backup.filePath).delete()
                backupHistoryDao.deleteById(backup.id)
                _state.update { it.copy(message = "Backup deleted") }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to delete: ${e.message}") }
            }
        }
    }

    fun exportDatabase(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sourceDb = context.getDatabasePath(AppDatabase.DATABASE_NAME)
                if (!sourceDb.exists()) {
                    _state.update { it.copy(error = "Database file not found") }
                    return@launch
                }
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    sourceDb.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                _state.update { it.copy(message = "Database exported successfully") }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Export failed: ${e.message}") }
            }
        }
    }

    fun importDatabase(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(error = null) }
            try {
                val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
                val fileName = "imported_${System.currentTimeMillis()}.db"
                val importFile = File(backupDir, fileName)

                context.contentResolver.openInputStream(uri)?.use { input ->
                    importFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val entity = BackupHistoryEntity(
                    fileName = fileName,
                    filePath = importFile.absolutePath,
                    fileSize = importFile.length(),
                    notes = "Imported from external source"
                )
                backupHistoryDao.insert(entity)

                _state.update { it.copy(message = "Database imported successfully") }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Import failed: ${e.message}") }
            }
        }
    }

    fun setBackupReminderInterval(days: Int) {
        viewModelScope.launch {
            appPreferences.setBackupReminderInterval(days)
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
