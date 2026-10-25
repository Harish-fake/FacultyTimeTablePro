package com.facultytimetable.pro.presentation.timetable.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.SlotType
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import com.facultytimetable.pro.presentation.common.subjectColor
import com.facultytimetable.pro.presentation.theme.SubjectBreak
import com.facultytimetable.pro.presentation.theme.SubjectLunch

val dayShortNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun DaySelector(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    days: List<Int> = listOf(1, 2, 3, 4, 5),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { day ->
            FilterChip(
                selected = selectedDay == day,
                onClick = { onDaySelected(day) },
                label = {
                    Text(
                        dayShortNames.getOrElse(day - 1) { "Day $day" },
                        fontWeight = if (selectedDay == day) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

/**
 * One period row in a day timeline. Used by the section and faculty timetable screens so
 * both views stay visually consistent.
 */
@Composable
fun PeriodRow(
    slot: TimeSlotEntity,
    entry: TimetableEntryEntity?,
    subject: SubjectEntity?,
    faculty: FacultyEntity?,
    room: RoomEntity?,
    secondaryLabel: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.width(54.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                slot.startTime,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                slot.endTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        when {
            slot.type == SlotType.LUNCH || slot.type == SlotType.BREAK -> {
                val color = if (slot.type == SlotType.LUNCH) SubjectLunch else SubjectBreak
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.10f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (slot.type == SlotType.LUNCH) Icons.Default.FreeBreakfast else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (slot.type == SlotType.LUNCH) "Lunch break" else "Short break",
                        style = MaterialTheme.typography.labelLarge,
                        color = color,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            entry != null && subject != null -> {
                val color = subjectColor(subject.type.name)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(color.copy(alpha = 0.09f))
                        .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = color.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    subject.code,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = color,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                subject.type.name.lowercase().replaceFirstChar(Char::uppercase),
                                style = MaterialTheme.typography.labelSmall,
                                color = color
                            )
                        }
                        Text(
                            subject.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            listOfNotNull(
                                faculty?.name,
                                room?.name,
                                secondaryLabel
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        "P${slot.periodNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            else -> {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Free period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
