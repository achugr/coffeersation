val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val slack_version: String by project
val google_cloud_sdk_version: String by project

val coroutinesVersion by extra("1.7.3")
val kotestVersion by extra("5.6.2")

plugins {
    application
    kotlin("jvm") version "1.9.0"
    id("io.ktor.plugin") version "2.3.3"
}

group = "com.achugr.coffeersation"
version = "0.0.1"

repositories {
    mavenCentral()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.google.cloud:google-cloud-logging-logback")
    implementation("org.slf4j:slf4j-api:2.0.6")
    implementation("com.slack.api:slack-api-client:$slack_version")
    implementation("com.slack.api:slack-app-backend:$slack_version")
    implementation("com.slack.api:bolt:$slack_version")
    implementation("com.slack.api:bolt-ktor:$slack_version")
    implementation("io.ktor:ktor-server-cio:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-jackson:$ktor_version")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.+")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:$coroutinesVersion")
    implementation("com.slack.api:slack-api-model-kotlin-extension:$slack_version")
    implementation("com.slack.api:slack-api-client-kotlin-extension:$slack_version")
    implementation(platform("com.google.cloud:libraries-bom:$google_cloud_sdk_version"))
    implementation("com.google.cloud:google-cloud-tasks")
    implementation("com.google.cloud:google-cloud-datastore")
    implementation("org.quartz-scheduler:quartz:2.3.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-framework-datatest:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
}

ktor {
    fatJar {
        archiveFileName.set("coffeersation.jar")
    }
    application {
        mainClass.set("com.achugr.coffeersation.ApplicationKt")
    }
}