# Phase 4 — Firebase Repos to Gitlive — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Prerequisite:** Plans 1+2+3 complete. `composeApp/` + `androidApp/` exist; ~95% of non-Firebase code in commonMain; networking on Ktor; java.time → kotlinx.datetime; resources via composeResources. Firebase Android SDK still in androidMain; sign-in still uses firebase-ui-auth; Microsoft sign-in code still present.

**Goal:** Replace all Firebase Android SDK usage with Gitlive Firebase-Kotlin in `commonMain`. Move all repositories, datasources, and use-cases to commonMain. Rebuild sign-in screen as Compose composable (deleting `firebase-ui-auth`). Add `expect/actual GoogleSignInClient` and `AppleSignInClient`. Restructure Koin into shared + platform modules. Delete Microsoft sign-in code.

**Architecture:** All `model/repository/*Impl.kt` move to commonMain calling Gitlive APIs (near-identical to Firebase Android SDK). Auth grows two `expect class`es with platform-specific OAuth client implementations. The single `KoinModules.kt` splits into `SharedModules.kt` (commonMain), `AndroidPlatformModule.kt` (androidMain), `IosPlatformModule.kt` (iosMain placeholder for Plan 6). Existing Firestore documents must continue to deserialize correctly — `@SerialName` annotations preserve field names.

**Tech Stack:** Gitlive Firebase-Kotlin 2.x (`dev.gitlive:firebase-*`), kotlinx-serialization, Koin 4. NOT included: `firebase-messaging` (KMPNotifier replaces in Plan 5).

---

## Critical context (from prior research)

**Highest-risk file:** `FirebaseUsersRepositoryImpl.kt` — 5 reasons: (1) auth state Flow replaces AuthStateListener (regression breaks all sign-in/sign-out); (2) `AuthUI.signOut(context)` → `Firebase.auth.signOut()` silently changes Google credential manager behavior; (3) `uploadMessagingToken` removal degrades push delivery for new sign-ins until Plan 5; (4) constructor loses Context — Koin DI must update or runtime crashes; (5) `setUserListener` semantics shift fire-and-forget → blocking collect.

**Listener leak prevention:** Gitlive `.snapshots()` returns Flow that keeps Firestore listener alive while collected. Cancel via `viewModelScope` cancellation (no manual `awaitClose { registration.remove() }` needed). Never collect in GlobalScope or unscoped CoroutineScope.

**`BookingStatus` in partial updates:** When calling `.update("status" to enum)` in Gitlive, Gitlive may serialize as object. Pass `enum.name` explicitly in update maps to ensure Firestore receives the string.

**`signOut` behavioral delta:** `Firebase.auth.signOut()` clears Firebase session only. Google credential manager is NOT cleared. UI layer must call `GoogleSignInClient.signOut()` after `usersRepository.logoutCurrentUser()`. Caller's responsibility.

**`OfflineRepository` cache semantics:** Old `Source.CACHE` threw if missing. New `.get()` always attempts network or returns cache if offline. Existing fallback logic in `GetUserUseCase` / `GetApplianceUseCase` still works.

**Gitlive version pinning:** Single `gitliveFirebase = "2.4.0"` in `libs.versions.toml`. No BOM exists for Gitlive; mixing versions across modules risks API incompatibility.

**iOS source set:** Plan 4 creates `iosMain` `actual` stubs but iOS targets are NOT yet declared in `composeApp/build.gradle.kts` (added in Plan 6). Source set must be declared in build script for iosMain stubs to compile:
```kotlin
sourceSets {
    val iosMain by creating { dependsOn(commonMain.get()) }
}
```
Without `iosArm64()` etc. in targets block. If this fails to compile, leave the iosMain stubs out and add them in Plan 6.

---

## Group A — Setup Gitlive dependencies

