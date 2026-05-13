# Phase 3 — Networking & DataStore — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Prerequisite:** Plans 1+2 executed. `composeApp/` (KMP, Android target only) + `androidApp/` exist; ~60% of code in commonMain; resources, java.time, and KMP-port libs already migrated.

**Goal:** Replace Retrofit + Gson + OkHttp-logging with Ktor 3.x + kotlinx-serialization across all networking, and migrate DataStore to KMP-friendly `datastore-preferences-core` with `expect fun dataStorePath()`.

**Architecture:** A single `Json` instance and a single `HttpClient` factory live in `commonMain`. The HTTP engine is supplied via `expect fun defaultHttpClientEngine()` (OkHttp on Android, Darwin on iOS). The FCM legacy server key is provided through Koin named bindings (`fcmServerKey`, `isDebug`) declared in `AndroidPlatformModule` reading from `BuildConfig`, avoiding cross-module BuildConfig references. DataStore uses `PreferenceDataStoreFactory.createWithPath` driven by an `expect/actual` path provider.

**Tech Stack:** Ktor 3.1.x, kotlinx-serialization-json 1.8.x, datastore-preferences-core 1.1.x, Koin 4.2.x, ktor-client-mock for `commonTest`.

---

## Patterns & Conventions Found

**Existing networking surface** (`app/src/main/java/ru/dvfu/appliances/model/repository/entity/notifications/`):

- `RetrofitInstance.kt:7–21` — companion-object singleton, lazy Retrofit builder with `GsonConverterFactory`; OkHttp interceptor declared in Gradle but unused here.
- `NotificationApi.kt:11–18` — Retrofit `interface NotificationAPI` with `suspend fun postNotification(@Header("Authorization") ..., @Body ...): Response<ResponseBody>`.
- `NotificationConstants.kt:6–8` — `BASE_URL = "https://fcm.googleapis.com"`, `SERVER_KEY` read from `BuildConfig.FCM_SERVER_KEY`.
- `PushNotification.kt`, `Notification.kt`, `NotificationData.kt` — plain `data class`, serialized via Gson through Retrofit converter.
- `NotificationData.kt:4–7` — holds a `NotificationType` enum from `Constants`, serialized as its name by Gson.

**Sole caller** (`compose/utils/NotificationManagerImpl.kt:224–231`):

```kotlin
private suspend fun sendMessage(pushNotification: PushNotification) {
    val key = NotificationConstants.SERVER_KEY
    if (key.isBlank()) return
    RetrofitInstance.api.postNotification(
        authorization = "key=$key",
        notification = pushNotification,
    )
}
```

The result is discarded. Caller only guards on a blank key. This is the full API surface contract to preserve.

**DataStore usage** (`model/datastore/UserDatastoreImpl.kt:14–48`):

- Android `preferencesDataStore` delegate (Android-only API).
- Gson used for `User` serialization to/from a `stringPreferencesKey`.
- `CalendarType` stored by enum name.
- Koin: `single<UserDatastore> { UserDatastoreImpl(androidContext()) }`.

**BuildConfig wiring** (`app/build.gradle.kts:62–68`): both `debug` and `release` blocks emit `buildConfigField("String", "FCM_SERVER_KEY", "\"$fcmServerKey\"")` where `fcmServerKey` is read from `local.properties` or env at configuration time.

---

## Architecture Decision

**Chosen:** Ktor `HttpClient` in `commonMain`, platform engine via `expect/actual`, key flowed through Koin (`single(named("fcmServerKey"))`) declared in `AndroidPlatformModule` reading from `BuildConfig`. Avoids `expect val FcmServerKey: String` referencing AGP-generated classes from `composeApp/androidMain` (which would create a circular `androidApp ↔ composeApp` dependency).

**Why MockEngine over WireMock / OkHttp MockWebServer:**

- Ktor's `MockEngine` runs in-process; available in `commonTest` (no JVM-only dep).
- The same test runs on JVM, Android, and iOS targets in Phase 6 — no test refactor.
- WireMock and MockWebServer are JVM-only.

**JSON config:**

```kotlin
Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    isLenient = true
}
```

- `ignoreUnknownKeys`: FCM responses contain fields we don't model.
- `explicitNulls = false`: matches Gson's default null-skipping behavior.
- `isLenient`: FCM legacy occasionally returns non-standard JSON.

---

