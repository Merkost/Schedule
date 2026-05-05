package ru.dvfu.appliances.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.logging.Logger
import io.ktor.serialization.kotlinx.json.json

fun createNotificationHttpClient(
    engine: HttpClientEngine,
    enableLogging: Boolean,
): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) { json(AppJson) }
    install(Logging) {
        level = if (enableLogging) LogLevel.HEADERS else LogLevel.NONE
        logger = Logger.SIMPLE
    }
}
