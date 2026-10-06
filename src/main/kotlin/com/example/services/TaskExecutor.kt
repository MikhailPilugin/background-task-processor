package com.example.services

import com.example.models.TaskProgress
import com.example.models.TaskStatus
import com.example.repositories.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.UUID

class TaskExecutor(
    private val repository: TaskRepository,
    private val eventHub: TaskEventHub,
    private val scope: CoroutineScope,
) {
    private val logger = LoggerFactory.getLogger(TaskExecutor::class.java)

    fun start(taskId: UUID, userId: String, durationSeconds: Int) {
        scope.launch {
            try {
                val current = repository.findById(taskId, userId)
                if (current?.status == TaskStatus.RUNNING || current?.status == TaskStatus.COMPLETED) {
                    logger.warn("Task {} is already {} — not restarting", taskId, current.status)
                    return@launch
                }

                val updated = repository.updateProgress(taskId, 0, TaskStatus.RUNNING)
                if (!updated) {
                    logger.warn("Task {} disappeared: progress update affected no rows", taskId)
                    return@launch
                }
                eventHub.publish(taskId, TaskProgress(0, TaskStatus.RUNNING))

                val steps = 20
                repeat(steps) { step ->
                    delay(durationSeconds * 1_000L / steps)
                    val progress = (step + 1) * 100 / steps
                    val status = if (progress == 100) TaskStatus.COMPLETED else TaskStatus.RUNNING
                    val updated = repository.updateProgress(taskId, progress, status)
                    if (!updated) {
                        logger.warn("Task {} disappeared: progress update affected no rows", taskId)
                        return@launch
                    }
                    eventHub.publish(taskId, TaskProgress(progress, status))
                }
                eventHub.cleanup(taskId)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                logger.error("Task {} failed", taskId, exception)
                val current = repository.findById(taskId, userId)
                if (current != null) {
                    repository.updateProgress(taskId, current.progress, TaskStatus.FAILED)
                    eventHub.publish(taskId, TaskProgress(current.progress, TaskStatus.FAILED))
                    eventHub.cleanup(taskId)
                }
            }
        }
    }
}
