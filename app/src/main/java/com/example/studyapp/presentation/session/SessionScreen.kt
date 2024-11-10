package com.example.studyapp.presentation.session

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studyapp.presentation.component.DeleteDialog
import com.example.studyapp.presentation.component.SubjectListBottomSheet
import com.example.studyapp.presentation.component.studySessionList
import com.example.studyapp.presentation.theme.Red
import com.example.studyapp.util.Constants.ACTION_SERVICE_CANCEL
import com.example.studyapp.util.Constants.ACTION_SERVICE_START
import com.example.studyapp.util.Constants.ACTION_SERVICE_STOP
import com.example.studyapp.util.SnackBarEvent
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit

@Destination(
    deepLinks = [DeepLink(
        action = Intent.ACTION_VIEW,
        uriPattern =  "study_app://dashboard/session"
    )]
)
@Composable
fun SessionScreenRoute(
    navigator: DestinationsNavigator,
    timerService: StudySessionTimerService
){
    val viewModel: SessionViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    SessionScreen(
        state = state,
        onEvents = viewModel::onEvent,
        onBackButtonClicked = {
            navigator.navigateUp()
        },
        timerService = timerService,
        snackBarEvent = viewModel.snackbarEventFlow
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    state: SessionState,
    onEvents: (SessionEvents) -> Unit,
    onBackButtonClicked: () -> Unit,
    timerService: StudySessionTimerService,
    snackBarEvent: SharedFlow<SnackBarEvent>
) {
    val hour by timerService.hours
    val minutes by timerService.minutes
    val seconds by timerService.seconds
    val currentTimerState by timerService.currentTimerState

    val context = LocalContext.current
    var isBottonSheetOpen by remember{ mutableStateOf(false) }
    var subjectSelected by remember {
        mutableStateOf("Select subject")
    }
    val sheetState = rememberModalBottomSheetState()
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

                SnackBarEvent.NavigateUp -> TODO()
            }
        }
    }

    LaunchedEffect(key1 = state.subject){
        val subjectId = timerService.subjectId.value
        onEvents(
            SessionEvents.UpdateSubjectIdAndRelatedSubject(
                subjectId = subjectId,
                relatedSubject = state.subject.find { it.subjectId == subjectId }?.name
            )
        )
    }

    var isDeleteSessionDialogOpen by rememberSaveable{ mutableStateOf(false) }
    DeleteDialog(
        isOpen = isDeleteSessionDialogOpen,
        title = "Delete Session",
        bodyText = "Are you sure, you want to delete this session? Your studied hours will be reduced",
        onDismissRequest = { isDeleteSessionDialogOpen = false},
        onConfirmButtonClick = {
            onEvents(SessionEvents.DeleteSession)
            isDeleteSessionDialogOpen = false
        }
    )

    SubjectListBottomSheet(
        sheetState = sheetState,
        isOpen = isBottonSheetOpen,
        subjects = state.subject ,
        onSubjectTitleClicked = { subject ->
            scope.launch{sheetState.hide()}.invokeOnCompletion {
            if(!sheetState.isVisible) isBottonSheetOpen = false
        }
            onEvents(SessionEvents.OnRelatedSubjectChange(subject))
        },
        onDismissRequest = { isBottonSheetOpen = false}
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {SessionScreenTopAppBar(
            onBackButtonClicked = onBackButtonClicked
        )}
    ) {paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ){
            item { 
                timerSection(modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                    hours = hour,
                    minutes = minutes,
                    seconds = seconds
                )
            }
            item {
                RelatedSubjectSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    relatedToSubject = state.relatedToSubject?:"",
                    selectSubjectButtonClicked = { isBottonSheetOpen = true},
                    seconds = seconds
                )
            }
            item {
                ButtonSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    startButtonClicked = {
                        if(state.subjectId!=null && state.relatedToSubject!=null) {
                            ServiceHelper.triggerForegroundService(
                                context = context,
                                action = if (currentTimerState == TimerState.STARTED) {
                                    ACTION_SERVICE_STOP
                                } else
                                    ACTION_SERVICE_START
                            )
                            timerService.subjectId.value = state.subjectId
                        }else{
                            onEvents(SessionEvents.CheckSubjectId)
                        }
                    },
                    cancelButtonClicked = {
                        ServiceHelper.triggerForegroundService(
                            context = context,
                            action = ACTION_SERVICE_CANCEL
                        )
                    },
                    finishedButtonClicked = {
                        val duration = timerService.duration.toLong(DurationUnit.SECONDS)
                        onEvents(SessionEvents.SaveSession(duration = duration))
                        if(duration>=36) {
                            ServiceHelper.triggerForegroundService(
                                context = context,
                                action = ACTION_SERVICE_CANCEL
                            )
                        }
                    },
                    timerState = currentTimerState,
                    seconds = seconds
                )
            }

            studySessionList(
                sectionTitle = "STUDY SESSIONS HISTORY",
                emptyListText = "You don't have any recent study session.\n"+
                        "Start a study session to begin your recording",
                sessions = state.sessions,
                onDeleteIconClick = {
                    onEvents(SessionEvents.OnDeleteSessionButtonClicked(it))
                    isDeleteSessionDialogOpen = true
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionScreenTopAppBar(
    onBackButtonClicked: () -> Unit
){
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBackButtonClicked) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        title = {
            Text(text = "Study Session", style = MaterialTheme.typography.headlineSmall)
        }
    )
}