## Component Design

**Files to create:**

- `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/network/AppJson.kt` — single `Json` instance.
- `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/network/NotificationHttpClient.kt` — factory function.
- `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/network/NotificationApi.kt` — class wrapping `HttpClient`.
- `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/network/HttpClientEngine.kt` — `expect fun defaultHttpClientEngine()`.
- `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/network/AndroidHttpClientEngine.kt` — actual using OkHttp.
- `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/network/IosHttpClientEngine.kt` — actual using Darwin (compiles, not yet wired since iOS targets land in Phase 6).
- `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/model/repository/entity/notifications/{PushNotification,FcmNotification,NotificationData,NotificationConstants}.kt` — `@Serializable` DTOs.
- `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/model/datastore/DataStorePath.kt` — `expect fun dataStorePath(filename: String): String`.
- `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/model/datastore/DataStorePathAndroid.kt` — actual using `context.filesDir`.
- `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/model/datastore/DataStorePathIos.kt` — stub (Phase 6 fills in).
- `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/di/NetworkModule.kt` — Koin module.
- `composeApp/src/commonTest/kotlin/ru/dvfu/appliances/network/NotificationApiTest.kt` — MockEngine test.

**Files to modify:**

- `gradle/libs.versions.toml` — add Ktor / serialization / datastore-core; remove retrofit / gson / okhttp-logging-interceptor / android-only datastore.
- `composeApp/build.gradle.kts` — wire new deps per source set.
- `androidApp/build.gradle.kts` — keep `FCM_SERVER_KEY` BuildConfig field.
- `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/di/AndroidPlatformModule.kt` — add named Koin bindings.
- `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/di/SharedModules.kt` — include `networkModule`.
- `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/utils/NotificationManagerImpl.kt` — inject `NotificationApi`.
- `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/model/repository/entity/User.kt` — add `@Serializable`.
- `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/model/datastore/UserDatastoreImpl.kt` — rewrite using `PreferenceDataStoreFactory.createWithPath`.

**Files to delete:**

- `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/model/repository/entity/notifications/NotificationApi.kt`
- `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/model/repository/entity/notifications/RetrofitInstance.kt`
- Old androidMain DTOs from notifications package (replaced in commonMain).

---

## Data Flow

```
NotificationManagerImpl.sendMessage(PushNotification)
    │
    ▼
NotificationApi.postNotification(payload)        [commonMain]
    │  client.post("https://fcm.googleapis.com/fcm/send") {
    │    header("Authorization", "key=$fcmServerKey")  [from Koin named]
    │    contentType(ContentType.Application.Json)
    │    setBody(payload)                              [serialized by AppJson]
    │  }
    ▼
HttpClientEngine (OkHttp on Android, Darwin on iOS)
    ▼
FCM legacy endpoint → 200 OK (response discarded)
```

```
UserDatastoreImpl.saveUser(user) → AppJson.encodeToString(user)
                               ↓
                       dataStore.edit { prefs[USER] = jsonString }
                               ↓
       PreferenceDataStoreFactory.createWithPath(dataStorePath("userSettings"))
                               ↓
            Android: context.filesDir/userSettings.preferences_pb
```

---

## Implementation

### Group A — Version catalog & dependency wiring

- [ ] **A1** In `gradle/libs.versions.toml` `[versions]`, add:
  ```toml
  ktor = "3.1.3"
  kotlinx-serialization = "1.8.1"
  datastore = "1.1.2"
  ```
  Change existing `datastore = "1.2.1"` to `"1.1.2"` (KMP-compatible release). Remove `retrofit`, `gson` versions (keep `okhttp` as transitive).

- [ ] **A2** In `[libraries]`, add:
  ```toml
  ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
  ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
  ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
  ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
  ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }
  ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
  ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
  kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
  kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
  androidx-datastore-preferences-core = { module = "androidx.datastore:datastore-preferences-core", version.ref = "datastore" }
  ```

- [ ] **A3** In `[libraries]`, remove `retrofit`, `retrofit-converter-gson`, `okhttp-logging-interceptor`, `gson`, `androidx-datastore-preferences`.

- [ ] **A4** In `[plugins]`, add:
  ```toml
  kotlinx-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
  ```

- [ ] **A5** In `composeApp/build.gradle.kts` `plugins { }`:
  ```kotlin
  alias(libs.plugins.kotlinx.serialization)
  ```

