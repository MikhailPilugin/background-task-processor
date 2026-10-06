package com.example.plugins

import com.example.models.TaskProgress
import com.example.models.TaskStatus
import kotlinx.coroutines.cancel
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
            if (taskId == null || userId.isNullOrEmpty()) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "taskId or userId missing"))
                return@webSocket
            }

            val task = taskService.getTask(taskId, userId)
            if (task == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Task not found"))
                return@webSocket
            }

            // Задача уже завершилась к моменту подключения — отправляем финальный статус и закрываем соединение
            if (task.status == TaskStatus.COMPLETED || task.status == TaskStatus.FAILED) {
                send(Frame.Text(Json.encodeToString(TaskProgress(task.progress, task.status))))
                close(CloseReason(CloseReason.Codes.NORMAL, "Task already finished"))
                return@webSocket
            }

            val flow = eventHub.stream(taskId)
            if (flow == null) {
                // Редкая гонка: задача завершилась между проверкой статуса и подпиской.
                // Финальный статус берём из БД.
                val latest = taskService.getTask(taskId, userId)
                if (latest != null) {
                    send(Frame.Text(Json.encodeToString(TaskProgress(latest.progress, latest.status))))
                }
                close(CloseReason(CloseReason.Codes.NORMAL, "Task finished"))
                return@webSocket
            }

            flow.collect { progress ->
                send(Frame.Text(Json.encodeToString(progress)))
                if (progress.status == TaskStatus.COMPLETED || progress.status == TaskStatus.FAILED) {
                    close(CloseReason(CloseReason.Codes.NORMAL, "Task finished"))
                    cancel()
                }
            }
        }

@kotlinx.serialization.Serializable
data class ErrorResponse(val error: String)