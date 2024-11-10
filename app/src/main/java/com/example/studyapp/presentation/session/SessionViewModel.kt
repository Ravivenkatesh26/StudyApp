package com.example.studyapp.presentation.session

import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyapp.domain.model.Session
import com.example.studyapp.domain.repository.SessionRepository
import com.example.studyapp.domain.repository.SubjectRepository
import com.example.studyapp.util.SnackBarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    subjectRepository: SubjectRepository,
    private val sessionRepository: SessionRepository
): ViewModel() {
    private val _state = MutableStateFlow(SessionState())
    private val _snackbarEventFlow =  MutableSharedFlow<SnackBarEvent>()
    val snackbarEventFlow = _snackbarEventFlow

    val state = combine(
        _state,
        subjectRepository.getAllSubjects(),
        sessionRepository.getAllSessions()
    ){state,subjects,sessions ->
        state.copy(
            subject = subjects,
            sessions = sessions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = SessionState()
    )

    fun onEvent(event: SessionEvents){
        when(event){
            SessionEvents.CheckSubjectId -> notifyToUpdateSubject()
            SessionEvents.DeleteSession -> deletionSession()
            is SessionEvents.OnDeleteSessionButtonClicked -> {
                _state.update {
                    it.copy(
                        session = event.session
                    )
                }
            }
            is SessionEvents.OnRelatedSubjectChange -> {
                _state.update {
                    it.copy(
                        relatedToSubject = event.subject.name,
                        subjectId = event.subject.subjectId
                    )
                }
            }
            is SessionEvents.SaveSession -> insertSession(event.duration)
            is SessionEvents.UpdateSubjectIdAndRelatedSubject -> {
                _state.update {
                    it.copy(
                        relatedToSubject = event.relatedSubject,
                        subjectId = event.subjectId
                    )
                }
            }
        }
    }

    private fun notifyToUpdateSubject() {
        viewModelScope.launch{
            if(state.value.subjectId==null || state.value.relatedToSubject == null){
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackMessage(
                        "Please select the related subject to start session"
                    )
                )
            }
        }
    }

    private fun deletionSession() {
        viewModelScope.launch {
            try {
                state.value.session?.let {
                    sessionRepository.deleteSession(it)
                }
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackMessage(
                        "Session deleted successfully"
                    )
                )
            }catch (e:Exception){
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackMessage(
                        "Session couldn't be deleted ${e.message}",
                        SnackbarDuration.Short
                    )
                )
            }
        }
    }

    private fun insertSession(duration: Long) {
        viewModelScope.launch {
            if(duration<36){
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackMessage(
                        "Single session must be greater than 36 sec",
                        SnackbarDuration.Short
                    )
                )
                return@launch
            }
            try {
                sessionRepository.insertSession(
                    session = Session(
                        sessionSubjectId = state.value.subjectId?:-1,
                        relatedSubject = state.value.relatedToSubject?:"",
                        date = Instant.now().toEpochMilli(),
                        duration = duration
                    )
                )
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackMessage(
                        "Session saved successfully"
                    )
                )
            }catch (e:Exception){
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackMessage(
                        "Couldn't save session ${e.message}",
                        SnackbarDuration.Short
                    )
                )
            }

        }
    }
}