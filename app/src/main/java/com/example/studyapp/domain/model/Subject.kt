package com.example.studyapp.domain.model

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.studyapp.presentation.theme.gradient1
import com.example.studyapp.presentation.theme.gradient2
import com.example.studyapp.presentation.theme.gradient3
import com.example.studyapp.presentation.theme.gradient4

@Entity
data class Subject(
    val name: String,
    val goalHours: Float,
    val colors: List<Int>,
    @PrimaryKey(autoGenerate = true)
    val subjectId: Int? = null
){
    companion object{
        val subjectCardColors = listOf(gradient1, gradient2, gradient3, gradient4)
    }
}
