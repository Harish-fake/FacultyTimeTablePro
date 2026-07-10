package com.facultytimetable.pro.presentation.timetable.faculty

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.facultytimetable.pro.data.local.db.entity.SlotType
import com.facultytimetable.pro.data.local.db.entity.SubjectType
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.EmptyState
import com.facultytimetable.pro.presentation.common.components.LoadingState
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
fun FacultyTimetableScreen(
    facultyId: Long,
    navController: NavController,
    viewModel: FacultyTimetableViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(facultyId) { viewModel.loadFaculty(facultyId) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = state.faculty?.name ?: "Faculty Timetable",
            onBackClick = { navController.popBackStack() }
        )

        if (state.isLoading) {
            LoadingState()
        } else if (state.faculty == null) {
            EmptyState(title = "Not Found", message = "Faculty member not found")
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FacultyViewMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.viewMode == mode,
                        onClick = { viewModel.setViewMode(mode) },
                        label = { Text(mode.name.replace("_", " ")) },
                        leadingIcon = {
                            Icon(
                                when (mode) {
                                    FacultyViewMode.TODAY -> Icons.Default.Today
                                    FacultyViewMode.WEEK -> Icons.Default.ViewWeek
                                    else -> Icons.Default.CalendarMonth
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            if (state.entries.isEmpty()) {
                EmptyState(
                    title = "No Classes",
                    message = "${state.faculty?.name} has no scheduled classes."
                )
            } else {
                FacultyWeeklyGrid(
                    timeSlots = state.timeSlots,
                    entries = state.entries,
                    subjects = state.subjects,
                    sections = state.sections,
                    rooms = state.rooms
                )
            }
        }
    }
}

@Composable
private fun FacultyWeeklyGrid(
    timeSlots: List<TimeSlotEntity>,
    entries: List<TimetableEntryEntity>,
    subjects: Map<Long, com.facultytimetable.pro.data.local.db.entity.SubjectEntity>,
    sections: Map<Long, com.facultytimetable.pro.data.local.db.entity.SectionEntity>,
    rooms: Map<Long, com.facultytimetable.pro.data.local.db.entity.RoomEntity>
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
                                FacultyGridCell(
                                    label = "Lunch",
                                    color = SubjectLunch,
                                    isSpecial = true
                                )
                            }
                            SlotType.BREAK -> {
                                FacultyGridCell(
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
                                    val subject = subjects[entry.subjectId]
                                    val section = sections[entry.sectionId]
                                    val room = rooms[entry.roomId]
                                    val type = subject?.type ?: SubjectType.THEORY
                                    val color = when (type) {
                                        SubjectType.LAB -> SubjectLab
                                        SubjectType.PROJECT -> SubjectProject
                                        SubjectType.SEMINAR -> SubjectSeminar
                                        SubjectType.LIBRARY -> SubjectLibrary
                                        SubjectType.SPORTS -> SubjectSports
                                        else -> SubjectTheory
                                    }

                                    FacultyGridCell(
                                        label = subject?.code ?: "SUBJ",
                                        subtitle = section?.name ?: "",
                                        color = color,
                                        roomName = room?.name ?: ""
                                    )
                                } else {
                                    FacultyGridCell(
                                        label = "",
                                        color = Color.Transparent,
                                        isEmpty = true
                                    )
                                }
                            }
                        }
                    } else {
                        FacultyGridCell(
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.FacultyGridCell(
    label: String,
    color: Color,
    subtitle: String = "",
    roomName: String = "",
    isSpecial: Boolean = false,
    isEmpty: Boolean = false
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
                    maxLines = 1,
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
                if (roomName.isNotBlank()) {
                    Text(
                        text = roomName,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                        color = color.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
