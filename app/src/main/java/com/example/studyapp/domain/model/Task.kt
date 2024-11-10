package com.example.studyapp.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Task(
    val title:String,
    val description:String,
    val dueDate: Long,
    val priority: Int,
    val relatedSubject: String,
    val isComplete: Boolean,
    val subjectId: Int,
    @PrimaryKey(autoGenerate = true)
    val taskId: Int? = null
)
