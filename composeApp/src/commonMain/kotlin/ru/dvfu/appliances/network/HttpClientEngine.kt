package ru.dvfu.appliances.network

import io.ktor.client.engine.HttpClientEngine

expect fun defaultHttpClientEngine(): HttpClientEngine