- [ ] **A6** In `composeApp/build.gradle.kts`, source-set deps:
  ```kotlin
  commonMain.dependencies {
      implementation(libs.ktor.client.core)
      implementation(libs.ktor.client.content.negotiation)
      implementation(libs.ktor.client.logging)
      implementation(libs.ktor.serialization.kotlinx.json)
      implementation(libs.kotlinx.serialization.json)
      implementation(libs.androidx.datastore.preferences.core)
  }
  androidMain.dependencies {
      implementation(libs.ktor.client.okhttp)
  }
  iosMain.dependencies {
      implementation(libs.ktor.client.darwin)
  }
  commonTest.dependencies {
      implementation(libs.ktor.client.mock)
      implementation(kotlin("test"))
      implementation(libs.kotlinx.coroutines.test)
  }
  ```
  Remove from any source set: `retrofit`, `retrofit-converter-gson`, `okhttp-logging-interceptor`, `gson`, `androidx-datastore-preferences`.

- [ ] **A7** In `androidApp/build.gradle.kts`, confirm `FCM_SERVER_KEY` BuildConfig field exists in `debug` and `release` blocks (copy pattern from `app/build.gradle.kts:62–68`). No Retrofit/Gson deps remain here.

- [ ] **A8** Run:
  ```bash
  ./gradlew :composeApp:dependencies --configuration commonMainImplementationDependenciesMetadata | grep -i ktor
  ```
  Expected: lines for `ktor-client-core:3.1.3`, `ktor-client-content-negotiation:3.1.3`, etc. Run also:
  ```bash
  ./gradlew :composeApp:dependencies | grep -iE "retrofit|^.*gson"
  ```
  Expected: empty output.

- [ ] **A9 — Commit:**
  ```bash
  git add gradle/libs.versions.toml composeApp/build.gradle.kts androidApp/build.gradle.kts
  git commit -m "chore: add Ktor and serialization deps, drop Retrofit/Gson"
  ```

---

### Group B — TDD: write MockEngine test BEFORE implementation

- [ ] **B1** Create `composeApp/src/commonTest/kotlin/ru/dvfu/appliances/network/NotificationApiTest.kt`:

  ```kotlin
  package ru.dvfu.appliances.network

  import io.ktor.client.HttpClient
  import io.ktor.client.engine.mock.MockEngine
  import io.ktor.client.engine.mock.respond
  import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
  import io.ktor.http.ContentType
  import io.ktor.http.HttpHeaders
  import io.ktor.http.HttpStatusCode
  import io.ktor.http.headersOf
  import io.ktor.serialization.kotlinx.json.json
  import io.ktor.utils.io.ByteReadChannel
  import kotlinx.coroutines.test.runTest
  import ru.dvfu.appliances.model.repository.entity.notifications.FcmNotification
  import ru.dvfu.appliances.model.repository.entity.notifications.NotificationData
  import ru.dvfu.appliances.model.repository.entity.notifications.PushNotification
  import kotlin.test.Test
  import kotlin.test.assertEquals
  import kotlin.test.assertNotNull
  import kotlin.test.assertTrue

  class NotificationApiTest {

      private val testFcmKey = "test-server-key-abc123"
      private val testToken = "device-fcm-token-xyz"

      private fun buildClient(
          handler: io.ktor.client.engine.mock.MockRequestHandleScope.(
              io.ktor.client.request.HttpRequestData,
          ) -> io.ktor.client.request.HttpResponseData,
      ): NotificationApi {
          val engine = MockEngine { request -> handler(request) }
          val client = HttpClient(engine) {
              install(ContentNegotiation) { json(AppJson) }
          }
          return NotificationApi(client, testFcmKey)
      }

      @Test
      fun postNotification_sendsToCorrectUrl() = runTest {
          var capturedPath: String? = null
          val api = buildClient { request ->
              capturedPath = request.url.encodedPath
              respond(
                  ByteReadChannel("""{"success":1}"""),
                  HttpStatusCode.OK,
                  headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
              )
          }
          api.postNotification(PushNotification(testToken, FcmNotification("T", "B"), NotificationData()))
          assertEquals("/fcm/send", capturedPath)
      }

      @Test
      fun postNotification_sendsCorrectAuthorizationHeader() = runTest {
          var capturedAuth: String? = null
          val api = buildClient { request ->
              capturedAuth = request.headers[HttpHeaders.Authorization]
              respond(ByteReadChannel("{}"), HttpStatusCode.OK,
                  headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
          }
          api.postNotification(PushNotification(testToken, FcmNotification("T", "B"), NotificationData()))
          assertEquals("key=$testFcmKey", capturedAuth)
      }

      @Test
      fun postNotification_sendsJsonContentType() = runTest {
          var capturedContentType: String? = null
          val api = buildClient { request ->
              capturedContentType = request.body.contentType?.toString()
              respond(ByteReadChannel("{}"), HttpStatusCode.OK,
                  headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
          }
          api.postNotification(PushNotification(testToken, FcmNotification("T", "B"), NotificationData()))
          assertNotNull(capturedContentType)
          assertTrue(capturedContentType!!.contains("application/json"))
      }

      @Test
      fun postNotification_bodyContainsTokenAndFields() = runTest {
          var capturedBody: String? = null
          val api = buildClient { request ->
              capturedBody = request.body.toByteArray().decodeToString()
              respond(ByteReadChannel("{}"), HttpStatusCode.OK,
                  headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
          }
          api.postNotification(PushNotification(
              to = testToken,
              notification = FcmNotification("Hello", "World"),
              data = NotificationData(notificationType = "MY_EVENT"),
          ))
          assertNotNull(capturedBody)
          assertTrue(capturedBody!!.contains(testToken))
          assertTrue(capturedBody!!.contains("Hello"))
          assertTrue(capturedBody!!.contains("MY_EVENT"))
      }
  }
  ```

