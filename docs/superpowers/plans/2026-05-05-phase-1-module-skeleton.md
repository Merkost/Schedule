# Phase 0+1 — Prereqs + Module Skeleton — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure the single `:app` module into `:composeApp` (KMP library with `androidTarget()` only) and `:androidApp` (thin Android application launcher), moving all ~115 Kotlin source files to `composeApp/src/androidMain/kotlin/` unchanged, so `./gradlew :androidApp:assembleDebug` produces a functionally identical APK.

**Architecture:** `:composeApp` is a `com.android.library` + `kotlin.multiplatform` module that owns all Kotlin code and resources; `:androidApp` is a thin `com.android.application` that depends on `:composeApp` and carries the manifest, `google-services.json`, and signing config. Designed so iOS targets (`iosX64`, `iosArm64`, `iosSimulatorArm64`) can be added to `:composeApp` in Phase 6 with zero source refactor.

**Tech Stack:** Kotlin 2.3.20, AGP 9.0.1, Compose Multiplatform plugin 1.10.x (alias added now, applied in Phase 6), Gradle 9.4.1, Koin 4.2.1.

---

## Phase 0 — Prerequisites (User Actions)

### T-00: Apple Developer + Firebase iOS Setup

👤 User actions — not code. Complete before starting Phase 1.

- [ ] 👤 Confirm Apple Developer Program membership active at developer.apple.com/account. Note **Team ID** (10-char string under Membership).
- [ ] 👤 In App Store Connect, create iOS App: bundle ID `ru.dvfu.appliances`, SKU `schedule-ios`, primary language Russian. Save Apple ID number.
- [ ] 👤 In Firebase Console → existing project → Project settings → Add app → iOS. Bundle ID `ru.dvfu.appliances`, nickname "Schedule iOS". Download `GoogleService-Info.plist` (used in Phase 6, not Phase 1).
- [ ] 👤 Firebase Console → Cloud Messaging → APNs Authentication Key: Upload a `.p8` from developer.apple.com → Keys (with APNs capability). Record Key ID and Team ID.
- [ ] 👤 Verify `app/google-services.json` exists with `package_name: ru.dvfu.appliances`.

### T-01: Toolchain Verification

- [ ] 👤 `xcodebuild -version` → `Xcode 16.x`.
- [ ] 👤 `brew install kdoctor && kdoctor`. Fix any errors before Phase 6.
- [ ] 👤 Android Studio → Settings → Build → Gradle → Gradle JVM = JDK 21.
- [ ] 👤 Branch confirmed: `git checkout -b feature/cmp-ios-migration` (already done — this plan executes on this branch).

### T-02: Baseline Green Build

- [ ] `./gradlew --version` → contains `Gradle 9.4.1`.
- [ ] `./gradlew :app:assembleDebug` succeeds (baseline before changes).
- [ ] Commit baseline: `git add -A && git commit --allow-empty -m "chore: baseline before Phase 1 module split"`.

---

## Phase 1 — Module Skeleton

### T-03: CMP Repository Decision

- [ ] CMP 1.10.x is on Maven Central (stable). No `maven.pkg.jetbrains.space/public/p/compose/dev` repo entry needed in `settings.gradle.kts`.

### T-04: Add Plugin Aliases to `gradle/libs.versions.toml`

- [ ] In `[versions]`:
  ```toml
  composeMultiplatform = "1.10.3"
  ```
- [ ] In `[plugins]`:
  ```toml
  android-library = { id = "com.android.library", version.ref = "agp" }
  kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
  compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
  ```
- [ ] Verify TOML valid: `./gradlew help --quiet`.

### T-05: Create `composeApp/` skeleton

- [ ] ```bash
  mkdir -p composeApp/src/androidMain/kotlin
  mkdir -p composeApp/src/androidMain/res
  mkdir -p composeApp/src/commonMain/kotlin
  mkdir -p composeApp/src/commonTest/kotlin
  ```
- [ ] Create `composeApp/consumer-rules.pro`:
  ```
  -keep class ru.dvfu.appliances.model.repository.entity.** { *; }
  -keep class ru.dvfu.appliances.compose.home.SelectedDate { *; }
  -keep class * implements android.os.Parcelable { public static final android.os.Parcelable$Creator *; }
  -keep class org.koin.** { *; }
  -keepnames class * extends org.koin.core.module.Module
  -keep class com.google.firebase.** { *; }
  -keep class com.google.android.gms.** { *; }
  -dontwarn com.crashlytics.**
  -keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
  -keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
  -keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
  ```
