package com.facultytimetable.pro.presentation.timetable.grid

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.EmptyState
import com.facultytimetable.pro.presentation.common.components.InfoRow
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.common.subjectColor
import com.facultytimetable.pro.presentation.navigation.Routes
import com.facultytimetable.pro.presentation.timetable.common.DaySelector
import com.facultytimetable.pro.presentation.timetable.common.PeriodRow
import com.facultytimetable.pro.presentation.timetable.common.dayShortNames
import java.util.Calendar

@Composable
fun TimetableGridScreen(
    navController: NavController,
    initialSectionId: Long? = null,
    showBack: Boolean = false,
    viewModel: TimetableGridViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedDay by rememberSaveable { mutableStateOf(todayIsoDay()) }
    var detailEntry by remember { mutableStateOf<TimetableEntryEntity?>(null) }

    LaunchedEffect(initialSectionId, state.isLoading) {
        if (!state.isLoading && initialSectionId != null) {
            viewModel.selectSectionById(initialSectionId)
        }
    }

    LaunchedEffect(state.days) {
        if (state.days.isNotEmpty() && selectedDay !in state.days) {
            selectedDay = state.days.first()
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = "Timetable",
                subtitle = state.selectedSection?.name ?: "Weekly schedule",
                onBackClick = if (showBack) ({ navController.popBackStack() }) else null,
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.TIMETABLE_GENERATOR) }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Generate timetable")
                    }
                }
            )

            when {
                state.isLoading -> LoadingState()

                state.sections.isEmpty() -> EmptyState(
                    title = "No sections yet",
                    message = "Add a section, subjects, faculty and rooms, then generate a timetable.",
                    actionLabel = "Add section",
                    onAction = { navController.navigate(Routes.sectionForm()) }
                )

                else -> {
                    SectionChips(
                        state = state,
                        onSelect = viewModel::selectSection,
                        onAddSection = { navController.navigate(Routes.sectionForm()) }
                    )

                    SummaryStrip(state)

                    DaySelector(
                        selectedDay = selectedDay,
                        onDaySelected = { selectedDay = it },
                        days = state.days.ifEmpty { listOf(1, 2, 3, 4, 5) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val daySlots = state.timeSlots
                        .filter { it.dayOfWeek == selectedDay }
                        .sortedBy { it.periodNumber }

                    if (daySlots.isEmpty()) {
                        EmptyState(
                            title = "No periods configured",
                            message = "Add time slots for ${dayShortNames.getOrElse(selectedDay - 1) { "this day" }}.",
                            actionLabel = "Configure time slots",
                            onAction = { navController.navigate(Routes.TIME_SLOT_CONFIG) }
                        )
                    } else if (state.entries.isEmpty()) {
                        EmptyState(
                            title = "Timetable is empty",
                            message = "Generate the weekly timetable to fill ${state.selectedSection?.name ?: "this section"}.",
                            actionLabel = "Generate timetable",
                            onAction = { navController.navigate(Routes.TIMETABLE_GENERATOR) }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp)
                        ) {
                            items(daySlots, key = { it.id }) { slot ->
                                val entry = state.entries.firstOrNull { it.timeSlotId == slot.id }
                                PeriodRow(
                                    slot = slot,
                                    entry = entry,
                                    subject = entry?.let { state.subjects[it.subjectId] },
                                    faculty = entry?.let { state.facultyMap[it.facultyId] },
                                    room = entry?.let { state.rooms[it.roomId] },
                                    onClick = entry?.let { { detailEntry = entry } }
                                )
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    detailEntry?.let { entry ->
        EntryDetailDialog(
            entry = entry,
            state = state,
            onDismiss = { detailEntry = null },
            onDelete = {
                viewModel.deleteEntry(entry)
                detailEntry = null
            }
        )
    }
}

@Composable
private fun SectionChips(
    state: TimetableGridState,
    onSelect: (com.facultytimetable.pro.data.local.db.entity.SectionEntity) -> Unit,
    onAddSection: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        state.sections.forEach { section ->
            FilterChip(
                selected = state.selectedSection?.id == section.id,
                onClick = { onSelect(section) },
                label = { Text(section.name) }
            )
        }
        OutlinedButton(
            onClick = onAddSection,
            shape = MaterialTheme.shapes.small,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Section", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SummaryStrip(state: TimetableGridState) {
    val regularPerWeek = state.timeSlots.count { it.type.name == "REGULAR" }
    val free = (regularPerWeek - state.weeklyPeriods).coerceAtLeast(0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryPill("${state.weeklyPeriods}", "periods", Modifier.weight(1f))
        SummaryPill("${state.labPeriods}", "labs", Modifier.weight(1f))
        SummaryPill("$free", "free", Modifier.weight(1f))
    }
}

@Composable
private fun SummaryPill(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EntryDetailDialog(
    entry: TimetableEntryEntity,
    state: TimetableGridState,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val subject = state.subjects[entry.subjectId]
    val faculty = state.facultyMap[entry.facultyId]
    val room = state.rooms[entry.roomId]
    val slot = state.timeSlots.firstOrNull { it.id == entry.timeSlotId }
    val color = subjectColor(subject?.type?.name ?: "THEORY")

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = color) },
        title = { Text(subject?.name ?: "Class period") },
        text = {
            Column {
                InfoRow("Code", subject?.code.orEmpty())
                InfoRow("Type", subject?.type?.name ?: "—")
                InfoRow("Faculty", faculty?.name.orEmpty())
                InfoRow("Room", room?.name.orEmpty())
                InfoRow("Day", dayShortNames.getOrElse(entry.dayOfWeek - 1) { "Day ${entry.dayOfWeek}" })
                InfoRow(
                    "Time",
                    slot?.let { "P${it.periodNumber} · ${it.startTime}–${it.endTime}" } ?: "—"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "This period belongs to ${state.selectedSection?.name ?: "the selected section"}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Remove period", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

private fun todayIsoDay(): Int {
    val value = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    return if (value == Calendar.SUNDAY) 7 else value - 1
}