- [ ] **A-1** Add to `gradle/libs.versions.toml`:
  ```toml
  gitliveFirebase = "2.4.0"
  ```
  ```toml
  gitlive-firebase-auth = { module = "dev.gitlive:firebase-auth", version.ref = "gitliveFirebase" }
  gitlive-firebase-firestore = { module = "dev.gitlive:firebase-firestore", version.ref = "gitliveFirebase" }
  gitlive-firebase-database = { module = "dev.gitlive:firebase-database", version.ref = "gitliveFirebase" }
  gitlive-firebase-storage = { module = "dev.gitlive:firebase-storage", version.ref = "gitliveFirebase" }
  gitlive-firebase-analytics = { module = "dev.gitlive:firebase-analytics", version.ref = "gitliveFirebase" }
  gitlive-firebase-crashlytics = { module = "dev.gitlive:firebase-crashlytics", version.ref = "gitliveFirebase" }
  gitlive-firebase-config = { module = "dev.gitlive:firebase-config", version.ref = "gitliveFirebase" }
  gitlive-firebase-common = { module = "dev.gitlive:firebase-common", version.ref = "gitliveFirebase" }
  ```

- [ ] **A-2** In `composeApp/build.gradle.kts` `commonMain.dependencies`:
  ```kotlin
  implementation(libs.gitlive.firebase.auth)
  implementation(libs.gitlive.firebase.firestore)
  implementation(libs.gitlive.firebase.database)
  implementation(libs.gitlive.firebase.storage)
  implementation(libs.gitlive.firebase.analytics)
  implementation(libs.gitlive.firebase.crashlytics)
  implementation(libs.gitlive.firebase.config)
  implementation(libs.gitlive.firebase.common)
  ```
  Do NOT add `dev.gitlive:firebase-messaging` — KMPNotifier handles it in Plan 5.

- [ ] **A-3** Verify Android build still passes:
  ```bash
  ./gradlew :androidApp:assembleDebug
  ```
  Gitlive deps work transparently with existing `google-services.json`.

- [ ] **A-4** Audit transitive deps:
  ```bash
  ./gradlew :composeApp:dependencies --configuration commonMainImplementation | grep gitlive
  ```
  All entries should be at `2.4.0`.

- [ ] **A-5 — Commit:** `build: add Gitlive Firebase deps to composeApp commonMain`

---

## Group B — Per-repository migration

Read existing impls first to enumerate. Likely: `FirebaseUsersRepositoryImpl`, `EventsRepositoryImpl`, `AppliancesRepositoryImpl`, `BookingRepositoryImpl`, `OfflineRepositoryImpl`, plus auxiliary classes.

### Common patterns — apply per repo

**Pattern P-AUTH:**
```kotlin
// Before (androidMain)
FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pw).await()
val user = FirebaseAuth.getInstance().currentUser

// After (commonMain, Gitlive)
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

Firebase.auth.signInWithEmailAndPassword(email, pw)
val user = Firebase.auth.currentUser
```

**Pattern P-AUTH-STATE-FLOW:**
```kotlin
// Before
FirebaseAuth.getInstance().addAuthStateListener { auth ->
    val user = auth.currentUser
    // ...
}

// After
Firebase.auth.authStateChanged.collect { user ->
    // ...
}
```

**Pattern P-FIRESTORE-COLLECTION:**
```kotlin
// Before
FirebaseFirestore.getInstance().collection("users")

// After
import dev.gitlive.firebase.firestore.firestore
Firebase.firestore.collection("users")
```

**Pattern P-FIRESTORE-READ-ONCE:**
```kotlin
// Before
val snapshot = collection.document(id).get().await()
val user = snapshot.toObject(User::class.java)

// After
val user = Firebase.firestore.collection("users").document(id).get().data<User>()
```

**Pattern P-FIRESTORE-READ-STREAM:**
```kotlin
// Before — callback flow
callbackFlow {
    val reg = collection.addSnapshotListener { snapshot, _ ->
        snapshot?.let { trySend(it.toObjects(User::class.java)) }
    }
    awaitClose { reg.remove() }
}

// After — Gitlive Flow
Firebase.firestore.collection("users").snapshots
    .map { qs -> qs.documents.map { it.data<User>() } }
```

