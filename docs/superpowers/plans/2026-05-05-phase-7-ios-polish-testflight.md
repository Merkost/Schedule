# Phase 7+8 — iOS Polish + TestFlight — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Prerequisites:** Plans 1–6 complete; `feature/cmp-ios-migration` builds green on Android and iOS Simulator.

**Goal:** Polish the iOS build to feature parity with Android, add App Store readiness artifacts, archive, and upload to TestFlight Internal.

**Architecture:** Layered cleanup — UI insets first (correctness), then platform-specific gaps (image picker), then visual consistency (fonts), then App Store gates (PrivacyInfo, dSYM, capabilities), then archive + upload. Every Scaffold uses `WindowInsets.safeDrawing`; SwiftUI host uses `.ignoresSafeArea(.container, edges: .all)` so Compose receives UIKit safe areas.

**Tech Stack:** Compose Multiplatform iOS, App Store Connect, TestFlight, Xcode 16+, KMPNotifier, Gitlive Firebase, PHPickerViewController, Crashlytics dSYM.

---

## Verify before starting

- [ ] Plans 1–6 complete; both Android and iOS Simulator builds green.
- [ ] Physical iPhone (iOS 17+) enrolled in Apple Developer account.
- [ ] `iosApp/iosApp/GoogleService-Info.plist` present.
- [ ] APNs `.p8` key uploaded in Firebase Console → Project Settings → Cloud Messaging.
- [ ] App Store Connect listing exists with bundle id `ru.dvfu.appliances`.
- [ ] Xcode 16+ with Command Line Tools; `xcrun simctl list` works.
- [ ] `kdoctor` reports no errors.
- [ ] Env: `APPLE_TEAM_ID`, `APP_STORE_CONNECT_API_KEY_ID` (needed for upload only).

---

## Section A — iOS UI Polish

### A-1: Simulator screenshot baseline

Open every screen in iPhone 16 Simulator (iOS 18) and capture screenshots:
- Sign-in (email, Google, Apple)
- Home / calendar (month + week views)
- Booking create sheet
- Booking list
- Event info sheet
- Profile, Edit profile
- Appliance list + detail
- Settings

Save under `docs/superpowers/ios-screenshots/baseline/`. Document visible issues:

| Screen | Issue observed | Root cause | Fix task |
|---|---|---|---|
| Sign-in | Content starts behind status bar | Missing top inset | A-2 |
| Home calendar | Bottom nav overlaps home indicator | Missing bottom inset | A-3 |
| All form screens | Keyboard hides active TextField | No imePadding | A-4 |
| Russian text | Cyrillic in SF Pro (thinner vs Android) | Font family not pinned | A-5 |
| Edit profile photo | Tapping crashes (Android picker) | expect/actual not wired | A-6 |
| Week calendar | Scroll jank on device | Dense recomposition | A-7 |

Commit: `chore: add iOS simulator baseline screenshots`

### A-2: Top-bar / status-bar inset audit

**Root cause:** `ScheduleApp` calls `Scaffold(contentWindowInsets = WindowInsets(0))`; `iOSApp.swift` uses `.ignoresSafeArea(.all)`. Top-bar content area is unprotected.

