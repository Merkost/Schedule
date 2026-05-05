# Compose Multiplatform + iOS Migration — Design Spec

**Date:** 2026-05-05
**Branch:** `feature/cmp-ios-migration` (cut from `master`)
**Author:** Costa (with Claude)
**Status:** Approved design, ready for implementation plan

---

## 1. Goal & scope

Migrate the Schedule Android app (Compose-only, Material3, Firebase-backed) to **Kotlin Multiplatform + Compose Multiplatform** so the same codebase ships on Android (Google Play, unchanged) and iOS (TestFlight initially; "private link" / unlisted prod distribution later if desired).

**Non-negotiables:**
- **Full feature parity day-one on iOS** — every screen and capability working before first TestFlight upload (per Q1: option A).
- Android behavior must be unchanged after migration (no regressions).
- Bundle id stays `ru.dvfu.appliances` on both platforms.

**Distribution channels considered equivalent for the purposes of this spec:**
- TestFlight Internal (≤100 testers, no review)
- TestFlight External (≤10k testers, light Beta App Review, ~24h)
- Unlisted App Store distribution (full review, search-hidden) — "private link prod"

The build artifact is identical for all three; channel choice is post-merge.

---

## 2. Module layout

```
Schedule/
├── composeApp/                       # KMP module — 95% of code
│   ├── src/
│   │   ├── commonMain/kotlin/        # Compose UI, ViewModels, repos,
│   │   │                             #   use-cases, models, Koin modules,
│   │   │                             #   navigation graph
│   │   ├── commonMain/composeResources/  # strings, drawables, fonts
│   │   ├── androidMain/kotlin/       # Crashlytics/Perf init, Google
│   │   │                             #   sign-in client, FCM_SERVER_KEY
│   │   │                             #   bridge, lifecycle glue
│   │   ├── iosMain/kotlin/           # Apple Sign-In, GoogleSignIn-iOS
│   │   │                             #   bridge, DataStore path, Koin
│   │   │                             #   iOS module, MainViewController()
│   │   └── commonTest/kotlin/
│   └── build.gradle.kts
├── androidApp/                       # ~30 lines: Application + Activity
│   ├── src/main/AndroidManifest.xml
│   ├── src/main/kotlin/ScheduleApplication.kt
│   ├── src/main/kotlin/MainActivity.kt
│   └── google-services.json
├── iosApp/                           # Xcode project (Swift)
│   ├── iosApp.xcodeproj
│   ├── iosApp/iOSApp.swift
│   ├── iosApp/AppDelegate.swift
│   ├── iosApp/GoogleService-Info.plist
│   ├── iosApp/Info.plist
│   ├── iosApp/iosApp.entitlements
│   └── iosApp/Assets.xcassets
├── gradle/libs.versions.toml         # restructured
└── settings.gradle.kts               # adds composeApp + androidApp
```

The current `:app` module is **deleted**; its contents move to `composeApp/` (commonMain or androidMain depending on platform deps) and `androidApp/` (manifest, MainActivity, signing config, google-services.json).

`iosApp/` consumes the Compose framework via **Swift Package Manager** (local SPM package, modern KMP template approach — faster than CocoaPods, no Ruby).

---

## 3. Dependency mapping

### 3.1 Kept (already KMP-ready)

| Dep | Action |
|---|---|
| Kotlin 2.3.20 | unchanged |
| Compose Compiler plugin | unchanged |
| Koin 4.x | `koin-core` + `koin-compose` to commonMain; `koin-android` + `koin-androidx-compose` to androidMain |
| navigation-compose | swap to `org.jetbrains.androidx.navigation:navigation-compose` (KMP) |
| lifecycle-viewmodel-compose | swap to `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose` (KMP) |
| kotlinx-coroutines-core | unchanged; drop `coroutines-android` and `coroutines-play-services` (move latter to androidMain only) |
| DataStore Preferences | swap to `datastore-preferences-core`; add `expect fun dataStorePath()` |
| ConstraintLayout-Compose | swap to `tech.annexflow.compose:constraintlayout-compose-multiplatform` |
| compose-calendar (Bogusz) | bump from `0.4.2` to ≥0.5.x (KMP-supporting); verify API compatibility against `MonthCalendar.kt` and `MonthCalendarViews.kt` |
| LeakCanary | androidMain debug only |

### 3.2 Replaced

