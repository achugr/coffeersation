package com.achugr.coffversation.slack

import com.slack.api.model.event.AppMentionEvent

fun eventsModule() {
    app.event(AppMentionEvent::class.java) { event, ctx ->
        ctx.say(menu())
        ctx.ack()
    }
}