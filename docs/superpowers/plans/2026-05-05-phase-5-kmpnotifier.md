# Phase 5 — KMPNotifier Swap-In — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Prerequisites:** Plans 1–4 complete — `composeApp/` + `androidApp/` exist; ~95% of code in commonMain; Gitlive Firebase (Auth/Firestore/RTDB/Storage/Analytics/Crashlytics/Config) in commonMain; `firebase-messaging` intentionally absent; `MyFirebaseMessagingService.kt` still in androidMain.

**Goal:** Replace the Android-only `MyFirebaseMessagingService` and `firebase-messaging` SDK with KMPNotifier in commonMain, deleting Glide as a side-effect.

**Architecture:** A single `AppNotifierListener` (commonMain) implements `NotifierManager.Listener`. Token refreshes forward to `UsersRepository.setNewMessagingToken`. Click events route through a process-lifetime `NotificationNavRouterDelegate` that holds a nullable reference to the active `NavController`-backed router; calls before UI attach are silently dropped. All five existing Android notification channels are preserved exactly — IDs untouched so user preferences survive.

**Tech Stack:** KMPNotifier `io.github.mirzemehdi:kmpnotifier:1.6.1`, Firebase Cloud Messaging (via KMPNotifier), Ktor (server-side legacy send — in commonMain since Plan 3, unchanged).

---

## Codebase baseline (post-Plan-4 state)

### Notification channels — must be preserved exactly

User notification preferences in Android Settings are keyed to channel IDs. Any ID change silently breaks user preferences. Channels are created by `ScheduleApplication.createNotificationChannels()`:

| Enum value | `channelId` | `title` (Russian) | `importance` |
|---|---|---|---|
| `DEFAULT` | `com.dvfu.appliances` | `Основные` | `IMPORTANCE_DEFAULT` |
| `APPLIANCE` | `channel_appliance` | `Приборы` | `IMPORTANCE_DEFAULT` |
| `EVENT` | `channel_event` | `События` | `IMPORTANCE_DEFAULT` |
| `NEW_EVENT` | `channel_new_event` | `Новые события` | `IMPORTANCE_DEFAULT` |
| `MY_EVENT` | `channel_my_event` | `Мои события` | `IMPORTANCE_DEFAULT` |

All five channels continue to be created by `ScheduleApplication` — KMPNotifier uses whichever channel ID is specified in the incoming FCM payload's `notification.android.channelId` field.

### Token upload

`UsersRepository.setNewMessagingToken(token: String)` calls `updateCurrentUserField(mapOf("msgToken" to token))`. After Plan 4 refactor lives in `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/model/repository/UsersRepository.kt`.

### Topic subscription

`Firebase.messaging.subscribeToTopic("weather")` in `NotificationManagerImpl.subscribeCurrentUser()` — only `Firebase.messaging` call in codebase. Replaced with KMPNotifier API.

### Service registration

`AndroidManifest.xml` has `<service android:name=".MyFirebaseMessagingService" android:exported="false">` with `com.google.firebase.MESSAGING_EVENT` intent-filter. KMPNotifier's AAR merges its own `FirebaseMessagingService` subclass automatically — manual entry must be removed to avoid duplicate service registration.

### Glide

Used only in `MyFirebaseMessagingService.showNotification()` to load `notification.imageUrl`. With service deleted, zero callers remain. Server code in `NotificationManagerImpl` does not set `imageUrl` in any push — image-in-notification is never active. Removing Glide is a non-regression.

### Koin bindings to remove

In `SharedModules.kt`:
- `single { MyFirebaseMessagingService() }`
- `single { FirebaseMessagingViewModel(usersRepository = get()) }`

---

## Deep-link payload schema

KMPNotifier delivers the FCM `data` map to `onNotificationClicked(data: PayloadData)` where `PayloadData = Map<String, *>`.