| Today | Replacement | Where |
|---|---|---|
| Retrofit 3 + Gson + OkHttp logging | Ktor Client + kotlinx.serialization + Ktor logging plugin | commonMain API; `ktor-client-okhttp` engine (androidMain), `ktor-client-darwin` engine (iosMain) |
| play-services-auth (Google Sign-In) | androidMain: keep play-services-auth. iosMain: GoogleSignIn-iOS via SPM (in iosApp/), bridged through expect/actual `GoogleSignInClient` | per-platform |
| firebase-ui-auth | **Removed** — sign-in screen rebuilt in Compose using Gitlive Firebase Auth | commonMain |
| Microsoft OAuth refs (`LoginScreen.kt`, `LoginActivity.kt`, `CurrentDevStatus.kt`) | **Deleted** | — |
| Firebase Android SDK (Auth/Firestore/RTDB/Storage/Analytics/Crashlytics/Config) | **Gitlive Firebase-Kotlin** (`dev.gitlive:firebase-*`) | commonMain |
| Firebase Messaging (Android SDK) + `MyFirebaseMessagingService.kt` | **KMPNotifier** (`io.github.mirzemehdi:kmpnotifier`) — single API on both platforms; deletes the messaging service | commonMain + thin per-platform init |
| accompanist-permissions | Notification permission: handled by KMPNotifier directly (no separate lib needed). Camera / photo-library permission on Android: **Moko-Permissions** (`dev.icerock.moko:permissions-compose`); on iOS the system prompts at the API call site (no lib). | commonMain |
| material-icons-extended (Android-only) | `org.jetbrains.compose.material:material-icons-extended` (KMP) | commonMain |
| `java.time.*` (42 files) | **kotlinx-datetime** | commonMain |
| activity-compose | androidMain only; iOS uses `ComposeUIViewController` | per-platform |

### 3.3 Added (iOS-only)

| Library / Framework | Purpose |
|---|---|
| `AuthenticationServices` (system framework, cinterop) | Sign in with Apple (App Store requirement when offering 3rd-party SSO like Google) |
| GoogleSignIn-iOS (SPM in iosApp/) | Google sign-in on iOS |
| Firebase iOS SDK (transitive via Gitlive) | iOS Firebase runtime |

### 3.4 Dropped

- **Both platforms:** Microsoft sign-in (deleted), Lottie (declared but unused), WorkManager (declared but unused), Glide (single use removed with FCM service deletion).
- **iOS only:** Firebase Performance, Firebase In-App Messaging Display.
- **Android only:** none — Android keeps everything Gitlive offers plus Performance and In-App Messaging.

### 3.5 `gradle/libs.versions.toml` net change

≈ −7 entries (retrofit, gson, glide, lottie, work, firebase-ui-auth, accompanist-permissions if Moko-not-needed), ≈ +10 entries (ktor-client-core/cio/okhttp/darwin/content-negotiation/logging, kotlinx-datetime, kotlinx-serialization-json, gitlive-firebase-* aliases, kmpnotifier).

---

## 4. Firebase wrapper layer & DI

### 4.1 Repositories (the boring win)

19 files under `model/repository/` currently call Firebase Android SDK directly. After migration they call **Gitlive Firebase-Kotlin** APIs which are deliberately near-identical:

```kotlin
// before (androidMain)
FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pw).await()

// after (commonMain)
Firebase.auth.signInWithEmailAndPassword(email, pw)   // suspend
```

Most edits are import swaps + replacing `Tasks.await()` with the Gitlive suspend equivalent + replacing Gson DTOs with `@Serializable` data classes.

### 4.2 Auth (the one repo that grows)

`AuthRepository` introduces platform-specific sign-in clients via expect/actual:

```kotlin
// commonMain
expect class GoogleSignInClient {
    suspend fun signIn(): String   // returns idToken
    suspend fun signOut()
}
expect class AppleSignInClient {
    suspend fun signIn(): AppleIdCredential
}
```

- `androidMain` — `GoogleSignInClient` uses existing play-services-auth flow with `ComponentActivity`. `AppleSignInClient` throws `UnsupportedOperationException` (button hidden in Android UI).
- `iosMain` — `GoogleSignInClient` calls `GIDSignIn.sharedInstance` via cinterop. `AppleSignInClient` calls `ASAuthorizationController` + `ASAuthorizationAppleIDProvider`.

Once the platform layer returns an idToken, the rest is shared:

```kotlin
val cred = GoogleAuthProvider.credential(idToken, null)
Firebase.auth.signInWithCredential(cred)
```

The sign-in screen is **rebuilt as a Compose composable** in commonMain replacing `firebase-ui-auth`. Visual parity with current Android UX is preserved; no redesign.

### 4.3 Push notifications (KMPNotifier)

