package com.facultytimetable.pro.presentation.faculty.leave

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.entity.FacultyLeaveEntity
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppFAB
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.EmptyState
import com.facultytimetable.pro.presentation.common.components.LoadingState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FacultyLeaveScreen(
    navController: NavController,
    viewModel: FacultyLeaveViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = "Leave records",
                subtitle = state.faculty?.name ?: "Faculty",
                onBackClick = { navController.popBackStack() }
            )

            if (state.isLoading) {
                LoadingState()
            } else if (state.leaves.isEmpty()) {
                EmptyState(
                    title = "No leave records",
                    message = "Record planned absences so substitute coverage can be arranged.",
                    actionLabel = "Add leave",
                    onAction = { showDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.leaves, key = { it.id }) { leave ->
                        LeaveCard(
                            leave = leave,
                            onToggle = { viewModel.toggleApproval(leave) },
                            onDelete = { viewModel.deleteLeave(leave) }
                        )
                    }
                }
            }
        }

        AppFAB(onClick = { showDialog = true }, icon = Icons.Default.Add, contentDescription = "Add leave")
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showDialog) {
        AddLeaveDialog(
            isSaving = state.isSaving,
            onDismiss = { showDialog = false },
            onSave = { date, start, end, reason, approved ->
                viewModel.addLeave(date, start, end, reason, approved)
                showDialog = false
            }
        )
    }
}

@Composable
private fun LeaveCard(
    leave: FacultyLeaveEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (leave.isApproved) Icons.Default.CheckCircle else Icons.Default.PendingActions,
                contentDescription = null,
                tint = if (leave.isApproved) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    formatDate(leave.leaveDate),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                val times = listOf(leave.startTime, leave.endTime)
                    .filter { it.isNotBlank() }
                    .joinToString("–")
                Text(
                    if (times.isBlank()) "Full day" else times,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (leave.reason.isNotBlank()) {
                    Text(
                        leave.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    if (leave.isApproved) "Approved" else "Pending approval",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (leave.isApproved) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (leave.isApproved) Icons.Default.PendingActions else Icons.Default.CheckCircle,
                    contentDescription = if (leave.isApproved) "Mark pending" else "Approve",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AddLeaveDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Boolean) -> Unit
) {
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var approved by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.EventBusy, contentDescription = null) },
        title = { Text("Add leave record") },
        text = {
            Column {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it.take(10) },
                    label = { Text("Date (YYYY-MM-DD) *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it.take(5) },
                        label = { Text("From") },
                        placeholder = { Text("09:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it.take(5) },
                        label = { Text("To") },
                        placeholder = { Text("17:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = approved, onCheckedChange = { approved = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Already approved", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(date, startTime, endTime, reason, approved) }, enabled = !isSaving) {
                Text(if (isSaving) "Saving…" else "Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatDate(time: Long): String =
    SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(time))
