package com.facultytimetable.pro.presentation.room

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.RoomType
import com.facultytimetable.pro.presentation.common.components.AppFAB
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.ConfirmDialog
import com.facultytimetable.pro.presentation.common.components.EmptyState
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.common.components.SearchBar
import com.facultytimetable.pro.presentation.navigation.Routes
import com.facultytimetable.pro.presentation.theme.SubjectLab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(
    navController: NavController,
    viewModel: RoomListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<RoomEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Rooms & Labs",
            actions = {
                if (state.rooms.isNotEmpty()) {
                    Text(
                        text = "${state.rooms.size}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            }
        )

        SearchBar(
            query = state.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            placeholder = "Search by name, building, or type...",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = state.selectedType == null,
                    onClick = { viewModel.onTypeFilterChange(null) },
                    label = { Text("All") }
                )
            }
            items(RoomType.entries) { type ->
                FilterChip(
                    selected = state.selectedType == type,
                    onClick = { viewModel.onTypeFilterChange(type) },
                    label = { Text(type.name.replace("_", " ")) }
                )
            }
        }

        Spacer(modifier = Modifier.size(8.dp))

        if (state.isLoading) {
            LoadingState()
        } else if (state.rooms.isEmpty()) {
            EmptyState(
                title = if (state.searchQuery.isNotBlank() || state.selectedType != null) "No Results Found" else "No Rooms",
                message = if (state.searchQuery.isNotBlank()) "Try adjusting your search terms"
                else "Add your first room or lab to get started"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(state.rooms, key = { it.id }) { room ->
                    RoomCard(
                        room = room,
                        onEdit = { navController.navigate(Routes.roomForm(room.id)) },
                        onDelete = { showDeleteDialog = room }
                    )
                }
            }
        }

        AppFAB(onClick = { navController.navigate(Routes.roomForm()) })
    }

    showDeleteDialog?.let { room ->
        ConfirmDialog(
            title = "Delete Room",
            message = "Are you sure you want to delete ${room.name}?",
            confirmText = "Delete",
            onConfirm = { viewModel.deleteRoom(room); showDeleteDialog = null },
            onDismiss = { showDeleteDialog = null },
            isDestructive = true
        )
    }
}

private fun roomTypeIcon(type: RoomType): ImageVector = when (type) {
    RoomType.CLASSROOM -> Icons.Default.MeetingRoom
    RoomType.LAB -> Icons.Default.Science
    RoomType.SEMINAR_HALL -> Icons.Default.MeetingRoom
    RoomType.AUDITORIUM -> Icons.Default.TheaterComedy
    RoomType.LIBRARY -> Icons.Default.LocalLibrary
    RoomType.LECTURE_HALL -> Icons.Default.MeetingRoom
    RoomType.SMART_CLASSROOM -> Icons.Default.MeetingRoom
}

private fun roomTypeColor(type: RoomType) = when (type) {
    RoomType.CLASSROOM -> MaterialTheme.colorScheme.primary
    RoomType.LAB -> SubjectLab
    RoomType.SEMINAR_HALL -> MaterialTheme.colorScheme.tertiary
    RoomType.AUDITORIUM -> MaterialTheme.colorScheme.error
    RoomType.LIBRARY -> MaterialTheme.colorScheme.secondary
    RoomType.LECTURE_HALL -> MaterialTheme.colorScheme.tertiary
    RoomType.SMART_CLASSROOM -> MaterialTheme.colorScheme.primary
}

@Composable
private fun RoomCard(room: RoomEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                roomTypeIcon(room.type),
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = roomTypeColor(room.type)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    room.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.size(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        room.type.name.replace("_", " "),
                        style = MaterialTheme.typography.labelMedium,
                        color = roomTypeColor(room.type)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        "$room.capacity seats",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (room.building.isNotBlank()) {
                    Text(
                        room.building,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EquipmentChip(
                        icon = if (room.hasProjector) Icons.Default.Videocam else Icons.Outlined.Videocam,
                        label = "Projector",
                        active = room.hasProjector
                    )
                    EquipmentChip(
                        icon = if (room.hasAC) Icons.Default.AcUnit else Icons.Outlined.AcUnit,
                        label = "AC",
                        active = room.hasAC
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun EquipmentChip(icon: ImageVector, label: String, active: Boolean) {
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    androidx.compose.material3.Surface(
        shape = MaterialTheme.shapes.small,
        color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
            Spacer(modifier = Modifier.size(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}