| Concern | Implementation |
|---|---|
| FCM token | `NotifierManager.getPushNotifier().getToken()` (commonMain) |
| Permission flow | `NotifierManager.requestPermission()` (commonMain, both platforms) |
| Foreground display + click handling | KMPNotifier's `Listener` interface in commonMain |
| Android init | `NotifierManager.initialize(NotificationPlatformConfigurationAndroid(...))` in `Application.onCreate()`. **`MyFirebaseMessagingService.kt` deleted** (and with it, the only Glide usage) |
| iOS init | `NotifierManager.initialize(NotificationPlatformConfigurationIos(...))` from `iOSApp.swift`. AppDelegate forwards APNs token via library helper. ~15 lines of Swift |

**Gitlive `firebase-messaging` is NOT used** — KMPNotifier already wraps both FCM (Android) and APNs+FCM (iOS); using both creates duplicate listeners.

### 4.4 Server-side push (FCM legacy — kept for now)

`NotificationApi.kt` + `RetrofitInstance.kt` move to commonMain and become **Ktor Client** hitting the same legacy endpoint `https://fcm.googleapis.com/fcm/send` with `Authorization: key=$FCM_SERVER_KEY`. Behavior identical; only HTTP client changes.

`FCM_SERVER_KEY` flow:
- androidApp: read from `local.properties` / env via BuildConfig (unchanged).
- iosApp: same key value, set as a build setting in xcconfig and exposed via `Info.plist` value.
- commonMain: `expect val FcmServerKey: String` provides the value to the Ktor client.

⚠️ **Known time bomb (deferred):** Google retired legacy HTTP/XMPP FCM send on **2024-06-20**. If your project's pushes are still landing today, you're on borrowed time; Firebase can disable the legacy path server-side at any moment with no client warning. Migrating `NotificationApi` to **FCM HTTP v1** (OAuth2 service-account token) is recorded as a follow-up item independent of this KMP migration. The Ktor `Authorization` header will be pre-wired to make the swap a one-file change.

### 4.5 Koin DI restructure

```
commonMain/.../di/
    SharedModules.kt          # repos, viewmodels, use-cases, network

androidMain/.../di/
    AndroidPlatformModule.kt  # GoogleSignInClient(activity),
                              #   Crashlytics + Performance init,
                              #   DataStore path (filesDir),
                              #   FCM_SERVER_KEY from BuildConfig

iosMain/.../di/
    IosPlatformModule.kt      # GoogleSignInClient (iOS),
                              #   AppleSignInClient,
                              #   DataStore path (NSDocumentDirectory),
                              #   Analytics init,
                              #   FCM_SERVER_KEY from Info.plist
```

`expect fun platformModule(): Module` resolves to the right module per platform. `App()` composable in commonMain calls `KoinApplication { modules(sharedModules + platformModule()) }`. ViewModels keep using `koinViewModel()` from JetBrains' KMP lifecycle-viewmodel-compose port.

---

## 5. iOS-specific glue

### 5.1 `iosApp/` Xcode project

Minimal Swift, no SwiftUI views beyond hosting:

```swift
// iOSApp.swift
@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    var body: some Scene {
        WindowGroup { ComposeView().ignoresSafeArea(.all) }
    }
}
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController =
        MainViewControllerKt.MainViewController()
}
```

`MainViewController()` is a fun exported from iosMain that returns `ComposeUIViewController { App() }`.

### 5.2 `AppDelegate.swift` responsibilities

- `application(_:didFinishLaunchingWithOptions:)`: `FirebaseApp.configure()` then `KMPNotifier.initialize(...)`.
- `application(_:didRegisterForRemoteNotificationsWithDeviceToken:)`: forward APNs token to KMPNotifier helper.
- `application(_:open:options:)`: forward URL to `GIDSignIn.sharedInstance.handle(url)` for Google sign-in callback.

### 5.3 `Info.plist` & entitlements (concrete checklist)

- `CFBundleURLTypes` — register reversed Google client ID URL scheme.
- `NSCameraUsageDescription`, `NSPhotoLibraryUsageDescription` — for image upload to Firebase Storage.
- `NSUserNotificationUsageDescription` — for push permission.
- `iosApp.entitlements` — add **Sign in with Apple** capability, **aps-environment** (push).
- `PrivacyInfo.xcprivacy` — privacy manifest (added in Phase 7; required by App Review).

### 5.4 Lifecycle, navigation, back gesture

- ViewModel lifecycle handled by JetBrains lifecycle-viewmodel-compose KMP port on both platforms.
- navigation-compose KMP port works identically; iOS swipe-from-edge gesture pops back stack out of the box.
- Existing top-bar back arrows in the UI continue working unchanged.

