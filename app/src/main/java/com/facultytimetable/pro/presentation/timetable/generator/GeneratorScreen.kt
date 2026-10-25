package com.facultytimetable.pro.presentation.timetable.generator

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.presentation.common.components.ActionButton
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.GradientBanner
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.common.components.SectionHeader
import com.facultytimetable.pro.presentation.common.components.StatusRow
import com.facultytimetable.pro.presentation.navigation.Routes

@Composable
fun GeneratorScreen(
    navController: NavController,
    viewModel: GeneratorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Auto generator",
            subtitle = "Constraint-aware weekly scheduling",
            onBackClick = { navController.popBackStack() }
        )

        if (state.isLoading) {
            LoadingState()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                GradientBanner(
                    title = "Build the timetable",
                    subtitle = "Labs first · deterministic · validated",
                    icon = Icons.Default.AutoAwesome,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                AppCard {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        SectionHeader("Readiness")
                        StatusRow(
                            title = "Sections",
                            subtitle = "${state.sections.size} active",
                            isOk = state.sections.isNotEmpty(),
                            onClick = { navController.navigate(Routes.SECTION_LIST) }
                        )
                        StatusRow(
                            title = "Subjects",
                            subtitle = "${state.subjects.size} active · ${state.requiredPeriods} weekly periods",
                            isOk = state.subjects.isNotEmpty(),
                            onClick = { navController.navigate(Routes.SUBJECT_LIST) }
                        )
                        StatusRow(
                            title = "Faculty",
                            subtitle = "${state.faculty.size} active",
                            isOk = state.faculty.isNotEmpty(),
                            onClick = { navController.navigate(Routes.FACULTY_LIST) }
                        )
                        StatusRow(
                            title = "Rooms & labs",
                            subtitle = "${state.rooms.size} rooms · ${state.labRooms} labs",
                            isOk = state.rooms.isNotEmpty(),
                            onClick = { navController.navigate(Routes.ROOM_LIST) }
                        )
                        StatusRow(
                            title = "Teaching periods",
                            subtitle = "${state.regularPeriodsPerWeek} regular periods per week",
                            isOk = state.regularPeriodsPerWeek > 0,
                            onClick = { navController.navigate(Routes.TIME_SLOT_CONFIG) }
                        )
                    }
                }

                if (state.dataIssues.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Fix these issues before generating",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            state.dataIssues.forEach { issue ->
                                Text(
                                    "• $issue",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AppCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader("What will be scheduled")
                        Text(
                            "${state.previewAssignments.size} subject allocations · ${state.requiredPeriods} periods",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Labs are placed first as consecutive blocks in laboratory rooms. " +
                                "Faculty, room and section clashes are rejected, and every result is " +
                                "re-validated before it is saved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MiniFact("${state.sections.size}", "sections", Modifier.weight(1f))
                            MiniFact("${state.labRooms}", "labs", Modifier.weight(1f))
                            MiniFact("${state.regularPeriodsPerWeek}", "slots", Modifier.weight(1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (state.isGenerating) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                "Generating timetable…",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = {
                                    if (state.progress.totalSteps > 0)
                                        state.progress.currentStep.toFloat() / state.progress.totalSteps
                                    else 0f
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                state.progress.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                } else {
                    ActionButton(
                        text = if (state.saved) "Generate again" else "Generate timetable",
                        onClick = viewModel::generate,
                        enabled = state.canGenerate
                    )
                    if (!state.canGenerate) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Add the missing data above to enable generation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(visible = state.result != null) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (state.isSuccess)
                                    MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (state.isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (state.isSuccess)
                                            MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        if (state.isSuccess) "Timetable ready" else "Saved with warnings",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.isSuccess)
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    state.result.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (state.isSuccess)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer
                                )

                                if (state.saved) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        MiniFact("${state.savedPeriods}", "periods", Modifier.weight(1f))
                                        MiniFact("${state.savedSections}", "sections", Modifier.weight(1f))
                                        MiniFact("${state.savedLabs}", "lab periods", Modifier.weight(1f))
                                    }
                                }

                                if (state.conflicts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        "${state.conflicts.size} validation warning(s)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    state.conflicts.take(12).forEach { conflict ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.Top) {
                                            Icon(
                                                Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    conflict.message,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    conflict.suggestion,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    if (state.conflicts.size > 12) {
                                        Text(
                                            "…and ${state.conflicts.size - 12} more",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                if (state.saved) {
                                    ActionButton(
                                        text = "View timetable",
                                        onClick = {
                                            navController.navigate(Routes.TIMETABLE_GRID) {
                                                popUpTo(Routes.TIMETABLE_GENERATOR) { inclusive = true }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            state.error!!,
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun MiniFact(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