| Key | Type | Values | Nav destination |
|---|---|---|---|
| `notificationType` | `String` | `MY_EVENT` | `MainDestinations.BOOKING_LIST` |
| `notificationType` | `String` | `NEW_EVENT` | `MainDestinations.BOOKING_LIST` |
| `notificationType` | `String` | `APPLIANCE` | `MainDestinations.APPLIANCES_ROUTE` |
| `notificationType` | `String` | `EVENT` | `MainDestinations.HOME_ROUTE` |
| `notificationType` | `String` | `DEFAULT` or absent | `MainDestinations.HOME_ROUTE` |
| `targetId` | `String` | optional | reserved for future, ignored in Plan 5 |

`NotificationData.kt` serializes `notificationType` as `enum.name()`. The key name `"notificationType"` is the canonical contract between server and client.

---

## KMPNotifier version decision

**Pinned: `1.6.1`** (released 2025-12-09).

Rationale: stable 1.x API, Kotlin 2.2.21 (compatible with project's 2.3.20 compiler), no breaking changes since 1.5.x. Upgrade path: minor bumps are safe — verify with `./gradlew :composeApp:compileKotlinAndroid`. Major bumps require auditing `NotifierManager`, `NotificationPlatformConfiguration`, and `Listener` interfaces.

Transitive dep check: KMPNotifier 1.6.1 depends on `koin-core:4.2.0-beta2` and `kotlinx-coroutines-core:1.10.2`. Critical: `firebase-messaging` must NOT appear transitively — verify with `./gradlew :composeApp:dependencies --configuration commonMainImplementation | grep firebase-messaging`.

---

## Implementation

### A — Add dependency and initialize

- [ ] **A1** Add KMPNotifier to `gradle/libs.versions.toml`

  In `[versions]`:
  ```toml
  kmpnotifier = "1.6.1"
  ```
  In `[libraries]`:
  ```toml
  kmpnotifier = { module = "io.github.mirzemehdi:kmpnotifier", version.ref = "kmpnotifier" }
  ```
  Commit: `build: add kmpnotifier 1.6.1 to version catalog`

- [ ] **A2** Add KMPNotifier to `composeApp/build.gradle.kts` commonMain

  Inside `commonMain.dependencies { }`:
  ```kotlin
  implementation(libs.kmpnotifier)
  ```
  Verify no transitive `firebase-messaging`:
  ```bash
  ./gradlew :composeApp:dependencies --configuration commonMainImplementation | grep firebase-messaging
  ```
  Expected: empty. If hit (unlikely), exclude:
  ```kotlin
  implementation(libs.kmpnotifier) {
      exclude(group = "com.google.firebase", module = "firebase-messaging")
  }
  ```
  Commit: `build: wire kmpnotifier to composeApp commonMain`

- [ ] **A3** Initialize KMPNotifier in `androidApp/src/main/kotlin/ScheduleApplication.kt`

  After existing `createNotificationChannels()`:
  ```kotlin
  NotifierManager.initialize(
      NotificationPlatformConfiguration.Android(
          notificationIconResId = R.drawable.ic_notification,
          showPushNotification = true,
      )
  )
  ```
  `ic_notification` must exist in `androidApp/src/main/res/drawable/`. If not, create a 24dp vector drawable tracing the launcher icon foreground, or use `R.mipmap.ic_launcher` as fallback.

  Imports: `com.mmk.kmpnotifier.notification.NotifierManager`, `com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration`.

  Commit: `feat(android): initialize KMPNotifier in ScheduleApplication`

- [ ] **A4** iosMain placeholder

  Create `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/notifications/IosNotifierInit.kt`:
  ```kotlin
  package ru.dvfu.appliances.notifications

  internal object IosNotifierInit
  ```
  Plan 6 replaces this with the Swift-side init call.

  Commit: `feat(ios): stub iosMain notifier init placeholder`

---

### B — Write tests first (TDD)

- [ ] **B1** Create `FakeUsersRepository` test double

  File: `composeApp/src/commonTest/kotlin/ru/dvfu/appliances/notifications/FakeUsersRepository.kt`

  ```kotlin
  package ru.dvfu.appliances.notifications

  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.emptyFlow
  import ru.dvfu.appliances.model.repository.UsersRepository
  import ru.dvfu.appliances.model.repository.entity.User
  import ru.dvfu.appliances.ui.Progress

  open class FakeUsersRepository : UsersRepository {
      override val currentUser: Flow<User?> = emptyFlow()
      override suspend fun getUsers(): Flow<List<User>> = emptyFlow()
      override suspend fun logoutCurrentUser(): Flow<Boolean> = emptyFlow()
      override suspend fun addNewUser(user: User): StateFlow<Progress> = MutableStateFlow(Progress.Complete)
      override suspend fun getUser(userId: String): Result<User> = Result.failure(NotImplementedError())
      override suspend fun updateUserField(userId: String, data: Map<String, Any>): Result<Unit> = Result.success(Unit)
      override suspend fun updateCurrentUserField(data: Map<String, Any>) {}
      override suspend fun setUserListener(user: User) {}
      override suspend fun setNewProfileData(userId: String, data: Map<String, Any>): Result<Unit> = Result.success(Unit)
  }
  ```
  Commit: `test: FakeUsersRepository test double for notifications tests`

- [ ] **B2** Write `AppNotifierListenerTest` (red until C2)

  File: `composeApp/src/commonTest/kotlin/ru/dvfu/appliances/notifications/AppNotifierListenerTest.kt`

  ```kotlin
  package ru.dvfu.appliances.notifications

  import kotlinx.coroutines.test.runTest
  import kotlin.test.Test
  import kotlin.test.assertEquals
  import kotlin.test.assertTrue

  class AppNotifierListenerTest {

      private val capturedTokens = mutableListOf<String>()
      private val capturedRoutes = mutableListOf<String?>()

      private val usersRepository = object : FakeUsersRepository() {
          override suspend fun setNewMessagingToken(token: String) {
              capturedTokens.add(token)
          }
      }

      private val router = object : NotificationNavRouter {
          override fun navigateTo(route: String?) { capturedRoutes.add(route) }
      }

      private val listener = AppNotifierListener(
          usersRepository = usersRepository,
          router = router,
      )

      @Test
      fun onNewToken_forwardsTokenToRepository() = runTest {
          listener.onNewToken("fcm-token-xyz")
          assertEquals(listOf("fcm-token-xyz"), capturedTokens)
      }

      @Test
      fun onNewToken_blankToken_isNotForwarded() = runTest {
          listener.onNewToken("")
          assertTrue(capturedTokens.isEmpty())
      }

      @Test
      fun onNotificationClicked_myEvent_navigatesToBookingList() {
          listener.onNotificationClicked(mapOf("notificationType" to "MY_EVENT"))
          assertEquals("booking_list_screen", capturedRoutes.last())
      }

      @Test
      fun onNotificationClicked_newEvent_navigatesToBookingList() {
          listener.onNotificationClicked(mapOf("notificationType" to "NEW_EVENT"))
          assertEquals("booking_list_screen", capturedRoutes.last())
      }

      @Test
      fun onNotificationClicked_appliance_navigatesToAppliances() {
          listener.onNotificationClicked(mapOf("notificationType" to "APPLIANCE"))
          assertEquals("appliances", capturedRoutes.last())
      }

      @Test
      fun onNotificationClicked_unknownType_navigatesToHome() {
          listener.onNotificationClicked(mapOf("notificationType" to "UNKNOWN_FUTURE_TYPE"))
          assertEquals("home", capturedRoutes.last())
      }

      @Test
      fun onNotificationClicked_missingKey_navigatesToHome() {
          listener.onNotificationClicked(emptyMap())
          assertEquals("home", capturedRoutes.last())
      }
  }
  ```
  Commit: `test: AppNotifierListener tests (TDD red)`

---

### C — Define commonMain listener components

- [ ] **C1** Create `NotificationNavRouter` interface

  File: `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/notifications/NotificationNavRouter.kt`
  ```kotlin
  package ru.dvfu.appliances.notifications

  interface NotificationNavRouter {
      fun navigateTo(route: String?)
  }
  ```
  Commit: `feat: add NotificationNavRouter interface`

- [ ] **C2** Implement `AppNotifierListener`

  File: `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/notifications/AppNotifierListener.kt`
  ```kotlin
  package ru.dvfu.appliances.notifications

  import com.mmk.kmpnotifier.notification.NotifierManager
  import com.mmk.kmpnotifier.notification.PayloadData
  import kotlinx.coroutines.CoroutineScope
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.SupervisorJob
  import kotlinx.coroutines.launch
  import ru.dvfu.appliances.compose.MainDestinations
  import ru.dvfu.appliances.model.repository.UsersRepository
  import ru.dvfu.appliances.model.utils.Constants.NotificationType

  class AppNotifierListener(
      private val usersRepository: UsersRepository,
      private val router: NotificationNavRouter,
  ) : NotifierManager.Listener {

      private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

      override fun onNewToken(token: String) {
          if (token.isBlank()) return
          scope.launch {
              usersRepository.setNewMessagingToken(token)
          }
      }

      override fun onNotificationClicked(data: PayloadData) {
          router.navigateTo(resolveRoute(data["notificationType"] as? String))
      }

      private fun resolveRoute(notificationType: String?): String {
          val type = notificationType?.let {
              runCatching { NotificationType.valueOf(it) }.getOrNull()
          }
          return when (type) {
              NotificationType.MY_EVENT, NotificationType.NEW_EVENT -> MainDestinations.BOOKING_LIST
              NotificationType.APPLIANCE -> MainDestinations.APPLIANCES_ROUTE
              NotificationType.EVENT, NotificationType.DEFAULT, null -> MainDestinations.HOME_ROUTE
          }
      }
  }
  ```

  `NotifierManager.Listener` has default no-op implementations for `onPushNotification`, `onPayloadData`, `onPushNotificationWithPayloadData` since 1.5.x — no need to override. If compiler requires them, add empty-body overrides.

  Commit: `feat: implement AppNotifierListener (TDD green)`

- [ ] **C3** Implement `NotificationNavRouterDelegate`

  File: `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/notifications/NotificationNavRouterDelegate.kt`
  ```kotlin
  package ru.dvfu.appliances.notifications

  import kotlinx.coroutines.flow.MutableStateFlow

  object NotificationNavRouterDelegate : NotificationNavRouter {
      private val delegate = MutableStateFlow<NotificationNavRouter?>(null)

      fun attach(router: NotificationNavRouter) { delegate.value = router }
      fun detach() { delegate.value = null }

      override fun navigateTo(route: String?) {
          delegate.value?.navigateTo(route)
      }
  }
  ```

  Process-lifetime singleton holding a nullable reference to the active router. Calls before UI attach are silently dropped. Plan 6 can add a pending-route buffer if cold-start deep-linking is needed.

  Commit: `feat: NotificationNavRouterDelegate process-lifetime router bridge`

- [ ] **C4** Platform routers

  File: `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/notifications/NavControllerNotificationRouter.kt`
  ```kotlin
  package ru.dvfu.appliances.notifications

  import androidx.navigation.NavController

  class NavControllerNotificationRouter(
      private val navController: NavController,
  ) : NotificationNavRouter {
      override fun navigateTo(route: String?) {
          if (route == null) return
          navController.navigate(route) { launchSingleTop = true }
      }
  }
  ```

  File: `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/notifications/NavControllerNotificationRouter.kt`
  ```kotlin
  package ru.dvfu.appliances.notifications

  class NavControllerNotificationRouter : NotificationNavRouter {
      override fun navigateTo(route: String?) {}
  }
  ```
  Commit: `feat: NavControllerNotificationRouter android + ios stub`

- [ ] **C5** Wire listener and router delegate in `App()`

  In `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/App.kt`, at start of `App()` body:
  ```kotlin
  val notifierListener: AppNotifierListener = koinInject()
  LaunchedEffect(Unit) {
      NotifierManager.addListener(notifierListener)
  }

  val appStateHolder = rememberAppStateHolder()
  DisposableEffect(appStateHolder.navController) {
      val router = NavControllerNotificationRouter(appStateHolder.navController)
      NotificationNavRouterDelegate.attach(router)
      onDispose { NotificationNavRouterDelegate.detach() }
  }
  ```

  `LaunchedEffect(Unit)` ensures `addListener` fires once per composition lifetime. `DisposableEffect` re-attaches if `NavController` changes.

  Commit: `feat: wire AppNotifierListener and NavRouterDelegate in App()`

- [ ] **C6** Update Koin bindings

  In `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/di/SharedModules.kt`:
  - Remove: `single { MyFirebaseMessagingService() }`
  - Remove: `single { FirebaseMessagingViewModel(usersRepository = get()) }`
  - Add: `single { AppNotifierListener(usersRepository = get(), router = NotificationNavRouterDelegate) }`

  `NotificationNavRouterDelegate` is an `object` — passed directly, no Koin registration.

  Commit: `refactor(koin): replace legacy messaging bindings with AppNotifierListener`

---

### Mid-phase code review gate

Dispatch `feature-dev:code-reviewer` on commits A1–C6. Focus:

1. `NotifierManager.addListener(notifierListener)` is inside `LaunchedEffect(Unit)` — confirm it cannot fire more than once.
2. `AppNotifierListener.scope` uses `SupervisorJob` — failed `setNewMessagingToken` does not cancel scope or block future calls.
3. `NotificationNavRouterDelegate.attach/detach` lifecycle — `DisposableEffect` disposes cleanly; no stale `NavController` reference held.
4. No reference to `MyFirebaseMessagingService` or `FirebaseMessagingViewModel` in Koin or tests.
5. `AppNotifierListenerTest` is green — `./gradlew :composeApp:jvmTest`.

---

### D — Delete legacy service

- [ ] **D1** Delete legacy files
  ```bash
  git rm composeApp/src/androidMain/kotlin/ru/dvfu/appliances/MyFirebaseMessagingService.kt
  git rm composeApp/src/androidMain/kotlin/ru/dvfu/appliances/model/FirebaseMessagingViewModel.kt
  ```
  Verify:
  ```bash
  grep -r "MyFirebaseMessagingService\|FirebaseMessagingViewModel" composeApp/src/
  ```
  Expected: zero matches.

  Commit: `feat: delete MyFirebaseMessagingService and FirebaseMessagingViewModel`

- [ ] **D2** Remove service entry from `androidApp/src/main/AndroidManifest.xml`

  Remove block:
  ```xml
  <service
      android:name=".MyFirebaseMessagingService"
      android:exported="false">
      <intent-filter>
          <action android:name="com.google.firebase.MESSAGING_EVENT"/>
      </intent-filter>
  </service>
  ```
  KMPNotifier registers its own service via AAR manifest merger.

  Commit: `feat(manifest): remove MyFirebaseMessagingService declaration`

- [ ] **D3** Remove Glide and firebase-messaging from build graph

  In `libs.versions.toml`:
  - `[versions]`: remove `glide = "5.0.5"`
  - `[libraries]`: remove `glide` and `firebase-messaging`

  In `composeApp/build.gradle.kts` androidMain deps: remove `implementation(libs.glide)`.

  If `firebase-messaging` already removed in Plan 4, skip that part.

  Verify:
  ```bash
  grep -r "glide\|Glide\|bumptech\|firebase-messaging" composeApp/ androidApp/ gradle/
  ```
  Expected: zero matches in source/build files.

  Commit: `build: remove glide and firebase-messaging from build graph`

---

### E — Permission flow

- [ ] **E1** Request notification permission on first home screen entry

  In `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/ScheduleApp.kt`, inside the home navigation graph, top-level composable:
  ```kotlin
  LaunchedEffect(Unit) {
      NotifierManager.requestPermission()
  }
  ```

  KMPNotifier handles version checks internally — no-op on Android <13. iOS gets handled in Plan 6.

  Commit: `feat: request notification permission on home screen entry`

- [ ] **E2** Add notification rationale strings

  `composeApp/src/commonMain/composeResources/values/strings.xml`:
  ```xml
  <string name="notification_permission_rationale">Enable notifications to stay updated on your bookings and appliance changes.</string>
  ```

  `composeApp/src/commonMain/composeResources/values-ru/strings.xml`:
  ```xml
  <string name="notification_permission_rationale">Разрешите уведомления, чтобы получать информацию об изменениях бронирований и приборов.</string>
  ```

  Commit: `feat: notification rationale strings en + ru`

---

### F — Topic subscription parity

- [ ] **F1** Replace `Firebase.messaging.subscribeToTopic`

  In `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/utils/NotificationManagerImpl.kt` `subscribeCurrentUser()`:

  Replace:
  ```kotlin
  Firebase.messaging.subscribeToTopic("weather")
      .addOnCompleteListener { ... }
  ```
  With:
  ```kotlin
  NotifierManager.getPushNotifier().subscribeToTopic("weather")
  ```
  `subscribeToTopic` is a suspend function — existing `scope.launch { }` provides the coroutine context. Remove unused `addOnCompleteListener` and `Firebase.messaging` import.

  Commit: `refactor: replace Firebase messaging subscribeToTopic with KMPNotifier`

- [ ] **F2** Audit commonMain
  ```bash
  grep -r "com.google.firebase.messaging\|firebase\.messaging\|FirebaseMessaging" composeApp/src/commonMain/
  ```
  Expected: zero matches.

  Commit: `chore: confirm no firebase-messaging SDK imports in commonMain`

---

### G — Build verification

- [ ] **G1** Android debug build
  ```bash
  ./gradlew :androidApp:assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL`. No duplicate `FirebaseMessagingService` manifest warning.

- [ ] **G2** Run commonTest
  ```bash
  ./gradlew :composeApp:jvmTest
  ```
  All seven `AppNotifierListenerTest` cases pass.

- [ ] **G3** Transitive dep audit
  ```bash
  ./gradlew :composeApp:dependencies --configuration commonMainImplementation | grep -E "firebase-messaging|glide"
  ```
  Expected: empty.

---

### H — Smoke test (real Android device)

- [ ] **H1** Clean install
  ```bash
  adb uninstall ru.dvfu.appliances
  ./gradlew :androidApp:installDebug
  ```

- [ ] **H2** Token upload — sign in. Open Firebase Console → Firestore → `users` → test user doc. Confirm `msgToken` field has valid token.

- [ ] **H3** Background push — backgound app. Firebase Console → Cloud Messaging → send test → token from H2. Notification appears in tray.

- [ ] **H4** Click routing — send push with `notificationType=MY_EVENT`. Tap → opens booking list. Repeat with `APPLIANCE` → appliances screen.

- [ ] **H5** Foreground push — bring app to foreground. Send push. Notification displays (because `showPushNotification = true`).

- [ ] **H6** Token refresh — `adb shell pm clear ru.dvfu.appliances`. Sign in. New `msgToken` in Firestore.

---

### I — Final code review gate

Dispatch `feature-dev:code-reviewer` on full Phase 5 diff. Focus:

1. **No duplicate listener registration** — `LaunchedEffect(Unit)` fires once per `App()` lifecycle; `koinInject()` returns same singleton across recompositions.
2. **Channel ID consistency** — `R.drawable.ic_notification` icon used; five existing channels unchanged; no new channel ID introduced.
3. **All old service references gone** — `grep -r "MyFirebaseMessagingService\|FirebaseMessagingViewModel\|bumptech.glide" composeApp/ androidApp/ gradle/` returns zero hits.
4. **Token upload on refresh** — `onNewToken` skips blank tokens, launches in `SupervisorJob` scope, calls `setNewMessagingToken`.
5. **No firebase-messaging** — `grep "firebase-messaging" gradle/libs.versions.toml composeApp/build.gradle.kts` returns zero hits.

---

## Files

### To create

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/notifications/AppNotifierListener.kt` | KMPNotifier Listener |
| `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/notifications/NotificationNavRouter.kt` | Decoupling interface |
| `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/notifications/NotificationNavRouterDelegate.kt` | Process-lifetime router bridge |
| `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/notifications/NavControllerNotificationRouter.kt` | Android nav-compose router |
| `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/notifications/NavControllerNotificationRouter.kt` | iOS stub |
| `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/notifications/IosNotifierInit.kt` | iOS placeholder |
| `composeApp/src/commonTest/kotlin/ru/dvfu/appliances/notifications/AppNotifierListenerTest.kt` | 7 unit tests |
| `composeApp/src/commonTest/kotlin/ru/dvfu/appliances/notifications/FakeUsersRepository.kt` | Test double |

### To delete

- `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/MyFirebaseMessagingService.kt`
- `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/model/FirebaseMessagingViewModel.kt`

### To modify

| File | Change |
|---|---|
| `gradle/libs.versions.toml` | +kmpnotifier, −glide, −firebase-messaging |
| `composeApp/build.gradle.kts` | +kmpnotifier in commonMain, −glide in androidMain |
| `androidApp/src/main/kotlin/ScheduleApplication.kt` | +`NotifierManager.initialize(...)` |
| `androidApp/src/main/AndroidManifest.xml` | −`MyFirebaseMessagingService` `<service>` block |
| `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/App.kt` | +listener `LaunchedEffect` + router `DisposableEffect` |
| `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/di/SharedModules.kt` | −legacy bindings, +`AppNotifierListener` |
| `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/utils/NotificationManagerImpl.kt` | Replace `Firebase.messaging.subscribeToTopic` |
| `composeApp/src/commonMain/composeResources/values/strings.xml` | +rationale en |
| `composeApp/src/commonMain/composeResources/values-ru/strings.xml` | +rationale ru |

---

## iOS deferred to Plan 6

- `AppDelegate.swift` → `NotifierManager.initialize(NotificationPlatformConfiguration.iOS(...))`
- APNs token forward via KMPNotifier helper
- `iosApp.entitlements` → `aps-environment`
- `iosMain/NavControllerNotificationRouter.kt` → real impl
- `UNNotificationServiceExtension` for rich notifications: not currently needed (server doesn't set `imageUrl`)

---

## Image-loading note

Deleted `MyFirebaseMessagingService.showNotification()` used Glide for `notification.imageUrl`. Server `NotificationManagerImpl` does not set this — capability never active. KMPNotifier's `showPushNotification = true` uses FCM SDK's built-in display (handles `notification.image` from FCM v1 natively on Android 12+). For future image-in-notification, use KMPNotifier's `NotificationImage.Url(...)` or upgrade server to FCM HTTP v1.

---

## Commit sequence summary

| # | Commit | Task |
|---|---|---|
| 1 | `build: add kmpnotifier 1.6.1 to version catalog` | A1 |
| 2 | `build: wire kmpnotifier to composeApp commonMain` | A2 |
| 3 | `feat(android): initialize KMPNotifier in ScheduleApplication` | A3 |
| 4 | `feat(ios): stub iosMain notifier init placeholder` | A4 |
| 5 | `test: FakeUsersRepository test double for notifications tests` | B1 |
| 6 | `test: AppNotifierListener tests (TDD red)` | B2 |
| 7 | `feat: add NotificationNavRouter interface` | C1 |
| 8 | `feat: implement AppNotifierListener (TDD green)` | C2 |
| 9 | `feat: NotificationNavRouterDelegate process-lifetime router bridge` | C3 |
| 10 | `feat: NavControllerNotificationRouter android + ios stub` | C4 |
| 11 | `feat: wire AppNotifierListener and NavRouterDelegate in App()` | C5 |
| 12 | `refactor(koin): replace legacy messaging bindings with AppNotifierListener` | C6 |
| — | **MID-PHASE CODE REVIEW** | |
| 13 | `feat: delete MyFirebaseMessagingService and FirebaseMessagingViewModel` | D1 |
| 14 | `feat(manifest): remove MyFirebaseMessagingService declaration` | D2 |
| 15 | `build: remove glide and firebase-messaging from build graph` | D3 |
| 16 | `feat: request notification permission on home screen entry` | E1 |
| 17 | `feat: notification rationale strings en + ru` | E2 |
| 18 | `refactor: replace Firebase messaging subscribeToTopic with KMPNotifier` | F1 |
| 19 | `chore: confirm no firebase-messaging SDK imports in commonMain` | F2 |
| — | **FINAL CODE REVIEW** | |

**Phase 5 exit criteria:** Android debug build green; all commonTest pass; push arrives on device from Firebase Console; click-through routes correctly; `msgToken` updated in Firestore on fresh install; no Glide, no `firebase-messaging`, no `MyFirebaseMessagingService` anywhere in source or build files.