### 5.5 Resources

- `composeResources/values/strings.xml` and `composeResources/values-ru/strings.xml` (Compose Resources / M-R format) replace Android `res/values*/strings.xml`. Generated `Res.string.*` API works on both platforms.
- iOS gets fresh icon set in `Assets.xcassets` from existing 1024×1024 source.
- Russian locale carries over.

### 5.6 Build & signing

- `composeApp` exposes `iosArm64` + `iosSimulatorArm64` + `iosX64` targets as a regular framework consumed via local SPM package.
- Xcode automatic signing with existing Apple Dev Team ID.
- Bundle id `ru.dvfu.appliances` shared with Android.

---

## 6. Migration sequence

Each phase ends with a green Android build. iOS doesn't compile until Phase 6 (intentional).

### Phase 0 — Prereqs (no code)

- Apple Developer Team ID confirmed; iOS app created in App Store Connect with bundle id `ru.dvfu.appliances`.
- Firebase Console: register iOS app, download `GoogleService-Info.plist`.
- APNs `.p8` Authentication Key generated and uploaded to Firebase iOS app config.
- Xcode 16+; `kdoctor` clean.

### Phase 1 — Module skeleton (Android still works)

- Create `composeApp/` KMP module with `androidTarget()` only.
- Create `androidApp/`; move `AndroidManifest.xml`, `MainActivity.kt`, `ScheduleApplication`, `google-services.json`, signing/keystore config.
- Update `settings.gradle.kts`. Delete `:app`.
- All current Kotlin code lives temporarily in `composeApp/androidMain/`.
- **Exit:** `./gradlew :androidApp:assembleDebug` succeeds; runtime behavior unchanged.

### Phase 2 — Common-ize (no Firebase, no Android imports)

- Move all entity / model / utils / pure-Compose-component files to `commonMain`.
- Swap parcelize → `@Serializable` for nav arg types.
- Replace `java.time.*` → `kotlinx.datetime.*` across 42 files.
- Swap material-icons-extended, navigation-compose, lifecycle-viewmodel-compose, constraintlayout-compose, compose-calendar to KMP variants.
- Convert string/drawable resources to composeResources (M-R).
- **Exit:** Android green; ~60% of `.kt` files now in commonMain.

### Phase 3 — Networking & DataStore

- Replace Retrofit + Gson + OkHttp logging → Ktor + kotlinx.serialization + Ktor logging. Keep API surface identical.
- DataStore: switch to `datastore-preferences-core`, add `expect fun dataStorePath()`.
- **Exit:** Android green; FCM legacy push end-to-end smoke-tested.

### Phase 4 — Firebase repos to Gitlive

- Add Gitlive deps (Auth, Firestore, RTDB, Storage, Analytics, Crashlytics, Config) — **not** firebase-messaging.
- Move all `model/repository/`, `model/datasource/`, `use_cases/` to commonMain.
- Replace Firebase Android SDK calls with Gitlive APIs.
- Introduce `expect class GoogleSignInClient` + `expect class AppleSignInClient`. Android: real impl + Apple stub. iOS: stubs (real impls in Phase 6).
- Rebuild sign-in UI in commonMain (replaces firebase-ui-auth). Delete Microsoft sign-in code.
- Move Koin modules: SharedModules.kt (commonMain), AndroidPlatformModule.kt (androidMain).
- **Exit:** Android sign-in (email + Google), Firestore reads/writes, Storage uploads work. Performance + In-App Messaging still wire on Android.

### Phase 5 — KMPNotifier

- Add KMPNotifier to commonMain.
- Initialize from `App()` and `androidApp` `Application.onCreate()`.
- **Delete `MyFirebaseMessagingService.kt`** and its manifest entry; KMPNotifier registers its own.
- Wire single `Listener` for push + click events that hits in-app navigator.
- `NotificationApi` (server send) stays in commonMain on Ktor.
- **Exit:** Android push arrives, click routes correctly, topic subscriptions and token refresh equivalent.

### Phase 6 — Add iOS targets

- Add `iosX64`, `iosArm64`, `iosSimulatorArm64` to composeApp; configure framework via SPM local package.
- Implement iOS `actual`s: `dataStorePath()`, `GoogleSignInClient`, `AppleSignInClient`, `platformModule()`, `FcmServerKey`, KMPNotifier permission integration.
- Create `iosApp/` Xcode project; SPM dependencies on Compose framework, Firebase iOS SDK (Auth/Firestore/Database/Storage/Analytics/Crashlytics/Config — **not** Performance, **not** In-App Messaging), GoogleSignIn-iOS.
- `iOSApp.swift` + `AppDelegate.swift` per §5.
- **Exit:** App launches in iOS Simulator, lands on sign-in screen.

