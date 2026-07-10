package com.facultytimetable.pro.presentation.room

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.RoomType
import com.facultytimetable.pro.data.local.db.entity.RoomType.CLASSROOM
import com.facultytimetable.pro.data.local.db.entity.RoomType.LAB
import com.facultytimetable.pro.domain.repository.RoomRepository
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.DropdownSelector
import com.facultytimetable.pro.presentation.common.components.LoadingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoomFormState(
    val name: String = "",
    val capacity: String = "60",
    val type: RoomType = CLASSROOM,
    val building: String = "",
    val floor: String = "",
    val hasProjector: Boolean = true,
    val hasSmartBoard: Boolean = false,
    val hasAC: Boolean = false,
    val labEquipment: String = "",
    val isActive: Boolean = true,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class RoomFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val roomId: Long? = savedStateHandle.get<Long>("roomId")?.takeIf { it > 0 }
    private val _state = MutableStateFlow(RoomFormState())
    val state: StateFlow<RoomFormState> = _state

    init { loadData() }

    private fun loadData() {
        if (roomId != null) {
            viewModelScope.launch {
                val room = roomRepository.getRoomById(roomId)
                if (room != null) {
                    _state.value = _state.value.copy(
                        name = room.name,
                        capacity = room.capacity.toString(),
                        type = room.type,
                        building = room.building,
                        floor = room.floor,
                        hasProjector = room.hasProjector,
                        hasAC = room.hasAC,
                        isActive = room.isActive,
                        isLoading = false
                    )
                }
            }
        } else {
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v, error = null) }
    fun onCapacityChange(v: String) { _state.value = _state.value.copy(capacity = v) }
    fun onTypeChange(t: RoomType) { _state.value = _state.value.copy(type = t) }
    fun onBuildingChange(v: String) { _state.value = _state.value.copy(building = v) }
    fun onFloorChange(v: String) { _state.value = _state.value.copy(floor = v) }
    fun onProjectorChange(v: Boolean) { _state.value = _state.value.copy(hasProjector = v) }
    fun onSmartBoardChange(v: Boolean) { _state.value = _state.value.copy(hasSmartBoard = v) }
    fun onACChange(v: Boolean) { _state.value = _state.value.copy(hasAC = v) }
    fun onLabEquipmentChange(v: String) { _state.value = _state.value.copy(labEquipment = v) }
    fun onActiveChange(v: Boolean) { _state.value = _state.value.copy(isActive = v) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.value = s.copy(error = "Room name is required"); return }
        if (s.building.isBlank()) { _state.value = s.copy(error = "Building is required"); return }
        if (s.capacity.toIntOrNull() == null || (s.capacity.toIntOrNull() ?: 0) <= 0) {
            _state.value = s.copy(error = "Valid capacity is required"); return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val entity = RoomEntity(
                    id = roomId ?: 0,
                    name = s.name.trim(),
                    capacity = s.capacity.toIntOrNull() ?: 60,
                    type = s.type,
                    building = s.building.trim(),
                    floor = s.floor.trim(),
                    hasProjector = s.hasProjector,
                    hasAC = s.hasAC,
                    isActive = s.isActive
                )
                if (roomId != null) roomRepository.update(entity)
                else roomRepository.insert(entity)
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Save failed")
            }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}

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

        if (state.isLoading) {
            LoadingState()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Room Number / Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.building,
                    onValueChange = viewModel::onBuildingChange,
                    label = { Text("Building *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.floor,
                    onValueChange = viewModel::onFloorChange,
                    label = { Text("Floor") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.capacity,
                    onValueChange = viewModel::onCapacityChange,
                    label = { Text("Capacity *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                DropdownSelector(
                    label = "Type *",
                    selectedItem = state.type,
                    items = RoomType.entries,
                    itemLabel = { it.name.replace("_", " ") },
                    onItemSelected = viewModel::onTypeChange,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Equipment",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                ToggleRow(
                    label = "Has Projector?",
                    checked = state.hasProjector,
                    onCheckedChange = viewModel::onProjectorChange
                )
                ToggleRow(
                    label = "Has Smart Board?",
                    checked = state.hasSmartBoard,
                    onCheckedChange = viewModel::onSmartBoardChange
                )
                ToggleRow(
                    label = "Has AC?",
                    checked = state.hasAC,
                    onCheckedChange = viewModel::onACChange
                )

                if (state.type == LAB) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.labEquipment,
                        onValueChange = viewModel::onLabEquipmentChange,
                        label = { Text("Lab Equipment") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = MaterialTheme.shapes.medium,
                        placeholder = { Text("List lab equipment...") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                ToggleRow(
                    label = "Active",
                    checked = state.isActive,
                    onCheckedChange = viewModel::onActiveChange
                )

                if (state.error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !state.isSaving,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        if (roomId != null) "Update Room" else "Add Room",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
