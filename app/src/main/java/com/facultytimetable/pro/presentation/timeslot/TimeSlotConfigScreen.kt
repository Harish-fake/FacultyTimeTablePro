package com.facultytimetable.pro.presentation.timeslot

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.entity.SlotType
import com.facultytimetable.pro.presentation.common.components.ActionButton
import com.facultytimetable.pro.presentation.common.components.ActionChip
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.DropdownSelector
import com.facultytimetable.pro.presentation.common.components.LoadingState

private val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSlotConfigScreen(
    navController: NavController,
    viewModel: TimeSlotConfigViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) navController.popBackStack()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Time Slot Settings",
            onBackClick = { navController.popBackStack() }
        )

        if (state.isLoading) {
            LoadingState()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = "Working Days",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.dayConfigs.forEach { dayConfig ->
                        FilterChip(
                            selected = dayConfig.isWorkingDay,
                            onClick = { viewModel.toggleDay(dayConfig.dayOfWeek) },
                            label = { Text(dayLabels[dayConfig.dayOfWeek - 1]) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                state.dayConfigs
                    .filter { it.isWorkingDay }
                    .forEach { dayConfig ->
                        DayPeriodSection(
                            dayLabel = dayLabels[dayConfig.dayOfWeek - 1],
                            periods = dayConfig.periods,
                            onAddPeriod = { viewModel.addPeriod(dayConfig.dayOfWeek) },
                            onRemovePeriod = { index -> viewModel.removePeriod(dayConfig.dayOfWeek, index) },
                            onStartTimeChange = { index, value ->
                                viewModel.updatePeriodStartTime(dayConfig.dayOfWeek, index, value)
                            },
                            onEndTimeChange = { index, value ->
                                viewModel.updatePeriodEndTime(dayConfig.dayOfWeek, index, value)
                            },
                            onTypeChange = { index, type ->
                                viewModel.updatePeriodType(dayConfig.dayOfWeek, index, type)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                if (state.error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                ActionButton(
                    text = "Save",
                    onClick = viewModel::save,
                    enabled = !state.isSaving
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DayPeriodSection(
    dayLabel: String,
    periods: List<PeriodModel>,
    onAddPeriod: () -> Unit,
    onRemovePeriod: (Int) -> Unit,
    onStartTimeChange: (Int, String) -> Unit,
    onEndTimeChange: (Int, String) -> Unit,
    onTypeChange: (Int, SlotType) -> Unit
) {
    Text(
        text = dayLabel,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    if (periods.isEmpty()) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "No periods configured",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                ActionChip(
                    text = "Add Period",
                    onClick = onAddPeriod
                )
            }
        }
    } else {
        periods.forEachIndexed { index, period ->
            PeriodCard(
                period = period,
                onDelete = { onRemovePeriod(index) },
                onStartTimeChange = { onStartTimeChange(index, it) },
                onEndTimeChange = { onEndTimeChange(index, it) },
                onTypeChange = { onTypeChange(index, it) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))
        ActionChip(
            text = "Add Period",
            onClick = onAddPeriod,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PeriodCard(
    period: PeriodModel,
    onDelete: () -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onTypeChange: (SlotType) -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val typeLabel = when (period.type) {
                    SlotType.REGULAR -> "Period"
                    SlotType.LUNCH -> "Lunch"
                    SlotType.BREAK -> "Break"
                }
                Text(
                    text = "$typeLabel ${period.periodNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete period",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = period.startTime,
                    onValueChange = onStartTimeChange,
                    label = { Text("Start") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    placeholder = { Text("HH:mm") }
                )
                OutlinedTextField(
                    value = period.endTime,
                    onValueChange = onEndTimeChange,
                    label = { Text("End") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    placeholder = { Text("HH:mm") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            DropdownSelector(
                label = "Type",
                selectedItem = period.type,
                items = SlotType.entries,
                itemLabel = { type ->
                    when (type) {
                        SlotType.REGULAR -> "Regular"
                        SlotType.LUNCH -> "Lunch"
                        SlotType.BREAK -> "Break"
                    }
                },
                onItemSelected = onTypeChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
