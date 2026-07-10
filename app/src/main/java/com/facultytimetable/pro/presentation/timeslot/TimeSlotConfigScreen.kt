package com.facultytimetable.pro.presentation.timeslot

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.entity.SlotType
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppFAB
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.ColorChip
import com.facultytimetable.pro.presentation.common.components.ConfirmDialog
import com.facultytimetable.pro.presentation.common.components.DropdownSelector
import com.facultytimetable.pro.presentation.common.components.EmptyState

private val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSlotConfigScreen(
    navController: NavController,
    viewModel: TimeSlotConfigViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<TimeSlotEntity?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Time Slots",
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            AppFAB(onClick = { viewModel.showAddDialog() })
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DaySelectorRow(
                selectedDay = state.selectedDay,
                timeSlots = state.timeSlots,
                onDaySelected = viewModel::selectDay
            )

            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 32.dp)
                )
            } else {
                val daySlots = state.timeSlots[state.selectedDay].orEmpty()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dayLabels.getOrElse(state.selectedDay - 1) { "Day ${state.selectedDay}" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(onClick = { viewModel.showGenerateDialog() }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Defaults")
                    }
                }

                if (daySlots.isEmpty()) {
                    EmptyState(
                        title = "No Slots",
                        message = "Generate defaults or tap + to add a period",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(daySlots, key = { it.id }) { slot ->
                            SlotCard(
                                slot = slot,
                                onEdit = { viewModel.showEditDialog(slot) },
                                onDelete = { showDeleteDialog = slot }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    showDeleteDialog?.let { slot ->
        ConfirmDialog(
            title = "Delete Slot",
            message = "Delete ${slot.periodName.ifBlank { "Period ${slot.periodNumber}" }}?",
            confirmText = "Delete",
            onConfirm = {
                viewModel.deleteSlot(slot)
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null },
            isDestructive = true
        )
    }

    if (state.showAddDialog) {
        SlotFormDialog(
            editingSlot = state.editingSlot,
            dayOfWeek = state.selectedDay,
            existingPeriodNumbers = (state.timeSlots[state.selectedDay].orEmpty()).map { it.periodNumber },
            onSave = { day, periodNumber, periodName, start, end, type ->
                viewModel.saveSlot(day, periodNumber, periodName, start, end, type)
            },
            onDismiss = { viewModel.dismissDialog() }
        )
    }

    if (state.showGenerateDialog) {
        GenerateDefaultsDialog(
            onGenerate = { count -> viewModel.generateDefaults(state.selectedDay, count) },
            onDismiss = { viewModel.dismissDialog() }
        )
    }

    state.error?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun DaySelectorRow(
    selectedDay: Int,
    timeSlots: Map<Int, List<TimeSlotEntity>>,
    onDaySelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (1..6).forEach { day ->
            val count = timeSlots[day].orEmpty().size
            FilterChip(
                selected = selectedDay == day,
                onClick = { onDaySelected(day) },
                label = {
                    Text("${dayLabels[day - 1]} ($count)")
                }
            )
        }
    }
}

@Composable
private fun SlotCard(
    slot: TimeSlotEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = slot.periodNumber.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(32.dp)
                    )
                    Column {
                        Text(
                            text = slot.periodName.ifBlank { "Period ${slot.periodNumber}" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
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
                                text = "${slot.startTime} - ${slot.endTime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            TypeBadge(type = slot.type)
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun TypeBadge(type: SlotType) {
    val (label, color) = when (type) {
        SlotType.REGULAR -> "Regular" to MaterialTheme.colorScheme.primary
        SlotType.LUNCH -> "Lunch" to MaterialTheme.colorScheme.tertiary
        SlotType.BREAK -> "Break" to MaterialTheme.colorScheme.secondary
    }
    ColorChip(label = label, color = color)
}

@Composable
private fun SlotFormDialog(
    editingSlot: TimeSlotEntity?,
    dayOfWeek: Int,
    existingPeriodNumbers: List<Int>,
    onSave: (dayOfWeek: Int, periodNumber: Int, periodName: String, startTime: String, endTime: String, type: SlotType) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditing = editingSlot != null
    var periodNumber by remember {
        mutableIntStateOf(editingSlot?.periodNumber ?: ((existingPeriodNumbers.maxOrNull() ?: 0) + 1))
    }
    var periodName by remember { mutableStateOf(editingSlot?.periodName ?: "") }
    var startTime by remember { mutableStateOf(editingSlot?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(editingSlot?.endTime ?: "09:55") }
    var type by remember { mutableStateOf(editingSlot?.type ?: SlotType.REGULAR) }

    val isValid = startTime.isNotBlank() && endTime.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (isEditing) Icons.Default.Edit else Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(if (isEditing) "Edit Period" else "Add Period") },
        text = {
            Column {
                OutlinedTextField(
                    value = periodNumber.toString(),
                    onValueChange = { it.toIntOrNull()?.let { v -> periodNumber = v } },
                    label = { Text("Period Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = periodName,
                    onValueChange = { periodName = it },
                    label = { Text("Period Name") },
                    placeholder = { Text("e.g., Period 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start") },
                        placeholder = { Text("HH:mm") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End") },
                        placeholder = { Text("HH:mm") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                DropdownSelector(
                    label = "Type",
                    selectedItem = type,
                    items = SlotType.entries,
                    itemLabel = { t ->
                        when (t) {
                            SlotType.REGULAR -> "Regular"
                            SlotType.LUNCH -> "Lunch"
                            SlotType.BREAK -> "Break"
                        }
                    },
                    onItemSelected = { type = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(dayOfWeek, periodNumber, periodName.trim(), startTime.trim(), endTime.trim(), type)
                },
                enabled = isValid
            ) {
                Text(if (isEditing) "Update" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun GenerateDefaultsDialog(
    onGenerate: (periodCount: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCount by remember { mutableIntStateOf(7) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = { Text("Generate Default Periods") },
        text = {
            Column {
                Text(
                    text = "Auto-generate periods with standard timings. Existing slots for this day will be replaced.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Number of Periods",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(6, 7, 8).forEach { count ->
                        FilterChip(
                            selected = selectedCount == count,
                            onClick = { selectedCount = count },
                            label = { Text("$count") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onGenerate(selectedCount) }) {
                Text("Generate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
