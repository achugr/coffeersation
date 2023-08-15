package com.achugr.coffeersation

import com.achugr.coffeersation.service.AsyncTaskService
import com.achugr.coffeersation.service.CoffeeTalkService
import com.achugr.coffeersation.service.IntroSchedulerService
import com.achugr.coffeersation.slack.SlackService
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.api.gax.core.GoogleCredentialsProvider
import com.google.auth.Credentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.tasks.v2beta3.CloudTasksClient
import com.google.cloud.tasks.v2beta3.CloudTasksSettings
import com.slack.api.Slack
import com.slack.api.methods.AsyncMethodsClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

val gcpCredentials: Credentials? by lazy {
    val credentialsFile = if (config.gcp.credentialsFileName != null) File(config.gcp.credentialsFileName!!) else null
    if (credentialsFile != null && credentialsFile.exists()) {
        logger.info("Initializing task client with fixed credentials provider.")
        credentialsFile.inputStream().use { inputStream ->
            ServiceAccountCredentials.fromStream(inputStream)
        }
    } else {
        null
    }
}

val cloudTasksClient: CloudTasksClient by lazy {
    val credentialsProvider = if (gcpCredentials != null) {
        logger.info("Initializing task client with fixed credentials provider.")
        FixedCredentialsProvider.create(gcpCredentials)
    } else {
        logger.info("Initializing task client with Google credentials provider.")
        GoogleCredentialsProvider.newBuilder().build()
    }
    CloudTasksClient.create(
        CloudTasksSettings.newHttpJsonBuilder()
            .setCredentialsProvider(credentialsProvider)
            .build()
    )
}

val datastore: Datastore by lazy {
    val creds = gcpCredentials
    if (creds != null) {
        DatastoreOptions.newBuilder().setCredentials(creds).build().service;
    } else {
        DatastoreOptions.getDefaultInstance().service
    }
}

val slackClient: AsyncMethodsClient by lazy {
    Slack().methodsAsync(config.slack.token)
}

val asyncTaskService: AsyncTaskService by lazy { AsyncTaskService(cloudTasksClient) }

val coffeeTalkService: CoffeeTalkService by lazy {
    CoffeeTalkService(
        SlackService(slackClient),
        IntroSchedulerService(),
        asyncTaskService
    )
}

val logger: Logger = LoggerFactory.getLogger("App.kt")

val executor: ExecutorService = Executors.newFixedThreadPool(2)

data class Config(val slack: SlackConfig, val webhook: WebhookConfig, val http: HttpConfig, val gcp: GcpConfig)
data class SlackConfig(val token: String)
data class HttpConfig(val port: Int)
data class GcpConfig(val project: String, val credentialsFileName: String?)
data class WebhookConfig(
    val secret: String,
    val triggerQueue: String,
    val initQueue: String,
    val queueRegion: String,
    val baseUrl: String,
    val audience: String,
    val serviceAccount: String
)

val config: Config = Config(
    slack = SlackConfig(token = System.getenv("SLACK_BOT_TOKEN")),
    webhook = WebhookConfig(
        secret = System.getenv("WEBHOOK_SECRET"),
        triggerQueue = System.getenv("TRIGGER_QUEUE"),
        initQueue = System.getenv("INIT_QUEUE"),
        queueRegion = System.getenv("WEBHOOK_QUEUE_REGION"),
        baseUrl = System.getenv("WEBHOOK_BASE_URL"),
        audience = System.getenv("WEBHOOK_AUDIENCE"),
        serviceAccount = System.getenv("WEBHOOK_SERVICE_ACCOUNT")
    ),
    http = HttpConfig(port = System.getenv("PORT").toInt()),
    gcp = GcpConfig(
        project = System.getenv("GCP_PROJECT"),
        credentialsFileName =
        System.getenv("GCP_CREDENTIALS_FILE") ?: null
    )
)
