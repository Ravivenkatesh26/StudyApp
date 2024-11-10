package com.example.studyapp.presentation.session

import com.example.studyapp.domain.model.Session
import com.example.studyapp.domain.model.Subject
import kotlin.time.Duration

sealed class SessionEvents {

    data class OnRelatedSubjectChange(val subject: Subject):SessionEvents()
    data class SaveSession(val duration: Long): SessionEvents()
    data class OnDeleteSessionButtonClicked(val session: Session): SessionEvents()
    data object DeleteSession: SessionEvents()
    data object CheckSubjectId: SessionEvents()
    data class UpdateSubjectIdAndRelatedSubject(
        val subjectId: Int?,
        val relatedSubject: String?
    ): SessionEvents()
}