- [ ] **B2** Run `./gradlew :composeApp:compileKotlinJvm`. Expected: **fails** with unresolved references to `AppJson`, `NotificationApi`, `FcmNotification`, `PushNotification`, `NotificationData`. This is the red phase.

- [ ] **B3 — Commit:**
  ```bash
  git add composeApp/src/commonTest/
  git commit -m "test: NotificationApi contract test with MockEngine (red)"
  ```

---

### Group C — Serializable DTOs

- [ ] **C1** Create `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/model/repository/entity/notifications/FcmNotification.kt`:
  ```kotlin
  package ru.dvfu.appliances.model.repository.entity.notifications

  import kotlinx.serialization.Serializable

  @Serializable
  data class FcmNotification(
      val title: String,
      val body: String,
  )
  ```

- [ ] **C2** Create `NotificationData.kt`:
  ```kotlin
  package ru.dvfu.appliances.model.repository.entity.notifications

  import kotlinx.serialization.SerialName
  import kotlinx.serialization.Serializable

  @Serializable
  data class NotificationData(
      @SerialName("notificationType")
      val notificationType: String = "DEFAULT",
  )
  ```
  Storing the type as a `String` decouples the DTO from `Constants.NotificationType`. Callers pass `NotificationType.MY_EVENT.name`.

- [ ] **C3** Create `PushNotification.kt`:
  ```kotlin
  package ru.dvfu.appliances.model.repository.entity.notifications

  import kotlinx.serialization.Serializable

  @Serializable
  data class PushNotification(
      val to: String,
      val notification: FcmNotification,
      val data: NotificationData,
  )
  ```

- [ ] **C4** Create `NotificationConstants.kt`:
  ```kotlin
  package ru.dvfu.appliances.model.repository.entity.notifications

  object NotificationConstants {
      const val BASE_URL = "https://fcm.googleapis.com"
      const val CONTENT_TYPE = "application/json"
  }
  ```

- [ ] **C5** Delete old Android-only DTOs from `composeApp/src/androidMain/.../notifications/` (`PushNotification.kt`, `Notification.kt`, `NotificationData.kt`, `NotificationConstants.kt`).

- [ ] **C6** Update `NotificationManagerImpl.kt` callsites: change `Notification(...)` to `FcmNotification(...)`; change `NotificationData(NotificationType.X)` to `NotificationData(notificationType = NotificationType.X.name)`.

- [ ] **C7 — Commit:**
  ```bash
  git add composeApp/src/commonMain/kotlin/ru/dvfu/appliances/model/repository/entity/notifications/
  git rm composeApp/src/androidMain/kotlin/ru/dvfu/appliances/model/repository/entity/notifications/PushNotification.kt
  git rm composeApp/src/androidMain/kotlin/ru/dvfu/appliances/model/repository/entity/notifications/Notification.kt
  git rm composeApp/src/androidMain/kotlin/ru/dvfu/appliances/model/repository/entity/notifications/NotificationData.kt
  git rm composeApp/src/androidMain/kotlin/ru/dvfu/appliances/model/repository/entity/notifications/NotificationConstants.kt
  git commit -m "refactor: replace Gson DTOs with @Serializable in notifications package"
  ```

