package com.facultytimetable.pro.presentation.setup

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.navigation.Routes

data class SetupStep(
    val id: Int,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: (Long?) -> String,
    val isOptional: Boolean = false
)

val setupSteps = listOf(
    SetupStep(1, "Departments", "Create departments with HOD and details", Icons.Default.Business, { Routes.DEPARTMENT_LIST }),
    SetupStep(2, "Academic Years", "Add academic years", Icons.Default.DateRange, { Routes.ACADEMIC_YEAR_LIST }),
    SetupStep(3, "Semesters", "Configure semesters for each year", Icons.Default.CalendarMonth, { Routes.SEMESTER_LIST }),
    SetupStep(4, "Sections", "Create sections with student strength", Icons.Default.ViewModule, { Routes.SECTION_LIST }),
    SetupStep(5, "Working Days", "Enable/disable working days", Icons.Default.CalendarViewWeek, { Routes.WORKING_DAY }),
    SetupStep(6, "Time Slots", "Define periods, breaks, and lunch", Icons.Default.Schedule, { Routes.TIME_SLOT_CONFIG }),
    SetupStep(7, "Rooms", "Create classrooms and lecture halls", Icons.Default.MeetingRoom, { Routes.ROOM_LIST }),
    SetupStep(8, "Labs", "Create laboratory rooms with equipment", Icons.Default.Science, { Routes.LAB_LIST }),
    SetupStep(9, "Faculty", "Add faculty members with details", Icons.Default.People, { Routes.FACULTY_LIST }),
    SetupStep(10, "Subjects", "Create subjects with codes and credits", Icons.Default.Book, { Routes.SUBJECT_LIST }),
    SetupStep(11, "Assignments", "Assign faculty to subjects", Icons.Default.Groups, { Routes.FACULTY_ASSIGNMENT }),
    SetupStep(12, "Timetable", "Auto-generate or manually create", Icons.Default.EventNote, { Routes.TIMETABLE_GENERATOR })
)

@Composable
fun SetupWizardScreen(
    navController: NavController,
    viewModel: SetupWizardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkProgress()
    }

    LaunchedEffect(state.setupComplete) {
        if (state.setupComplete) {
            navController.popBackStack()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Setup Wizard",
            onBackClick = { navController.popBackStack() }
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Text(
                text = "Complete all steps to set up your timetable system",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { state.completionProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${state.completedSteps}/${setupSteps.size} steps completed",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(setupSteps) { index, step ->
                    val stepState = state.stepStates.getOrNull(index)
                    SetupStepCard(
                        step = step,
                        stepState = stepState,
                        isCurrent = step.id == state.currentStepId,
                        onClick = {
                            if (stepState?.isUnlocked == true) {
                                val route = step.route(null)
                                if (route.isNotEmpty()) navController.navigate(route)
                            }
                        }
                    )
                }
                item {
                    if (state.setupComplete) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text("Setup Complete - Go to Dashboard")
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SetupStepCard(
    step: SetupStep,
    stepState: StepState?,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val isCompleted = stepState?.isCompleted == true
    val isUnlocked = stepState?.isUnlocked == true
    val count = stepState?.count ?: 0

    val containerColor = when {
        isCompleted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        isCurrent -> MaterialTheme.colorScheme.secondaryContainer
        isUnlocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    Card(
        onClick = onClick,
        enabled = isUnlocked,
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(
                    when {
                        isCompleted -> MaterialTheme.colorScheme.primary
                        isCurrent -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    }
                ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        step.icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isCurrent) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isCurrent || isCompleted) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isCompleted) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("$count", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
            } else if (!isUnlocked) {
                Icon(Icons.Default.RadioButtonUnchecked, contentDescription = "Locked", tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }
            if (isUnlocked) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
