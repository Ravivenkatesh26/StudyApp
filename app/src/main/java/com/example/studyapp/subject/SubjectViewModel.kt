package com.example.studyapp.subject

import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyapp.domain.model.Subject
import com.example.studyapp.domain.model.Task
import com.example.studyapp.domain.repository.SessionRepository
import com.example.studyapp.domain.repository.SubjectRepository
import com.example.studyapp.domain.repository.TaskRepository
import com.example.studyapp.navArgs
import com.example.studyapp.util.SnackBarEvent
import com.example.studyapp.util.toHours
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SubjectViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    savedStateHandle: SavedStateHandle
): ViewModel() {


    private val navArgs : SubjectScreenNavArgs = savedStateHandle.navArgs()
    private val _state = MutableStateFlow(SubjectState())

    val state = combine(
        _state,
        taskRepository.getupComingTasksForSubject(navArgs.subjectId),
        taskRepository.getCompletedTasksForSubject(navArgs.subjectId),
        sessionRepository.getRecentTenSession(navArgs.subjectId),
        sessionRepository.getTotalSessionDurationBySubjectId(navArgs.subjectId)
    ){
        state, upcomingTask,completedTask,recentSession,totalsSessionDuration ->

        state.copy(
            upcomingTask = upcomingTask,
            completedTask = completedTask,
            recentSession = recentSession,
            studiedHours = totalsSessionDuration.toHours()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = SubjectState()
    )

    private val _snackbarEventFlow =  MutableSharedFlow<SnackBarEvent>()
    val snackbarEventFlow = _snackbarEventFlow


    init {
        fetchSubject()
    }

    fun onEVent(event: SubjectEvent){
        when(event){
            SubjectEvent.DeleteSession -> deletionSession()
            SubjectEvent.DeleteSubject -> deleteSubject()
            is SubjectEvent.OnDeleteSessionButtonClick -> {
                _state.update {
                    it.copy(
                        session = event.session
                    )
                }
            }
            is SubjectEvent.OnGoalStudyHoursChange -> {
                _state.update {
                    it.copy(goalStudyHours = event.hours)
                }
            }
            is SubjectEvent.OnSubjectCardColorChange ->{
                _state.update {
                    it.copy(subjectCardColors = event.color)
                }
            }
            is SubjectEvent.OnSubjectNameChange -> {
                _state.update {
                    it.copy(subjectName = event.name)
                }
            }
            is SubjectEvent.OnTaskIsCompleteChange -> updateTask(event.task)
            SubjectEvent.UpdateSubject -> updateSubject()
            SubjectEvent.UpDateProgress -> {
                val goalStudyHours = state.value.goalStudyHours.toFloatOrNull()?:1f
                _state.update {
                    it.copy(
                        progress = (state.value.studiedHours/goalStudyHours).coerceIn(0f,1f)
                    )
                }
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

    private fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.upsertTask(
                    task = task.copy(
                        isComplete = !task.isComplete
                    )
                )
                if(task.isComplete) {
                    _snackbarEventFlow.emit(
                        SnackBarEvent.ShowSnackMessage(
                            "Task saved in completed task",
                            SnackbarDuration.Short
                        )
                    )
                }else{
                    _snackbarEventFlow.emit(
                        SnackBarEvent.ShowSnackMessage(
                            "Task saved in upcoming task",
                            SnackbarDuration.Short
                        )
                    )
                }
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

    private fun deleteSubject() {
        viewModelScope.launch{
            try {
                val currentSubjectId = state.value.currentSubjectId

                if(currentSubjectId!=null){
                    withContext(Dispatchers.IO){
                        subjectRepository.deleteSubject(currentSubjectId)
                    }
                    _snackbarEventFlow.emit(
                        SnackBarEvent.ShowSnackMessage(
                            message = "Subject deleted successfully"
                        )
                    )
                    _snackbarEventFlow.emit(SnackBarEvent.NavigateUp)
                }else{
                    _snackbarEventFlow.emit(
                        SnackBarEvent.ShowSnackMessage(
                            message = "Subject couldn't be found"
                        )
                    )
                }
            }catch (e:Exception){
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackMessage(
                        message = "couldn't delete subject ${e.message}",
                        SnackbarDuration.Short
                    )
                )
            }
        }
    }

    private fun updateSubject() {
        viewModelScope.launch {
            try {
                subjectRepository.upsertSubject(
                    subject = Subject(
                        subjectId = state.value.currentSubjectId,
                        name = state.value.subjectName,
                        goalHours = state.value.goalStudyHours.toFloatOrNull() ?: 1f,
                        colors = state.value.subjectCardColors.map { it.toArgb() }
                    )
                )
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackMessage(
                        message = "Subject updated successfully",
                        SnackbarDuration.Short
                    )
                )
            }catch (e:Exception){
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackMessage(
                        message = "Couldn't update subject,please try again ${e.message}",
                        SnackbarDuration.Short
                    )
                )
            }
        }
    }

    private fun fetchSubject(){
        viewModelScope.launch {
            subjectRepository
                .getSubjectById(navArgs.subjectId)?.let{ subject ->
                    _state.update { it.copy(
                        subjectName = subject.name,
                        goalStudyHours = subject.goalHours.toString(),
                        subjectCardColors = subject.colors.map { Color(it) },
                        currentSubjectId = subject.subjectId
                    ) }
                }
        }
    }
}