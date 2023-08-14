package com.achugr.coffversation

import com.achugr.coffversation.service.taskHandlerModule
import com.achugr.coffversation.slack.slackRootModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*

fun main() {
    val server = embeddedServer(CIO, port = config.http.port, module = Application::rootModule)
    server.start(wait = true)
}

fun Application.rootModule() {
    install(ContentNegotiation) {
        jackson()
    }
    taskHandlerModule()
    slackRootModule()
}