**Pattern P-FIRESTORE-WRITE:**
```kotlin
// Before
collection.document(id).set(user).await()

// After
Firebase.firestore.collection("users").document(id).set(user)
```

**Pattern P-FIRESTORE-UPDATE-MAP:**
```kotlin
// Before
collection.document(id).update("status" to BookingStatus.APPROVED).await()

// After — pass enum.name explicitly
Firebase.firestore.collection("events").document(id).update(
    "status" to BookingStatus.APPROVED.name
)
```

**Pattern P-STORAGE-UPLOAD:**
```kotlin
// Before
FirebaseStorage.getInstance().reference.child("photos/$id").putFile(uri).await()

// After
import dev.gitlive.firebase.storage.storage
Firebase.storage.reference("photos/$id").putFile(file)
```
(`putFile` accepts a Gitlive `File` type. On Android it wraps `Uri`. On iOS it wraps `NSURL`. Phase 6 wires the iOS variant.)

**Pattern P-DTO-SERIALIZABLE:**
- Every entity passed to/from Firestore must be `@Serializable`.
- For each property where the Firestore field name differs from the property name, add `@SerialName("firestoreFieldName")`.
- Verify against existing data: read 1–2 documents from Firebase Console; ensure each property in `User`, `Event`, `Appliance` matches the Firestore JSON key.

### Per-file tasks

- [ ] **B-1 — `FirebaseUsersRepositoryImpl.kt` (HIGHEST RISK)**
  - Move from `androidMain` to `commonMain`. Path: `model/datasource/FirebaseUsersRepositoryImpl.kt`.
  - Drop `Context` constructor parameter.
  - Replace AuthStateListener → `Firebase.auth.authStateChanged` Flow.
  - Replace `AuthUI.getInstance().signOut(context)` → `Firebase.auth.signOut()`. Document the credential-manager delta in commit body.
  - Remove `uploadMessagingToken` method. Document gap in commit message: "FCM token upload deferred to Plan 5 KMPNotifier listener."
  - Refactor `setUserListener` to return Flow:
    ```kotlin
    override fun setUserListener(user: User): Flow<User?> =
        Firebase.firestore.collection("users").document(user.userId).snapshots
            .map { it.data<User?>() }
    ```
    Update all callers to `viewModelScope.launch { setUserListener(user).collect { ... } }`.
  - Apply patterns P-AUTH, P-AUTH-STATE-FLOW, P-FIRESTORE-READ-ONCE, P-FIRESTORE-READ-STREAM, P-FIRESTORE-WRITE.
  - Verify Android build green after this single file.
  - **Commit:** `feat: migrate FirebaseUsersRepositoryImpl to Gitlive (auth, listener flows, signOut delta)`

- [ ] **B-2 — `EventsRepositoryImpl.kt`**
  - Move to commonMain.
  - Apply patterns. All `.snapshots` flows scoped via callers' viewModelScope.
  - Field-name verification: ensure `Event` properties match Firestore field names. Add `@SerialName` where they don't.
  - Verify Android build green.
  - **Commit:** `feat: migrate EventsRepositoryImpl to Gitlive Firestore`

- [ ] **B-3 — `AppliancesRepositoryImpl.kt`**
  - Move to commonMain. Handle `whereArrayContains`, `whereIn`, document-level snapshot listeners.
  - Apply patterns.
  - **Commit:** `feat: migrate AppliancesRepositoryImpl to Gitlive Firestore`

- [ ] **B-4 — `BookingRepositoryImpl.kt`**
  - Move. Note: status updates use `BookingStatus.name` (P-FIRESTORE-UPDATE-MAP).
  - **Commit:** `feat: migrate BookingRepositoryImpl to Gitlive`

- [ ] **B-5 — `OfflineRepositoryImpl.kt`**
  - Move. `Source.CACHE` has no Gitlive equivalent — switch to plain `.get()`. Document semantics in commit body.
  - **Commit:** `feat: migrate OfflineRepositoryImpl to Gitlive (drop Source.CACHE)`

