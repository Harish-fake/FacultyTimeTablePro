package com.facultytimetable.pro.presentation.timetable.faculty

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyTimetableScreen(facultyId: Long, navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Faculty Timetable", onBackClick = { navController.popBackStack() })
        EmptyState(title = "Faculty Timetable", message = "Viewing timetable for faculty $facultyId")
    }
}
