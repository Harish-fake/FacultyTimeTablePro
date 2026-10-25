package com.facultytimetable.pro.presentation.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppScaffold
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.GradientBanner
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.common.components.ProgressRow
import com.facultytimetable.pro.presentation.common.components.SectionHeader

private enum class ReportTab(val label: String) {
    WORKLOAD("Workload"),
    ROOMS("Rooms"),
    SECTIONS("Sections"),
    DEPARTMENTS("Departments")
}

@Composable
fun ReportsScreen(
    navController: NavController,
    showBack: Boolean = true,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableStateOf(ReportTab.WORKLOAD) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) viewModel.exportCsv(uri)
    }

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
                title = "Reports",
                subtitle = "Workload, utilisation and coverage",
                onBackClick = if (showBack) ({ navController.popBackStack() }) else null,
                actions = {
                    IconButton(onClick = { exportLauncher.launch(ReportsViewModel.suggestedFileName()) }) {
                        Icon(Icons.Default.Download, contentDescription = "Export CSV")
                    }
                }
            )
        },
        snackbarHostState = snackbarHostState
    ) { padding ->
        if (state.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    GradientBanner(
                        title = "${state.totalPeriods} scheduled periods",
                        subtitle = "${state.facultyCount} faculty · ${state.roomCount} rooms · ${state.sectionCount} sections",
                        icon = Icons.Default.Analytics,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReportTab.entries.forEach { entry ->
                            FilterChip(
                                selected = tab == entry,
                                onClick = { tab = entry },
                                label = { Text(entry.label) }
                            )
                        }
                    }
                }

                when (tab) {
                    ReportTab.WORKLOAD -> {
                        if (state.workload.isEmpty()) {
                            item { EmptyReport("No faculty yet", "Add faculty members to see workload.") }
                        } else {
                            item { SectionHeader(title = "TEACHING LOAD") }
                            items(state.workload, key = { it.facultyId }) { row ->
                                AppCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.People,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    row.name,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    row.department,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (row.overloaded) {
                                                Icon(
                                                    Icons.Default.Warning,
                                                    contentDescription = "Over workload",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        ProgressRow(
                                            label = "Assigned periods",
                                            valueLabel = "${row.assigned} / ${row.maxHours}",
                                            progress = row.percent,
                                            color = if (row.overloaded) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ReportTab.ROOMS -> {
                        item { SectionHeader(title = "ROOM UTILISATION") }
                        items(state.rooms, key = { it.roomId }) { row ->
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.MeetingRoom,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                row.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                row.type,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    ProgressRow(
                                        label = "Scheduled periods",
                                        valueLabel = "${row.used} / ${row.available}",
                                        progress = row.percent,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }

                    ReportTab.SECTIONS -> {
                        item { SectionHeader(title = "SECTION COVERAGE") }
                        items(state.sections, key = { it.sectionId }) { row ->
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.School,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            row.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    ProgressRow(
                                        label = "Covered periods",
                                        valueLabel = "${row.covered} / ${row.available}",
                                        progress = row.percent,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    ReportTab.DEPARTMENTS -> {
                        item { SectionHeader(title = "DEPARTMENT SUMMARY") }
                        items(state.departments, key = { it.code }) { row ->
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                row.code,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            row.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Metric("Sections", row.sections.toString())
                                        Metric("Faculty", row.faculty.toString())
                                        Metric("Periods", row.periods.toString())
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyReport(title: String, message: String) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