- [ ] Create `composeApp/src/androidMain/AndroidManifest.xml`:
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <manifest />
  ```

### T-06: Write `composeApp/build.gradle.kts`

```kotlin
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val fcmServerKey: String = localProps.getProperty("FCM_SERVER_KEY")
    ?: System.getenv("FCM_SERVER_KEY")
    ?: ""

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
}

kotlin {
    androidTarget {
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_21)
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.kotlinx.coroutines.play.services)

            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.appcompat)
            implementation(libs.google.material)
            implementation(libs.androidx.constraintlayout)
            implementation(libs.androidx.splashscreen)
            implementation(libs.androidx.preference.ktx)
            implementation(libs.androidx.work.runtime.ktx)

            implementation(libs.androidx.navigation.fragment.ktx)
            implementation(libs.androidx.navigation.ui.ktx)
            implementation(libs.androidx.navigation.compose)

            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.viewmodel.ktx)
            implementation(libs.androidx.lifecycle.livedata.ktx)
            implementation(libs.androidx.datastore.preferences)

            implementation(platform(libs.androidx.compose.bom))
            implementation(libs.androidx.compose.runtime)
            implementation(libs.androidx.compose.foundation)
            implementation(libs.androidx.compose.foundation.layout)
            implementation(libs.androidx.compose.ui)
            implementation(libs.androidx.compose.ui.util)
            implementation(libs.androidx.compose.material3)
            implementation(libs.androidx.compose.material3.window.size)
            implementation(libs.androidx.compose.material.icons.extended)
            implementation(libs.androidx.compose.animation)
            implementation(libs.androidx.compose.ui.tooling)
            implementation(libs.androidx.constraintlayout.compose)
            implementation(libs.lottie.compose)

            debugImplementation(libs.androidx.compose.ui.test.manifest)

            implementation(libs.koin.android)
            implementation(libs.koin.android.compat)
            implementation(libs.koin.androidx.workmanager)
            implementation(libs.koin.androidx.compose)

            implementation(libs.accompanist.permissions)

            implementation(libs.glide)
            implementation(libs.coil.compose)

            implementation(libs.retrofit)
            implementation(libs.retrofit.converter.gson)
            implementation(libs.okhttp.logging.interceptor)
            implementation(libs.gson)

            implementation(platform(libs.firebase.bom))
            implementation(libs.firebase.messaging)
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
            implementation(libs.firebase.perf)
            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.database)
            implementation(libs.firebase.storage)
            implementation(libs.firebase.inappmessaging.display)
            implementation(libs.firebase.ui.auth)
            implementation(libs.play.services.auth)

            implementation(libs.compose.calendar)

            debugImplementation(libs.leakcanary)
        }
    }
}

