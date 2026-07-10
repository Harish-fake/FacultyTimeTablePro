package com.facultytimetable.pro.presentation.reports

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.data.model.ConflictReport
import com.facultytimetable.pro.data.model.ConflictType
import com.facultytimetable.pro.data.model.FacultyWorkload
import com.facultytimetable.pro.data.model.RoomUtilization
import kotlinx.coroutines.launch
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.ErrorState
import com.facultytimetable.pro.presentation.common.components.LoadingState

enum class ReportType(
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    FACULTY_WORKLOAD("Faculty Workload", "Teaching hours per faculty member", Icons.Default.People),
    DEPARTMENT_SUMMARY("Department Summary", "Overview of all departments", Icons.Default.School),
    SUBJECT_ALLOCATION("Subject Allocation", "Subjects grouped by department", Icons.Default.Book),
    ROOM_UTILIZATION("Room Utilization", "Room usage statistics", Icons.Default.MeetingRoom),
    LAB_UTILIZATION("Lab Utilization", "Lab room usage", Icons.Default.Analytics),
    FREE_PERIODS("Free Periods", "Time slots with no allocations", Icons.Default.CalendarMonth),
    MISSING_HOURS("Missing Hours", "Subjects with insufficient hours", Icons.Default.Warning),
    CONFLICTS("Conflict Report", "Scheduling conflicts", Icons.Default.ErrorOutline)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Reports",
                onBackClick = { navController.popBackStack() },
                actions = {
                    TextButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> LoadingState()
                state.error != null -> ErrorState(
                    message = state.error!!,
                    onRetry = { viewModel.refresh() }
                )
                else -> ReportsContent(state, viewModel::selectReport, snackbarHostState)
            }
        }
    }
}

