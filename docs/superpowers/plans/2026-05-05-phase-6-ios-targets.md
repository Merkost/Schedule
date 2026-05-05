# Phase 6 — Add iOS Targets and Make iOS Compile and Launch — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Prerequisites:** Plans 1–5 complete. `composeApp/` exists with `androidTarget()` only; all `expect` declarations and iosMain stub `actual` files in place; `./gradlew :androidApp:assembleDebug` green.

**Goal:** Add iOS Kotlin targets to `composeApp/`, implement all iosMain `actual` declarations, and create a minimal `iosApp/` Xcode project that launches the app on iOS Simulator and physical iPhone.

**Architecture:** Three iOS Kotlin/Native targets (`iosX64`, `iosArm64`, `iosSimulatorArm64`) build a static `ComposeApp.framework`. A SwiftUI `iosApp` Xcode project hosts the framework via `UIViewControllerRepresentable`. Google Sign-In on iOS uses a Swift bridge that conforms to a Kotlin-generated ObjC protocol — chosen over cinterop to avoid coupling Kotlin compilation to Xcode's SPM resolution.

**Tech Stack:** Kotlin Multiplatform iOS targets, Compose Multiplatform iOS, Swift Package Manager, GoogleSignIn-iOS 7.x, AuthenticationServices, Firebase iOS SDK 11.x, KMPNotifier.

---

## Phase 0 prerequisites checklist

- [ ] Phases 0–5 complete: `composeApp/` exists with `androidTarget()` only; all `expect` declarations and iosMain stub `actual` files in place; `./gradlew :androidApp:assembleDebug` green.
- [ ] `GoogleService-Info.plist` downloaded from Firebase Console for bundle id `ru.dvfu.appliances`.
- [ ] APNs `.p8` Authentication Key uploaded to the Firebase iOS app configuration.
- [ ] Apple Developer Team ID confirmed (referenced as `$TEAM_ID` throughout).
- [ ] Xcode 16.x installed; `kdoctor` reports no blocking issues.
- [ ] `REVERSED_CLIENT_ID` value noted from `GoogleService-Info.plist` (format: `com.googleusercontent.apps.<project-id>`).

---

## Design decisions

### Why `isStatic = true`

A static Kotlin/Native framework bundles all code into a single Mach-O blob. SPM local package references and direct Xcode framework references both require static linkage when the framework has no separate dylib dependencies that the host can resolve at runtime. Dynamic framework would require embedding and re-signing, which adds complexity. Trade-off: larger binary (framework not shared between processes) — acceptable for single-app deployment. Future macOS Catalyst would use the same setting.

### Why SPM instead of CocoaPods

