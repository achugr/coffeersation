package com.achugr.coffeersation.service

import com.achugr.coffeersation.*
import com.achugr.coffeersation.model.IntroFrequency
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.api.core.ApiFutureToListenableFuture
import com.google.cloud.tasks.v2beta3.*
import com.google.cloud.tasks.v2beta3.HttpMethod
import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.guava.asDeferred
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.time.Instant

class AsyncTaskService(private val client: CloudTasksClient) {
    private val log = LoggerFactory.getLogger(AsyncTaskService::class.java)
    private val mapper = jacksonObjectMapper()

    suspend fun execute(task: AsyncTask, time: Instant? = null) {
        val queuePath =
            QueueName.of(config.gcp.project, config.webhook.queueRegion, config.webhook.triggerQueue).toString()
        val tokenBuilder = OidcToken.newBuilder()
            .setAudience(config.webhook.audience)
            .setServiceAccountEmail(config.webhook.serviceAccount)
        val taskBuilder = Task.newBuilder().setHttpRequest(
            HttpRequest.newBuilder().setUrl("${config.webhook.baseUrl}/${config.webhook.secret}/task")
                .setHttpMethod(HttpMethod.POST)
                .setOidcToken(tokenBuilder)
                .putHeaders(HttpHeaders.ContentType, ContentType.Application.Json.toString()).setBody(
                    ByteString.copyFrom(
                        mapper.writeValueAsString(task),
                        Charset.defaultCharset()
                    )
                ).build()
        );
        if (time != null) {
            taskBuilder.scheduleTime = Timestamp.newBuilder().setSeconds(time.epochSecond).build()
        }
        val future = client.createTaskCallable().futureCall(
            CreateTaskRequest.newBuilder()
                .setParent(queuePath)
                .setTask(taskBuilder.build())
                .build()
        )
        ApiFutureToListenableFuture(future)
            .asDeferred()
            .await()
        log.info("Task of kind ${task.kind} has been scheduled")
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes(
    JsonSubTypes.Type(value = TriggerTalkRound::class, name = "trigger"),
    JsonSubTypes.Type(value = InitCoffeeTalk::class, name = "init"),
    JsonSubTypes.Type(value = RequestInfo::class, name = "info")
)
abstract class AsyncTask(val kind: String) {
}

data class TriggerTalkRound(val channel: String, val version: Int) : AsyncTask(kind = "trigger")
data class InitCoffeeTalk(val channel: String, val freq: IntroFrequency) : AsyncTask(kind = "init")
data class RequestInfo(val channel: String) : AsyncTask(kind = "info")

fun Application.taskHandlerModule() {
    routing {
        post("/${config.webhook.secret}/task") {
            try {
                when (val task = call.receive<AsyncTask>()) {
                    is TriggerTalkRound -> {
                        logger.info("Triggering round for channel: ${task.channel}")
                        coffeeTalkService.triggerRound(task)
                    }

                    is InitCoffeeTalk -> {
                        logger.info("Initializing coffee talk for channel: ${task.channel}")
                        coffeeTalkService.initTalk(task)
                    }

                    is RequestInfo -> {
                        logger.info("Posting status of coffee talk for channel: ${task.channel}")
                        coffeeTalkService.postInfo(task)
                    }
                }
                call.respond(HttpStatusCode.OK, "ok")
            } catch (e: Exception) {
                logger.error("Error while handling task", e)
                call.respond(HttpStatusCode.InternalServerError, "error")
            }
        }
    }
}