@Composable
private fun ReportsContent(
    state: ReportsState,
    onSelectReport: (ReportType) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Select a report type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ReportType.entries.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { reportType ->
                            ReportTypeCard(
                                reportType = reportType,
                                isSelected = state.selectedReport == reportType,
                                onClick = { onSelectReport(reportType) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        val selected = state.selectedReport
        if (selected != null) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = selected.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                ExportActions(selected, snackbarHostState)
            }

            item {
                when (selected) {
                    ReportType.FACULTY_WORKLOAD -> FacultyWorkloadReport(state.facultyWorkload)
                    ReportType.DEPARTMENT_SUMMARY -> DepartmentSummaryReport(state.departmentSummaries)
                    ReportType.SUBJECT_ALLOCATION -> SubjectAllocationReport(state.subjectAllocations)
                    ReportType.ROOM_UTILIZATION -> RoomUtilizationReport(state.roomUtilizations, "Room")
                    ReportType.LAB_UTILIZATION -> RoomUtilizationReport(state.labUtilizations, "Lab")
                    ReportType.FREE_PERIODS -> FreePeriodsReport(state.freePeriods)
                    ReportType.MISSING_HOURS -> MissingHoursReport(state.missingHours)
                    ReportType.CONFLICTS -> ConflictsReport(state.conflicts)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ReportTypeCard(
    reportType: ReportType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

    Card(
        onClick = onClick,
        modifier = modifier.animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    reportType.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = reportType.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExportActions(
    reportType: ReportType,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = {
                scope.launch {
                    try {
                        val file = exportReportAsCsv(context, reportType)
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share ${reportType.title}"))
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Error: ${e.message}")
                    }
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("CSV", style = MaterialTheme.typography.labelLarge)
        }
        FilledTonalButton(
            onClick = {
                scope.launch {
                    try {
                        val file = exportReportAsCsv(context, reportType)
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "text/csv")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                        snackbarHostState.showSnackbar("Exported ${reportType.title}")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Error: ${e.message}")
                    }
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Export", style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun exportReportAsCsv(context: Context, reportType: ReportType): java.io.File {
    val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        ?: context.filesDir
    if (!downloadsDir.exists()) downloadsDir.mkdirs()
    val file = java.io.File(downloadsDir, "${reportType.title.replace(" ", "_")}_${System.currentTimeMillis()}.csv")
    file.bufferedWriter().use { out ->
        out.write("${reportType.title} Report\n")
        out.write("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}\n\n")
        when (reportType) {
            ReportType.FACULTY_WORKLOAD -> out.write("Faculty,Department,Max Hours,Assigned Hours,Utilization %\n")
            ReportType.DEPARTMENT_SUMMARY -> out.write("Department,Code,Head,Faculty Count,Subject Count\n")
            ReportType.SUBJECT_ALLOCATION -> out.write("Department,Total Subjects,Theory,Lab,Project,Seminar\n")
            ReportType.ROOM_UTILIZATION, ReportType.LAB_UTILIZATION -> out.write("Room,Total Slots,Used Slots,Utilization %\n")
            ReportType.FREE_PERIODS -> out.write("Day,Period,Start,End\n")
            ReportType.MISSING_HOURS -> out.write("Subject,Code,Department,Required,Allocated,Missing\n")
            ReportType.CONFLICTS -> out.write("Type,Message,Suggestion\n")
        }
    }
    return file
}

@Composable
private fun SummaryStat(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun UtilProgressBar(
    label: String,
    value: String,
    progress: Float,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun FacultyWorkloadReport(workloads: List<FacultyWorkload>) {
    if (workloads.isEmpty()) {
        EmptyReport("No faculty workload data available")
        return
    }

    val avgUtil = if (workloads.isNotEmpty()) workloads.sumOf { it.utilizationPercent.toDouble() } / workloads.size else 0.0
    val overloaded = workloads.count { it.utilizationPercent > 100f }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryStat("Faculty", "${workloads.size}", modifier = Modifier.weight(1f))
        SummaryStat("Avg Util", "${String.format("%.1f", avgUtil)}%", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        SummaryStat("Overloaded", "$overloaded", if (overloaded > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(12.dp))

    workloads.forEach { w ->
        val color = when {
            w.utilizationPercent > 100f -> MaterialTheme.colorScheme.error
            w.utilizationPercent >= 80f -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        }
        Card(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(w.facultyName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(w.departmentName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "${w.assignedHours}/${w.maxHours} hrs",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                UtilProgressBar(
                    label = "Utilization",
                    value = "${String.format("%.1f", w.utilizationPercent)}%",
                    progress = w.utilizationPercent / 100f,
                    color = color
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun DepartmentSummaryReport(summaries: List<DepartmentSummary>) {
    if (summaries.isEmpty()) {
        EmptyReport("No departments found")
        return
    }

    val totalFaculty = summaries.sumOf { it.facultyCount }
    val totalSubjects = summaries.sumOf { it.subjectCount }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryStat("Departments", "${summaries.size}", modifier = Modifier.weight(1f))
        SummaryStat("Faculty", "$totalFaculty", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        SummaryStat("Subjects", "$totalSubjects", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(12.dp))

    summaries.forEach { dept ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(dept.departmentName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${dept.departmentCode} | Head: ${dept.headName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatChip("Faculty", "${dept.facultyCount}", MaterialTheme.colorScheme.primary)
                    StatChip("Subjects", "${dept.subjectCount}", MaterialTheme.colorScheme.tertiary)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.1f)).padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SubjectAllocationReport(allocations: List<SubjectAllocation>) {
    if (allocations.isEmpty()) {
        EmptyReport("No subject data available")
        return
    }

    val totalSubjects = allocations.sumOf { it.totalSubjects }
    val totalTheory = allocations.sumOf { it.theoryCount }
    val totalLab = allocations.sumOf { it.labCount }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryStat("Subjects", "$totalSubjects", modifier = Modifier.weight(1f))
        SummaryStat("Theory", "$totalTheory", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        SummaryStat("Lab", "$totalLab", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(12.dp))

    allocations.forEach { alloc ->
        Card(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(alloc.departmentName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${alloc.totalSubjects}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (alloc.theoryCount > 0) StatChip("Theory", "${alloc.theoryCount}", MaterialTheme.colorScheme.primary)
                    if (alloc.labCount > 0) StatChip("Lab", "${alloc.labCount}", MaterialTheme.colorScheme.tertiary)
                    if (alloc.projectCount > 0) StatChip("Project", "${alloc.projectCount}", MaterialTheme.colorScheme.secondary)
                    if (alloc.seminarCount > 0) StatChip("Seminar", "${alloc.seminarCount}", MaterialTheme.colorScheme.error)
                }
                if (alloc.subjectNames.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = alloc.subjectNames.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun RoomUtilizationReport(utilizations: List<RoomUtilization>, label: String) {
    if (utilizations.isEmpty()) {
        EmptyReport("No $label utilization data available")
        return
    }

    val avgUtil = if (utilizations.isNotEmpty()) utilizations.sumOf { it.utilizationPercent.toDouble() } / utilizations.size else 0.0
    val highUtil = utilizations.count { it.utilizationPercent >= 75f }
    val lowUtil = utilizations.count { it.utilizationPercent < 25f }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryStat("${label}s", "${utilizations.size}", modifier = Modifier.weight(1f))
        SummaryStat("Avg Util", "${String.format("%.1f", avgUtil)}%", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        SummaryStat("High", "$highUtil", if (highUtil > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        SummaryStat("Low", "$lowUtil", if (lowUtil > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(12.dp))

    utilizations.forEach { util ->
        val color = when {
            util.utilizationPercent >= 75f -> MaterialTheme.colorScheme.tertiary
            util.utilizationPercent >= 40f -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.error
        }
        Card(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(util.roomName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${util.usedSlots}/${util.totalSlots} slots",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                UtilProgressBar(
                    label = "Utilization",
                    value = "${String.format("%.1f", util.utilizationPercent)}%",
                    progress = util.utilizationPercent / 100f,
                    color = color
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun FreePeriodsReport(freePeriods: List<FreePeriod>) {
    if (freePeriods.isEmpty()) {
        EmptyReport("No free periods found - all time slots are occupied")
        return
    }

    val groupedByDay = freePeriods.groupBy { it.dayOfWeek }
    val daysWithFree = groupedByDay.size

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryStat("Free Slots", "${freePeriods.size}", modifier = Modifier.weight(1f))
        SummaryStat("Days", "$daysWithFree", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        SummaryStat("Max/Day", "${groupedByDay.maxOfOrNull { it.value.size } ?: 0}", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(12.dp))

    groupedByDay.forEach { (day, periods) ->
        val dayName = periods.first().dayName
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(dayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${periods.size} free", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                periods.forEach { period ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Period ${period.periodNumber}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("${period.startTime} - ${period.endTime}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun MissingHoursReport(missing: List<MissingHour>) {
    if (missing.isEmpty()) {
        EmptyReport("All subjects have sufficient hours allocated")
        return
    }

    val totalMissing = missing.sumOf { it.missingHours }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryStat("Subjects", "${missing.size}", modifier = Modifier.weight(1f))
        SummaryStat("Missing Hrs", "$totalMissing", MaterialTheme.colorScheme.error, Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(12.dp))

    missing.forEach { m ->
        Card(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(m.subjectName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("${m.subjectCode} | ${m.departmentName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "-${m.missingHours}h",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                UtilProgressBar(
                    label = "Allocated: ${m.allocatedHours}h / Required: ${m.requiredHours}h",
                    value = "${if (m.requiredHours > 0) (m.allocatedHours.toFloat() / m.requiredHours * 100).toInt() else 0}%",
                    progress = if (m.requiredHours > 0) m.allocatedHours.toFloat() / m.requiredHours else 0f,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ConflictsReport(conflicts: List<ConflictReport>) {
    if (conflicts.isEmpty()) {
        EmptyReport("No scheduling conflicts detected")
        return
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryStat("Conflicts", "${conflicts.size}", MaterialTheme.colorScheme.error, Modifier.weight(1f))
        val byType = conflicts.groupBy { it.type }.map { "${it.key.name.take(4)}:${it.value.size}" }.joinToString(" ")
        Text(
            text = byType,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(2f).align(Alignment.CenterVertically)
        )
    }
    Spacer(modifier = Modifier.height(12.dp))

    conflicts.forEachIndexed { index, conflict ->
        val typeColor = when (conflict.type) {
            ConflictType.FACULTY_CLASH -> MaterialTheme.colorScheme.error
            ConflictType.ROOM_CLASH -> MaterialTheme.colorScheme.tertiary
            ConflictType.SECTION_CLASH -> MaterialTheme.colorScheme.secondary
            ConflictType.WORKLOAD_EXCEEDED -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        }

        Card(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = typeColor.copy(alpha = 0.05f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(8.dp).clip(CircleShape).background(typeColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = conflict.type.name.replace("_", " "),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = typeColor
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "#${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(conflict.message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Suggestion: ${conflict.suggestion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val details = buildList {
                    if (conflict.facultyName.isNotBlank()) add(conflict.facultyName)
                    if (conflict.roomName.isNotBlank()) add(conflict.roomName)
                    if (conflict.sectionName.isNotBlank()) add(conflict.sectionName)
                    if (conflict.dayOfWeek > 0) add(ReportsViewModel.getDayName(conflict.dayOfWeek))
                    if (conflict.periodNumber > 0) add("Period ${conflict.periodNumber}")
                }
                if (details.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = details.joinToString(" | "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun EmptyReport(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
