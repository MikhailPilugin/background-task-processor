package com.example

import com.example.models.TaskProgress
import com.example.models.TaskStatus
import com.example.services.TaskEventHub
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskEventHubTest {
    @Test
    fun `latest progress is replayed to late subscribers`() {
        val hub = TaskEventHub()
        val taskId = UUID.randomUUID()
        val completed = TaskProgress(progress = 100, status = TaskStatus.COMPLETED)

        hub.publish(taskId, completed)

        assertEquals(completed, hub.stream(taskId).replayCache.single())
    }
}