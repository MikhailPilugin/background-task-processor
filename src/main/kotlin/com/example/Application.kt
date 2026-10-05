package com.example

import com.example.plugins.configureSerialization
import com.example.plugins.configureSockets
import com.example.plugins.configureRouting
import com.example.repositories.TaskRepository
import com.example.services.TaskEventHub
import com.example.services.TaskExecutor
import com.example.services.TaskService
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    DatabaseFactory.init()

    val taskScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val repository = TaskRepository(DatabaseFactory.database)
    val eventHub = TaskEventHub()
    val executor = TaskExecutor(repository, eventHub, taskScope)
    val taskService = TaskService(repository, executor)

    environment.monitor.subscribe(io.ktor.server.application.ApplicationStopping) {
        taskScope.cancel()
        DatabaseFactory.close()
    }

    configureSerialization()
    configureSockets()
    configureRouting(taskService, eventHub)
}