---

### Group D — AppJson + Ktor client + NotificationApi implementation

- [ ] **D1** Create `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/network/AppJson.kt`:
  ```kotlin
  package ru.dvfu.appliances.network

  import kotlinx.serialization.json.Json

  val AppJson = Json {
      ignoreUnknownKeys = true
      explicitNulls = false
      isLenient = true
  }
  ```

- [ ] **D2** Create `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/network/HttpClientEngine.kt`:
  ```kotlin
  package ru.dvfu.appliances.network

  import io.ktor.client.engine.HttpClientEngine

  expect fun defaultHttpClientEngine(): HttpClientEngine
  ```

- [ ] **D3** Create `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/network/AndroidHttpClientEngine.kt`:
  ```kotlin
  package ru.dvfu.appliances.network

  import io.ktor.client.engine.HttpClientEngine
  import io.ktor.client.engine.okhttp.OkHttp

  actual fun defaultHttpClientEngine(): HttpClientEngine = OkHttp.create()
  ```

- [ ] **D4** Create `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/network/IosHttpClientEngine.kt`:
  ```kotlin
  package ru.dvfu.appliances.network

  import io.ktor.client.engine.HttpClientEngine
  import io.ktor.client.engine.darwin.Darwin

  actual fun defaultHttpClientEngine(): HttpClientEngine = Darwin.create()
  ```

- [ ] **D5** Create `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/network/NotificationHttpClient.kt`:
  ```kotlin
  package ru.dvfu.appliances.network

  import io.ktor.client.HttpClient
  import io.ktor.client.engine.HttpClientEngine
  import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
  import io.ktor.client.plugins.logging.LogLevel
  import io.ktor.client.plugins.logging.Logger
  import io.ktor.client.plugins.logging.Logging
  import io.ktor.serialization.kotlinx.json.json

  fun createNotificationHttpClient(
      engine: HttpClientEngine,
      enableLogging: Boolean,
  ): HttpClient = HttpClient(engine) {
      install(ContentNegotiation) { json(AppJson) }
      install(Logging) {
          level = if (enableLogging) LogLevel.HEADERS else LogLevel.NONE
          logger = Logger.DEFAULT
      }
  }
  ```

- [ ] **D6** Create `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/network/NotificationApi.kt`:
  ```kotlin
  package ru.dvfu.appliances.network

  import io.ktor.client.HttpClient
  import io.ktor.client.request.header
  import io.ktor.client.request.post
  import io.ktor.client.request.setBody
  import io.ktor.http.ContentType
  import io.ktor.http.HttpHeaders
  import io.ktor.http.contentType
  import ru.dvfu.appliances.model.repository.entity.notifications.NotificationConstants
  import ru.dvfu.appliances.model.repository.entity.notifications.PushNotification

  class NotificationApi(
      private val client: HttpClient,
      private val fcmServerKey: String,
  ) {
      suspend fun postNotification(payload: PushNotification) {
          if (fcmServerKey.isBlank()) return
          client.post("${NotificationConstants.BASE_URL}/fcm/send") {
              header(HttpHeaders.Authorization, "key=$fcmServerKey")
              contentType(ContentType.Application.Json)
              setBody(payload)
          }
      }
  }
  ```

- [ ] **D7** Delete old Retrofit files:
  ```bash
  git rm composeApp/src/androidMain/kotlin/ru/dvfu/appliances/model/repository/entity/notifications/NotificationApi.kt
  git rm composeApp/src/androidMain/kotlin/ru/dvfu/appliances/model/repository/entity/notifications/RetrofitInstance.kt
  ```

- [ ] **D8** Run `./gradlew :composeApp:compileKotlinJvm`. Expected: green. Run `./gradlew :composeApp:jvmTest`. Expected: 4/4 tests pass.

