package com.example

import com.example.models.TaskProgress
import com.example.models.TaskStatus
import com.example.services.TaskEventHub
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TaskEventHubTest {
    private val hub = TaskEventHub()
    private val taskId = UUID.randomUUID()

    @Test
    fun `stream returns null before the first publish`() {
        assertNull(hub.stream(taskId))
    }

    @Test
    fun `latest progress is replayed to subscribers`() {
        val progress = TaskProgress(progress = 50, status = TaskStatus.RUNNING)

        hub.publish(taskId, progress)

        val flow = assertNotNull(hub.stream(taskId))
        assertEquals(progress, flow.replayCache.single())
    }

    @Test
    fun `flow is evicted after a terminal status`() {
        val completed = TaskProgress(progress = 100, status = TaskStatus.COMPLETED)

        hub.publish(taskId, completed)

        assertNull(hub.stream(taskId))
    }

    @Test
    fun `cleanup removes the flow`() {
        val progress = TaskProgress(progress = 10, status = TaskStatus.RUNNING)

        hub.publish(taskId, progress)
        assertNotNull(hub.stream(taskId))

        hub.cleanup(taskId)

        assertNull(hub.stream(taskId))
    }
}
