package com.achugr.coffeersation.slack

import com.achugr.coffeersation.asyncTaskService
import com.achugr.coffeersation.service.RequestInfo
import kotlinx.coroutines.runBlocking

fun commandsModule() {
    app.command("/menu") { req, ctx ->
        ctx.say(menu())
        ctx.ack()
    }

    app.command("/status") { req, ctx ->
        runBlocking {
            asyncTaskService.execute(RequestInfo(req.payload.channelId))
            ctx.ack("Info request accepted.")
        }
    }
}