package com.facultytimetable.pro.presentation.room

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.facultytimetable.pro.presentation.common.components.LoadingState
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
    var searchQuery by remember { mutableStateOf("") }

    val filteredRooms = remember(state.rooms, searchQuery) {
        if (searchQuery.isBlank()) state.rooms
        else {
            val q = searchQuery.lowercase()
            state.rooms.filter { room ->
                room.name.lowercase().contains(q) ||
                room.building.lowercase().contains(q) ||
                room.type.name.lowercase().contains(q)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Rooms & Labs", onBackClick = { navController.popBackStack() })

        if (state.isLoading) {
            LoadingState()
        } else {
            androidx.compose.material3.OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name, building, or type...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            if (filteredRooms.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.MeetingRoom,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        Text(
                            "No Rooms Found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "Try adjusting your search"
                            else "Add your first room or lab to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(filteredRooms, key = { it.id }) { room ->
                        RoomCard(
                            room = room,
                            onEdit = { navController.navigate(Routes.roomForm(room.id)) },
                            onDelete = { showDeleteDialog = room }
                        )
                    }
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
}

@Composable
private fun roomTypeColor(type: RoomType) = when (type) {
    RoomType.CLASSROOM -> MaterialTheme.colorScheme.primary
    RoomType.LAB -> SubjectLab
    RoomType.SEMINAR_HALL -> MaterialTheme.colorScheme.tertiary
    RoomType.AUDITORIUM -> MaterialTheme.colorScheme.error
    RoomType.LIBRARY -> MaterialTheme.colorScheme.secondary
}

@Composable
private fun RoomCard(room: RoomEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    var elevated by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(
        targetValue = if (elevated) 6.dp else 1.dp,
        animationSpec = tween(150)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        onClick = { elevated = !elevated }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        CapacityBadge(capacity = room.capacity)
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.size(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (room.building.isNotBlank()) {
                    InfoChip(icon = Icons.Default.Business, text = room.building)
                }
                if (room.floor.isNotBlank()) {
                    InfoChip(icon = null, text = "Floor ${room.floor}")
                }
            }

            Spacer(modifier = Modifier.size(8.dp))

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
                if (!room.isActive) {
                    EquipmentChip(
                        icon = Icons.Outlined.CheckCircle,
                        label = "Inactive",
                        active = false
                    )
                }
            }

            AnimatedVisibility(visible = elevated) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        "Created: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(room.createdAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CapacityBadge(capacity: Int) {
    androidx.compose.material3.Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            "$capacity seats",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun InfoChip(icon: ImageVector?, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(4.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