- [ ] **D9 — Commit:**
  ```bash
  git add composeApp/src/commonMain/kotlin/ru/dvfu/appliances/network/
  git add composeApp/src/androidMain/kotlin/ru/dvfu/appliances/network/
  git add composeApp/src/iosMain/kotlin/ru/dvfu/appliances/network/
  git commit -m "feat: Ktor HttpClient and NotificationApi replacing Retrofit (tests green)"
  ```

---

### Group E — Koin wiring

- [ ] **E1** Create `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/di/NetworkModule.kt`:
  ```kotlin
  package ru.dvfu.appliances.di

  import org.koin.core.qualifier.named
  import org.koin.dsl.module
  import ru.dvfu.appliances.network.NotificationApi
  import ru.dvfu.appliances.network.createNotificationHttpClient
  import ru.dvfu.appliances.network.defaultHttpClientEngine

  val networkModule = module {
      single {
          createNotificationHttpClient(
              engine = defaultHttpClientEngine(),
              enableLogging = get(named("isDebug")),
          )
      }
      single {
          NotificationApi(
              client = get(),
              fcmServerKey = get(named("fcmServerKey")),
          )
      }
  }
  ```

- [ ] **E2** In `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/di/AndroidPlatformModule.kt`, add inside the platform module:
  ```kotlin
  single(named("fcmServerKey")) { ru.dvfu.appliances.BuildConfig.FCM_SERVER_KEY }
  single(named("isDebug")) { ru.dvfu.appliances.BuildConfig.DEBUG }
  ```
  If the `BuildConfig` import doesn't resolve from `composeApp/androidMain` (depends on Plan 1 module-structure decision), declare the same `buildConfigField` in `composeApp/build.gradle.kts` `android { buildTypes { ... } }` so `composeApp` generates its own BuildConfig too.

- [ ] **E3** In `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/di/SharedModules.kt`, include `networkModule`:
  ```kotlin
  val sharedModules = listOf(repositoryModule, applicationModule, networkModule, useCasesModule)
  ```

- [ ] **E4** Update `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/utils/NotificationManagerImpl.kt`:
  - Add `notificationApi: NotificationApi` to constructor.
  - Replace `sendMessage` body:
    ```kotlin
    private suspend fun sendMessage(pushNotification: PushNotification) {
        notificationApi.postNotification(pushNotification)
    }
    ```
  - Remove `val key = NotificationConstants.SERVER_KEY; if (key.isBlank()) return` (the guard now lives in `NotificationApi.postNotification`).

- [ ] **E5** Update Koin binding for `NotificationManager` in `SharedModules.kt`:
  ```kotlin
  single<NotificationManager> {
      NotificationManagerImpl(
          userDatastore = get(),
          usersRepository = get(),
          getUserUseCase = get(),
          getApplianceUseCase = get(),
          notificationApi = get(),
      )
  }
  ```

- [ ] **E6** Run `./gradlew :androidApp:assembleDebug`. Expected: green.

- [ ] **E7 — Commit:**
  ```bash
  git add composeApp/src/commonMain/kotlin/ru/dvfu/appliances/di/
  git add composeApp/src/androidMain/kotlin/ru/dvfu/appliances/di/
  git add composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/utils/NotificationManagerImpl.kt
  git commit -m "feat: inject NotificationApi and FcmServerKey via Koin"
  ```

---

### Code Review Gate — Ktor swap complete

- [ ] **Gate-1** Dispatch `feature-dev:code-reviewer` agent on the diff for Groups A–E. Reviewer checklist:
  1. No `import retrofit2.*`, `import com.google.gson.*`, or `import okhttp3.logging.*` anywhere.
  2. `NotificationApiTest` is in `commonTest` (not `androidTest`).
  3. `AppJson` is a single top-level `val`, not recreated per call.
  4. `createNotificationHttpClient` accepts engine as parameter (platform-agnostic).
  5. `defaultHttpClientEngine()` has expect/actual for both Android and iOS.
  6. `fcmServerKey.isBlank()` guard inside `NotificationApi`, not scattered.
  7. `NotificationData` stores type as `String` (no Android-coupled enum in DTO).
  8. No comments in code except where there's a non-obvious invariant.

  Address all findings before proceeding to Group F.

---

### Group F — DataStore migration

- [ ] **F1** Create `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/model/datastore/DataStorePath.kt`:
  ```kotlin
  package ru.dvfu.appliances.model.datastore

  expect fun dataStorePath(filename: String): String
  ```

