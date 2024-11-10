package com.example.studyapp.task

import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyapp.domain.model.Task
import com.example.studyapp.domain.repository.SubjectRepository
import com.example.studyapp.domain.repository.TaskRepository
import com.example.studyapp.navArgs
import com.example.studyapp.util.Priority
import com.example.studyapp.util.SnackBarEvent
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
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val taskRepository: TaskRepository,
    savedStateHandle: SavedStateHandle
):ViewModel() {

    private val navArgs: TaskScreenNavArgs = savedStateHandle.navArgs()
    private val _state = MutableStateFlow(TaskState())
    val state = combine(
        _state,
        subjectRepository.getAllSubjects()
    ){ state, subjects ->
        state.copy(
            subjects = subjects
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = TaskState()
    )

    private val _snackbarEventFlow =  MutableSharedFlow<SnackBarEvent>()
    val snackbarEventFlow = _snackbarEventFlow

    init {
        fetchTask()
        fetchSubject()
    }

    fun onEvent(event: TaskEvent) {
        when (event) {
            TaskEvent.DeleteTask -> deleteTask()
            is TaskEvent.OnDateChange -> {
                _state.update {
                    it.copy(
                        dueDate = event.millis
                    )
                }
            }
            is TaskEvent.OnDescriptionChange -> {
                _state.update {
                    it.copy(
                        description = event.description
                    )
                }
            }
            TaskEvent.OnIsCompleteChange ->{
                _state.update {
                    it.copy(
                        isTaskComplete = !_state.value.isTaskComplete
                    )
                }
            }
            is TaskEvent.OnPriorityChange -> {
                _state.update {
                    it.copy(
                        priority = event.priority
                    )
                }
            }
            is TaskEvent.OnRelatedSubjectSelect -> {
                _state.update {
                    it.copy(
                        relatedToSubject = event.subject.name,
                        subjectId = event.subject.subjectId
                    )
                }
            }
            is TaskEvent.OnTitleChange -> {
                _state.update {
                    it.copy(
                        title = event.title
                    )
                }
            }
            TaskEvent.SaveTask -> saveTask()
        }
    }

    private fun deleteTask() {
        viewModelScope.launch {
            try {
                val currentTaskId = state.value.currentTaskId

                if(currentTaskId!=null){
                    withContext(Dispatchers.IO){
                        taskRepository.deleteTask(currentTaskId)
                    }
                    _snackbarEventFlow.emit(
                        SnackBarEvent.ShowSnackMessage(
                            message = "Task deleted successfully"
                        )
                    )
                    _snackbarEventFlow.emit(SnackBarEvent.NavigateUp)
                }else{
                    _snackbarEventFlow.emit(
                        SnackBarEvent.ShowSnackMessage(
                            message = "Task couldn't be found"
                        )
                    )
                }
            }catch (e:Exception){
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackMessage(
                        message = "Task delete subject ${e.message}",
                        SnackbarDuration.Short
                    )
                )
            }
        }
    }

    private fun saveTask() {
        viewModelScope.launch {
            val state = _state.value
            if(state.relatedToSubject == null || state.subjectId == null){
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackMessage(
                        "Please select the related subject to task",
                        SnackbarDuration.Short
                    )
                )
                return@launch
            }
            try {
                taskRepository.upsertTask(
                    task = Task(
                        title = state.title,
                        description = state.description,
                        dueDate = state.dueDate ?: Instant.now().toEpochMilli(),
                        priority = state.priority.value,
                        relatedSubject = state.relatedToSubject,
                        isComplete = state.isTaskComplete,
                        taskId = state.currentTaskId,
                        subjectId = state.subjectId
                    )
                )
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackMessage(
                        "Task saved successfully}",
                        SnackbarDuration.Short
                    )
                )
                _snackbarEventFlow.emit(
                    SnackBarEvent.NavigateUp
                )
            }catch(e:Exception){
                _snackbarEventFlow.emit(
                    SnackBarEvent.ShowSnackMessage(
                        "Couldn't save task ${e.message}",
                        SnackbarDuration.Short
                    )
                )
            }
        }
    }

    private fun fetchTask(){
        viewModelScope.launch {
            navArgs.taskId?.let {
                taskRepository.getTaskById(it)?.let { task ->
                    _state.update {
                        it.copy(
                            title = task.title,
                            description = task.description,
                            dueDate = task.dueDate,
                            isTaskComplete = task.isComplete,
                            relatedToSubject = task.relatedSubject,
                            priority = Priority.fromInt(task.priority),
                            subjectId = task.subjectId,
                            currentTaskId = task.taskId
                        )
                    }
                }
            }
        }
    }

    private fun fetchSubject(){
        viewModelScope.launch{
            navArgs.subjectId?.let {
                subjectRepository.getSubjectById(it)?.let {subject ->
                    _state.update {
                        it.copy(
                            subjectId = subject.subjectId,
                            relatedToSubject = subject.name
                        )
                    }
                }
            }
        }
    }
}