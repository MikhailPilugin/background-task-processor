package com.example.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.UUID

object TasksTable : Table("tasks") {
    val id = uuid("id")
    val status = enumerationByName("status", 16, TaskStatus::class)
    val progress = integer("progress")
    val durationSeconds = integer("duration_seconds")
    val createdAt = timestamp("created_at")
    val userId = varchar("user_id", 255)

    override val primaryKey = PrimaryKey(id)
}

data class Task(
    val id: UUID,
    val status: TaskStatus,
    val progress: Int,
    val durationSeconds: Int,
    val createdAt: Instant,
    val userId: String,
)

@Serializable
data class CreateTaskRequest(val duration: Int)

@Serializable
data class CreateTaskResponse(val taskId: String)

@Serializable
data class TaskResponse(
    val id: String,
    val status: TaskStatus,
    val progress: Int,
    val durationSeconds: Int,
    val createdAt: String,
)

@Serializable
data class TaskProgress(
    val progress: Int,
    val status: TaskStatus,
)

fun Task.toResponse() = TaskResponse(
    id = id.toString(),
    status = status,
    progress = progress,
    durationSeconds = durationSeconds,
    createdAt = createdAt.toString(),
)

fun Task.toProgress() = TaskProgress(progress = progress, status = status)