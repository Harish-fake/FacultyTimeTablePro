package com.facultytimetable.pro.presentation.room

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.entity.RoomType
import com.facultytimetable.pro.presentation.common.components.ActionButton
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomFormScreen(
    roomId: Long?,
    navController: NavController,
    viewModel: RoomFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saveSuccess) { if (state.saveSuccess) navController.popBackStack() }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = if (roomId != null) "Edit Room" else "Add Room",
            onBackClick = { navController.popBackStack() }
        )

        if (state.isLoading) LoadingState()
        else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
            ) {
                OutlinedTextField(
                    value = state.name, onValueChange = viewModel::onNameChange,
                    label = { Text("Room Name *") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.capacity, onValueChange = viewModel::onCapacityChange,
                    label = { Text("Capacity") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Room Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Column(modifier = Modifier.selectableGroup()) {
                    RoomType.entries.forEach { type ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .selectable(selected = state.type == type, onClick = { viewModel.onTypeChange(type) }, role = Role.RadioButton)
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = state.type == type, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(type.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.building, onValueChange = viewModel::onBuildingChange,
                    label = { Text("Building") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                if (state.error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(24.dp))
                ActionButton(
                    text = if (roomId != null) "Update Room" else "Add Room",
                    onClick = viewModel::save, enabled = !state.isSaving
                )
            }
        }
    }
}
