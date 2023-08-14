package com.achugr.coffversation.slack

import com.achugr.coffversation.asyncTaskService
import com.achugr.coffversation.model.IntroFrequency
import com.achugr.coffversation.service.InitCoffeeTalk
import kotlinx.coroutines.runBlocking

fun actionsModule() {
    app.blockAction("start") { req, ctx ->
        runBlocking {
            val value = IntroFrequency.valueOf(req.payload.actions[0].selectedOption.value)
            val channel = req.payload.channel.id
            asyncTaskService.execute(InitCoffeeTalk(channel, value))
            ctx.respond { res ->
                val freqMessage = when (value) {
                    IntroFrequency.NOW_ONCE -> "triggered coffee talk round"
                    IntroFrequency.MONDAY_ONCE_A_WEEK -> "configured coffee talk round once a week, on Monday"
                    IntroFrequency.MONDAY_ONCE_TWO_WEEKS -> "configured coffee talk round once in two weeks, on Monday"
                    IntroFrequency.PAUSED -> "paused coffee talk"
                    IntroFrequency.EVERY_MINUTE_TESTING -> "triggered every minute for testing purposes"
                    else -> throw IllegalArgumentException("Unknown intro frequency: $value")
                }
                res.responseType("in_channel").text("<@${req.payload.user.id}> $freqMessage.")
            }
        }
        ctx.ack()
    }
}