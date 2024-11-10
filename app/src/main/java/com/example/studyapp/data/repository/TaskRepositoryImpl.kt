package com.example.studyapp.data.repository

import com.example.studyapp.data.local.TaskDao
import com.example.studyapp.domain.model.Task
import com.example.studyapp.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
): TaskRepository {
    override suspend fun upsertTask(task: Task) {
        taskDao.upsertTask(task)
    }

    override suspend fun deleteTask(taskId: Int) {
        taskDao.deleteTask(taskId)
    }

    override suspend fun getTaskById(taskId: Int): Task? {
        return taskDao.getTaskById(taskId)
    }

    override fun getupComingTasksForSubject(subjectInt: Int): Flow<List<Task>> {
        return taskDao.getTaskForSubject(subjectInt)
            .map { task -> task.filter{ it.isComplete.not()} }
            .map { task-> sortTask(task) }
    }

    override fun getCompletedTasksForSubject(subjectInt: Int): Flow<List<Task>> {
        return taskDao.getTaskForSubject(subjectInt)
            .map { task -> task.filter{ it.isComplete} }
            .map { task-> sortTask(task) }
    }

    override fun getAllUpcomingTasks(): Flow<List<Task>> {
        return taskDao.getAllTask()
            .map { task ->task.filter { it.isComplete.not() } }
            .map { task-> sortTask(task) }
    }

    private fun sortTask(task: List<Task>):List<Task>{
        return task.sortedWith(compareBy<Task>{it.dueDate}.thenBy { it.description })
    }
}