- [ ] **B-6 — Auxiliary datasource classes** (e.g., `RepositoryUtils.kt`, `deprecated/CloudFirestoreDatabaseImpl.kt` if reachable)
  - Move or delete. Audit for any remaining Firebase Android SDK imports.
  - **Commit:** `chore: migrate remaining datasource classes to Gitlive`

### Code Review Gate B

- [ ] Dispatch `feature-dev:code-reviewer` on diff B-1..B-6. Focus:
  - Every `.snapshots` Flow has a defined consumer scope (viewModelScope or use-case).
  - No `Tasks.await()` calls remain.
  - No `com.google.firebase.auth/firestore/database/storage` imports in commonMain.
  - All `@Serializable` data classes have `@SerialName` where Firestore field name differs.
  - `BookingStatus` enum in update maps uses `.name`.

---

## Group C — Auth: expect/actual sign-in clients

- [ ] **C-1** Create `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/auth/GoogleSignInClient.kt`:
  ```kotlin
  package ru.dvfu.appliances.auth

  expect class GoogleSignInClient {
      suspend fun signIn(): String
      suspend fun signOut()
  }
  ```

- [ ] **C-2** Create `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/auth/AppleSignInClient.kt`:
  ```kotlin
  package ru.dvfu.appliances.auth

  data class AppleIdCredential(
      val identityToken: String,
      val authorizationCode: String?,
      val fullName: String?,
      val email: String?,
  )

  expect class AppleSignInClient {
      suspend fun signIn(): AppleIdCredential
  }

  expect val isAppleSignInSupported: Boolean
  ```

- [ ] **C-3** Android `actual GoogleSignInClient`:
  `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/auth/GoogleSignInClient.android.kt`
  ```kotlin
  package ru.dvfu.appliances.auth

  import androidx.activity.ComponentActivity
  import androidx.activity.result.ActivityResultLauncher
  import com.google.android.gms.auth.api.signin.GoogleSignIn
  import com.google.android.gms.auth.api.signin.GoogleSignInOptions
  import kotlinx.coroutines.suspendCancellableCoroutine
  import kotlin.coroutines.resume
  import kotlin.coroutines.resumeWithException

  actual class GoogleSignInClient(private val activity: ComponentActivity) {
      private val client = GoogleSignIn.getClient(
          activity,
          GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
              .requestIdToken(/* webClientId from BuildConfig */)
              .requestEmail()
              .build(),
      )

      actual suspend fun signIn(): String {
          // Existing flow: launch Intent via ActivityResultContracts.StartIntentSenderForResult,
          // suspend until result, return idToken from GoogleSignInAccount.
          // Reuse existing code from LoginActivity / LoginViewModel.
          TODO("Wire existing play-services-auth flow")
      }

      actual suspend fun signOut() {
          client.signOut()
      }
  }
  ```

- [ ] **C-4** Android `actual AppleSignInClient`:
  `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/auth/AppleSignInClient.android.kt`
  ```kotlin
  package ru.dvfu.appliances.auth

  actual class AppleSignInClient {
      actual suspend fun signIn(): AppleIdCredential =
          throw UnsupportedOperationException("Apple Sign-In is iOS only")
  }

  actual val isAppleSignInSupported: Boolean = false
  ```

- [ ] **C-5** iOS stubs (declared as iosMain even before iOS targets exist; if source set creation fails, defer to Plan 6):
  `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/auth/GoogleSignInClient.ios.kt`
  ```kotlin
  package ru.dvfu.appliances.auth

  actual class GoogleSignInClient {
      actual suspend fun signIn(): String =
          throw NotImplementedError("Wired in Plan 6")
      actual suspend fun signOut() {
          throw NotImplementedError("Wired in Plan 6")
      }
  }
  ```
  Same pattern for `AppleSignInClient.ios.kt` and `isAppleSignInSupported = true`.

