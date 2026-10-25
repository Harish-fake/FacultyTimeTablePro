package com.facultytimetable.pro.presentation.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.backup.BackupManager
import com.facultytimetable.pro.data.local.db.entity.BackupHistoryEntity
import com.facultytimetable.pro.presentation.common.components.ActionButton
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppScaffold
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.ConfirmDialog
import com.facultytimetable.pro.presentation.common.components.EmptyState
import com.facultytimetable.pro.presentation.common.components.GradientBanner
import com.facultytimetable.pro.presentation.common.components.InfoRow
import com.facultytimetable.pro.presentation.common.components.SectionHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(
    navController: NavController,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    var deleteTarget by remember { mutableStateOf<BackupHistoryEntity?>(null) }

    val createLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(BackupManager.mimeType)
    ) { uri ->
        if (uri != null) viewModel.createBackup(uri)
    }
    val openLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingRestore = uri
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    AppScaffold(
        topBar = {
            AppTopBar(
                title = "Backup & Restore",
                subtitle = "Portable JSON snapshots",
                onBackClick = { navController.popBackStack() }
            )
        },
        snackbarHostState = snackbarHostState
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GradientBanner(
                    title = "${state.history.size} backup${if (state.history.size == 1) "" else "s"}",
                    subtitle = state.lastBackup?.let {
                        "Last saved ${formatDateTime(it.createdAt)}"
                    } ?: "Create your first snapshot",
                    icon = Icons.Default.CloudDone,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    Icons.Default.Backup,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(9.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Create a backup",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "All departments, faculty, rooms, sections and timetable entries.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        if (state.isBusy) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Working…", style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            ActionButton(
                                text = "Save backup file",
                                onClick = { createLauncher.launch(BackupManager.suggestedFileName()) }
                            )
                        }
                    }
                }
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Restore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Restore a snapshot",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Restoring replaces the current data with the contents of the selected JSON backup.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "This action cannot be undone.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        ActionButton(
                            text = "Choose backup file",
                            onClick = { openLauncher.launch(arrayOf("application/json", "text/*")) },
                            enabled = !state.isBusy
                        )
                    }
                }
            }

            item {
                SectionHeader(title = "BACKUP HISTORY")
            }

            if (state.history.isEmpty()) {
                item {
                    EmptyState(
                        title = "No backups yet",
                        message = "Your saved backup files will appear here.",
                        modifier = Modifier.height(220.dp)
                    )
                }
            } else {
                items(state.history, key = { it.id }) { item ->
                    HistoryCard(
                        item = item,
                        onDelete = { deleteTarget = item }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(28.dp)) }
        }
    }

    pendingRestore?.let { uri ->
        ConfirmDialog(
            title = "Restore this backup?",
            message = "Every current department, faculty member, room, section and timetable entry will be replaced by the backup contents.",
            confirmText = "Restore",
            isDestructive = true,
            onConfirm = {
                viewModel.restoreBackup(uri)
                pendingRestore = null
            },
            onDismiss = { pendingRestore = null }
        )
    }

    deleteTarget?.let { item ->
        ConfirmDialog(
            title = "Remove history entry",
            message = "Remove ${item.fileName} from the list? The saved file itself is not deleted.",
            confirmText = "Remove",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteHistory(item)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun HistoryCard(
    item: BackupHistoryEntity,
    onDelete: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${formatDateTime(item.createdAt)} · ${formatBytes(item.fileSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    item.notes.ifBlank { "Snapshot" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove from history",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis))

private fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> "—"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}
