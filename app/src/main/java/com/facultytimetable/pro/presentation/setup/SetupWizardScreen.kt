package com.facultytimetable.pro.presentation.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    val route: String,
    val isOptional: Boolean = false
)

val setupSteps = listOf(
    SetupStep(1, "Create College", "Set up your college name and details", Icons.Default.Lock, Routes.SETTINGS),
    SetupStep(2, "Add Departments", "Create departments with HOD and details", Icons.Default.Lock, Routes.DEPARTMENT_FORM),
    SetupStep(3, "Academic Years", "Add academic years and semesters", Icons.Default.Lock, Routes.ACADEMIC_YEAR_LIST),
    SetupStep(4, "Create Semesters", "Configure semesters for each year", Icons.Default.Lock, Routes.SEMESTER_LIST),
    SetupStep(5, "Add Sections", "Create sections with student strength", Icons.Default.Lock, Routes.SECTION_LIST),
    SetupStep(6, "Configure Working Days", "Enable/disable working days", Icons.Default.Lock, Routes.TIME_SLOT_CONFIG),
    SetupStep(7, "Set Time Slots", "Define periods, breaks, and lunch", Icons.Default.Lock, Routes.TIME_SLOT_CONFIG),
    SetupStep(8, "Add Rooms", "Create classrooms and lecture halls", Icons.Default.Lock, Routes.ROOM_LIST),
    SetupStep(9, "Add Labs", "Create laboratory rooms with equipment", Icons.Default.Lock, Routes.ROOM_LIST),
    SetupStep(10, "Add Faculty", "Add faculty members with details", Icons.Default.Lock, Routes.FACULTY_LIST),
    SetupStep(11, "Add Subjects", "Create subjects with codes and credits", Icons.Default.Lock, Routes.SUBJECT_LIST),
    SetupStep(12, "Assign Faculty", "Assign faculty to subjects", Icons.Default.Lock, Routes.FACULTY_LIST),
    SetupStep(13, "Generate Timetable", "Auto-generate or manually create", Icons.Default.Lock, Routes.TIMETABLE_GENERATOR)
)

@Composable
fun SetupWizardScreen(
    navController: NavController,
    viewModel: SetupWizardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Setup Wizard",
            onBackClick = { navController.popBackStack() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Complete all steps to set up your timetable system",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { state.completionProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${state.completedSteps}/${setupSteps.size} steps completed",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(setupSteps) { index, step ->
                    SetupStepCard(
                        step = step,
                        isCompleted = state.completedStepIds.contains(step.id),
                        isCurrent = step.id == state.currentStepId,
                        onClick = {
                            navController.navigate(step.route)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupStepCard(
    step: SetupStep,
    isCompleted: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when {
        isCompleted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        isCurrent -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> MaterialTheme.colorScheme.primary
                            isCurrent -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = step.id.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) MaterialTheme.colorScheme.onSecondary
                        else MaterialTheme.colorScheme.onSurfaceVariant
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
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