- [ ] **C-6** Refactor `AuthRepository` (in commonMain after B-1) to use the new clients. After platform sign-in returns idToken, common code is:
  ```kotlin
  import dev.gitlive.firebase.auth.GoogleAuthProvider

  suspend fun signInWithGoogle(idToken: String) {
      val cred = GoogleAuthProvider.credential(idToken, null)
      Firebase.auth.signInWithCredential(cred)
  }
  ```

- [ ] **C-7 — Commit:** `feat: expect/actual GoogleSignInClient + AppleSignInClient with Android impls`

---

## Group D — Sign-in UI rebuild + Microsoft removal

- [ ] **D-1** Read existing `LoginScreen.kt` (androidMain) to capture current UX. Note buttons, layout, error strings.

- [ ] **D-2** Create new commonMain `compose/auth/LoginScreen.kt`:
  ```kotlin
  package ru.dvfu.appliances.compose.auth

  import androidx.compose.foundation.layout.*
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.ui.Modifier
  import org.jetbrains.compose.resources.stringResource
  import ru.dvfu.appliances.auth.isAppleSignInSupported
  import ru.dvfu.appliances.generated.resources.Res
  import ru.dvfu.appliances.generated.resources.*

  @Composable
  fun LoginScreen(viewModel: LoginViewModel = koinViewModel()) {
      val state by viewModel.state.collectAsState()
      Column(
          modifier = Modifier.fillMaxSize().padding(24.dp),
          verticalArrangement = Arrangement.Center,
      ) {
          OutlinedTextField(
              value = state.email,
              onValueChange = viewModel::onEmailChange,
              label = { Text(stringResource(Res.string.email)) },
              modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
              value = state.password,
              onValueChange = viewModel::onPasswordChange,
              label = { Text(stringResource(Res.string.password)) },
              modifier = Modifier.fillMaxWidth(),
          )
          Button(onClick = viewModel::signInWithEmail, modifier = Modifier.fillMaxWidth()) {
              Text(stringResource(Res.string.sign_in))
          }
          OutlinedButton(onClick = viewModel::signInWithGoogle, modifier = Modifier.fillMaxWidth()) {
              Text(stringResource(Res.string.sign_in_with_google))
          }
          if (isAppleSignInSupported) {
              OutlinedButton(onClick = viewModel::signInWithApple, modifier = Modifier.fillMaxWidth()) {
                  Text(stringResource(Res.string.sign_in_with_apple))
              }
          }
      }
  }
  ```
  Add `Res.string.email`, `password`, `sign_in`, `sign_in_with_google`, `sign_in_with_apple` to `composeResources/values/strings.xml` (and ru).

- [ ] **D-3** Create commonMain `LoginViewModel`:
  ```kotlin
  class LoginViewModel(
      private val authRepo: UsersRepository,
      private val googleClient: GoogleSignInClient,
      private val appleClient: AppleSignInClient,
  ) : ViewModel() {
      // state, onEmailChange, onPasswordChange, signInWithEmail (calls Firebase.auth.signInWithEmailAndPassword),
      // signInWithGoogle (googleClient.signIn() → authRepo.signInWithGoogle(idToken)),
      // signInWithApple (appleClient.signIn() → authRepo.signInWithApple(credential))
  }
  ```
  Move from `androidMain/.../viewmodels/LoginViewModel.kt`. Drop Firebase Android imports.

- [ ] **D-4** Delete `firebase-ui-auth` dep from `gradle/libs.versions.toml` and any module deps:
  ```bash
  grep -r "firebase-ui-auth\|firebaseUiAuth\|com.firebaseui" gradle/ composeApp/ androidApp/
  ```
  Remove all hits.

- [ ] **D-5** Delete Microsoft sign-in code:
  ```bash
  grep -rln "Microsoft\|microsoft\|OAuthProvider.newBuilder(\"microsoft" composeApp/ androidApp/
  ```
  Remove from `LoginScreen.kt`, `LoginActivity.kt`, `CurrentDevStatus.kt`. Delete entire methods related to Microsoft OAuth.

