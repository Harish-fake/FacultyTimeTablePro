package com.facultytimetable.pro.presentation.timetable.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.SlotType
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectType
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.EmptyState
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.data.model.ConflictReport
import com.facultytimetable.pro.presentation.navigation.Routes
import com.facultytimetable.pro.presentation.theme.SubjectBreak
import com.facultytimetable.pro.presentation.theme.SubjectLab
import com.facultytimetable.pro.presentation.theme.SubjectLibrary
import com.facultytimetable.pro.presentation.theme.SubjectLunch
import com.facultytimetable.pro.presentation.theme.SubjectProject
import com.facultytimetable.pro.presentation.theme.SubjectSeminar
import com.facultytimetable.pro.presentation.theme.SubjectSports
import com.facultytimetable.pro.presentation.theme.SubjectTheory

private val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableGridScreen(
    navController: NavController,
    viewModel: TimetableGridViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AppTopBar(
                title = "Timetable",
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Auto Generate") },
                                onClick = {
                                    showMenu = false
                                    navController.navigate(Routes.TIMETABLE_GENERATOR)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Department View") },
                                onClick = { showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Room View") },
                                onClick = { showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Print") },
                                onClick = { showMenu = false },
                                leadingIcon = {
                                    Icon(Icons.Default.Print, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            )

            if (state.isLoading) {
                LoadingState()
            } else if (state.sections.isEmpty()) {
                EmptyState(
                    title = "No Data",
                    message = "Add sections, faculty, subjects, and rooms to create a timetable."
                )
            } else {
                SectionSelector(
                    sections = state.sections,
                    selectedSection = state.selectedSection,
                    onSectionSelected = viewModel::onSectionSelected
                )

                if (state.selectedSection != null && state.entries.isEmpty()) {
                    EmptyState(
                        title = "Empty Timetable",
                        message = "${state.selectedSection?.name} has no entries. " +
                                "Click on a cell to add an entry or use Auto Generate."
                    )
                } else if (state.selectedSection != null) {
                    WeeklyGrid(
                        timeSlots = state.timeSlots,
                        entries = state.entries,
                        subjects = state.subjects,
                        facultyMap = state.facultyMap,
                        onCellClick = { day, timeSlot ->
                            val existing = state.entries.find {
                                it.dayOfWeek == day && it.timeSlotId == timeSlot.id
                            }
                            if (existing != null) {
                                viewModel.showEditSheet(existing)
                            } else {
                                viewModel.showAddSheet(day, timeSlot)
                            }
                        }
                    )
                }
            }
        }
    }

    if (state.showEditSheet) {
        TimetableEditSheet(
            isNew = state.editingEntry?.id == 0L,
            entry = state.editingEntry,
            selectedTimeSlot = state.selectedTimeSlot,
            subjects = state.availableSubjects,
            faculty = state.availableFaculty,
            rooms = state.availableRooms,
            validationErrors = state.validationErrors,
            onDismiss = viewModel::dismissSheet,
            onSubjectChange = viewModel::updateEditingSubject,
            onFacultyChange = viewModel::updateEditingFaculty,
            onRoomChange = viewModel::updateEditingRoom,
            onSave = {
                val entry = state.editingEntry
                if (entry != null && entry.id == 0L) {
                    viewModel.addEntry()
                } else {
                    viewModel.updateEntry()
                }
            },
            onDelete = {
                state.editingEntry?.let { viewModel.deleteEntry(it) }
            }
        )
    }
}

@Composable
private fun SectionSelector(
    sections: List<SectionEntity>,
    selectedSection: SectionEntity?,
    onSectionSelected: (SectionEntity) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sections.forEach { section ->
            val isSelected = section.id == selectedSection?.id
            Card(
                onClick = { onSectionSelected(section) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isSelected) 4.dp else 0.dp
                )
            ) {
                Text(
                    text = section.name,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeeklyGrid(
    timeSlots: List<TimeSlotEntity>,
    entries: List<TimetableEntryEntity>,
    subjects: Map<Long, SubjectEntity>,
    facultyMap: Map<Long, FacultyEntity>,
    onCellClick: (Int, TimeSlotEntity) -> Unit
) {
    val groupedByDay = timeSlots.groupBy { it.dayOfWeek }
    val days = groupedByDay.keys.sorted().take(5)
    val maxPeriods = groupedByDay.values.maxOfOrNull { it.size } ?: 8

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                Spacer(modifier = Modifier.width(54.dp))
                days.forEach { day ->
                    Text(
                        text = dayLabels.getOrElse(day - 1) { "D$day" },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        items((1..maxPeriods).toList()) { periodNumber ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            ) {
                Column(
                    modifier = Modifier.width(54.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "P$periodNumber",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }

                days.forEach { day ->
                    val daySlots = groupedByDay[day] ?: emptyList()
                    val timeSlot = daySlots.getOrNull(periodNumber - 1)

                    if (timeSlot != null) {
                        when (timeSlot.type) {
                            SlotType.LUNCH -> {
                                GridCell(
                                    label = "Lunch",
                                    color = SubjectLunch,
                                    isSpecial = true
                                )
                            }
                            SlotType.BREAK -> {
                                GridCell(
                                    label = "Break",
                                    color = SubjectBreak,
                                    isSpecial = true
                                )
                            }
                            else -> {
                                val entry = entries.find {
                                    it.dayOfWeek == day && it.timeSlotId == timeSlot.id
                                }
                                if (entry != null) {
                                    val resolved = subjects[entry.subjectId]
                                    val faculty = facultyMap[entry.facultyId]
                                    val subjType = resolved?.type ?: SubjectType.THEORY
                                    val color = when (subjType) {
                                        SubjectType.LAB -> SubjectLab
                                        SubjectType.PROJECT -> SubjectProject
                                        SubjectType.SEMINAR -> SubjectSeminar
                                        SubjectType.LIBRARY -> SubjectLibrary
                                        SubjectType.SPORTS -> SubjectSports
                                        else -> SubjectTheory
                                    }
                                    val initials = faculty?.let {
                                        it.name.split(" ").filter { it.isNotBlank() }
                                            .take(2).joinToString("") { w ->
                                                w.first().uppercase()
                                            }
                                    } ?: "?"

                                    GridCell(
                                        label = resolved?.code ?: "SUBJ",
                                        subtitle = initials,
                                        color = color,
                                        onClick = { onCellClick(day, timeSlot) }
                                    )
                                } else {
                                    GridCell(
                                        label = "",
                                        color = Color.Transparent,
                                        isEmpty = true,
                                        onClick = { onCellClick(day, timeSlot) }
                                    )
                                }
                            }
                        }
                    } else {
                        GridCell(
                            label = "",
                            color = Color.Transparent,
                            isEmpty = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.GridCell(
    label: String,
    color: Color,
    subtitle: String = "",
    isSpecial: Boolean = false,
    isEmpty: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(2.dp)
            .height(68.dp)
            .clip(RoundedCornerShape(6.dp))
            .then(
                if (isSpecial) Modifier.background(color.copy(alpha = 0.2f))
                else if (!isEmpty) Modifier.background(color.copy(alpha = 0.15f))
                else Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            )
            .then(
                if (!isEmpty && !isSpecial)
                    Modifier.border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                else Modifier
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isEmpty) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    fontWeight = FontWeight.Medium,
                    color = color,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                        color = color.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimetableEditSheet(
    isNew: Boolean,
    entry: TimetableEntryEntity?,
    selectedTimeSlot: TimeSlotEntity?,
    subjects: List<SubjectEntity>,
    faculty: List<FacultyEntity>,
    rooms: List<RoomEntity>,
    validationErrors: List<ConflictReport>,
    onDismiss: () -> Unit,
    onSubjectChange: (Long) -> Unit,
    onFacultyChange: (Long) -> Unit,
    onRoomChange: (Long) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentEntry = entry ?: return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (isNew) "Add Timetable Entry" else "Edit Timetable Entry",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            val selectedSubject = subjects.find { it.id == currentEntry.subjectId }
            val selectedFaculty = faculty.find { it.id == currentEntry.facultyId }
            val selectedRoom = rooms.find { it.id == currentEntry.roomId }

            DropdownSelector(
                label = "Subject",
                value = selectedSubject?.let { "${it.code} - ${it.name}" } ?: "",
                items = subjects,
                selectedItem = selectedSubject,
                itemLabel = { "${it.code} - ${it.name}" },
                onItemSelected = { onSubjectChange(it.id) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            DropdownSelector(
                label = "Faculty",
                value = selectedFaculty?.name ?: "",
                items = faculty,
                selectedItem = selectedFaculty,
                itemLabel = { it.name },
                onItemSelected = { onFacultyChange(it.id) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            DropdownSelector(
                label = "Room",
                value = selectedRoom?.name ?: "",
                items = rooms,
                selectedItem = selectedRoom,
                itemLabel = { it.name },
                onItemSelected = { onRoomChange(it.id) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            val dayName = dayLabels.getOrElse(currentEntry.dayOfWeek - 1) {
                "Day ${currentEntry.dayOfWeek}"
            }
            val timeText = selectedTimeSlot?.let {
                "P${it.periodNumber} (${it.startTime} - ${it.endTime})"
            } ?: ""

            OutlinedTextField(
                value = "$dayName \u2022 $timeText",
                onValueChange = {},
                readOnly = true,
                label = { Text("Day & Time") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                enabled = false
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (validationErrors.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Conflicts detected:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    validationErrors.forEach { conflict ->
                        Text(
                            text = "\u2022 ${conflict.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isNew) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    enabled = currentEntry.subjectId != 0L &&
                            currentEntry.facultyId != 0L &&
                            currentEntry.roomId != 0L
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isNew) "Add" else "Save")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun <T> DropdownSelector(
    label: String,
    value: String,
    items: List<T>,
    selectedItem: T?,
    itemLabel: (T) -> String,
    onItemSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Select $label") },
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Select")
                    }
                },
                shape = MaterialTheme.shapes.medium
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                if (items.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No items available") },
                        onClick = { expanded = false }
                    )
                } else {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(itemLabel(item)) },
                            onClick = {
                                onItemSelected(item)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
