package com.facultytimetable.pro.presentation.common

import androidx.compose.ui.graphics.Color
import com.facultytimetable.pro.presentation.theme.SubjectLab
import com.facultytimetable.pro.presentation.theme.SubjectLibrary
import com.facultytimetable.pro.presentation.theme.SubjectProject
import com.facultytimetable.pro.presentation.theme.SubjectSeminar
import com.facultytimetable.pro.presentation.theme.SubjectSports
import com.facultytimetable.pro.presentation.theme.SubjectTheory

/** Consistent colour for a subject type across the timetable, reports and dashboard. */
fun subjectColor(type: String): Color = when (type.uppercase()) {
    "LAB" -> SubjectLab
    "PROJECT" -> SubjectProject
    "SEMINAR" -> SubjectSeminar
    "LIBRARY" -> SubjectLibrary
    "SPORTS" -> SubjectSports
    else -> SubjectTheory
}
