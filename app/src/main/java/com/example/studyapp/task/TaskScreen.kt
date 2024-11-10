package com.example.studyapp.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studyapp.presentation.component.DeleteDialog
import com.example.studyapp.presentation.component.SubjectListBottomSheet
import com.example.studyapp.presentation.component.TaskCheckBox
import com.example.studyapp.presentation.component.TaskDatePicker
import com.example.studyapp.util.Priority
import com.example.studyapp.util.SnackBarEvent
import com.example.studyapp.util.changeMillisToDateString
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant

data class TaskScreenNavArgs(
    val taskId: Int?,
    val subjectId: Int?
)

@Destination(navArgsDelegate = TaskScreenNavArgs::class)
@Composable
fun TaskScreenRoute(
    navigator: DestinationsNavigator
){
    val viewModel: TaskViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    TaskScreen(
        state = state ,
        onEvent = viewModel::onEvent,
        snackBarEvent = viewModel.snackbarEventFlow,
        onBackButtonClicked = {
            navigator.navigateUp()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    state: TaskState,
    onEvent: (TaskEvent) -> Unit,
    snackBarEvent: SharedFlow<SnackBarEvent>,
    onBackButtonClicked: () -> Unit
){
    var taskTitleError by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    taskTitleError = when{
        state.title.isEmpty() -> "Please enter a task title"
        state.title.length < 4 -> "Task title is too short"
        state.title.length > 30 -> "Task title is too long"
        else -> null
    }

    var isDeleteSessionDialogOpen by rememberSaveable{ mutableStateOf(false) }

    var isDatePickerDialogOpen by rememberSaveable{ mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Instant.now().toEpochMilli()
    )

    var isBottonSheetOpen by remember{ mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    //changes i made
    var subjectSelected by remember {
        mutableStateOf("Select subject")
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true){
        snackBarEvent.collectLatest { event ->
            when(event){
                is SnackBarEvent.ShowSnackMessage -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = event.duration
                    )
                }

                SnackBarEvent.NavigateUp -> {
                    onBackButtonClicked()
                }
            }
        }
    }

    DeleteDialog(
        isOpen = isDeleteSessionDialogOpen,
        title = "Delete Task",
        bodyText = "Are you sure, you want to delete this task?",
        onDismissRequest = { isDeleteSessionDialogOpen = false},
        onConfirmButtonClick = {
            onEvent(TaskEvent.DeleteTask)
            isDeleteSessionDialogOpen = false
        }
    )

    TaskDatePicker(
        state = datePickerState,
        isOpen = isDatePickerDialogOpen,
        onDismissRequest = { isDatePickerDialogOpen = false},
        onConfirmationButtonClicked = {
            onEvent(TaskEvent.OnDateChange(millis = datePickerState.selectedDateMillis))
            isDatePickerDialogOpen = false
        }
    )

    SubjectListBottomSheet(
        sheetState = sheetState,
        isOpen = isBottonSheetOpen,
        subjects = state.subjects ,
        onSubjectTitleClicked = {subject ->
            scope.launch{sheetState.hide()}.invokeOnCompletion {
            if(!sheetState.isVisible) isBottonSheetOpen = false
        }
            onEvent(TaskEvent.OnRelatedSubjectSelect(subject))
                                },
        onDismissRequest = { isBottonSheetOpen = false}
    )
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TaskScreenTopBar(
                isTaskExists = state.currentTaskId!=null,
                isComplete = state.isTaskComplete,
                checkBoxColor = state.priority.color,
                onCheckBoxButtonClick = {onEvent(TaskEvent.OnIsCompleteChange)},
                onDeleteButtonClick = { isDeleteSessionDialogOpen = true},
                onBackButtonClick = onBackButtonClicked
            )
        }
    ) {paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState()) //this make app to scroll if description is too long
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.title,
                onValueChange = { onEvent(TaskEvent.OnTitleChange(title = it))},
                label = { Text(text = "Title")},
                singleLine = true,
                isError = taskTitleError != null && state.title.isNotBlank(),
                supportingText = { Text(text = taskTitleError.orEmpty())}
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.description,
                onValueChange = { onEvent(TaskEvent.OnDescriptionChange(description = it))},
                label = { Text(text = "Description")},
                supportingText = { Text(text = "Please enter a description")}
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Due Date", style = MaterialTheme.typography.bodySmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.dueDate.changeMillisToDateString(),
                    style = MaterialTheme.typography.bodyLarge
                )
                IconButton(onClick = { isDatePickerDialogOpen = true }) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = "Select due date")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Priority",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row( modifier = Modifier.fillMaxWidth()) {
                Priority.entries.forEach{priority ->
                    priorityButton(
                        modifier = Modifier.weight(1f),
                        label = priority.title,
                        backgroundColor = priority.color,
                        borderColor = if(priority == state.priority) Color.White else Color.Transparent,
                        labelColor = if(priority == state.priority) Color.White else Color.White.copy(alpha = 0.7f),
                        onClick = {
                            onEvent(TaskEvent.OnPriorityChange(priority))
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Related to subject",
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val firstSubject = state.subjects.firstOrNull()?.name?: ""
                Text(
                    text = state.relatedToSubject?:firstSubject,
                    style = MaterialTheme.typography.bodyLarge
                )
                IconButton(onClick = { isBottonSheetOpen = true }) {
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Select subject")
                }
            }
            Button(
                enabled = taskTitleError == null,
                onClick = {
                          onEvent(TaskEvent.SaveTask)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(text = "Save")
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TaskScreenTopBar(
    isTaskExists:Boolean,
    isComplete: Boolean,
    checkBoxColor: Color,
    onBackButtonClick: () -> Unit,
    onDeleteButtonClick: () -> Unit,
    onCheckBoxButtonClick: () -> Unit
){
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBackButtonClick) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "NavigateBack")
            }
        },
        title = { Text(text = "Task", style = MaterialTheme.typography.headlineSmall)},
        actions = {
            if(isTaskExists){
                TaskCheckBox(
                    isComplete = isComplete,
                    borderColor = checkBoxColor,
                    onCheckBoxClick = onCheckBoxButtonClick
                )

                IconButton(onClick = onDeleteButtonClick) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    )
}

@Composable
private fun priorityButton(
    modifier: Modifier = Modifier,
    label: String,
    backgroundColor: Color,
    borderColor: Color,
    labelColor: Color,
    onClick: () -> Unit
){
    Box(
        modifier = modifier
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(5.dp)
            .border(1.dp, borderColor, RoundedCornerShape(5.dp))
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = labelColor)
    }
}