android {
    namespace = "ru.dvfu.appliances"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            buildConfigField("boolean", "USE_MOCK_REPOS", "false")
            buildConfigField("String", "FCM_SERVER_KEY", "\"$fcmServerKey\"")
        }
        debug {
            buildConfigField("boolean", "USE_MOCK_REPOS", "false")
            buildConfigField("String", "FCM_SERVER_KEY", "\"$fcmServerKey\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}
```

### T-07: `git mv` Kotlin sources

```bash
git mv app/src/main/java/ru/dvfu/appliances composeApp/src/androidMain/kotlin/ru/dvfu/appliances
```

- [ ] Verify: `find composeApp/src/androidMain/kotlin -name "*.kt" | wc -l` ≈ 115.
- [ ] `git status | grep renamed | head -5` shows `renamed:` for moved files.

### T-08: `git mv` resources

```bash
git mv app/src/main/res composeApp/src/androidMain/res
```

- [ ] Verify: `ls composeApp/src/androidMain/res/` shows `values`, `values-ru`, `mipmap-*`, `drawable*`, `layout`.

### T-09: Create `androidApp/` skeleton

```bash
mkdir -p androidApp/src/main
```

### T-10: Write `androidApp/build.gradle.kts`

```kotlin
import java.util.Properties

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
}

android {
    namespace = "ru.dvfu.appliances.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    signingConfigs {
        create("release") {
            val storePath = keystoreProps.getProperty("storeFile")
                ?: System.getenv("SCHEDULE_KEYSTORE_FILE")
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = keystoreProps.getProperty("storePassword")
                    ?: System.getenv("SCHEDULE_KEYSTORE_PASSWORD")
                keyAlias = keystoreProps.getProperty("keyAlias")
                    ?: System.getenv("SCHEDULE_KEY_ALIAS")
                keyPassword = keystoreProps.getProperty("keyPassword")
                    ?: System.getenv("SCHEDULE_KEY_PASSWORD")
            }
        }
    }

    defaultConfig {
        applicationId = "ru.dvfu.appliances"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro", getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            extra["enableCrashlytics"] = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":composeApp"))

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
```

### T-11: `androidApp/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name="ru.dvfu.appliances.application.Schedule"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Schedule">

        <service
            android:name="ru.dvfu.appliances.MyFirebaseMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT"/>
            </intent-filter>
        </service>

        <meta-data
            android:name="firebase_performance_logcat_enabled"
            android:value="true" />

        <activity
            android:exported="true"
            android:name="ru.dvfu.appliances.ui.SplashScreen"
            android:theme="@style/SplashTheme">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <activity
            android:exported="true"
            android:name="ru.dvfu.appliances.ui.LoginActivity">
        </activity>
        <activity
            android:exported="true"
            android:screenOrientation="portrait"
            android:windowSoftInputMode="adjustResize"
            android:name="ru.dvfu.appliances.compose.MainActivity"
            android:theme="@style/Theme.Schedule.NoActionBar" />

    </application>

</manifest>
```

### T-12: `git mv` `google-services.json`

```bash
git mv app/google-services.json androidApp/google-services.json
```

### T-13: Copy `proguard-rules.pro` to `androidApp/`

Same content as current `app/proguard-rules.pro` (~57 lines):

```
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep public class * extends java.lang.Exception

-keep class ru.dvfu.appliances.model.repository.entity.** { *; }
-keep class ru.dvfu.appliances.compose.home.SelectedDate { *; }

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-dontwarn sun.misc.**

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

-keep class org.koin.** { *; }
-keepnames class * extends org.koin.core.module.Module

-keep class com.google.firebase.** { *; }
-keep class com.crashlytics.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.crashlytics.**

-keep class androidx.compose.runtime.** { *; }

-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
```

### T-14: Update `settings.gradle.kts`

Replace entire file:

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Schedule"
include(":composeApp", ":androidApp")
```

### T-15: Delete `:app` module

```bash
git rm -r --cached app/
rm -rf app/
```

Commit:
```bash
git add -A
git commit -m "feat: Phase 1 module skeleton — split :app into :composeApp + :androidApp"
```

### T-16: First Gradle sync

- [ ] `./gradlew :composeApp:tasks --quiet` — task list, no errors.
- [ ] `./gradlew :androidApp:tasks --quiet` — task list, no errors.
- [ ] If `Could not resolve` errors, verify alias names in `composeApp/build.gradle.kts` match `libs.versions.toml`.

### T-17: Verify SnackbarManager package reference

- [ ] `grep -n "package" composeApp/src/androidMain/kotlin/ru/dvfu/appliances/compose/components/SnackbarManager.kt` — `1:package ru.dvfu.appliances.application` (file in `compose/components/` but declares `application` package — valid, unchanged).
- [ ] `grep -n "SnackbarManager" composeApp/src/androidMain/kotlin/ru/dvfu/appliances/di/KoinModules.kt` — import + usage referencing correct package.

### T-18: First build

```bash
./gradlew :androidApp:assembleDebug 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL`. If failed, proceed to T-19.

### T-19: Fix compilation errors (if any)

**Common errors:**

| Error | Cause | Fix |
|---|---|---|
| `Unresolved reference: BuildConfig` | Should be auto-generated | Verify `buildConfig = true` in `composeApp/build.gradle.kts` |
| `package ru.dvfu.appliances.databinding does not exist` | ViewBinding remnant | `grep -r "databinding\|ViewBinding" composeApp/src/androidMain/kotlin/`. Remove imports if found (no ViewBinding actually used). |
| `google-services plugin requires Application` | Plugin on wrong module | Verify plugin only on `androidApp`, not `composeApp` |
| `Namespace not specified` | Missing namespace | Verify `namespace = "ru.dvfu.appliances"` in `composeApp` and `"ru.dvfu.appliances.app"` in `androidApp` |

- [ ] `./gradlew :androidApp:assembleDebug 2>&1 | grep -E "^e:|error:" | head -20`.
- [ ] Apply fixes; repeat until green.

### T-20: Verify signing config

- [ ] `cat keystore.properties` shows `storeFile=../keystore/schedule.jks`.
- [ ] `file(storePath)` in `androidApp/build.gradle.kts` resolves relative to `androidApp/`. So `../keystore/schedule.jks` from `androidApp/` → `<root>/keystore/schedule.jks`. Verify file exists.
- [ ] `./gradlew :androidApp:assembleRelease` — green if keystore present. Otherwise note for CI.

### T-21: Green debug build confirmation

```bash
./gradlew :androidApp:assembleDebug
```

- [ ] `BUILD SUCCESSFUL`.
- [ ] APK exists: `ls androidApp/build/outputs/apk/debug/androidApp-debug.apk` — file >10MB.

### T-22: Verify BuildConfig fields

- [ ] `grep -n "BuildConfig" composeApp/src/androidMain/kotlin/ru/dvfu/appliances/application/Schedule.kt` shows references.
- [ ] `./gradlew :composeApp:generateDebugBuildConfig` — green.
- [ ] `cat composeApp/build/generated/source/buildConfig/androidMain/debug/ru/dvfu/appliances/BuildConfig.kt` contains `FCM_SERVER_KEY`, `USE_MOCK_REPOS`.

### T-23: Unit test compilation check

- [ ] `./gradlew :composeApp:compileDebugUnitTestKotlin` — green.
- [ ] `./gradlew :androidApp:compileDebugUnitTestKotlin` — green.

### T-24: Commit checkpoint

```bash
git add -A
git status
```

If changes from T-19 fixes:
```bash
git commit -m "fix: Phase 1 compilation fixes after module split"
```

- [ ] `git log --oneline -3` shows recent commits.
- [ ] `git push -u origin feature/cmp-ios-migration`.

### T-25: Runtime smoke test

- [ ] `adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk` → `Success`.
- [ ] Launch app on device:
  - Splash screen renders (gradient background, schedule icon animates).
  - Routes to LoginActivity if not signed in, MainActivity if signed in.
  - Google Sign-In button on login screen.
  - Sign-in completes successfully.
  - Main screen loads — calendar with week view.
  - Push notification received (Firebase Console → Cloud Messaging → Send test to FCM token from logcat).
  - No crashes.
- [ ] `adb logcat -s "SCHEDULE" | head -20` — no `FATAL EXCEPTION`.

### T-26: Phase Review — code-reviewer dispatch

- [ ] Dispatch `feature-dev:code-reviewer` on diff `master..feature/cmp-ios-migration`. Reviewer verifies:
  - `settings.gradle.kts` includes `:composeApp` + `:androidApp`, excludes `:app`.
  - `composeApp/build.gradle.kts` has `androidTarget()` in `kotlin {}` (with comment marking iOS targets placeholder).
  - `androidApp/build.gradle.kts` has `implementation(project(":composeApp"))` + Firebase plugins.
  - No Kotlin source file modified (only `git mv`).
  - `google-services.json` in `androidApp/`.
  - `consumer-rules.pro` in `composeApp/` has all keep rules from original proguard.
  - `libs.versions.toml` has new entries; no original entries removed.
  - No M2 imports introduced.
- [ ] Address reviewer comments.
- [ ] Final green build: `./gradlew :androidApp:assembleDebug`.
- [ ] Tag: `git tag phase-1-complete && git push origin phase-1-complete`.

---

## Phase Exit State

```
Schedule/
├── composeApp/                            # KMP library (androidTarget only)
│   ├── src/androidMain/kotlin/            # All ~115 .kt files (unchanged)
│   ├── src/androidMain/res/               # All original resources
│   ├── src/androidMain/AndroidManifest.xml  # Minimal <manifest />
│   ├── src/commonMain/kotlin/             # Empty (Phase 2 populates)
│   ├── consumer-rules.pro
│   └── build.gradle.kts
├── androidApp/                            # Thin launcher
│   ├── src/main/AndroidManifest.xml       # Full app manifest
│   ├── google-services.json
│   ├── proguard-rules.pro
│   └── build.gradle.kts
├── gradle/libs.versions.toml             # Added composeMultiplatform + 3 plugin aliases
├── settings.gradle.kts                   # :composeApp + :androidApp
└── keystore/schedule.jks                  # Unchanged
```

**What works:** `./gradlew :androidApp:assembleDebug` and `:assembleRelease` produce APKs functionally identical to the original `:app:assemble*`.

**Deferred to Phase 2:** Moving files from `androidMain/kotlin/` to `commonMain/kotlin/`.

**Top 2 risks:**
1. **Keystore path resolution:** `file(storePath)` in `androidApp/build.gradle.kts` resolves relative to `androidApp/`. The path `../keystore/schedule.jks` resolves correctly to root-level `keystore/`. If `keystore.properties` uses absolute or different relative path, signing fails silently.
2. **`google-services` plugin placement:** Must be on application module (`androidApp`) only. If accidentally on `composeApp` (library), build fails immediately with clear error.

---

**Summary:** 26 task groups; ~650 lines; exit state has `:app` deleted, `:composeApp` + `:androidApp` modules with `./gradlew :androidApp:assembleDebug` green and runtime behavior identical to baseline.