- [ ] **D-6** Delete androidMain `LoginActivity.kt` if no longer needed (sign-in is now a composable). Update `MainActivity` to host the composable navigation graph that includes the login screen. Keep splash if existing.

- [ ] **D-7 — Commit:** `feat: rebuild sign-in screen as Compose composable; delete firebase-ui-auth and Microsoft sign-in`

---

## Group E — Koin restructure

- [ ] **E-1** Create `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/di/SharedModules.kt`:
  ```kotlin
  package ru.dvfu.appliances.di

  import org.koin.core.module.Module
  import org.koin.dsl.module
  // ... imports

  val repositoryModule = module {
      single<UsersRepository> { FirebaseUsersRepositoryImpl(get(), get()) }
      single<EventsRepository> { EventsRepositoryImpl() }
      single<AppliancesRepository> { AppliancesRepositoryImpl() }
      single<BookingRepository> { BookingRepositoryImpl() }
      single<OfflineRepository> { OfflineRepositoryImpl() }
  }

  val useCasesModule = module {
      // GetApplianceUseCase, GetEventUseCase, etc.
  }

  val viewModelsModule = module {
      // viewModel { ... } definitions for all 14 ViewModels
  }

  fun sharedModules(): List<Module> = listOf(
      repositoryModule, useCasesModule, viewModelsModule, networkModule
  )
  ```
  (`networkModule` from Plan 3.)

- [ ] **E-2** Replace `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/di/AndroidPlatformModule.kt` (was `PlatformModule.kt` from Plan 2):
  ```kotlin
  package ru.dvfu.appliances.di

  import org.koin.core.module.Module
  import org.koin.dsl.module
  import org.koin.android.ext.koin.androidContext
  import org.koin.core.qualifier.named

  actual fun platformModule(): Module = module {
      single<UserDatastore> { UserDatastoreImpl() }
      single { GoogleSignInClient(activity = get()) }
      single { AppleSignInClient() }
      single(named("fcmServerKey")) { ru.dvfu.appliances.BuildConfig.FCM_SERVER_KEY }
      single(named("isDebug")) { ru.dvfu.appliances.BuildConfig.DEBUG }
      // Crashlytics + Performance init triggered here on Android
  }
  ```

- [ ] **E-3** Create `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/di/IosPlatformModule.kt` placeholder:
  ```kotlin
  package ru.dvfu.appliances.di

  import org.koin.core.module.Module
  import org.koin.dsl.module

  actual fun platformModule(): Module = module {
      // Plan 6 fills in: GoogleSignInClient (iOS), AppleSignInClient (iOS),
      // dataStore path, FcmServerKey from Info.plist, Analytics init.
  }
  ```

- [ ] **E-4** Update `androidApp/.../ScheduleApplication.kt`:
  ```kotlin
  startKoin {
      androidContext(this@ScheduleApplication)
      modules(sharedModules() + platformModule())
  }
  ```

- [ ] **E-5** Delete the original `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/di/KoinModules.kt` once contents are split between SharedModules + AndroidPlatformModule.

- [ ] **E-6 — Commit:** `refactor(koin): split into SharedModules (commonMain) + Android/Ios PlatformModules`

---

## Group F — Phase Review

- [ ] **F-1** Dispatch `feature-dev:code-reviewer` on full Phase 4 diff. Focus:
  - **(a)** Every `.snapshots` Flow scoped to viewModelScope or use-case scope.
  - **(b)** Error-handling parity with old Android SDK calls (try/catch around each Firebase op that previously had it).
  - **(c)** `@SerialName` annotations on every Firestore-bound property where the property name differs from the Firestore field name. **CRITICAL** — wrong serial name = data not loading.
  - **(d)** No `com.google.firebase.*` imports in commonMain.
  - **(e)** No `firebase-ui-auth` references anywhere.
  - **(f)** No Microsoft sign-in references anywhere.
  - **(g)** UI sign-out flow calls both `usersRepository.logoutCurrentUser()` AND `googleSignInClient.signOut()` to clear Google credential manager.

