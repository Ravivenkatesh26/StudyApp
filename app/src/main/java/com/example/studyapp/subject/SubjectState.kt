package com.example.studyapp.subject

import androidx.compose.ui.graphics.Color
import com.example.studyapp.domain.model.Session
import com.example.studyapp.domain.model.Subject
import com.example.studyapp.domain.model.Task

data class SubjectState (
    val currentSubjectId: Int? = null,
    val subjectName: String = "",
    val goalStudyHours: String = "",
    val subjectCardColors: List<Color> = Subject.subjectCardColors.random(),
    val studiedHours: Float = 0f,
    val progress: Float = 0f,
    val recentSession: List<Session> = emptyList(),
    val upcomingTask: List<Task> = emptyList(),
    val completedTask: List<Task> = emptyList(),
    val session: Session? = null
)