### Phase 7 — iOS feature parity & polish

- E2E pass on physical iPhone:
  - Email + Google + Apple sign-in
  - Calendar / schedule rendering
  - Booking create / cancel / comment
  - Push arrives + click routes
  - Image upload (Firebase Storage) from camera + gallery
  - Russian locale
- Fix iOS-only quirks: insets, font fallbacks, IME handling, keyboard.
- Crashlytics dSYM upload script in Xcode build phases.
- Add `PrivacyInfo.xcprivacy`.
- **Exit:** all 8 use cases work on both platforms.

### Phase 8 — TestFlight

- Archive in Xcode, upload to App Store Connect.
- Internal testers (no review) for first round.
- External TestFlight (Beta App Review) once stable; share public link if private-link prod is preferred over full App Store listing.
- **Exit:** build installable from a TestFlight invite link.

---

## 7. Risks & mitigations

### High-risk

| # | Risk | Mitigation |
|---|---|---|
| 1 | Gitlive Firebase API drift from Android SDK (e.g., listeners as Flow vs ListenerRegistration) | Per-repo PR in Phase 4; smoke-test against real Firebase project after each. Don't batch all repos into one commit. |
| 2 | GoogleSignIn-iOS cinterop friction (callback APIs need wrapping) | 30-min spike at the Phase 0/6 boundary: tiny app that signs in via Google and prints idToken. Validates pattern before full integration. |
| 3 | FCM legacy endpoint kill switch (deprecated 2024-06-20) | Tracked as known follow-up. Pre-wire Ktor `Authorization` header so HTTP v1 swap is one-file. |
| 4 | Compose iOS rendering perf on dense calendar grid | Profile calendar on physical iPhone early in Phase 7. If problematic, throttle recompositions or split into LazyColumn of week rows. |
| 5 | iOS keyboard / IME insets behave differently | Add `imePadding` audit pass at end of Phase 7. |
| 6 | navigation-compose KMP saved-state restoration on iOS process death | Acceptable for TestFlight scope; document as known limitation; revisit if testers report lost state. |
| 7 | Russian locale + iOS Cyrillic font fallback | Bundle Roboto via composeResources/font/ if visual consistency matters; otherwise accept SF Pro fallback. |
| 8 | App Store Review surprises (privacy manifest, usage descriptions, Apple Sign-In) | All three pre-empted: Apple Sign-In in scope, usage descriptions in Info.plist, privacy manifest in Phase 7. |

### Medium-risk

- compose-calendar (Bogusz) KMP version API delta from `0.4.2`. Mitigation: thin wrapper if breaking changes.
- constraintlayout-compose-multiplatform (annexflow) is a community port. Mitigation: fall back to nested Box/Row for the 1–2 ConstraintLayout uses.
- DataStore Preferences KMP path management — silently broken until fresh-install test. Mitigation: smoke test on fresh install in Phase 3.
- KMPNotifier permission flow on iOS 13/14 — verify minIosVersion (set to 14).

### Low-risk

- kotlinx-coroutines, kotlinx-datetime, kotlinx-serialization, Ktor, Koin, Compose Resources — mature, widely used.
- Xcode automatic signing — well-documented.

---

## 8. Prerequisites checklist

- [x] Apple Developer Program account active
- [x] Mac + Xcode 16+ available
- [ ] iOS app registered in Firebase Console with bundle id `ru.dvfu.appliances`
- [ ] APNs `.p8` Authentication Key generated and uploaded to Firebase
- [ ] `GoogleService-Info.plist` downloaded
- [ ] App Store Connect listing created
- [ ] CI: macOS runner provisioned (out of scope for this spec; flagged)

---

## 9. Out of scope

- FCM HTTP v1 migration (kept legacy per decision; documented as time bomb).
- Firebase Performance on iOS.
- Firebase In-App Messaging on iOS.
- Microsoft sign-in (deleted on both platforms).
- iOS widgets, Live Activities, iPad-specific layouts.
- New localizations beyond ru / en.
- UX redesign — sign-in screen rebuilt in Compose but matches current visual.

---

## 10. Open unknowns (accepted)

- Exact LOC delta — estimated 60% of files move untouched, 30% touch imports + small API changes (Gson→Serializable, java.time→kotlinx-datetime), 10% need real refactor (auth, push, repos using Firebase listener APIs).
- iOS framework binary size — typically 30–50 MB before strip; acceptable.
- iOS build times — clean framework build ~2–5 min on recent Mac; incremental ~10–30 sec.
