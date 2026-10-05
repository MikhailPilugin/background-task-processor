package com.example.plugins

import com.example.models.CreateTaskRequest
import com.example.models.CreateTaskResponse
import com.example.models.TaskResponse
import com.example.models.toResponse
import com.example.services.TaskEventHub
import com.example.services.TaskService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.send
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

fun Application.configureRouting(taskService: TaskService, eventHub: TaskEventHub) {
    routing {
        get("/") {
            val page = javaClass.classLoader.getResource("index.html")
            if (page == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respondText(page.readText(), ContentType.Text.Html)
            }
        }

        route("/tasks") {
            post {
                val userId = call.request.headers["X-User-Id"]?.trim()
                if (userId.isNullOrEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("X-User-Id header is required"))
                    return@post
                }
                val request = runCatching { call.receive<CreateTaskRequest>() }.getOrNull()
                if (request == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("A JSON body with duration is required"))
                    return@post
                }
                val task = runCatching { taskService.createTask(userId, request.duration) }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(it.message ?: "Invalid duration"))
                    return@post
                }
                call.respond(HttpStatusCode.Created, CreateTaskResponse(task.id.toString()))
            }

            get {
                val userId = call.request.headers["X-User-Id"]?.trim()
                if (userId.isNullOrEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("X-User-Id header is required"))
                    return@get
                }
                call.respond(taskService.getTasks(userId).map { it.toResponse() })
            }

            get("/{id}") {
                val userId = call.request.headers["X-User-Id"]?.trim()
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                if (userId.isNullOrEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("X-User-Id header is required"))
                } else if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid task id"))
                } else {
                    val task = taskService.getTask(id, userId)
                    if (task == null) call.respond(HttpStatusCode.NotFound)
                    else call.respond(task.toResponse())
                }
            }
        }

        webSocket("/tasks/{taskId}/progress") {
            val taskId = call.parameters["taskId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            val userId = call.request.queryParameters["userId"]?.trim()
            if (taskId == null || userId.isNullOrEmpty() || taskService.getTask(taskId, userId) == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Task not found or userId missing"))
                return@webSocket
            }

            eventHub.stream(taskId).collect { progress ->
                send(Frame.Text(Json.encodeToString(progress)))
            }
        }
    }
}

@kotlinx.serialization.Serializable
data class ErrorResponse(val error: String)