- [ ] **F2** Create `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/model/datastore/DataStorePathAndroid.kt`:
  ```kotlin
  package ru.dvfu.appliances.model.datastore

  import android.content.Context
  import org.koin.core.context.GlobalContext

  actual fun dataStorePath(filename: String): String {
      val context = GlobalContext.get().get<Context>()
      return context.filesDir.resolve("$filename.preferences_pb").absolutePath
  }
  ```

- [ ] **F3** Create `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/model/datastore/DataStorePathIos.kt`:
  ```kotlin
  package ru.dvfu.appliances.model.datastore

  actual fun dataStorePath(filename: String): String = ""
  ```
  Phase 6 fills this in with `NSDocumentDirectory`.

- [ ] **F4** Rewrite `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/model/datastore/UserDatastoreImpl.kt`:
  ```kotlin
  package ru.dvfu.appliances.model.datastore

  import androidx.datastore.core.DataStore
  import androidx.datastore.preferences.core.PreferenceDataStoreFactory
  import androidx.datastore.preferences.core.Preferences
  import androidx.datastore.preferences.core.edit
  import androidx.datastore.preferences.core.stringPreferencesKey
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.catch
  import kotlinx.coroutines.flow.map
  import kotlinx.serialization.encodeToString
  import okio.Path.Companion.toPath
  import ru.dvfu.appliances.compose.calendars.CalendarType
  import ru.dvfu.appliances.model.repository.entity.User
  import ru.dvfu.appliances.network.AppJson

  class UserDatastoreImpl : UserDatastore {

      private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
          produceFile = { dataStorePath("userSettings").toPath() },
      )

      private companion object {
          val USER = stringPreferencesKey("USER")
          val CALENDAR_TYPE = stringPreferencesKey("CALENDAR")
      }

      override val getCurrentUser: Flow<User> = dataStore.data.map { prefs ->
          prefs[USER]?.let { AppJson.decodeFromString<User>(it) } ?: User()
      }

      override val getCalendarType: Flow<CalendarType> = dataStore.data.map { prefs ->
          CalendarType.valueOf(prefs[CALENDAR_TYPE] ?: CalendarType.MONTH.name)
      }.catch { e ->
          if (e is IllegalArgumentException) emit(CalendarType.MONTH)
      }

      override suspend fun saveCalendarType(calendarType: CalendarType) {
          dataStore.edit { prefs -> prefs[CALENDAR_TYPE] = calendarType.name }
      }

      override suspend fun saveUser(user: User) {
          dataStore.edit { prefs -> prefs[USER] = AppJson.encodeToString(user) }
      }
  }
  ```

- [ ] **F5** In `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/model/repository/entity/User.kt`, add `@Serializable`:
  ```kotlin
  import kotlinx.serialization.Serializable

  @Serializable
  data class User(...)
  ```
  Verify Plan 2 already removed `@Parcelize`. If not, remove it now (commonMain cannot use Android Parcelize).

- [ ] **F6** Update Koin: change `single<UserDatastore> { UserDatastoreImpl(androidContext()) }` to `single<UserDatastore> { UserDatastoreImpl() }` (Context resolved internally via Koin in `dataStorePath`).

- [ ] **F7** Run `./gradlew :androidApp:assembleDebug`. Expected: green. Run `./gradlew :composeApp:jvmTest`. Expected: 4/4 still green.

- [ ] **F8 — Commit:**
  ```bash
  git add composeApp/src/commonMain/kotlin/ru/dvfu/appliances/model/datastore/
  git add composeApp/src/androidMain/kotlin/ru/dvfu/appliances/model/datastore/
  git add composeApp/src/iosMain/kotlin/ru/dvfu/appliances/model/datastore/
  git add composeApp/src/commonMain/kotlin/ru/dvfu/appliances/model/repository/entity/User.kt
  git commit -m "feat: migrate DataStore to KMP-compatible datastore-preferences-core"
  ```

---

### Group G — Cleanup verification

- [ ] **G1** Run:
  ```bash
  ./gradlew :composeApp:dependencies --configuration androidDebugRuntimeClasspath 2>&1 | grep -E "retrofit|gson|logging-interceptor"
  ```
  Expected: empty. If transitive deps remain, add `configurations.all { exclude(group = "com.squareup.retrofit2"); exclude(group = "com.google.code.gson") }` to `composeApp/build.gradle.kts`.