Part 1 — `iosApp/iosApp/iOSApp.swift`:
```swift
import SwiftUI

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.container, edges: .all)
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

Part 2 — `composeApp/src/commonMain/.../ScheduleApp.kt`: change `contentWindowInsets = WindowInsets(0)` to `contentWindowInsets = WindowInsets.safeDrawing`. Existing `consumeWindowInsets(innerPadding)` in NavHost prevents double-application.

Verify: status-bar area clear on all screens.

Commit: `fix(ios): restore safeDrawing insets — ScheduleApp + iOSApp.swift`

### A-3: Bottom navigation safe-area

With A-2's `WindowInsets.safeDrawing`, Scaffold pads bottom bar slot automatically. Verify `ScheduleBottomBar` doesn't override with `windowInsets = WindowInsets(0)` on `NavigationBar`. Remove if present.

Test on iPhone 15 Pro (Face ID): NavigationBar labels not obscured by home indicator.

Commit: `fix(ios): bottom nav safe area inherits from Scaffold`

### A-4: IME / keyboard inset audit

**Affected screens:** `LoginScreen`, `EditProfile`, `AddEvent`, `EventInfo`, `BookingScreens`.

Pattern — apply `imePadding()` to inner scrollable, NOT the Scaffold:
```kotlin
Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = { ... },
) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) { /* TextFields */ }
}
```

`ComposeUIViewController` forwards UIKit keyboard insets automatically. No Swift code needed.

Verify: tap bottom-most TextField on each screen; keyboard does not overlap.

Commit: `fix(ios): imePadding on all form screens for keyboard avoidance`

### A-5: Bundle Roboto for Cyrillic consistency

Step 1 — add fonts to `composeApp/src/commonMain/composeResources/font/`:
- `Roboto_Regular.ttf`
- `Roboto_Medium.ttf`
- `Roboto_Bold.ttf`

(Underscores, not hyphens — Compose Resources accessor name derives from filename.)

Step 2 — `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/ui/theme/AppFonts.kt`:
```kotlin
package ru.dvfu.appliances.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import schedule.composeapp.generated.resources.Res
import schedule.composeapp.generated.resources.Roboto_Bold
import schedule.composeapp.generated.resources.Roboto_Medium
import schedule.composeapp.generated.resources.Roboto_Regular

val RobotoFontFamily: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.Roboto_Regular, FontWeight.Normal),
        Font(Res.font.Roboto_Medium, FontWeight.Medium),
        Font(Res.font.Roboto_Bold, FontWeight.Bold),
    )
```

Step 3 — apply in `ScheduleTheme.kt`: build a `Typography` referencing `RobotoFontFamily` for all text styles. Pass to `MaterialTheme(typography = scheduleTypography)`.

Step 4 — verify on iOS Simulator with Russian locale: every screen shows Cyrillic in Roboto matching Android.

Commit: `feat(ios): bundle Roboto for Cyrillic cross-platform consistency`

### A-6: Image picker expect/actual

`composeApp/src/commonMain/kotlin/ru/dvfu/appliances/platform/ImagePicker.kt`:
```kotlin
package ru.dvfu.appliances.platform

import androidx.compose.runtime.Composable

interface ImagePickerLauncher {
    fun launch()
}

@Composable
expect fun rememberImagePickerLauncher(
    onImageSelected: (ByteArray) -> Unit,
): ImagePickerLauncher
```

`composeApp/src/androidMain/kotlin/ru/dvfu/appliances/platform/ImagePicker.android.kt`:
```kotlin
package ru.dvfu.appliances.platform

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream

@Composable
actual fun rememberImagePickerLauncher(
    onImageSelected: (ByteArray) -> Unit,
): ImagePickerLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        onImageSelected(stream.toByteArray())
    }
    return remember(launcher) {
        object : ImagePickerLauncher {
            override fun launch() {
                launcher.launch(ActivityResultContracts.PickVisualMedia.ImageOnly)
            }
        }
    }
}
```

`composeApp/src/iosMain/kotlin/ru/dvfu/appliances/platform/ImagePicker.ios.kt`:
```kotlin
package ru.dvfu.appliances.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberImagePickerLauncher(
    onImageSelected: (ByteArray) -> Unit,
): ImagePickerLauncher {
    val callback = remember { onImageSelected }
    return remember {
        object : ImagePickerLauncher {
            override fun launch() {
                val config = PHPickerConfiguration()
                config.filter = PHPickerFilter.imagesFilter
                config.selectionLimit = 1
                val picker = PHPickerViewController(configuration = config)
                val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
                    override fun picker(
                        picker: PHPickerViewController,
                        didFinishPicking: List<*>,
                    ) {
                        picker.dismissViewControllerAnimated(true, null)
                        val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
                        result.itemProvider.loadDataRepresentationForTypeIdentifier(
                            "public.jpeg"
                        ) { data, _ ->
                            val nsData = data as? NSData ?: return@loadDataRepresentationForTypeIdentifier
                            val bytes = ByteArray(nsData.length.toInt())
                            memcpy(bytes.refTo(0), nsData.bytes, nsData.length)
                            callback(bytes)
                        }
                    }
                }
                picker.delegate = delegate
                UIApplication.sharedApplication.keyWindow
                    ?.rootViewController
                    ?.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}
```

Wire into `EditProfile.kt` and appliance image upload composables: replace existing launcher with `rememberImagePickerLauncher { bytes -> viewModel.onImageSelected(bytes) }`. ViewModel's `onImageSelected(bytes)` uploads via Gitlive's Storage.

Test in commonTest:
```kotlin
package ru.dvfu.appliances.platform

import kotlin.test.Test
import kotlin.test.assertNotNull

class ImagePickerLauncherContractTest {
    @Test
    fun launcherInterfaceHasLaunchMethod() {
        val launcher = object : ImagePickerLauncher {
            override fun launch() {}
        }
        assertNotNull(launcher)
    }
}
```

Commit: `feat: image picker expect/actual — PHPickerViewController on iOS`

### A-7: Calendar performance on physical iPhone

1. Connect iPhone, run via Xcode Product → Run.
2. Profile week calendar in Xcode Debug Navigator → CPU & Memory.
3. If <50fps:
   - Wrap event-slot composables in `key(event.id) { ... }`.
   - Move color calculations into `remember(event.status) { ... }`.
   - If still slow: replace fixed-height column with `LazyColumn` of week rows.
4. If ≥55fps: document as "smooth" in smoke-test checklist.

Commit (only if changes): `perf(ios): reduce calendar recompositions in week view`

### A-8: Back gesture verification

Swipe-from-left-edge must pop on every navigable screen. Test:
- Home → Appliance Detail → swipe left
- Home → Event Info → swipe left
- Home → Edit Profile → swipe left
- Home → Add Event sheet → swipe down

If a screen blocks: check for unconditional `BackHandler(enabled = true)` and make conditional.

Commit (only if changes): `fix(ios): conditional BackHandler permits swipe-back`

### A-9: PullToRefreshBox on booking list

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingBookingsList(
    bookings: List<CalendarEvent>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullState,
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(bookings) { BookingItem(it) }
        }
    }
}
```

Add `isRefreshing` + `onRefresh` to `BookingListViewModel` if missing.

Commit: `feat: PullToRefreshBox on booking list screens`

### A-10: Russian locale full pass

Set Simulator → Settings → Language & Region → Russian.

Verify:
- All strings in Cyrillic via `Res.string.*`.
- Date formatting (month/day names) in Russian via kotlinx-datetime.
- No clipped text (Russian ~30% longer than English).
- Cyrillic in Roboto (A-5).

Add missing translations to `values-ru/strings.xml`. If date formatting falls back to English, pass `Locale.getDefault()` to format calls.

Commit: `fix(ios): Russian locale strings and date formatting complete`

### A-11: Apple Sign-In E2E on physical device

1. Tap "Sign in with Apple".
2. Face ID → authenticate.
3. `AppleSignInClient.signIn()` returns `AppleIdCredential` with `identityToken` + `rawNonce`.
4. `Firebase.auth.signInWithCredential(OAuthProvider.newCredentialWithProviderID("apple.com", idToken = sha256(rawNonce), rawNonce = rawNonce))` — **raw nonce to Apple, SHA256 hash to Firebase**.
5. App navigates to Home.
6. Sign out, sign in again — second time `displayName` is null (Apple only provides on first sign-in). Verify graceful fallback.

If `INVALID_CREDENTIAL`: check entitlements (`com.apple.developer.applesignin = ["Default"]`), Firebase Console (Apple provider enabled with bundle id), nonce flow.

Commit: `fix(ios): apple sign-in nonce flow + null displayName guard`

### A-12: Google Sign-In E2E on physical device

1. Tap "Sign in with Google".
2. `GIDSignIn.sharedInstance` shows account chooser (requires URL scheme in Info.plist).
3. Sign-in → idToken returned.
4. `Firebase.auth.signInWithCredential(GoogleAuthProvider.credential(idToken, null))`.
5. App navigates to Home.

If sheet doesn't appear: verify `AppDelegate.application(_:open:options:)` forwards URL to `GIDSignIn.sharedInstance.handle(url)`.

Commit: `fix(ios): google sign-in URL scheme + AppDelegate callback`

---

## Section B — Push Notifications on iOS

### B-1: APNs token reaches Firebase

After app launch on physical device:
1. Firebase Console → Firestore → `users` → your doc.
2. `fcmToken` field populated.

If empty: verify `AppDelegate.application(_:didRegisterForRemoteNotificationsWithDeviceToken:)` forwards to `NotifierManager.shared`. Verify init order against KMPNotifier docs.

Reference `AppDelegate.swift`:
```swift
import UIKit
import FirebaseCore
import GoogleSignIn
import KMPNotifierKit

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        FirebaseApp.configure()
        NotifierManager.shared.initialize(
            configuration: NotificationPlatformConfigurationIos(
                showPushNotification: true,
                askNotificationPermissionOnStart: false
            )
        )
        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        NotifierManager.shared.application(
            application,
            didRegisterForRemoteNotificationsWithDeviceToken: deviceToken
        )
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        return GIDSignIn.sharedInstance.handle(url)
    }
}
```

Commit: `fix(ios): AppDelegate APNs token forwarding to KMPNotifier`

### B-2: Foreground push test

```bash
FCM_TOKEN="<from Firestore>"
FCM_KEY="<FCM_SERVER_KEY from local.properties>"

curl -X POST https://fcm.googleapis.com/fcm/send \
  -H "Authorization: key=${FCM_KEY}" \
  -H "Content-Type: application/json" \
  -d "{
    \"to\": \"${FCM_TOKEN}\",
    \"notification\": {\"title\": \"Test push\", \"body\": \"Phase 7 test\"},
    \"data\": {\"notificationType\": \"DEFAULT\"}
  }"
```

Expected: `{"multicast_id":...,"success":1,"failure":0}` and in-app banner on device.

If `failure:1, "InvalidRegistration"`: token stale. Re-sign-in or refresh token.

Commit: `test(ios): foreground push verified`

### B-3: Background push + tap routing

App killed (swipe up):
1. Send curl from B-2.
2. Notification in tray.
3. Tap → `Listener.onNotificationClicked(data)` fires with `notificationType`.
4. App routes to correct screen.

If tap doesn't trigger: add `NotifierManager.shared.onApplicationDidBecomeActive()` in `applicationDidBecomeActive`.

Commit: `fix(ios): background notification tap routing`

### B-4: Permission flow post-sign-in

In `App()` composable:
```kotlin
LaunchedEffect(isSignedIn) {
    if (isSignedIn) {
        NotifierManager.requestPermission()
    }
}
```

Verify: fresh install → sign in → permission prompt → grant/deny respected on subsequent launches.

Commit: `fix(ios): notification permission requested post-sign-in only`

### B-5: UNNotificationServiceExtension (deferred)

Current `NotificationData` has no image URL. Skip implementation. Document as future enhancement.

---

## Mid-Phase Code Review Gate

Before D-2 archive, dispatch `feature-dev:code-reviewer` on Phase 7 diff. Reviewer checks:
- [ ] Every Scaffold in commonMain uses `contentWindowInsets = WindowInsets.safeDrawing`.
- [ ] Every TextField screen has `.imePadding()` on inner scrollable.
- [ ] `iOSApp.swift` uses `.ignoresSafeArea(.container, edges: .all)`.
- [ ] No `android.*` imports in commonMain.
- [ ] No `@StringRes` / `R.string.*` in commonMain.
- [ ] `rememberImagePickerLauncher` actuals in both androidMain and iosMain.
- [ ] Font files use underscores; `RobotoFontFamily` is `@Composable` getter.

Address all comments before archive.

---

## Section C — App Store Readiness

### C-1: Crashlytics dSYM build phase

Xcode → `iosApp` target → Build Phases → "+" → New Run Script Phase. Place after "Compile Sources". Name: "Upload dSYMs to Crashlytics". "Based on dependency analysis": unchecked.

Script:
```bash
#!/bin/bash
if [ "${CONFIGURATION}" = "Release" ]; then
  "${BUILD_DIR%Build/*}SourcePackages/checkouts/firebase-ios-sdk/Crashlytics/run"
fi
```

Input Files:
- `${DWARF_DSYM_FOLDER_PATH}/${DWARF_DSYM_FILE_NAME}/Contents/Resources/DWARF/${TARGET_NAME}`

Also commit `iosApp/scripts/upload_dsyms.sh`:
```bash
#!/bin/bash
set -euo pipefail
if [ "${CONFIGURATION}" = "Release" ]; then
  CRASHLYTICS_RUN="${BUILD_DIR%Build/*}SourcePackages/checkouts/firebase-ios-sdk/Crashlytics/run"
  "${CRASHLYTICS_RUN}" "${DWARF_DSYM_FOLDER_PATH}"
fi
```

(If using CocoaPods: replace path with `"${PODS_ROOT}/FirebaseCrashlytics/run"`.)

Commit: `feat(ios): Crashlytics dSYM build phase + upload_dsyms.sh`

### C-2: PrivacyInfo.xcprivacy

Create `iosApp/iosApp/PrivacyInfo.xcprivacy`. In Xcode: drag into Project Navigator → check "Add to targets: iosApp".

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>NSPrivacyTracking</key>
    <false/>
    <key>NSPrivacyTrackingDomains</key>
    <array/>
    <key>NSPrivacyCollectedDataTypes</key>
    <array>
        <dict>
            <key>NSPrivacyCollectedDataType</key>
            <string>NSPrivacyCollectedDataTypeEmailAddress</string>
            <key>NSPrivacyCollectedDataTypeLinked</key>
            <true/>
            <key>NSPrivacyCollectedDataTypeTracking</key>
            <false/>
            <key>NSPrivacyCollectedDataTypePurposes</key>
            <array>
                <string>NSPrivacyCollectedDataTypePurposeAppFunctionality</string>
            </array>
        </dict>
        <dict>
            <key>NSPrivacyCollectedDataType</key>
            <string>NSPrivacyCollectedDataTypeName</string>
            <key>NSPrivacyCollectedDataTypeLinked</key>
            <true/>
            <key>NSPrivacyCollectedDataTypeTracking</key>
            <false/>
            <key>NSPrivacyCollectedDataTypePurposes</key>
            <array>
                <string>NSPrivacyCollectedDataTypePurposeAppFunctionality</string>
            </array>
        </dict>
        <dict>
            <key>NSPrivacyCollectedDataType</key>
            <string>NSPrivacyCollectedDataTypePhotosorVideos</string>
            <key>NSPrivacyCollectedDataTypeLinked</key>
            <true/>
            <key>NSPrivacyCollectedDataTypeTracking</key>
            <false/>
            <key>NSPrivacyCollectedDataTypePurposes</key>
            <array>
                <string>NSPrivacyCollectedDataTypePurposeAppFunctionality</string>
            </array>
        </dict>
        <dict>
            <key>NSPrivacyCollectedDataType</key>
            <string>NSPrivacyCollectedDataTypeCrashData</string>
            <key>NSPrivacyCollectedDataTypeLinked</key>
            <false/>
            <key>NSPrivacyCollectedDataTypeTracking</key>
            <false/>
            <key>NSPrivacyCollectedDataTypePurposes</key>
            <array>
                <string>NSPrivacyCollectedDataTypePurposeAppFunctionality</string>
            </array>
        </dict>
        <dict>
            <key>NSPrivacyCollectedDataType</key>
            <string>NSPrivacyCollectedDataTypeOtherUsageData</string>
            <key>NSPrivacyCollectedDataTypeLinked</key>
            <false/>
            <key>NSPrivacyCollectedDataTypeTracking</key>
            <false/>
            <key>NSPrivacyCollectedDataTypePurposes</key>
            <array>
                <string>NSPrivacyCollectedDataTypePurposeAnalytics</string>
            </array>
        </dict>
    </array>
    <key>NSPrivacyAccessedAPITypes</key>
    <array>
        <dict>
            <key>NSPrivacyAccessedAPIType</key>
            <string>NSPrivacyAccessedAPICategoryUserDefaults</string>
            <key>NSPrivacyAccessedAPITypeReasons</key>
            <array><string>CA92.1</string></array>
        </dict>
        <dict>
            <key>NSPrivacyAccessedAPIType</key>
            <string>NSPrivacyAccessedAPICategoryFileTimestamp</string>
            <key>NSPrivacyAccessedAPITypeReasons</key>
            <array><string>C617.1</string></array>
        </dict>
        <dict>
            <key>NSPrivacyAccessedAPIType</key>
            <string>NSPrivacyAccessedAPICategoryDiskSpace</string>
            <key>NSPrivacyAccessedAPITypeReasons</key>
            <array><string>E174.1</string></array>
        </dict>
    </array>
</dict>
</plist>
```

Reason codes:
- `CA92.1` — UserDefaults for app settings (DataStore uses UserDefaults internally on iOS).
- `C617.1` — Firebase SDK accesses file timestamps for cache management.
- `E174.1` — Firebase Storage SDK queries disk space before upload.

Firebase iOS SDK 11+ ships its own `PrivacyInfo.xcprivacy`; no manual entries for Firebase internals needed in app-level manifest.

Commit: `feat(ios): PrivacyInfo.xcprivacy with API + data type declarations`

### C-3: Privacy nutrition labels (App Store Connect)

App Store Connect → App Privacy → Get Started.

| Data type | Collected | Linked | Tracking | Purpose |
|---|---|---|---|---|
| Email | Yes | Yes | No | App Functionality |
| Name | Yes | Yes | No | App Functionality |
| Photos/Videos | Yes (Storage upload) | Yes | No | App Functionality |
| Crash Data | Yes (Crashlytics) | No | No | App Functionality |
| Performance | No (not on iOS) | — | — | — |
| Analytics / Usage | Yes (FB Analytics) | No | No | Analytics |
| Device ID | Yes (FB Analytics) | No | No | Analytics |

Do NOT declare third-party advertising or tracking.

Screenshot saved label preview to `docs/superpowers/ios-screenshots/privacy-labels.png`.

Commit: `docs: App Store Connect privacy nutrition labels documented`

### C-4: Bundle version strategy

`Info.plist`:
```xml
<key>CFBundleShortVersionString</key>
<string>1.1.0</string>
<key>CFBundleVersion</key>
<string>1</string>
```

`CFBundleShortVersionString` matches `versionName` from `libs.versions.toml`. `CFBundleVersion` increments per upload (1, 2, 3...). For CI: `agvtool new-version -all $(date +%Y%m%d%H%M)`.

Commit: `chore(ios): set CFBundleVersion 1 for first TestFlight upload`

### C-5: Capabilities + entitlements

Xcode → `iosApp` → Signing & Capabilities:
- Push Notifications (APNs)
- Sign in with Apple (required when offering Google SSO)

`iosApp.entitlements`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>aps-environment</key>
    <string>production</string>
    <key>com.apple.developer.applesignin</key>
    <array><string>Default</string></array>
</dict>
</plist>
```

Use `development` for Debug; archive uses distribution profile mapping to `production` automatically when "Automatically manage signing" enabled.

Commit: `fix(ios): entitlements production APNs + apple sign-in`

### C-6: App Store Connect listing

Required for External / App Store (skip for Internal):
- App Name: Schedule
- Primary Language: Russian
- SKU: any unique string
- Description (Russian, 170+ chars)
- Keywords: расписание, бронирование, оборудование, университет
- Support URL
- Screenshots: 6.7" iPhone (1290×2796) AND 5.5" iPhone (1242×2208) — at least 3 each. Capture in Simulator with `Cmd+S`.

Commit: `docs: App Store Connect listing screenshots`

### C-7: Info.plist usage descriptions

```xml
<key>NSCameraUsageDescription</key>
<string>Используется для загрузки фотографии профиля и фото оборудования.</string>

<key>NSPhotoLibraryUsageDescription</key>
<string>Используется для выбора фотографии из библиотеки для профиля или оборудования.</string>

<key>NSUserNotificationUsageDescription</key>
<string>Уведомления о статусе бронирований и изменениях в расписании.</string>

<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>com.googleusercontent.apps.YOUR_REVERSED_CLIENT_ID</string>
        </array>
    </dict>
</array>
```

Replace `YOUR_REVERSED_CLIENT_ID` with `REVERSED_CLIENT_ID` value from `GoogleService-Info.plist`.

Commit: `fix(ios): Info.plist usage descriptions + Google URL scheme`

---

## Section D — TestFlight upload

### D-1: Final build

```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:linkReleaseFrameworkIosArm64
```

Both green; no unresolved expect/actual warnings.

### D-2: Archive in Xcode

1. Open `iosApp/iosApp.xcodeproj`.
2. Scheme: `iosApp`. Destination: `Any iOS Device (arm64)`.
3. Product → Archive (3–7 min).
4. Organizer opens automatically.

Common failures:
- Missing provisioning: Xcode → Settings → Accounts → Download Manual Profiles.
- Framework not found: verify SPM points to `composeApp/build/XCFrameworks/release/composeApp.xcframework`. Run D-1 first.
- Signing error: enable "Automatically manage signing", select team.

### D-3: Upload via Xcode Organizer

1. Select archive → Distribute App → App Store Connect → Upload.
2. Defaults: App Thinning: All compatible; Strip Swift symbols: Yes; Upload symbols: Yes.
3. Authenticate, click Upload.

CI/headless alternative:
```bash
xcrun altool \
  --upload-app \
  --type ios \
  --file "/path/to/iosApp.ipa" \
  --apiKey "${APP_STORE_CONNECT_API_KEY_ID}" \
  --apiIssuer "${APP_STORE_CONNECT_ISSUER_ID}" \
  --verbose
```

### D-4: Processing + Export Compliance

1. App Store Connect → TestFlight → iOS → build appears "Processing" (10–25 min).
2. Status changes to "Ready to Submit".
3. Click build → answer Export Compliance: "Does your app use encryption beyond OS-provided?" → No (TLS-only). Sets `ITSAppUsesNonExemptEncryption = NO`.
4. Status → "Active".

Future-proof Info.plist:
```xml
<key>ITSAppUsesNonExemptEncryption</key>
<false/>
```

Commit: `chore(ios): ITSAppUsesNonExemptEncryption false`

### D-5: Internal testers + 8-UC smoke test

1. App Store Connect → TestFlight → Internal → "+" → group "Internal".
2. Add Apple ID emails (≤100; no review).
3. Testers install via TestFlight app.

**Smoke-test checklist (physical iPhone, TestFlight build):**

- [ ] **UC-1** Email sign-in → Home.
- [ ] **UC-2** Google sign-in → Home.
- [ ] **UC-3** Apple sign-in → Home.
- [ ] **UC-4** Calendar — month + week views render correctly.
- [ ] **UC-5** Booking flow — create, comment, status changes.
- [ ] **UC-6** Push notification — curl test arrives, click routes correctly.
- [ ] **UC-7** Image upload — PHPicker sheet → select → upload → display.
- [ ] **UC-8** Russian locale — every screen Cyrillic, no clipping.

All 8 must pass before milestone declared complete.

Commit: `chore: TestFlight 1.1.0 (1) smoke-test passed`

---

## Section E — Optional follow-up (post-milestone)

### E-1: External TestFlight Beta App Review

1. App Store Connect → External Testing → group "Beta".
2. Add build, fill "What to Test".
3. Submit for Beta App Review (~24h).
4. Once approved: shareable public link (≤10k testers).

### E-2: Public App Store

Same archive. Submit for full App Store Review (1–3 days first time).

### E-3: Unlisted distribution

Apple's unlisted: full review, search-hidden, share via direct link. Available for B2B-adjacent apps.

---

## Final Phase Review

Dispatch `feature-dev:code-reviewer` on full Phase 7 diff. Focus:

**(a) Insets:** Every Scaffold uses `WindowInsets.safeDrawing`; every TextField screen has `imePadding`; iOSApp.swift uses `.container` edges.

**(b) No Android API in commonMain:** Zero `android.*` imports; zero `R.string.*`; zero `Parcelable`/`@Parcelize`.

**(c) PrivacyInfo.xcprivacy:** File present and added to target; tracking false; access reasons + data types declared.

**(d) Crashlytics build phase:** Present in `project.pbxproj`; guarded by `Release`; `upload_dsyms.sh` committed.

**(e) Image picker:** Both actuals present; iOS uses `PHPickerViewController` (not deprecated `UIImagePickerController`); `memcpy` with `@OptIn(ExperimentalForeignApi::class)`.

**(f) Fonts:** Files use underscores; `RobotoFontFamily` is `@Composable` getter; `ScheduleTheme` Typography uses Roboto.

---

## Known limitations shipping with first TestFlight

| Limitation | Impact | Resolution |
|---|---|---|
| Firebase Performance not on iOS | No iOS perf in console | By design |
| Firebase In-App Messaging not on iOS | No in-app campaigns iOS | By design |
| Nav back-stack lost on iOS process death | Cold-start to Home after OS kill | Known KMP nav-compose limit; revisit if reported |
| FCM legacy endpoint | Push may stop if Firebase disables | One-file Ktor change pre-wired (spec §4.4) |
| Push image attachments | No image preview | Not currently used |
| iPad layout | iPhone compatibility mode | By design |
| Pull-to-refresh on calendar | Manual (navigate away + back) | Future enhancement |
| Microsoft sign-in | Removed | By design |

---

## Commit sequence

| # | Commit | Section |
|---|---|---|
| 1 | `chore: add iOS simulator baseline screenshots` | A-1 |
| 2 | `fix(ios): restore safeDrawing insets` | A-2 |
| 3 | `fix(ios): bottom nav safe area` | A-3 |
| 4 | `fix(ios): imePadding on form screens` | A-4 |
| 5 | `feat(ios): bundle Roboto for Cyrillic` | A-5 |
| 6 | `feat: image picker expect/actual` | A-6 |
| 7 | `perf(ios): calendar recompositions` | A-7 (if needed) |
| 8 | `fix(ios): conditional BackHandler` | A-8 (if needed) |
| 9 | `feat: PullToRefreshBox on booking list` | A-9 |
| 10 | `fix(ios): Russian locale complete` | A-10 |
| 11 | `fix(ios): apple sign-in nonce flow` | A-11 |
| 12 | `fix(ios): google sign-in URL scheme` | A-12 |
| 13 | `fix(ios): AppDelegate APNs forwarding` | B-1 |
| 14 | `test(ios): foreground push verified` | B-2 |
| 15 | `fix(ios): background notification routing` | B-3 |
| 16 | `fix(ios): permission post-sign-in` | B-4 |
| — | **MID-PHASE CODE REVIEW** | |
| 17 | `feat(ios): Crashlytics dSYM build phase` | C-1 |
| 18 | `feat(ios): PrivacyInfo.xcprivacy` | C-2 |
| 19 | `docs: privacy nutrition labels` | C-3 |
| 20 | `chore(ios): CFBundleVersion 1` | C-4 |
| 21 | `fix(ios): entitlements production` | C-5 |
| 22 | `docs: App Store listing` | C-6 |
| 23 | `fix(ios): Info.plist descriptions + URL scheme` | C-7 |
| 24 | `chore(ios): ITSAppUsesNonExemptEncryption false` | D-4 |
| 25 | `chore: TestFlight 1.1.0 (1) smoke-test passed` | D-5 |
| — | **FINAL CODE REVIEW** | |

---

## 5-line summary — biggest blocker for first TestFlight upload

The single most common blocker is provisioning profile mismatch between the distribution certificate on the developer's Mac and the App ID registered in App Store Connect. The archive succeeds locally but upload fails with "No suitable application records were found" or similar. Resolve by: (1) confirming bundle id in `iosApp` Build Settings (`PRODUCT_BUNDLE_IDENTIFIER`) exactly matches `ru.dvfu.appliances`; (2) ensuring distribution certificate is trusted in Xcode → Settings → Accounts → Manage Certificates; (3) Product → Clean Build Folder before archive. Everything else (PrivacyInfo, dSYM, Export Compliance) is resolvable post-upload via App Store Connect without rebuild.
