package com.achugr.coffeersation.slack

import com.achugr.coffeersation.executor
import com.achugr.coffeersation.logger
import com.slack.api.bolt.App
import com.slack.api.bolt.ktor.respond
import com.slack.api.bolt.ktor.toBoltRequest
import com.slack.api.bolt.util.SlackRequestParser
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlin.system.measureTimeMillis

val app: App by lazy {
    App()
}

fun Application.slackRootModule() {
    executor.submit {
        app.start()
    }
    commandsModule()
    eventsModule()
    actionsModule()
    val requestParser = SlackRequestParser(app.config())
    routing {
        post("/") {
            logger.info("Incoming request from slack.")
            val duration = measureTimeMillis {
                respond(call, app.run(toBoltRequest(call, requestParser)))
            }
            logger.info("Request took $duration ms to complete.")
        }
    }
}