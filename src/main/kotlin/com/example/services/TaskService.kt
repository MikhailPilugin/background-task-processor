package com.example.services

import com.example.models.Task
import com.example.repositories.TaskRepository
import java.util.UUID

class TaskService(
    private val repository: TaskRepository,
    private val executor: TaskExecutor,
) {
    suspend fun createTask(userId: String, durationSeconds: Int): Task {
        require(durationSeconds in 1..86_400) { "duration must be between 1 and 86400 seconds" }
        val task = repository.create(userId, durationSeconds)
        executor.start(task.id, userId, durationSeconds)
        return task
    }

    suspend fun getTasks(userId: String): List<Task> = repository.findAll(userId)

    suspend fun getTask(id: UUID, userId: String): Task? = repository.findById(id, userId)
}