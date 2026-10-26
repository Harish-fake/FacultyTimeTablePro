package com.facultytimetable.pro.presentation.sampledata

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.seed.SectionCoverage
import com.facultytimetable.pro.presentation.common.components.ActionButton
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppScaffold
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.ConfirmDialog
import com.facultytimetable.pro.presentation.common.components.GradientBanner
import com.facultytimetable.pro.presentation.common.components.SectionHeader
import com.facultytimetable.pro.presentation.common.components.StatsCard

@Composable
fun SampleDataScreen(
    navController: NavController,
    viewModel: SampleDataViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showConfirm by remember { mutableStateOf(false) }

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
                title = "Sample data",
                subtitle = "Verified demo college · v${com.facultytimetable.pro.BuildConfig.VERSION_NAME}",
                onBackClick = { navController.popBackStack() }
            )
        },
        snackbarHostState = snackbarHostState
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GradientBanner(
                    title = "Verified sample college",
                    subtitle = "${state.expectedPeriods} weekly periods · ${state.sections} sections · conflict-free by design",
                    icon = Icons.Default.AutoAwesome,
                    actionLabel = if (state.isRunning) "Working…" else "Load & generate",
                    onAction = { if (!state.isRunning) showConfirm = true },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (state.isRunning) {
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    "Building the sample timetable",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Seeding the college, assigning faculty and validating every constraint…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            state.result?.let { result ->
                item {
                    ResultCard(
                        isComplete = result.isComplete,
                        periods = result.scheduledPeriods,
                        expected = result.expectedPeriods,
                        warnings = result.warnings
                    )
                }
            }

            item { SectionHeader("INCLUDED DATASET") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Stat(Icons.Default.School, "Departments", state.departments, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    Stat(Icons.Default.People, "Faculty", state.faculty, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Stat(Icons.Default.Book, "Subjects", state.subjects, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                    Stat(Icons.Default.MeetingRoom, "Rooms", state.rooms, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Stat(Icons.Default.Group, "Sections", state.sections, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    Stat(Icons.Default.Schedule, "Time slots", state.timeSlots, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                }
            }

            item { SectionHeader("EXPECTED COVERAGE") }
            item {
                CoverageCard(
                    coverage = state.result?.coverage ?: state.sectionPreviews.map {
                        SectionCoverage(it.name, it.expectedPeriods, 0)
                    },
                    previewOnly = state.result == null
                )
            }

            item { SectionHeader("HOW IT WORKS") }
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Step(1, "Seed the college", "Three departments, six faculty, ten subjects, eight rooms, four sections and a five-day timetable grid.")
                        Step(2, "Assign and schedule", "Theory is spread across the week; labs get consecutive periods in real lab rooms.")
                        Step(3, "Validate and save", "Every hard constraint is re-checked independently. The exact period count is compared before the timetable is stored.")
                    }
                }
            }

            item {
                ActionButton(
                    text = if (state.isRunning) "Generating…" else "Load sample data & generate timetable",
                    onClick = { if (!state.isRunning) showConfirm = true },
                    enabled = !state.isRunning,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text(
                    "This replaces every department, faculty member, subject, room, section, time slot and timetable entry. Theme and PIN settings are kept.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showConfirm) {
        ConfirmDialog(
            title = "Replace data with the sample college?",
            message = "All current timetable data will be replaced by the verified sample dataset, and a new timetable will be generated immediately. This cannot be undone.",
            confirmText = "Load & generate",
            isDestructive = true,
            onConfirm = {
                showConfirm = false
                viewModel.loadSampleData()
            },
            onDismiss = { showConfirm = false }
        )
    }
}

@Composable
private fun Stat(
    icon: ImageVector,
    title: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    StatsCard(
        title = title,
        value = value.toString(),
        icon = { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp)) },
        color = color,
        modifier = modifier
    )
}

@Composable
private fun ResultCard(isComplete: Boolean, periods: Int, expected: Int, warnings: List<String>) {
    val tint = if (isComplete) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = tint.copy(alpha = 0.14f), shape = CircleShape, modifier = Modifier.size(44.dp)) {
                    Icon(
                        if (isComplete) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isComplete) "Timetable verified" else "Timetable needs attention",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "$periods of $expected required periods scheduled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (warnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                warnings.take(6).forEach { warning ->
                    Text(
                        "• $warning",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
                if (warnings.size > 6) {
                    Text(
                        "+ ${warnings.size - 6} more warning(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Text(
                    "No clashes, no missing periods, no invalid labs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun CoverageCard(coverage: List<SectionCoverage>, previewOnly: Boolean) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            coverage.forEachIndexed { index, section ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (previewOnly || section.isComplete) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (previewOnly || section.isComplete) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        section.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (previewOnly) "${section.expectedPeriods} periods"
                        else "${section.scheduledPeriods}/${section.expectedPeriods} periods",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (index != coverage.lastIndex) {
                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
            if (previewOnly) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Expected quotas are computed from the same dataset the app ships with.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Step(number: Int, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    number.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
