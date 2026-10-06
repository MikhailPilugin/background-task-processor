package com.example.services

import com.example.models.TaskProgress
import com.example.models.TaskStatus
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TaskEventHub {
    private val events = ConcurrentHashMap<UUID, MutableSharedFlow<TaskProgress>>()

    /**
     * Возвращает null, если flow задачи ещё не создан или уже удалён
     * после завершения задачи. Вызывающая сторона (WS-маршрут) должна
     * в этом случае взять актуальный статус из БД.
     */
    fun stream(taskId: UUID): SharedFlow<TaskProgress>? = events[taskId]

    fun publish(taskId: UUID, progress: TaskProgress) {
        val flow = flowFor(taskId)
        flow.tryEmit(progress)
        if (progress.status == TaskStatus.COMPLETED || progress.status == TaskStatus.FAILED) {
            events.remove(taskId)
        }
    }

    /** Удаляет flow задачи после финального статуса — иначе map растёт бесконечно. */
    fun cleanup(taskId: UUID) {
        events.remove(taskId)
    }

    private fun flowFor(taskId: UUID): MutableSharedFlow<TaskProgress> =
        events.computeIfAbsent(taskId) {
            MutableSharedFlow(replay = 1, extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        }
}
