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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.entity.AcademicYearEntity
import com.facultytimetable.pro.data.local.db.entity.HolidayEntity
import com.facultytimetable.pro.data.local.db.entity.SemesterEntity
import com.facultytimetable.pro.presentation.common.components.ActionChip
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppScaffold
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.ConfirmDialog
import com.facultytimetable.pro.presentation.common.components.EmptyState
import com.facultytimetable.pro.presentation.common.components.GradientBanner
import com.facultytimetable.pro.presentation.common.components.InfoRow
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.common.components.SectionHeader
import java.time.LocalDate

@Composable
fun AcademicCalendarScreen(
    navController: NavController,
    viewModel: AcademicCalendarViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showYearDialog by remember { mutableStateOf(false) }
    var editingYear by remember { mutableStateOf<AcademicYearEntity?>(null) }
    var showSemesterDialog by remember { mutableStateOf(false) }
    var editingSemester by remember { mutableStateOf<SemesterEntity?>(null) }
    var showHolidayDialog by remember { mutableStateOf(false) }
    var editingHoliday by remember { mutableStateOf<HolidayEntity?>(null) }
    var deleteYear by remember { mutableStateOf<AcademicYearEntity?>(null) }
    var deleteSemester by remember { mutableStateOf<SemesterEntity?>(null) }
    var deleteHoliday by remember { mutableStateOf<HolidayEntity?>(null) }

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
                title = "Academic Calendar",
                subtitle = "Years, semesters and holidays",
                onBackClick = { navController.popBackStack() }
            )
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
                    title = state.currentYear?.name ?: "Academic calendar",
                    subtitle = state.activeSemester?.name ?: "No semester active",
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow("Current year", state.currentYear?.name ?: "Not set")
                        InfoRow("Active semester", state.activeSemester?.name ?: "Not set")
                        InfoRow("Holidays configured", state.holidays.size.toString())
                        InfoRow(
                            "Next holiday",
                            state.upcomingHolidays.firstOrNull()?.let {
                                "${it.name} · ${formatReadableDate(it.date)}"
                            } ?: "None upcoming"
                        )
                    }
                }

                SectionHeader(
                    title = "ACADEMIC YEARS",
                    action = {
                        ActionChip(
                            text = "Add",
                            icon = Icons.Default.Add,
                            onClick = {
                                editingYear = null
                                showYearDialog = true
                            }
                        )
                    }
                )

                if (state.years.isEmpty()) {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                "No academic years yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Add a year, then create its semesters and sections.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.years.forEach { year ->
                            YearCard(
                                year = year,
                                onSetCurrent = { viewModel.setCurrentYear(year) },
                                onEdit = {
                                    editingYear = year
                                    showYearDialog = true
                                },
                                onDelete = { deleteYear = year }
                            )
                        }
                    }
                }

                SectionHeader(
                    title = "SEMESTERS",
                    action = {
                        ActionChip(
                            text = "Add",
                            icon = Icons.Default.Add,
                            onClick = {
                                if (state.years.isNotEmpty()) {
                                    editingSemester = null
                                    showSemesterDialog = true
                                }
                            }
                        )
                    }
                )

                if (state.years.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.years, key = { it.id }) { year ->
                            FilterChip(
                                selected = state.selectedYearId == year.id,
                                onClick = { viewModel.selectYear(year.id) },
                                label = { Text(year.name) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                val semesters = state.semestersForSelectedYear
                if (semesters.isEmpty()) {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                "No semesters for this year",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Sections and the timetable generator use the active semester.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        semesters.forEach { semester ->
                            SemesterCard(
                                semester = semester,
                                onActivate = { viewModel.setActiveSemester(semester) },
                                onEdit = {
                                    editingSemester = semester
                                    showSemesterDialog = true
                                },
                                onDelete = { deleteSemester = semester }
                            )
                        }
                    }
                }

                SectionHeader(
                    title = "HOLIDAYS",
                    action = {
                        ActionChip(
                            text = "Add",
                            icon = Icons.Default.Add,
                            onClick = {
                                editingHoliday = null
                                showHolidayDialog = true
                            }
                        )
                    }
                )

                if (state.holidays.isEmpty()) {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "No holidays configured. Teaching days run Monday to Friday.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.holidays.sortedBy { it.date }.forEach { holiday ->
                            HolidayCard(
                                holiday = holiday,
                                onEdit = {
                                    editingHoliday = holiday
                                    showHolidayDialog = true
                                },
                                onDelete = { deleteHoliday = holiday }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showYearDialog) {
        YearDialog(
            existing = editingYear,
            nextName = suggestYearName(state.years),
            onDismiss = { showYearDialog = false },
            onSave = { name, start, end, current ->
                viewModel.saveYear(editingYear, name, start, end, current)
                showYearDialog = false
            }
        )
    }

    if (showSemesterDialog) {
        val yearId = state.selectedYearId ?: state.currentYear?.id ?: return
        val nextNumber = (state.semestersForSelectedYear.maxOfOrNull { it.semesterNumber } ?: 0) + 1
        SemesterDialog(
            existing = editingSemester,
            yearId = yearId,
            nextNumber = nextNumber,
            onDismiss = { showSemesterDialog = false },
            onSave = { name, number, active ->
                viewModel.saveSemester(editingSemester, name, yearId, number, active)
                showSemesterDialog = false
            }
        )
    }

    if (showHolidayDialog) {
        HolidayDialog(
            existing = editingHoliday,
            onDismiss = { showHolidayDialog = false },
            onSave = { name, date, recurring ->
                viewModel.saveHoliday(editingHoliday, name, date, recurring)
                showHolidayDialog = false
            }
        )
    }

    deleteYear?.let { year ->
        ConfirmDialog(
            title = "Delete academic year",
            message = "Delete ${year.name}? Semesters and sections in this year must be removed first.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteYear(year)
                deleteYear = null
            },
            onDismiss = { deleteYear = null }
        )
    }

    deleteSemester?.let { semester ->
        ConfirmDialog(
            title = "Delete semester",
            message = "Delete ${semester.name}? Sections in this semester must be removed first.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteSemester(semester)
                deleteSemester = null
            },
            onDismiss = { deleteSemester = null }
        )
    }

    deleteHoliday?.let { holiday ->
        ConfirmDialog(
            title = "Remove holiday",
            message = "Remove ${holiday.name} from the calendar?",
            confirmText = "Remove",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteHoliday(holiday)
                deleteHoliday = null
            },
            onDismiss = { deleteHoliday = null }
        )
    }
}

