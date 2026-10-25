package com.facultytimetable.pro.presentation.timetable.section

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.facultytimetable.pro.presentation.timetable.grid.TimetableGridScreen

@Composable
fun SectionTimetableScreen(
    sectionId: Long,
    navController: NavController
) {
    TimetableGridScreen(
        navController = navController,
        initialSectionId = sectionId,
        showBack = true
    )
}
