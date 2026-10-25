package com.facultytimetable.pro.presentation.academic

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.entity.SlotType
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppFAB
import com.facultytimetable.pro.presentation.common.components.AppScaffold
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.ConfirmDialog
import com.facultytimetable.pro.presentation.common.components.DropdownSelector
import com.facultytimetable.pro.presentation.common.components.EmptyState
import com.facultytimetable.pro.presentation.common.components.GradientBanner
import com.facultytimetable.pro.presentation.common.components.InfoRow
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.common.components.SectionHeader

@Composable
fun TimeSlotConfigScreen(
    navController: NavController,
    viewModel: TimeSlotConfigViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TimeSlotEntity?>(null) }
    var deleting by remember { mutableStateOf<TimeSlotEntity?>(null) }

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
                title = "Time Slots",
                subtitle = "Periods, lunch and breaks",
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            AppFAB(onClick = {
                editing = null
                showDialog = true
            })
        },
        snackbarHostState = snackbarHostState
    ) { padding ->
        if (state.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                GradientBanner(
                    title = "${state.regularCount} teaching periods",
                    subtitle = "Across ${state.activeDays} active days",
                    icon = Icons.Default.Schedule,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow("Configured periods", state.allSlots.size.toString())
                        InfoRow("Active periods", state.allSlots.count { it.isActive }.toString())
                        InfoRow("Lunch / break periods", state.allSlots.count { it.type != SlotType.REGULAR }.toString())
                    }
                }

                SectionHeader(title = "DAY")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((1..5).toList()) { day ->
                        FilterChip(
                            selected = state.selectedDay == day,
                            onClick = { viewModel.selectDay(day) },
                            label = { Text(dayShortLabel(day)) }
                        )
                    }
                }

                SectionHeader(title = (dayLabels[state.selectedDay] ?: "Day").uppercase() + " SCHEDULE")

                if (state.slotsForDay.isEmpty()) {
                    EmptyState(
                        title = "No periods",
                        message = "Add the first period for this day.",
                        modifier = Modifier.height(260.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.slotsForDay.forEach { slot ->
                            SlotCard(
                                slot = slot,
                                onToggle = { active -> viewModel.toggleActive(slot, active) },
                                onEdit = {
                                    editing = slot
                                    showDialog = true
                                },
                                onDelete = { deleting = slot }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    if (showDialog) {
        val nextPeriod = (state.slotsForDay.maxOfOrNull { it.periodNumber } ?: 0) + 1
        SlotDialog(
            existing = editing,
            day = state.selectedDay,
            nextPeriod = nextPeriod,
            onDismiss = { showDialog = false },
            onSave = { period, start, end, type, active ->
                viewModel.saveSlot(editing, state.selectedDay, period, start, end, type, active)
                showDialog = false
            }
        )
    }

    deleting?.let { slot ->
        ConfirmDialog(
            title = "Delete period",
            message = "Delete period ${slot.periodNumber} (${slot.startTime}-${slot.endTime})? Timetable entries in this period will also be removed.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteSlot(slot)
                deleting = null
            },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun SlotCard(
    slot: TimeSlotEntity,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = slotTypeColor(slot.type)
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (slot.type == SlotType.REGULAR) Icons.Default.Schedule else Icons.Default.FreeBreakfast,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "P${slot.periodNumber}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${slot.startTime} - ${slot.endTime}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    slotTypeLabel(slot.type),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = slot.isActive,
                onCheckedChange = onToggle
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit period", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete period", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun slotTypeLabel(type: SlotType): String = when (type) {
    SlotType.REGULAR -> "Teaching period"
    SlotType.LUNCH -> "Lunch break"
    SlotType.BREAK -> "Short break"
}

private fun slotTypeColor(type: SlotType): Color = when (type) {
    SlotType.REGULAR -> Color(0xFF1565C0)
    SlotType.LUNCH -> Color(0xFFEF6C00)
    SlotType.BREAK -> Color(0xFF6A1B9A)
}

@Composable
private fun SlotDialog(
    existing: TimeSlotEntity?,
    day: Int,
    nextPeriod: Int,
    onDismiss: () -> Unit,
    onSave: (String, String, String, SlotType, Boolean) -> Unit
) {
    var period by remember(existing) { mutableStateOf((existing?.periodNumber ?: nextPeriod).toString()) }
    var start by remember(existing) { mutableStateOf(existing?.startTime ?: "") }
    var end by remember(existing) { mutableStateOf(existing?.endTime ?: "") }
    var type by remember(existing) { mutableStateOf(existing?.type ?: SlotType.REGULAR) }
    var active by remember(existing) { mutableStateOf(existing?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add period" else "Edit period") },
        text = {
            Column {
                Text(
                    dayLabels[day] ?: "Day",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = period,
                    onValueChange = { period = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Period number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("Start") },
                        placeholder = { Text("08:30") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("End") },
                        placeholder = { Text("09:25") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                DropdownSelector(
                    label = "Slot type",
                    selectedItem = type,
                    items = SlotType.entries,
                    itemLabel = { slotTypeLabel(it) },
                    onItemSelected = { type = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = active, onCheckedChange = { active = it })
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Available to the generator", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(period, start.trim(), end.trim(), type, active) }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
