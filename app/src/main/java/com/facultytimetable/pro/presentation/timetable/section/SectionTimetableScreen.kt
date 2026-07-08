package com.facultytimetable.pro.presentation.timetable.section

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionTimetableScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Section Timetable", onBackClick = { navController.popBackStack() })
        EmptyState(title = "Section Timetable", message = "Select a section to view its timetable")
    }
}
