package com.example.studyapp.presentation.dashboard

import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyapp.domain.model.Session
import com.example.studyapp.domain.model.Subject
import com.example.studyapp.domain.model.Task
import com.example.studyapp.domain.repository.SessionRepository
import com.example.studyapp.domain.repository.SubjectRepository
import com.example.studyapp.domain.repository.TaskRepository
import com.example.studyapp.util.SnackBarEvent
import com.example.studyapp.util.toHours
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val sessionRepository: SessionRepository,
    private val taskRepository: TaskRepository
): ViewModel() {

        private val _state = MutableStateFlow(DashboardState())
        val state = combine(
            _state,
            subjectRepository.getTotalSubjectCount(),
            subjectRepository.getTotalGoalHours(),
            subjectRepository.getAllSubjects(),
            sessionRepository.getTotalSessionDuration()
        ) { state, subjectCount, goalHours, subjects, totalSessionDuration ->
            state.copy(
                totalSubjectCount = subjectCount,
                totalGoalStudyHours = goalHours,
                subjects = subjects,
                totalStudiedHours = totalSessionDuration.toHours()
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = DashboardState()
        )

        val tasks: StateFlow<List<Task>> = taskRepository.getAllUpcomingTasks()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                initialValue = emptyList()
            )

        val recentSessions: StateFlow<List<Session>> = sessionRepository.getRecentFiveSessions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                initialValue = emptyList()
            )

        private val _snackbarEventFlow =  MutableSharedFlow<SnackBarEvent>()
        val snackbarEventFlow = _snackbarEventFlow


        fun onEvent(event: DashboardEvent) {
            when (event) {
                is DashboardEvent.OnSubjectNameChange -> {
                    _state.update {
                        it.copy(subjectName = event.name)
                    }
                }

                is DashboardEvent.OnGoalStudyHoursChange -> {
                    _state.update {
                        it.copy(goalStudyHours = event.hours)
                    }
                }

                is DashboardEvent.OnSubjectCardColorChange -> {
                    _state.update {
                        it.copy(subjectCardColors = event.colors)
                    }
                }

                is DashboardEvent.OnDeleteSessionButtonClick -> {
                    _state.update {
                        it.copy(session = event.session)
                    }
                }

                DashboardEvent.SaveSubject -> {
                    saveSubject()
                }
                DashboardEvent.DeleteSession -> deletionSession()
                is DashboardEvent.OnTaskIsCompleteChange -> updateTask(event.task)
            }
        }

    private fun updateTask(task:Task) {
        viewModelScope.launch {
            try {
                taskRepository.upsertTask(
                    task = task.copy(
                        isComplete = !task.isComplete
                    )
                )
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackMessage(
                        "Task saved in completed task",
                        SnackbarDuration.Short
                    )
                )
            }catch(e:Exception){
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackMessage(
                        "Couldn't save,please try again ${e.message}",
                        SnackbarDuration.Short
                    )
                )
            }
        }
    }

    private fun saveSubject() {
        viewModelScope.launch {
            try {
                subjectRepository.upsertSubject(
                    subject = Subject(
                        name = state.value.subjectName,
                        goalHours = state.value.goalStudyHours.toFloatOrNull()?:1f,
                        colors = state.value.subjectCardColors.map { it.toArgb() },
                    )
                )
                _state.update {
                    it.copy(
                        subjectName = " ",
                        goalStudyHours = " ",
                        subjectCardColors = Subject.subjectCardColors.random()
                    )
                }
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackMessage(
                        "Subject saved successfully",
                        SnackbarDuration.Short
                    )
                )
            }catch(e:Exception){
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackMessage(
                        "Couldn't save subject ${e.message}",
                        SnackbarDuration.Short
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

}