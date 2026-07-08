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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.SlotType
import com.facultytimetable.pro.data.local.db.entity.SubjectType
import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.EmptyState
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.navigation.Routes
import com.facultytimetable.pro.presentation.theme.SubjectBreak
import com.facultytimetable.pro.presentation.theme.SubjectLab
import com.facultytimetable.pro.presentation.theme.SubjectLunch
import com.facultytimetable.pro.presentation.theme.SubjectProject
import com.facultytimetable.pro.presentation.theme.SubjectSeminar
import com.facultytimetable.pro.presentation.theme.SubjectSports
import com.facultytimetable.pro.presentation.theme.SubjectLibrary
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

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Timetable",
            actions = {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Auto Generate") }, onClick = {
                            showMenu = false
                            navController.navigate(Routes.TIMETABLE_GENERATOR)
                        }, leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) })
                        DropdownMenuItem(text = { Text("Department View") }, onClick = { showMenu = false })
                        DropdownMenuItem(text = { Text("Room View") }, onClick = { showMenu = false })
                    }
                }
            }
        )

        if (state.isLoading) {
            LoadingState()
        } else if (state.sections.isEmpty()) {
            EmptyState(title = "No Data", message = "Add sections, faculty, subjects, and rooms to create a timetable. Go to Settings or use the generator.")
        } else {
            SectionSelector(
                sections = state.sections,
                selectedSection = state.selectedSection,
                onSectionSelected = viewModel::selectSection
            )

            if (state.entries.isEmpty()) {
                EmptyState(
                    title = "Empty Timetable",
                    message = state.selectedSection?.let {
                        "No entries for ${it.name}. Use Auto Generate to create one."
                    } ?: "Select a section to view"
                )
            } else {
                WeeklyGrid(
                    state = state
                )
            }
        }
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
                )
            ) {
                Text(
                    text = section.name,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeeklyGrid(
    state: TimetableGridState
) {
    val groupedByDay = state.timeSlots.groupBy { it.dayOfWeek }
    val days = groupedByDay.keys.sorted().take(5)
    val maxPeriods = groupedByDay.values.maxOfOrNull { it.size } ?: 8

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.width(48.dp))
                days.forEach { day ->
                    Text(
                        text = dayLabels.getOrElse(day - 1) { "Day$day" },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        items((1..maxPeriods).toList()) { period ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            ) {
                Text(
                    text = "P$period",
                    modifier = Modifier
                        .width(48.dp)
                        .padding(end = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )

                days.forEach { day ->
                    val entry = state.entries.find {
                        val daySlots = groupedByDay[day] ?: emptyList()
                        val slot = daySlots.getOrNull(period - 1)
                        it.dayOfWeek == day && slot != null && it.timeSlotId == slot.id
                    }
                    val timeSlot = groupedByDay[day]?.getOrNull(period - 1)

                    if (timeSlot?.type == SlotType.LUNCH) {
                        GridCell(
                            label = "Lunch",
                            color = SubjectLunch,
                            isSpecial = true
                        )
                    } else if (timeSlot?.type == SlotType.BREAK) {
                        GridCell(
                            label = "Break",
                            color = SubjectBreak,
                            isSpecial = true
                        )
                    } else if (entry != null) {
                        val resolved = state.subjects[entry.subjectId]
                        val faculty = state.facultyMap[entry.facultyId]
                        val subjType = resolved?.type ?: SubjectType.THEORY
                        val color = when (subjType) {
                            SubjectType.LAB -> SubjectLab
                            SubjectType.PROJECT -> SubjectProject
                            SubjectType.SEMINAR -> SubjectSeminar
                            SubjectType.LIBRARY -> SubjectLibrary
                            SubjectType.SPORTS -> SubjectSports
                            else -> SubjectTheory
                        }
                        GridCell(
                            label = resolved?.name ?: "Subject",
                            subtitle = faculty?.name ?: "",
                            color = color,
                            onClick = { }
                        )
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
                if (!isEmpty && !isSpecial) Modifier.border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
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