@Composable
private fun YearCard(
    year: AcademicYearEntity,
    onSetCurrent: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    year.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (year.isCurrent) {
                    CurrentBadge()
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "${formatReadableDate(year.startDate)} → ${formatReadableDate(year.endDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!year.isCurrent) {
                    TextButton(onClick = onSetCurrent) { Text("Set current") }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit year", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete year", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun SemesterCard(
    semester: SemesterEntity,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        semester.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (semester.isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CurrentBadge(label = "ACTIVE")
                    }
                }
                Text(
                    "Semester ${semester.semesterNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!semester.isActive) {
                TextButton(onClick = onActivate) { Text("Activate") }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit semester", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete semester", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun HolidayCard(
    holiday: HolidayEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Event,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(holiday.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    formatReadableDate(holiday.date) + if (holiday.isRecurring) " · repeats yearly" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit holiday", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Remove holiday", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun CurrentBadge(label: String = "CURRENT") {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

private fun suggestYearName(years: List<AcademicYearEntity>): String {
    val current = LocalDate.now().year
    val start = if (LocalDate.now().monthValue >= 7) current else current - 1
    val default = "$start-${start + 1}"
    if (years.none { it.name == default }) return default
    var offset = 1
    while (years.any { it.name == "${start + offset}-${start + offset + 1}" }) offset++
    return "${start + offset}-${start + offset + 1}"
}

@Composable
private fun YearDialog(
    existing: AcademicYearEntity?,
    nextName: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit
) {
    var name by remember(existing) { mutableStateOf(existing?.name ?: nextName) }
    var start by remember(existing) {
        mutableStateOf(existing?.let { formatDate(it.startDate) } ?: "${LocalDate.now().year}-07-01")
    }
    var end by remember(existing) {
        mutableStateOf(existing?.let { formatDate(it.endDate) } ?: "${LocalDate.now().year + 1}-06-30")
    }
    var isCurrent by remember(existing) { mutableStateOf(existing?.isCurrent ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add academic year" else "Edit academic year") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("2026-2027") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = { Text("Start date (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text("End date (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isCurrent, onCheckedChange = { isCurrent = it })
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Make this the current year", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, start, end, isCurrent) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SemesterDialog(
    existing: SemesterEntity?,
    yearId: Long,
    nextNumber: Int,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean) -> Unit
) {
    var name by remember(existing, nextNumber) {
        mutableStateOf(
            existing?.name ?: if (nextNumber % 2 == 1) "Odd Semester" else "Even Semester"
        )
    }
    var number by remember(existing, nextNumber) {
        mutableStateOf((existing?.semesterNumber ?: nextNumber).toString())
    }
    var active by remember(existing) { mutableStateOf(existing?.isActive ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add semester" else "Edit semester") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Semester number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = active, onCheckedChange = { active = it })
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Set as active semester", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, number, active) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun HolidayDialog(
    existing: HolidayEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean) -> Unit
) {
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var date by remember(existing) {
        mutableStateOf(existing?.let { formatDate(it.date) } ?: LocalDate.now().toString())
    }
    var recurring by remember(existing) { mutableStateOf(existing?.isRecurring ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add holiday" else "Edit holiday") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Holiday name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = recurring, onCheckedChange = { recurring = it })
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Repeats every year", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, date, recurring) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
