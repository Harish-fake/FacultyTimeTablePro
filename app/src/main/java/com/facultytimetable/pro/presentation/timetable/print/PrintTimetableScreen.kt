package com.facultytimetable.pro.presentation.timetable.print

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
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
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.navigation.Routes
import com.facultytimetable.pro.presentation.theme.SubjectBreak
import com.facultytimetable.pro.presentation.theme.SubjectLab
import com.facultytimetable.pro.presentation.theme.SubjectLunch
import com.facultytimetable.pro.presentation.theme.SubjectTheory
import kotlinx.coroutines.launch

private val dayLabelsFull = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
private val dayLabelsShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
private val periodColors = listOf(
    SubjectTheory, SubjectLab, SubjectTheory, SubjectLunch,
    SubjectTheory, SubjectLab, SubjectBreak, SubjectTheory, SubjectLab
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintTimetableScreen(
    navController: NavController,
    viewModel: PrintTimetableViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Print Timetable",
                onBackClick = { navController.popBackStack() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading) {
            LoadingState()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                SectionQuickSelector(
                    sections = state.sections,
                    selectedSection = state.selectedSection,
                    onSectionSelected = viewModel::onSectionSelected
                )

                Spacer(modifier = Modifier.height(12.dp))

                ProfessionalTimetableCard(state)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                try {
                                    exportPdf(context, state)
                                    snackbarHostState.showSnackbar("PDF saved")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error: ${e.message}")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Print, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PDF")
                    }
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val file = exportPdf(context, state)
                                    val uri = FileProvider.getUriForFile(
                                        context, "${context.packageName}.fileprovider", file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Timetable"))
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error: ${e.message}")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share")
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun SectionQuickSelector(
    sections: List<com.facultytimetable.pro.data.local.db.entity.SectionEntity>,
    selectedSection: com.facultytimetable.pro.data.local.db.entity.SectionEntity?,
    onSectionSelected: (com.facultytimetable.pro.data.local.db.entity.SectionEntity) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(sections, key = { it.id }) { section ->
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
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProfessionalTimetableCard(state: PrintTimetableState) {
    val selected = state.selectedSection ?: return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CollegeHeader(
                departmentName = state.departmentName,
                sectionName = selected.name
            )

            HorizontalDivider(color = Color(0xFF1A237E), thickness = 2.dp)

            PeriodHeaderRow(state.timeSlots)

            val groupedByDay = state.timeSlots.groupBy { it.dayOfWeek }
            val days = groupedByDay.keys.sorted().take(5)
            val maxPeriods = groupedByDay.values.maxOfOrNull { it.size } ?: 8

            days.forEachIndexed { index, day ->
                DayRow(
                    dayLabel = dayLabelsFull.getOrElse(index) { "D$day" },
                    dayShort = dayLabelsShort.getOrElse(index) { "D$day" },
                    dayOfWeek = day,
                    timeSlots = groupedByDay[day] ?: emptyList(),
                    entries = state.entries,
                    subjects = state.subjects,
                    faculty = state.faculty,
                    rooms = state.rooms,
                    maxPeriods = maxPeriods
                )
                if (index < days.size - 1) {
                    HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
                }
            }

            HorizontalDivider(color = Color(0xFF1A237E), thickness = 1.dp)

            SubjectSummaryFooter(state)
        }
    }
}

@Composable
private fun CollegeHeader(departmentName: String, sectionName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A237E))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "FACULTY TIMETABLE PRO",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Department of $departmentName",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Section: $sectionName | Academic Year ${java.time.Year.now().value}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun PeriodHeaderRow(timeSlots: List<TimeSlotEntity>) {
    val allPeriods = timeSlots
        .groupBy { it.periodNumber }
        .mapValues { it.value.first() }
        .entries
        .sortedBy { it.key }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        Text(
            text = "Day",
            modifier = Modifier.width(80.dp).padding(horizontal = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A237E)
        )
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(allPeriods.toList()) { (_, slot) ->
                val label = when (slot.type) {
                    SlotType.LUNCH -> "LUNCH"
                    SlotType.BREAK -> "BREAK"
                    else -> "${slot.startTime}\n${slot.endTime}"
                }
                Text(
                    text = "P${slot.periodNumber}\n$label",
                    modifier = Modifier.width(70.dp).padding(horizontal = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun DayRow(
    dayLabel: String,
    dayShort: String,
    dayOfWeek: Int,
    timeSlots: List<TimeSlotEntity>,
    entries: List<com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity>,
    subjects: Map<Long, com.facultytimetable.pro.data.local.db.entity.SubjectEntity>,
    faculty: Map<Long, com.facultytimetable.pro.data.local.db.entity.FacultyEntity>,
    rooms: Map<Long, com.facultytimetable.pro.data.local.db.entity.RoomEntity>,
    maxPeriods: Int
) {
    val isAlt = dayOfWeek % 2 == 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isAlt) Color(0xFFFAFAFA) else Color.White)
            .padding(vertical = 2.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayShort,
            modifier = Modifier.width(80.dp).padding(horizontal = 4.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1A237E)
        )
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(timeSlots.sortedBy { it.periodNumber }) { slot ->
                val entry = entries.find { it.dayOfWeek == dayOfWeek && it.timeSlotId == slot.id }
                if (entry != null) {
                    val subject = subjects[entry.subjectId]
                    val fac = faculty[entry.facultyId]
                    val room = rooms[entry.roomId]
                    val color = when (subject?.type) {
                        SubjectType.LAB -> SubjectLab
                        else -> SubjectTheory
                    }
                    Column(
                        modifier = Modifier
                            .width(70.dp)
                            .padding(horizontal = 1.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color.copy(alpha = 0.15f))
                            .padding(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = subject?.code ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                            fontWeight = FontWeight.Bold,
                            color = color,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = fac?.name?.split(" ")?.last() ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp),
                            color = Color(0xFF616161),
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = room?.name ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp),
                            color = Color(0xFF9E9E9E),
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    when (slot.type) {
                        SlotType.LUNCH -> Text(
                            text = "L",
                            modifier = Modifier.width(70.dp).padding(2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = Color(0xFFBDBDBD),
                            textAlign = TextAlign.Center
                        )
                        SlotType.BREAK -> Text(
                            text = "B",
                            modifier = Modifier.width(70.dp).padding(2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = Color(0xFFBDBDBD),
                            textAlign = TextAlign.Center
                        )
                        else -> Box(
                            modifier = Modifier
                                .width(70.dp)
                                .padding(1.dp)
                                .height(24.dp)
                                .border(0.5.dp, Color(0xFFEEEEEE), RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectSummaryFooter(state: PrintTimetableState) {
    val groupedSubjects = state.entries
        .mapNotNull { e -> state.subjects[e.subjectId] }
        .distinctBy { it.id }
        .groupBy { it.code }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .padding(12.dp)
    ) {
        Text(
            text = "Subject Summary",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A237E)
        )
        Spacer(modifier = Modifier.height(6.dp))
        groupedSubjects.forEach { (code, subs) ->
            val sub = subs.first()
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${sub.name} ($code)",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                    color = Color(0xFF424242),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${sub.hoursPerWeek}h/w",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                    color = Color(0xFF757575)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Generated by Faculty TimeTable Pro | ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp),
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun exportPdf(context: Context, state: PrintTimetableState): java.io.File {
    val section = state.selectedSection ?: throw IllegalStateException("No section selected")
    val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        ?: context.filesDir
    if (!downloadsDir.exists()) downloadsDir.mkdirs()
    val file = java.io.File(downloadsDir, "Timetable_${section.name}_${System.currentTimeMillis()}.pdf")

    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas

    val titlePaint = Paint().apply {
        textSize = 18f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#1A237E")
    }
    val headerPaint = Paint().apply {
        textSize = 12f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#FFFFFF")
    }
    val cellTextPaint = Paint().apply {
        textSize = 9f; color = android.graphics.Color.parseColor("#424242")
    }
    val smallTextPaint = Paint().apply {
        textSize = 7f; color = android.graphics.Color.parseColor("#757575")
    }
    val linePaint = Paint().apply {
        color = android.graphics.Color.parseColor("#1A237E"); strokeWidth = 2f
    }
    val cellBgPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#F5F5F5")
    }

    var y = 40f
    canvas.drawText("FACULTY TIMETABLE PRO", 20f, y, titlePaint)
    y += 20f
    canvas.drawText("Department: ${state.departmentName} | Section: ${section.name}", 20f, y,
        Paint().apply { textSize = 11f; color = android.graphics.Color.parseColor("#424242") })
    y += 25f

    val days = state.timeSlots.groupBy { it.dayOfWeek }.keys.sorted().take(5)
    val allPeriods = state.timeSlots
        .groupBy { it.periodNumber }
        .mapValues { it.value.first() }
        .entries.sortedBy { it.key }

    val colCount = days.size + 1
    val colWidths = intArrayOf(80) + IntArray(days.size) { 140 }
    val rowHeight = 28f
    val xStarts = IntArray(colCount).also { arr ->
        var x = 20
        for (i in 0 until colCount) {
            arr[i] = x; x += colWidths[i]
        }
    }

    val headerBgPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#1A237E")
    }
    val headerH = 24f

    val p = Paint().apply { textSize = 9f; isFakeBoldText = true; color = android.graphics.Color.WHITE }
    canvas.drawRect(xStarts[0].toFloat(), y, (xStarts[0] + colWidths[0]).toFloat(), y + headerH, headerBgPaint)
    canvas.drawText("Day", xStarts[0] + 4f, y + 16f, p)
    days.forEachIndexed { i, _ ->
        val label = dayLabelsShort.getOrElse(i) { "D${i + 1}" }
        canvas.drawRect(xStarts[i + 1].toFloat(), y, (xStarts[i + 1] + colWidths[i + 1]).toFloat(), y + headerH, headerBgPaint)
        canvas.drawText(label, xStarts[i + 1] + 4f, y + 16f, p)
    }
    y += headerH + 4f

    allPeriods.forEach { (periodNum, slot) ->
        val isAlt = periodNum % 2 == 0
        if (isAlt) {
            canvas.drawRect(xStarts[0].toFloat(), y, (xStarts.last() + colWidths.last()).toFloat(), y + rowHeight, cellBgPaint)
        }
        canvas.drawText(
            "P$periodNum", xStarts[0] + 4f, y + 18f,
            Paint().apply { textSize = 9f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#1A237E") }
        )
        days.forEachIndexed { di, day ->
            val dayEntries = state.entries.filter { it.dayOfWeek == day && it.timeSlotId == slot.id }
            val entry = dayEntries.firstOrNull()
            val cx = xStarts[di + 1] + 4f
            if (entry != null) {
                val subject = state.subjects[entry.subjectId]
                val fac = state.faculty[entry.facultyId]
                canvas.drawText(subject?.code ?: "", cx, y + 12f, cellTextPaint)
                canvas.drawText(fac?.name?.split(" ")?.last() ?: "", cx, y + 22f, smallTextPaint)
            } else {
                when (slot.type) {
                    SlotType.REGULAR -> { }
                    SlotType.LUNCH -> canvas.drawText("LUNCH", cx, y + 18f,
                        Paint().apply { textSize = 8f; color = android.graphics.Color.parseColor("#BDBDBD") })
                    SlotType.BREAK -> canvas.drawText("BREAK", cx, y + 18f,
                        Paint().apply { textSize = 8f; color = android.graphics.Color.parseColor("#BDBDBD") })
                }
            }
        }
        y += rowHeight
    }

    y += 20f
    canvas.drawLine(20f, y, 822f, y, linePaint)
    y += 8f
    canvas.drawText("Subject Summary", 20f, y + 10f,
        Paint().apply { textSize = 11f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#1A237E") })

    val distinctSubjects = state.entries.mapNotNull { e -> state.subjects[e.subjectId] }.distinctBy { it.id }
    distinctSubjects.forEach { sub ->
        y += 16f
        canvas.drawText("${sub.name} (${sub.code}) - ${sub.hoursPerWeek}h/w", 20f, y, smallTextPaint)
    }

    y += 20f
    canvas.drawText("Generated by Faculty TimeTable Pro", 20f, y + 8f,
        Paint().apply { textSize = 7f; color = android.graphics.Color.parseColor("#9E9E9E") })

    document.finishPage(page)
    document.writeTo(java.io.FileOutputStream(file))
    document.close()
    return file
}