- [ ] **G2** Run:
  ```bash
  grep -r "import retrofit2\|import com.google.gson\|import okhttp3.logging" composeApp/src/
  ```
  Expected: no results.

- [ ] **G3** Run `./gradlew :composeApp:jvmTest`. Expected: 4/4 green.

- [ ] **G4 — Commit (only if anything changed):**
  ```bash
  git add composeApp/build.gradle.kts
  git commit -m "chore: verify no direct OkHttp logging or Gson deps remain"
  ```

---

### Group H — Smoke-test

**Prerequisites:**
- `local.properties` has `FCM_SERVER_KEY=<real key>`.
- Physical Android device with debug build installed.
- Logcat filter on the Ktor logging output.

**FCM push test:**

1. `./gradlew :androidApp:installDebug`.
2. Launch app, sign in, FCM token uploaded.
3. From a second account, trigger a notification (create booking on appliance where signed-in user is superuser).
4. Logcat shows:
   ```
   [Ktor] --> POST https://fcm.googleapis.com/fcm/send
   [Ktor] Authorization: key=<value>
   [Ktor] Content-Type: application/json
   [Ktor] <-- 200 https://fcm.googleapis.com/fcm/send
   ```
5. Push arrives on target device.
6. Tap → app opens to correct screen.

**DataStore test:**

1. Launch app. Switch calendar from MONTH to WEEK.
2. `adb shell am force-stop ru.dvfu.appliances`.
3. Relaunch. Calendar still WEEK ✅.
4. Sign in. Force-stop. Relaunch. User session restored ✅.

**If FCM push fails:** check `FCM_SERVER_KEY` not blank in BuildConfig; check Firebase Console → Cloud Messaging → if legacy API shows disabled, this is the §4.4 spec time-bomb firing — escalate to FCM HTTP v1 migration follow-up.

---

### Group I — Final Phase Review

- [ ] **I1** Dispatch `feature-dev:code-reviewer` on the full diff against pre-Phase-3 baseline. Reviewer checklist:
  - No retrofit/gson/okhttp-logging imports anywhere.
  - `NotificationApiTest` runs in `commonTest`, all 4 assertions valid.
  - `AppJson` used consistently in DataStore + Ktor (one instance).
  - `dataStorePath` has expect + actuals for android and ios.
  - `PreferenceDataStoreFactory.createWithPath` used (not the deprecated `preferencesDataStore` delegate).
  - `User` is `@Serializable`; no Gson reference in UserDatastoreImpl.
  - `NotificationApi.postNotification` guards on blank key.
  - Koin named bindings `fcmServerKey` and `isDebug` declared in `AndroidPlatformModule`, consumed in `NetworkModule`.
  - CLAUDE.md constraints: no M2 imports, no superfluous comments, all versions in `libs.versions.toml`.

- [ ] **I2** All Group H smoke tests pass.

- [ ] **I3** `./gradlew :androidApp:assembleRelease` succeeds.

- [ ] **I4** File a follow-up issue: "Migrate NotificationApi from FCM legacy to HTTP v1" referencing spec §4.4. Tag `phase-9-followup`.

- [ ] **I5 — Commit:**
  ```bash
  git commit --allow-empty -m "chore: phase 3 complete — Ktor+DataStore migration verified"
  ```

---

## Critical Details

**Error handling:** Ktor 3.x `expectSuccess` defaults to `false`. The original Retrofit impl discarded responses; preserving that behavior. If Phase 5 introduces error reporting, set `expectSuccess = true` then.

**Logging in release:** `enableLogging = get(named("isDebug"))` ensures `LogLevel.NONE` in release builds — the `Authorization` header (FCM key) never appears in production Logcat. Security-critical.

**DataStore migration path safety:** the new `createWithPath` factory writes to `context.filesDir/userSettings.preferences_pb`. The old `preferencesDataStore` delegate wrote to `context.filesDir/datastore/userSettings.preferences_pb`. Different paths — on upgrade from old build, existing DataStore data is not found and users get defaults. Acceptable: cached user is re-fetched from Firebase on sign-in; calendar resets to MONTH. If preference-loss is unacceptable, add a one-time migration in `UserDatastoreImpl` reading from the old path. Document in PR description.

**Why `MockEngine` over WireMock:** runs in `commonTest` on all targets without a JVM dependency. Same tests work on iOS in Phase 6 with no changes.