- [ ] **F-2** Field-name compatibility smoke test on device:
  ```bash
  ./gradlew :androidApp:installDebug
  adb shell am force-stop ru.dvfu.appliances
  ```
  Sign in with existing test account. Verify:
  - User profile loads with all fields (name, role, photoUrl, etc. — every field that was on the old Firestore doc).
  - Calendar loads existing bookings.
  - Appliance list loads with names and colors.
  - If any field is missing/null, check `@SerialName` mismatch.

- [ ] **F-3** Full smoke test:
  - Email sign-in.
  - Google sign-in.
  - Sign out → re-sign-in (Google account picker should re-appear; if not, `GoogleSignInClient.signOut()` not called from UI).
  - Create booking → appears in real-time on second device (validates `.snapshots` Flow).
  - Approve/decline booking (validates `BookingStatus.name` in update maps).
  - Storage: upload profile photo → verify file in Firebase Console.
  - Real-time listener: user receives notification when role changes (validates user listener Flow scoped correctly).

- [ ] **F-4 — Commit:** `chore: phase 4 complete — Firebase Android SDK fully replaced with Gitlive`

---

## Files

### Created
- `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/auth/GoogleSignInClient.kt` (expect)
- `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/auth/AppleSignInClient.kt` (expect + AppleIdCredential)
- `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/auth/GoogleSignInClient.android.kt` (actual)
- `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/auth/AppleSignInClient.android.kt` (actual stub throwing)
- `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/auth/GoogleSignInClient.ios.kt` (actual stub for Plan 6)
- `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/auth/AppleSignInClient.ios.kt` (actual stub for Plan 6)
- `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/auth/LoginScreen.kt` (Compose)
- `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/viewmodels/LoginViewModel.kt`
- `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/di/SharedModules.kt`
- `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/di/IosPlatformModule.kt` (placeholder)

### Moved (androidMain → commonMain)
- `model/datasource/FirebaseUsersRepositoryImpl.kt`
- `model/datasource/EventsRepositoryImpl.kt`
- `model/datasource/AppliancesRepositoryImpl.kt`
- `model/datasource/BookingRepositoryImpl.kt`
- `model/datasource/OfflineRepositoryImpl.kt`
- All remaining `model/datasource/*` auxiliary files
- All remaining `compose/use_cases/*` not yet moved (e.g., `GetEventTimeAvailabilityUseCase` after Context dep is removed)

### Deleted
- `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/ui/LoginScreen.kt` (replaced by commonMain Compose)
- `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/ui/LoginActivity.kt` (if redundant)
- `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/di/KoinModules.kt` (split into SharedModules + AndroidPlatformModule)
- All Microsoft sign-in code (in `LoginScreen.kt`, `LoginActivity.kt`, `CurrentDevStatus.kt`)

### Modified
- `gradle/libs.versions.toml` — add Gitlive entries; remove `firebase-ui-auth`.
- `composeApp/build.gradle.kts` — add Gitlive deps to commonMain; remove `firebase-ui-auth` from androidMain.
- `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/di/AndroidPlatformModule.kt` — move/rename from PlatformModule.kt; add Google/Apple clients + named bindings.
- `androidApp/.../ScheduleApplication.kt` — Koin init uses `sharedModules() + platformModule()`.
- `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/ScheduleApp.kt` — wire LoginScreen route.

---

## Phase 4 exit criteria

- Android build green; release build green.
- Sign-in works (email + Google).
- Sign-out fully clears both Firebase and Google credential manager.
- Firestore reads return populated objects (field-name compat verified).
- Firestore writes succeed (bookings created, status changes propagate).
- Storage uploads succeed.
- Real-time listeners (`.snapshots` Flows) emit on changes from another device.
- Crashlytics and Performance still report on Android (Performance Android-only is intentional).
- Zero `com.google.firebase.*` imports in commonMain.
- Zero `firebase-ui-auth` references.
- Zero Microsoft sign-in references.
- iOS source-set stubs compile (or are deferred to Plan 6 if iosMain fails to compile without iOS targets).
