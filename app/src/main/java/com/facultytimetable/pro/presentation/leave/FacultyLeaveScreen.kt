package com.facultytimetable.pro.presentation.leave

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.entity.FacultyLeaveEntity
import com.facultytimetable.pro.presentation.common.components.ActionButton
import com.facultytimetable.pro.presentation.common.components.AppBottomSheet
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppFAB
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.ColorChip
import com.facultytimetable.pro.presentation.common.components.ConfirmDialog
import com.facultytimetable.pro.presentation.common.components.DropdownSelector
import com.facultytimetable.pro.presentation.common.components.EmptyState
import com.facultytimetable.pro.presentation.common.components.LoadingState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyLeaveScreen(
    navController: NavController,
    viewModel: FacultyLeaveViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<LeaveWithFaculty?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Leave Records",
            onBackClick = { navController.popBackStack() }
        )

        FacultyFilterBar(
            faculties = state.faculties,
            selectedFacultyId = state.selectedFacultyId,
            onFacultySelected = viewModel::onFacultyFilterChanged
        )

        if (state.isLoading) {
            LoadingState()
        } else if (state.leaves.isEmpty()) {
            EmptyState(
                title = "No Leave Records",
                message = "Tap + to add a leave record"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(state.leaves, key = { it.leave.id }) { leaveWithFaculty ->
                    LeaveCard(
                        leaveWithFaculty = leaveWithFaculty,
                        onDelete = { showDeleteDialog = leaveWithFaculty }
                    )
                }
            }
        }

        AppFAB(onClick = { showBottomSheet = true })
    }

    showDeleteDialog?.let { leaveWithFaculty ->
        ConfirmDialog(
            title = "Delete Leave",
            message = "Are you sure you want to delete this leave record for ${leaveWithFaculty.facultyName}?",
            confirmText = "Delete",
            onConfirm = {
                viewModel.deleteLeave(leaveWithFaculty.leave)
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null },
            isDestructive = true
        )
    }

    if (showBottomSheet) {
        LeaveFormBottomSheet(
            faculties = state.faculties,
            preSelectedFacultyId = state.selectedFacultyId,
            onDismiss = { showBottomSheet = false },
            onSave = { leave ->
                viewModel.insertLeave(leave)
                showBottomSheet = false
            }
        )
    }
}

@Composable
private fun FacultyFilterBar(
    faculties: List<com.facultytimetable.pro.data.local.db.entity.FacultyEntity>,
    selectedFacultyId: Long?,
    onFacultySelected: (Long?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFacultyId == null,
            onClick = { onFacultySelected(null) },
            label = { Text("All") }
        )
        faculties.forEach { faculty ->
            FilterChip(
                selected = selectedFacultyId == faculty.id,
                onClick = { onFacultySelected(faculty.id) },
                label = { Text(faculty.name) }
            )
        }
    }
}

@Composable
private fun LeaveCard(
    leaveWithFaculty: LeaveWithFaculty,
    onDelete: () -> Unit
) {
    val leave = leaveWithFaculty.leave
    val dateText = remember(leave.leaveDate) {
        Instant.ofEpochMilli(leave.leaveDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    }

    AppCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = leaveWithFaculty.facultyName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${leave.startTime} - ${leave.endTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (leave.reason.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = leave.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                ColorChip(
                    label = if (leave.isApproved) "Approved" else "Pending",
                    color = if (leave.isApproved) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeaveFormBottomSheet(
    faculties: List<com.facultytimetable.pro.data.local.db.entity.FacultyEntity>,
    preSelectedFacultyId: Long?,
    onDismiss: () -> Unit,
    onSave: (FacultyLeaveEntity) -> Unit
) {
    var selectedFaculty by remember {
        mutableStateOf(preSelectedFacultyId?.let { id -> faculties.find { it.id == id } })
    }
    var leaveDate by remember { mutableStateOf<Long?>(null) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var isApproved by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var timeError by remember { mutableStateOf(false) }

    val dateText = leaveDate?.let {
        Instant.ofEpochMilli(it)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    }

    AppBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Add Leave",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))

            DropdownSelector(
                label = "Faculty",
                selectedItem = selectedFaculty,
                items = faculties,
                itemLabel = { it.name },
                onItemSelected = { selectedFaculty = it }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Leave Date",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = dateText ?: "Select Date")
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = startTime,
                onValueChange = { startTime = it; timeError = false },
                label = { Text("Start Time") },
                placeholder = { Text("HH:mm") },
                modifier = Modifier.fillMaxWidth(),
                isError = timeError && startTime.isNotBlank(),
                supportingText = if (timeError && startTime.isNotBlank()) {
                    { Text("Use HH:mm format") }
                } else null,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = endTime,
                onValueChange = { endTime = it; timeError = false },
                label = { Text("End Time") },
                placeholder = { Text("HH:mm") },
                modifier = Modifier.fillMaxWidth(),
                isError = timeError && endTime.isNotBlank(),
                supportingText = if (timeError && endTime.isNotBlank()) {
                    { Text("Use HH:mm format") }
                } else null,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Approved", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = isApproved, onCheckedChange = { isApproved = it })
            }
            Spacer(modifier = Modifier.height(24.dp))

            ActionButton(
                text = "Save",
                onClick = {
                    if (selectedFaculty == null || leaveDate == null ||
                        startTime.isBlank() || endTime.isBlank()
                    ) return@ActionButton
                    val timePattern = Regex("""^([01]\d|2[0-3]):[0-5]\d$""")
                    if (!timePattern.matches(startTime.trim()) || !timePattern.matches(endTime.trim())) {
                        timeError = true
                        return@ActionButton
                    }
                    onSave(
                        FacultyLeaveEntity(
                            facultyId = selectedFaculty!!.id,
                            leaveDate = leaveDate!!,
                            startTime = startTime.trim(),
                            endTime = endTime.trim(),
                            reason = reason.trim(),
                            isApproved = isApproved
                        )
                    )
                },
                enabled = selectedFaculty != null && leaveDate != null &&
                    startTime.isNotBlank() && endTime.isNotBlank()
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = leaveDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { leaveDate = it }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
