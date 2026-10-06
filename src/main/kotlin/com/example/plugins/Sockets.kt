package com.example.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.*
import java.time.Duration

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(30)
        maxFrameSize = 64 * 1024L
    }
}
