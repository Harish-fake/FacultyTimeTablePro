package com.facultytimetable.pro.presentation.subject

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppFAB
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.ConfirmDialog
import com.facultytimetable.pro.presentation.common.components.EmptyState
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.navigation.Routes
import com.facultytimetable.pro.presentation.theme.SubjectLab
import com.facultytimetable.pro.presentation.theme.SubjectProject
import com.facultytimetable.pro.presentation.theme.SubjectSeminar
import com.facultytimetable.pro.presentation.theme.SubjectTheory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectListScreen(
    navController: NavController,
    viewModel: SubjectListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<SubjectEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Subjects", onBackClick = { navController.popBackStack() })

        if (state.isLoading) LoadingState()
        else if (state.subjects.isEmpty()) EmptyState(title = "No Subjects", message = "Add subjects to get started")
        else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                items(state.subjects, key = { it.id }) { subject ->
                    SubjectCard(
                        subject = subject,
                        onEdit = { navController.navigate(Routes.subjectForm(subject.id)) },
                        onDelete = { showDeleteDialog = subject }
                    )
                }
            }
        }

        AppFAB(onClick = { navController.navigate(Routes.subjectForm()) })
    }

    showDeleteDialog?.let { subject ->
        ConfirmDialog(
            title = "Delete Subject",
            message = "Are you sure you want to delete ${subject.name} (${subject.code})?",
            confirmText = "Delete",
            onConfirm = { viewModel.deleteSubject(subject); showDeleteDialog = null },
            onDismiss = { showDeleteDialog = null },
            isDestructive = true
        )
    }
}

@Composable
private fun SubjectCard(subject: SubjectEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val typeColor = when (subject.type) {
        com.facultytimetable.pro.data.local.db.entity.SubjectType.LAB -> SubjectLab
        com.facultytimetable.pro.data.local.db.entity.SubjectType.PROJECT -> SubjectProject
        com.facultytimetable.pro.data.local.db.entity.SubjectType.SEMINAR -> SubjectSeminar
        else -> SubjectTheory
    }

    AppCard(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(48.dp), tint = typeColor)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text("${subject.code} | ${subject.type.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${subject.hoursPerWeek} hrs/week", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
        }
    }
}