@Composable
private fun timerSection(
    modifier: Modifier,
    hours: String,
    minutes: String,
    seconds: String,
){
    Box(
        modifier = modifier.padding(50.dp),
        contentAlignment = Alignment.Center
    ){
        Box(
            modifier = modifier
                .size(250.dp)
                .border(5.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        )
        Row {
            AnimatedContent(targetState = hours, label = hours, transitionSpec = {timerAnimation()}) {hours ->
                Text(text = "$hours:", style = MaterialTheme.typography.titleLarge.copy(fontSize = 45.sp))
            }
            AnimatedContent(targetState = minutes, label = minutes, transitionSpec = {timerAnimation()}) {minutes ->
                Text(text = "$minutes:", style = MaterialTheme.typography.titleLarge.copy(fontSize = 45.sp))
            }
            AnimatedContent(targetState = seconds, label = seconds, transitionSpec = {timerAnimation()}) {seconds ->
                Text(text = seconds, style = MaterialTheme.typography.titleLarge.copy(fontSize = 45.sp))
            }
        }

    }
}

@Composable
private fun RelatedSubjectSection(
    modifier: Modifier,
    relatedToSubject: String,
    selectSubjectButtonClicked: () -> Unit,
    seconds: String
){
    Column(
        modifier = modifier
    ) {
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
            Text(
                text = relatedToSubject,//changes i made
                style = MaterialTheme.typography.bodyLarge
            )
            IconButton(
                onClick = selectSubjectButtonClicked,
                enabled = seconds == "00"
            ) {
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Select subject")
            }
        }
    }
}

@Composable
private fun ButtonSection(
    modifier: Modifier,
    startButtonClicked: () -> Unit,
    cancelButtonClicked: () -> Unit,
    finishedButtonClicked: () -> Unit,
    timerState: TimerState,
    seconds: String
){
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(onClick = cancelButtonClicked, enabled = seconds!="00" && timerState != TimerState.STARTED) {
            Text(text = "Cancel")
        }
        Button(onClick = startButtonClicked, colors = ButtonDefaults.buttonColors(
            containerColor = if(timerState == TimerState.STARTED) Red else  MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )) {
            Text(
                text = when(timerState){
                    TimerState.STARTED -> "  END  "
                    TimerState.STOPPED -> "Resume"
                    else -> "  Start  "
                }
            )
        }
        Button(onClick = finishedButtonClicked, enabled = seconds!="00" && timerState != TimerState.STARTED) {
            Text(text = "Finish")
        }
    }
}

private fun timerAnimation(duration: Int = 600):ContentTransform{
    return slideInVertically(animationSpec = tween(duration)) { fullHeight -> fullHeight }+
            fadeIn(animationSpec = tween(duration)) togetherWith
            slideOutVertically ( animationSpec = tween(duration) ){ fullHeight ->  fullHeight}+
            fadeOut(animationSpec = tween(duration))
}