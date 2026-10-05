package com.example

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskApiTest {
    @Test
    fun `tasks are visible only to their owner`() = testApplication {
        application { module() }

        val created = client.post("/tasks") {
            header("X-User-Id", "alice")
            contentType(ContentType.Application.Json)
            setBody("""{"duration":60}""")
        }

        assertEquals(HttpStatusCode.Created, created.status)
        val taskId = Json.parseToJsonElement(created.bodyAsText()).jsonObject
            .getValue("taskId").jsonPrimitive.content

        val ownerRead = client.get("/tasks/$taskId") {
            header("X-User-Id", "alice")
        }
        val otherUserRead = client.get("/tasks/$taskId") {
            header("X-User-Id", "bob")
        }
        val otherUserList = client.get("/tasks") {
            header("X-User-Id", "bob")
        }

        assertEquals(HttpStatusCode.OK, ownerRead.status)
        assertEquals(HttpStatusCode.NotFound, otherUserRead.status)
        assertEquals(0, Json.parseToJsonElement(otherUserList.bodyAsText()).jsonArray.size)

        val socketClient = createClient {
            install(WebSockets)
        }
        socketClient.webSocket("/tasks/$taskId/progress?userId=alice") {
            val frame = incoming.receive() as Frame.Text
            val event = Json.parseToJsonElement(frame.readText()).jsonObject
            assertEquals(0, event.getValue("progress").jsonPrimitive.content.toInt())
            assertEquals("RUNNING", event.getValue("status").jsonPrimitive.content)
        }
    }
}