CocoaPods requires Ruby + Bundler + `pod install` after every framework rebuild. SPM uses only Xcode toolchain. The `ComposeApp.framework` is referenced directly via Framework Search Path in Xcode (Gradle plugin doesn't generate `Package.swift` for non-XCFramework outputs). SPM dependency declaration is used only for third-party Swift packages. Minimal build system surface.

### GoogleSignIn-iOS: Path 1 vs Path 2

**Path 1 — cinterop:** Risk: GoogleSignIn XCFramework headers are an `iosApp/` SPM dependency, not a `composeApp` build input — Kotlin compiler can't find them during `compileKotlinIosSimulatorArm64`. Resolving requires duplicating the dependency at Gradle level (CocoaPods interop or manual header extraction), both of which couple Kotlin build to SPM resolution order in ways that break on clean CI.

**Path 2 — Swift bridge (chosen):** `GoogleSignInClient` in iosMain holds a `GoogleSignInBridge` interface reference. A Swift class in `iosApp/` conforms to the Kotlin-generated ObjC protocol and calls `GIDSignIn.sharedInstance` directly. Pattern used by KMP apps in production, survives SDK version bumps, zero cinterop config.

**Recommendation: Path 2.** Path 1 documented in appendix.

### Scalability for future Apple targets

`listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { ... }` extends to `macosX64()` / `macosArm64()` without restructure. `applyDefaultHierarchyTemplate()` creates an `appleMain` intermediate source set. Future macOS code goes in `appleMain` instead of `iosMain`. No Phase 6 structure needs to change.

---

## Section A — Configure iOS Kotlin targets

### A1 — Add version catalog entries

- [ ] In `gradle/libs.versions.toml` `[versions]`, add (only what's not already present from Plans 1–5):
  ```toml
  composeMultiplatform = "1.8.0"
  ktor = "3.1.3"
  gitliveFirebase = "2.1.0"
  kotlinxDatetime = "0.6.2"
  kotlinxSerialization = "1.8.1"
  datastore = "1.1.2"
  kmpnotifier = "1.6.1"
  koin = "4.2.1"
  ```
- [ ] In `[libraries]`, add:
  ```toml
  ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
  ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
  ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
  ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
  ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }
  ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }

  gitlive-firebase-auth = { module = "dev.gitlive:firebase-auth", version.ref = "gitliveFirebase" }
  gitlive-firebase-firestore = { module = "dev.gitlive:firebase-firestore", version.ref = "gitliveFirebase" }
  gitlive-firebase-database = { module = "dev.gitlive:firebase-database", version.ref = "gitliveFirebase" }
  gitlive-firebase-storage = { module = "dev.gitlive:firebase-storage", version.ref = "gitliveFirebase" }
  gitlive-firebase-analytics = { module = "dev.gitlive:firebase-analytics", version.ref = "gitliveFirebase" }
  gitlive-firebase-crashlytics = { module = "dev.gitlive:firebase-crashlytics", version.ref = "gitliveFirebase" }
  gitlive-firebase-config = { module = "dev.gitlive:firebase-config", version.ref = "gitliveFirebase" }

  kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinxDatetime" }
  kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
  kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
  datastore-preferences-core = { module = "androidx.datastore:datastore-preferences-core", version.ref = "datastore" }
  kmpnotifier = { module = "io.github.mirzemehdi:kmpnotifier", version.ref = "kmpnotifier" }

  koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
  koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
  koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }

  jetbrains-navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version = "2.9.0-beta03" }
  jetbrains-lifecycle-viewmodel-compose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose", version = "2.9.0-beta03" }
  jetbrains-lifecycle-runtime-compose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose", version = "2.9.0-beta03" }

  compose-material-icons-extended = { module = "org.jetbrains.compose.material:material-icons-extended", version.ref = "composeMultiplatform" }
  constraintlayout-compose-multiplatform = { module = "tech.annexflow.compose:constraintlayout-compose-multiplatform", version = "0.4.0" }
  coil-compose = { module = "io.coil-kt.coil3:coil-compose", version = "3.1.0" }
  compose-calendar-multiplatform = { module = "io.github.boguszpawlowski.composecalendar:composecalendar", version = "1.4.0" }
  moko-permissions-compose = { module = "dev.icerock.moko:permissions-compose", version = "0.18.0" }
  kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
  ```
- [ ] In `[plugins]`, add:
  ```toml
  kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
  compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
  kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
  android-library = { id = "com.android.library", version.ref = "agp" }
  ```
- [ ] In root `build.gradle.kts`:
  ```kotlin
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.compose.multiplatform) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.android.library) apply false
  ```

### A2 — Complete `composeApp/build.gradle.kts`

```kotlin
import java.util.Properties
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions.jvmTarget = "21"
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            binaryOption("bundleId", "ru.dvfu.appliances")
            binaryOption("bundleVersion", "1")
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.compose.material.icons.extended)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.jetbrains.navigation.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.lifecycle.runtime.compose)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.datastore.preferences.core)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.gitlive.firebase.auth)
            implementation(libs.gitlive.firebase.firestore)
            implementation(libs.gitlive.firebase.database)
            implementation(libs.gitlive.firebase.storage)
            implementation(libs.gitlive.firebase.analytics)
            implementation(libs.gitlive.firebase.crashlytics)
            implementation(libs.gitlive.firebase.config)

            implementation(libs.kmpnotifier)
            implementation(libs.constraintlayout.compose.multiplatform)
            implementation(libs.moko.permissions.compose)
            implementation(libs.coil.compose)
            implementation(libs.compose.calendar.multiplatform)
        }

        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.androidx.activity.compose)
            implementation(libs.play.services.auth)
            implementation(libs.firebase.perf)
            implementation(libs.firebase.inappmessaging.display)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        compilations.configureEach {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xbinary=iosDeploymentTarget=14.0")
            }
        }
    }
}

android {
    namespace = "ru.dvfu.appliances"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        val fcmKey = localProps.getProperty("FCM_SERVER_KEY")
            ?: System.getenv("FCM_SERVER_KEY") ?: ""
        buildConfigField("String", "FCM_SERVER_KEY", "\"$fcmKey\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "ru.dvfu.appliances.resources"
    generateResClass = always
}

tasks.register("buildIosFrameworksDebug") {
    group = "ios"
    dependsOn(
        "linkDebugFrameworkIosSimulatorArm64",
        "linkDebugFrameworkIosArm64",
        "linkDebugFrameworkIosX64",
    )
}

tasks.register("buildIosFrameworksRelease") {
    group = "ios"
    dependsOn(
        "linkReleaseFrameworkIosSimulatorArm64",
        "linkReleaseFrameworkIosArm64",
        "linkReleaseFrameworkIosX64",
    )
}
```

### A3 — Verify deployment target flag

`-Xbinary=iosDeploymentTarget=14.0` is the stable approach for Kotlin 1.9+. For Kotlin 2.3.x, also valid. If issues arise, set via `binaries.framework { embedBitcode(BitcodeEmbeddingMode.DISABLE) }` and the deployment target via `compilerOptions`.

### A4 — Update `settings.gradle.kts`

- [ ] Verify the file reads:
  ```kotlin
  include(":composeApp", ":androidApp")
  ```
  (After Plan 1, `:app` was deleted.)

### A5 — Smoke-test iOS compilation

- [ ] `./gradlew :composeApp:compileKotlinIosSimulatorArm64`
- [ ] Expected: errors about unresolved `actual` declarations (from B1–B8), NOT about missing targets, plugins, or dep resolution. Fix any classpath/plugin issues before Section B.

---

## Section B — Implement iosMain `actual` declarations

All files under `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/`.

### B1 — `actual fun dataStorePath`

- [ ] Replace `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/datastore/DataStorePath.kt`:
  ```kotlin
  package ru.dvfu.appliances.datastore

  import platform.Foundation.NSDocumentDirectory
  import platform.Foundation.NSSearchPathForDirectoriesInDomains
  import platform.Foundation.NSUserDomainMask

  actual fun dataStorePath(filename: String): String {
      val directory = NSSearchPathForDirectoriesInDomains(
          directory = NSDocumentDirectory,
          domainMask = NSUserDomainMask,
          expandTilde = true,
      ).first() as String
      return "$directory/$filename"
  }
  ```

### B2 — iosTest for `dataStorePath`

- [ ] Create `composeApp/src/iosTest/kotlin/ru/dvfu/appliances/datastore/DataStorePathTest.kt`:
  ```kotlin
  package ru.dvfu.appliances.datastore

  import kotlin.test.Test
  import kotlin.test.assertTrue

  class DataStorePathTest {

      @Test
      fun pathEndsWithFilename() {
          val result = dataStorePath("test.preferences_pb")
          assertTrue(result.endsWith("test.preferences_pb"))
      }

      @Test
      fun pathContainsDocuments() {
          val result = dataStorePath("x")
          assertTrue(result.contains("Documents"))
      }

      @Test
      fun pathIsAbsolute() {
          val result = dataStorePath("y")
          assertTrue(result.startsWith("/"))
      }
  }
  ```

### B3 — `actual val FcmServerKey`

- [ ] Replace `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/notifications/FcmServerKey.kt`:
  ```kotlin
  package ru.dvfu.appliances.notifications

  import platform.Foundation.NSBundle

  actual val FcmServerKey: String
      get() = NSBundle.mainBundle.objectForInfoDictionaryKey("FCM_SERVER_KEY") as? String ?: ""
  ```
  Property `get()` form re-reads on every access — important in tests where main bundle is the test host.

### B4 — iosTest for `FcmServerKey`

- [ ] Create `composeApp/src/iosTest/kotlin/ru/dvfu/appliances/notifications/FcmServerKeyTest.kt`:
  ```kotlin
  package ru.dvfu.appliances.notifications

  import kotlin.test.Test
  import kotlin.test.assertNotNull

  class FcmServerKeyTest {

      @Test
      fun keyIsNonNullString() {
          val key = FcmServerKey
          assertNotNull(key)
      }
  }
  ```

### B5 — `MainViewController()`

- [ ] Replace `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/MainViewController.kt`:
  ```kotlin
  package ru.dvfu.appliances

  import androidx.compose.ui.window.ComposeUIViewController
  import platform.UIKit.UIViewController
  import ru.dvfu.appliances.auth.GoogleSignInBridge

  fun MainViewController(googleSignInBridge: GoogleSignInBridge): UIViewController =
      ComposeUIViewController {
          AppWithBridge(googleSignInBridge = googleSignInBridge)
      }
  ```

### B6 — `GoogleSignInBridge` interface and `GoogleSignInClient` actual

- [ ] **B6a** Create `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/auth/GoogleSignInBridge.kt`:
  ```kotlin
  package ru.dvfu.appliances.auth

  interface GoogleSignInBridge {
      suspend fun signIn(): String
      suspend fun signOut()
  }
  ```

- [ ] **B6b** Replace `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/auth/GoogleSignInClient.kt`:
  ```kotlin
  package ru.dvfu.appliances.auth

  actual class GoogleSignInClient(private val bridge: GoogleSignInBridge) {
      actual suspend fun signIn(): String = bridge.signIn()
      actual suspend fun signOut() = bridge.signOut()
  }
  ```

- [ ] **B6c** Note on `expect` constructor: declare `expect class GoogleSignInClient` in commonMain WITHOUT a primary constructor; both platforms define their own. Common code only holds `GoogleSignInClient` as an injected dependency (via Koin), never constructs it directly.

- [ ] **B6d** Add `AppWithBridge` to `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/App.kt`:
  ```kotlin
  @Composable
  fun AppWithBridge(googleSignInBridge: GoogleSignInBridge) {
      KoinApplication(application = {
          modules(sharedModules() + iosPlatformModuleWithBridge(googleSignInBridge))
      }) {
          AppContent()
      }
  }
  ```

### B7 — `AppleSignInClient`

- [ ] Create `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/auth/AppleIdCredential.kt`:
  ```kotlin
  package ru.dvfu.appliances.auth

  data class AppleIdCredential(
      val identityToken: String,
      val authorizationCode: String?,
      val fullName: String?,
      val email: String?,
  )
  ```

- [ ] Create `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/ui/ViewControllerExtensions.kt`:
  ```kotlin
  package ru.dvfu.appliances.ui

  import platform.UIKit.UIApplication
  import platform.UIKit.UIViewController
  import platform.UIKit.UIWindowScene

  fun UIViewController.Companion.topMostViewController(): UIViewController {
      val scene = UIApplication.sharedApplication.connectedScenes
          .filterIsInstance<UIWindowScene>()
          .firstOrNull { it.activationState == platform.UIKit.UISceneActivationStateForegroundActive }
      val rootVC = scene?.windows?.firstOrNull { it.isKeyWindow }?.rootViewController
          ?: error("No key window or root view controller found")
      var top = rootVC
      while (true) {
          top = top.presentedViewController ?: return top
      }
  }
  ```

- [ ] Replace `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/auth/AppleSignInClient.kt`:
  ```kotlin
  package ru.dvfu.appliances.auth

  import kotlinx.coroutines.suspendCancellableCoroutine
  import platform.AuthenticationServices.ASAuthorization
  import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
  import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
  import platform.AuthenticationServices.ASAuthorizationController
  import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
  import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
  import platform.AuthenticationServices.ASAuthorizationErrorCanceled
  import platform.AuthenticationServices.ASAuthorizationScopeEmail
  import platform.AuthenticationServices.ASAuthorizationScopeFullName
  import platform.Foundation.NSError
  import platform.UIKit.UIViewController
  import platform.UIKit.UIWindow
  import platform.darwin.NSObject
  import ru.dvfu.appliances.ui.topMostViewController
  import kotlin.coroutines.resume
  import kotlin.coroutines.resumeWithException

  actual class AppleSignInClient {

      actual suspend fun signIn(): AppleIdCredential = suspendCancellableCoroutine { cont ->
          val provider = ASAuthorizationAppleIDProvider()
          val request = provider.createRequest().apply {
              requestedScopes = listOf(ASAuthorizationScopeFullName, ASAuthorizationScopeEmail)
          }

          val delegate = object : NSObject(),
              ASAuthorizationControllerDelegateProtocol,
              ASAuthorizationControllerPresentationContextProvidingProtocol {

              override fun authorizationController(
                  controller: ASAuthorizationController,
                  didCompleteWithAuthorization: ASAuthorization,
              ) {
                  val appleCredential = didCompleteWithAuthorization.credential
                      as? ASAuthorizationAppleIDCredential
                  if (appleCredential == null) {
                      cont.resumeWithException(IllegalStateException("Unexpected Apple credential type"))
                      return
                  }
                  val idTokenBytes = appleCredential.identityToken
                  if (idTokenBytes == null) {
                      cont.resumeWithException(IllegalStateException("Apple Sign-In: missing identity token"))
                      return
                  }
                  val idToken = idTokenBytes.bytes?.let {
                      String(ByteArray(idTokenBytes.length.toInt()) { i -> (it + i).toByte() })
                  } ?: run {
                      cont.resumeWithException(IllegalStateException("Apple Sign-In: identity token unreadable"))
                      return
                  }
                  val authCode = appleCredential.authorizationCode?.let { data ->
                      data.bytes?.let {
                          String(ByteArray(data.length.toInt()) { i -> (it + i).toByte() })
                      }
                  }
                  val fullName = appleCredential.fullName?.let { name ->
                      listOfNotNull(name.givenName, name.familyName)
                          .joinToString(" ")
                          .takeIf { it.isNotBlank() }
                  }
                  cont.resume(AppleIdCredential(
                      identityToken = idToken,
                      authorizationCode = authCode,
                      fullName = fullName,
                      email = appleCredential.email,
                  ))
              }

              override fun authorizationController(
                  controller: ASAuthorizationController,
                  didCompleteWithError: NSError,
              ) {
                  if (didCompleteWithError.code == ASAuthorizationErrorCanceled) {
                      cont.cancel()
                  } else {
                      cont.resumeWithException(
                          Exception("Apple Sign-In failed: ${didCompleteWithError.localizedDescription}")
                      )
                  }
              }

              override fun presentationAnchorForAuthorizationController(
                  controller: ASAuthorizationController,
              ): UIWindow {
                  return UIViewController.topMostViewController().view?.window
                      ?: error("No window available for Apple Sign-In presentation")
              }
          }

          val authController = ASAuthorizationController(listOf(request))
          authController.delegate = delegate
          authController.presentationContextProvider = delegate
          authController.performRequests()

          cont.invokeOnCancellation {
              authController.cancel()
          }
      }
  }
  ```

  Memory note: `delegate` is retained by `authController.delegate` and the coroutine continuation frame. Modern Kotlin/Native MM (default since 1.7.20) handles correctly. No `StableRef` required.

  Alternative simpler NSData→String if `kotlinx-io` is available transitively from `ktor-client-darwin`:
  ```kotlin
  val idToken = idTokenBytes.toByteArray().decodeToString()
  ```

### B8 — `platformModule()` and bridge-aware Koin module

- [ ] Replace `composeApp/src/iosMain/kotlin/ru/dvfu/appliances/platform/IosPlatformModule.kt`:
  ```kotlin
  package ru.dvfu.appliances.platform

  import org.koin.dsl.module
  import ru.dvfu.appliances.auth.AppleSignInClient
  import ru.dvfu.appliances.auth.GoogleSignInBridge
  import ru.dvfu.appliances.auth.GoogleSignInClient

  actual fun platformModule() = module {
      single { AppleSignInClient() }
  }

  fun iosPlatformModuleWithBridge(bridge: GoogleSignInBridge) = module {
      includes(platformModule())
      single<GoogleSignInBridge> { bridge }
      single { GoogleSignInClient(bridge = get()) }
  }
  ```

### B9 — Code review gate: iosMain actuals

- [ ] No `java.*` or `android.*` imports in any `iosMain/` file.
- [ ] `AppleSignInClient` uses `suspendCancellableCoroutine` (not `suspendCoroutine`).
- [ ] `NSObject()` from `platform.darwin` or `platform.Foundation`.
- [ ] `UIViewController.topMostViewController()` is companion extension.
- [ ] `./gradlew :composeApp:compileKotlinIosSimulatorArm64` — zero compilation errors.
- [ ] `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` — framework produced at `composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework`.
- [ ] Inspect generated header:
  ```bash
  cat composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework/Headers/ComposeApp.h | grep -i "GoogleSignInBridge"
  ```
  Note exact ObjC protocol name — needed in Section C.

---

## Section C — Create `iosApp/` Xcode project

### C1 — Directory scaffold

```bash
mkdir -p /Users/merkost/AndroidStudioProjects/Schedule/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset
mkdir -p /Users/merkost/AndroidStudioProjects/Schedule/iosApp/iosApp/Assets.xcassets/AccentColor.colorset
```

### C2 — `Info.plist`

- [ ] Create `iosApp/iosApp/Info.plist`:
  ```xml
  <?xml version="1.0" encoding="UTF-8"?>
  <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
    "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
  <plist version="1.0">
  <dict>
      <key>CFBundleDisplayName</key>
      <string>Schedule</string>
      <key>CFBundleExecutable</key>
      <string>$(EXECUTABLE_NAME)</string>
      <key>CFBundleIdentifier</key>
      <string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>
      <key>CFBundleInfoDictionaryVersion</key>
      <string>6.0</string>
      <key>CFBundleName</key>
      <string>$(PRODUCT_NAME)</string>
      <key>CFBundlePackageType</key>
      <string>$(PRODUCT_BUNDLE_PACKAGE_TYPE)</string>
      <key>CFBundleShortVersionString</key>
      <string>$(MARKETING_VERSION)</string>
      <key>CFBundleVersion</key>
      <string>$(CURRENT_PROJECT_VERSION)</string>
      <key>CFBundleURLTypes</key>
      <array>
          <dict>
              <key>CFBundleTypeRole</key>
              <string>Editor</string>
              <key>CFBundleURLName</key>
              <string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>
              <key>CFBundleURLSchemes</key>
              <array>
                  <string>$(REVERSED_CLIENT_ID)</string>
              </array>
          </dict>
      </array>
      <key>NSCameraUsageDescription</key>
      <string>Камера используется для загрузки фотографии профиля и изображений приборов.</string>
      <key>NSPhotoLibraryUsageDescription</key>
      <string>Галерея используется для выбора фотографии профиля и изображений приборов.</string>
      <key>NSUserNotificationUsageDescription</key>
      <string>Уведомления информируют об изменениях событий и бронирований.</string>
      <key>UILaunchStoryboardName</key>
      <string>LaunchScreen</string>
      <key>UISupportedInterfaceOrientations</key>
      <array>
          <string>UIInterfaceOrientationPortrait</string>
      </array>
      <key>UIApplicationSceneManifest</key>
      <dict>
          <key>UIApplicationSupportsMultipleScenes</key>
          <false/>
      </dict>
      <key>FCM_SERVER_KEY</key>
      <string>$(FCM_SERVER_KEY)</string>
      <key>NSAppTransportSecurity</key>
      <dict>
          <key>NSAllowsArbitraryLoads</key>
          <false/>
      </dict>
      <key>UIStatusBarStyle</key>
      <string>UIStatusBarStyleDefault</string>
      <key>UIViewControllerBasedStatusBarAppearance</key>
      <true/>
      <key>ITSAppUsesNonExemptEncryption</key>
      <false/>
  </dict>
  </plist>
  ```

### C3 — `iOSApp.swift`

- [ ] Create `iosApp/iosApp/iOSApp.swift`:
  ```swift
  import SwiftUI
  import ComposeApp

  @main
  struct iOSApp: App {
      @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

      var body: some Scene {
          WindowGroup {
              ComposeView(googleSignInBridge: appDelegate.googleSignInBridge)
                  .ignoresSafeArea(.all)
          }
      }
  }

  struct ComposeView: UIViewControllerRepresentable {
      let googleSignInBridge: GoogleSignInBridgeImpl

      func makeUIViewController(context: Context) -> UIViewController {
          MainViewControllerKt.MainViewController(googleSignInBridge: googleSignInBridge)
      }

      func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
  }
  ```

### C4 — `AppDelegate.swift`

- [ ] Create `iosApp/iosApp/AppDelegate.swift`:
  ```swift
  import UIKit
  import FirebaseCore
  import GoogleSignIn
  import ComposeApp

  class AppDelegate: NSObject, UIApplicationDelegate {

      let googleSignInBridge = GoogleSignInBridgeImpl()

      func application(
          _ application: UIApplication,
          didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
      ) -> Bool {
          FirebaseApp.configure()

          let config = NotificationPlatformConfigurationIos(
              showPushNotification: true,
              askNotificationPermissionOnStart: false
          )
          NotifierManager.shared.initialize(configuration: config)

          return true
      }

      func application(
          _ application: UIApplication,
          didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
      ) {
          let hexToken = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
          NotifierManager.shared.onNewToken(token: hexToken)
      }

      func application(
          _ application: UIApplication,
          didFailToRegisterForRemoteNotificationsWithError error: Error
      ) {
          print("APNs registration failed: \(error.localizedDescription)")
      }

      func application(
          _ application: UIApplication,
          open url: URL,
          options: [UIApplication.OpenURLOptionsKey: Any] = [:]
      ) -> Bool {
          return GIDSignIn.sharedInstance.handle(url)
      }

      func application(
          _ application: UIApplication,
          didReceiveRemoteNotification userInfo: [AnyHashable: Any],
          fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
      ) {
          NotifierManager.shared.onApplicationDidReceiveRemoteNotification(userInfo: userInfo)
          completionHandler(.newData)
      }
  }
  ```

### C5 — `GoogleSignInBridgeImpl.swift`

- [ ] Create `iosApp/iosApp/GoogleSignInBridgeImpl.swift`:
  ```swift
  import Foundation
  import UIKit
  import GoogleSignIn
  import ComposeApp

  class GoogleSignInBridgeImpl: NSObject, GoogleSignInBridgeProtocol {

      func signIn(completionHandler: @escaping (NSString?, Error?) -> Void) {
          guard let presentingVC = topViewController() else {
              completionHandler(nil, NSError(
                  domain: "GoogleSignIn",
                  code: -1,
                  userInfo: [NSLocalizedDescriptionKey: "No presenting view controller available"]
              ))
              return
          }

          GIDSignIn.sharedInstance.signIn(withPresenting: presentingVC) { result, error in
              if let error = error {
                  completionHandler(nil, error)
                  return
              }
              guard let idToken = result?.user.idToken?.tokenString else {
                  completionHandler(nil, NSError(
                      domain: "GoogleSignIn",
                      code: -2,
                      userInfo: [NSLocalizedDescriptionKey: "Google Sign-In: missing id token"]
                  ))
                  return
              }
              completionHandler(idToken as NSString, nil)
          }
      }

      func signOut(completionHandler: @escaping (Error?) -> Void) {
          GIDSignIn.sharedInstance.signOut()
          completionHandler(nil)
      }

      private func topViewController() -> UIViewController? {
          let scene = UIApplication.shared.connectedScenes
              .compactMap { $0 as? UIWindowScene }
              .first { $0.activationState == .foregroundActive }
          guard let window = scene?.windows.first(where: { $0.isKeyWindow }) else {
              return nil
          }
          var top = window.rootViewController
          while let presented = top?.presentedViewController {
              top = presented
          }
          return top
      }
  }
  ```

  Critical: Kotlin `suspend fun signIn(): String` generates ObjC method with completion handler `(NSString * _Nullable, NSError * _Nullable) -> void`. Swift conformance uses `NSString?` not `String?`. Verify in `ComposeApp-Swift.h`. Protocol name: `GoogleSignInBridge` → `GoogleSignInBridgeProtocol` (Kotlin/Native ObjC export naming rule).

### C6 — `iosApp.entitlements`

- [ ] Create `iosApp/iosApp/iosApp.entitlements`:
  ```xml
  <?xml version="1.0" encoding="UTF-8"?>
  <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
    "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
  <plist version="1.0">
  <dict>
      <key>com.apple.developer.applesignin</key>
      <array>
          <string>Default</string>
      </array>
      <key>aps-environment</key>
      <string>development</string>
  </dict>
  </plist>
  ```
  Change `aps-environment` to `production` for archived/TestFlight builds.

### C7 — `LaunchScreen.storyboard`

- [ ] Create minimal launch storyboard at `iosApp/iosApp/LaunchScreen.storyboard` (use Xcode's File → New → File → Storyboard template with empty view controller; system background color).

### C8 — `Assets.xcassets`

- [ ] Create `iosApp/iosApp/Assets.xcassets/Contents.json`:
  ```json
  { "info": { "author": "xcode", "version": 1 } }
  ```
- [ ] Create `iosApp/iosApp/Assets.xcassets/AccentColor.colorset/Contents.json`:
  ```json
  {
    "colors": [{ "idiom": "universal" }],
    "info": { "author": "xcode", "version": 1 }
  }
  ```
- [ ] Create `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/Contents.json`:
  ```json
  {
    "images": [
      {
        "idiom": "universal",
        "platform": "ios",
        "size": "1024x1024",
        "filename": "AppIcon-1024.png",
        "scale": "1x"
      }
    ],
    "info": { "author": "xcode", "version": 1 }
  }
  ```
- [ ] Generate `AppIcon-1024.png` (1024×1024). If only Android assets available:
  ```bash
  sips -z 1024 1024 \
    app/build/intermediates/packaged_res/release/packageReleaseResources/mipmap-xxxhdpi-v4/ic_launcher.png \
    --out iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png
  ```
  Refine icon work in Phase 7.

### C9 — `GoogleService-Info.plist`

- [ ] Copy from Firebase Console download:
  ```bash
  cp /path/to/downloaded/GoogleService-Info.plist iosApp/iosApp/GoogleService-Info.plist
  ```
- [ ] Add to `.gitignore`:
  ```
  iosApp/iosApp/GoogleService-Info.plist
  ```
- [ ] In Xcode, add to `iosApp` target so it's bundled.

### C10 — `Config.xcconfig`

- [ ] Create `iosApp/Config.xcconfig`:
  ```
  REVERSED_CLIENT_ID = com.googleusercontent.apps.YOUR_PROJECT_NUMBER-HASH
  FCM_SERVER_KEY =
  ```
- [ ] Add to `.gitignore`. Developers populate `FCM_SERVER_KEY` locally; CI sets via `$FCM_SERVER_KEY` env var.

### C11 — Xcode project creation

- [ ] Open Xcode 16 → File → New → Project → iOS → App.
  - Name: `iosApp`
  - Team: `$TEAM_ID`
  - Bundle ID: `ru.dvfu.appliances`
  - Interface: SwiftUI; Language: Swift
  - Tests: unchecked
  - Save: `iosApp/`
  - "Create Git repository": unchecked

- [ ] In Xcode navigator:
  1. Delete generated `ContentView.swift` and `iosAppApp.swift`.
  2. Add Existing Files: select all the files created in C2–C10. Uncheck "Copy items if needed".
  3. Signing & Capabilities → Automatic signing, Team `$TEAM_ID`.
  4. Build Settings → "Code Signing Entitlements" → `iosApp/iosApp.entitlements`.
  5. iOS Deployment Target: 14.0.
  6. Marketing Version: 1.1.0; Build: 7.
  7. Configurations → set `Config.xcconfig` for Debug and Release.

### C12 — Framework reference in Xcode

- [ ] Build framework first:
  ```bash
  ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
  ```
- [ ] Target → General → Frameworks, Libraries, and Embedded Content → `+` Add Other → Add Files.
  - Path: `composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework`
  - Embed: "Do Not Embed" (static).
- [ ] Build Settings → Framework Search Paths:
  - Debug Simulator: `$(SRCROOT)/../composeApp/build/bin/iosSimulatorArm64/debugFramework`
  - Debug Device: `$(SRCROOT)/../composeApp/build/bin/iosArm64/debugFramework`
  - Release Device: `$(SRCROOT)/../composeApp/build/bin/iosArm64/releaseFramework`

- [ ] Add Run Script build phase (before Compile Sources):
  - Shell: `/bin/zsh`
  - Script:
    ```zsh
    cd "${SRCROOT}/.."
    if [[ "$PLATFORM_NAME" == *simulator* ]]; then
        ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
    else
        ./gradlew :composeApp:linkDebugFrameworkIosArm64
    fi
    ```
  - "Based on dependency analysis": unchecked.

### C13 — SPM dependencies

- [ ] File → Add Package Dependencies:
  1. `https://github.com/google/GoogleSignIn-iOS` → 7.0.0+ → product `GoogleSignIn` → target `iosApp`.
  2. `https://github.com/firebase/firebase-ios-sdk` → 11.0.0+ → select:
     - `FirebaseAuth`, `FirebaseFirestore`, `FirebaseDatabase`, `FirebaseStorage`, `FirebaseAnalytics`, `FirebaseCrashlytics`, `FirebaseRemoteConfig`, `FirebaseMessaging`
     - **Do NOT select:** `FirebasePerformance`, `FirebaseInAppMessaging`, `FirebaseInAppMessagingSwift`

### C14 — Verify KMPNotifier Swift API names

- [ ] After framework link:
  ```bash
  grep -i "NotifierManager\|NotificationPlatformConfiguration" \
    composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework/Headers/ComposeApp.h
  ```
- [ ] Confirm names match what `AppDelegate.swift` references. Update AppDelegate if names differ.

---

## Section D — End-to-end build and launch

### D1 — Build framework

- [ ] `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`
- [ ] Confirm: `file composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework/ComposeApp` shows Mach-O 64-bit.

### D2 — Build Xcode project

- [ ] Open `iosApp/iosApp.xcodeproj`. Scheme `iosApp`. Destination: iPhone 16 Simulator.
- [ ] Product → Build (`Cmd+B`).
- [ ] Most likely error sources:
  - `GoogleSignInBridgeProtocol` name mismatch — check generated header.
  - `MainViewControllerKt.MainViewController` parameter label mismatch — check generated header.
  - KMPNotifier types not found — version mismatch; bump in `libs.versions.toml`.

- [ ] CI build:
  ```bash
  xcodebuild \
    -project iosApp/iosApp.xcodeproj \
    -scheme iosApp \
    -sdk iphonesimulator \
    -destination 'platform=iOS Simulator,name=iPhone 16,OS=latest' \
    -configuration Debug \
    CODE_SIGN_IDENTITY="" \
    CODE_SIGNING_REQUIRED=NO \
    build | xcbeautify
  ```

### D3 — Launch in Simulator

```bash
xcrun simctl boot "iPhone 16"
open -a Simulator

DERIVED_DATA=$(xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp -showBuildSettings 2>/dev/null | grep -m 1 BUILT_PRODUCTS_DIR | awk '{print $3}')

xcrun simctl install booted "$DERIVED_DATA/iosApp.app"
xcrun simctl launch --console booted ru.dvfu.appliances
```

Expected: Firebase config + Koin init logs, sign-in screen renders.

### D4 — Android regression check

- [ ] `./gradlew :androidApp:assembleDebug` — zero errors.

---

## Section E — Phase review

### E1 — Automated checks

- [ ] `./gradlew :composeApp:compileKotlinIosSimulatorArm64` — green.
- [ ] `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` — framework produced.
- [ ] `./gradlew :androidApp:assembleDebug` — green.
- [ ] `./gradlew :composeApp:iosSimulatorArm64Test` — `DataStorePathTest` and `FcmServerKeyTest` pass.

### E2 — Code reviewer dispatch

Dispatch `feature-dev:code-reviewer` on Phase 6 diff. Focus:

**Info.plist completeness:** all URL types, usage descriptions, FCM key reference, scene manifest, ATS, launch storyboard match.

**cinterop / Swift bridge:** GIDSignIn 7.x API correct (not deprecated 5.x); completion handler returns `NSString?`; `AuthenticationServices` from `platform.AuthenticationServices.*`; `cancel()` on cancellation; `NSObject` superclass present.

**Memory ownership:** Modern KMP MM (default since 1.7.20); no `freeze()`; delegate retained correctly; bridge is app-lifetime.

**Framework linking:** `isStatic = true`; "Do Not Embed"; `FirebaseApp.configure()` first.

### E3 — Manual smoke test

On Simulator (Russian locale set via Settings):
1. App launches in <5s.
2. Sign-in screen with "Войти с помощью Google" / "Войти с помощью Apple".
3. No crashes; Koin + Firebase logs visible.

On physical iPhone (iOS 14+):
1. Google sign-in completes, navigates home.
2. Apple sign-in completes (requires capability + dev account).
3. Push permission prompt appears on first relevant action.
4. Russian Cyrillic renders cleanly.

---

## Appendix — Path 1: cinterop for GoogleSignIn-iOS (NOT chosen)

For completeness only:

`composeApp/src/iosMain/cinterop/GoogleSignIn.def`:
```
language = Objective-C
headers = GoogleSignIn/GIDSignIn.h
headerFilter = GoogleSignIn/**
package = cocoapods.GoogleSignIn
linkerOpts = -framework GoogleSignIn
```

Why this doesn't work cleanly: GoogleSignIn XCFramework is an SPM dep in `iosApp/`, not visible to Kotlin compiler. Headers at machine-specific SPM cache paths. Workarounds (CocoaPods interop or pre-extract headers) add significant build complexity. **Path 2 (Swift bridge) is strictly preferable.**

---

## Risk register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Generated ObjC protocol name mismatch | High | Medium | Inspect generated header after B9; update Swift files. |
| `suspend fun` ObjC completion handler signature mismatch | Medium | High | Check generated header; use `NSString?` if ObjC types. |
| KMPNotifier Swift API name differences | Medium | Medium | Inspect header (C14); update AppDelegate. |
| `ASAuthorizationController.performRequests()` thread issue | Low | High | `suspendCancellableCoroutine` runs on Main by default. Wrap in `withContext(Dispatchers.Main)` if needed. |
| `FirebaseApp.configure()` called twice | Low | High | KMPNotifier doesn't call `configure()`. Verify in source. |
| APNs token format mismatch | Low | Medium | Check KMPNotifier docs; some accept `Data` directly. |
| Bundle ID not registered in App Store Connect | Medium | High | Phase 0 prereq. |

---

## 5-line summary

1. Most likely failure: **D2 / C5 (GoogleSignInBridgeImpl compiling against generated protocol)** — Kotlin/Native ObjC export name depends on package + Kotlin version, must be verified empirically from generated header.
2. Second-highest risk: **B7 NSData→String** — byte-extraction varies between Kotlin/Native versions; `toByteArray().decodeToString()` is cleanest if `kotlinx-io` is transitive.
3. Critical path: A1–A5 (catalog + build.gradle) blocks all subsequent sections.
4. Most time-consuming manual step: C11–C13 Xcode project creation (GUI required; `project.pbxproj` not hand-authorable).
5. End state: Kotlin framework links; Xcode builds; app launches in Simulator showing Russian sign-in screen; Android build remains green.
