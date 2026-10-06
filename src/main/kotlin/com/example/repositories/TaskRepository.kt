package com.example.repositories

import com.example.models.Task
import com.example.models.TaskStatus
import com.example.models.TasksTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskRepository(private val database: Database) {
    suspend fun create(userId: String, durationSeconds: Int): Task = withContext(Dispatchers.IO) {
        val task = Task(
            id = UUID.randomUUID(),
            status = TaskStatus.CREATED,
            progress = 0,
            durationSeconds = durationSeconds,
            createdAt = Instant.now(),
            userId = userId,
        )
        transaction(database) {
            TasksTable.insert {
                it[id] = task.id
                it[status] = task.status
                it[progress] = task.progress
                it[TasksTable.durationSeconds] = task.durationSeconds
                it[createdAt] = task.createdAt
                it[TasksTable.userId] = task.userId
            }
        }
        task
    }

    suspend fun findById(id: UUID, userId: String): Task? = withContext(Dispatchers.IO) {
        transaction(database) {
            TasksTable.selectAll()
                .where { (TasksTable.id eq id) and (TasksTable.userId eq userId) }
                .singleOrNull()
                ?.toTask()
        }
    }

    suspend fun findAll(userId: String): List<Task> = withContext(Dispatchers.IO) {
        transaction(database) {
            TasksTable.selectAll()
                .where { TasksTable.userId eq userId }
                .orderBy(TasksTable.createdAt)
                .map { it.toTask() }
        }
    }

    suspend fun updateProgress(id: UUID, progress: Int, status: TaskStatus): Boolean =
    withContext(Dispatchers.IO) {
        transaction(database) {
            TasksTable.update({ TasksTable.id eq id }) {
                it[TasksTable.progress] = progress
                it[TasksTable.status] = status
            } > 0
        }
    }

    private fun ResultRow.toTask() = Task(
        id = this[TasksTable.id],
        status = this[TasksTable.status],
        progress = this[TasksTable.progress],
        durationSeconds = this[TasksTable.durationSeconds],
        createdAt = this[TasksTable.createdAt],
        userId = this[TasksTable.userId],
    )
}