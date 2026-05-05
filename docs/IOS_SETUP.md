# iOS Setup — what you need to do

The code migration created Kotlin/Native iOS targets, a static `ComposeApp` framework, all required Swift source files, `Info.plist`, entitlements, `PrivacyInfo.xcprivacy`, and a Crashlytics dSYM upload script. **The one thing it could not do is create the Xcode project itself** — that requires the Xcode GUI.

Follow this guide once. After it, every code change in `composeApp/` automatically rebuilds the iOS framework and Xcode picks it up.

---

## Prerequisites

- macOS with Xcode 16+ installed (`xcodebuild -version` should show 16.x).
- Active Apple Developer Program membership. Note your **Team ID** (10-char string from developer.apple.com → Membership).
- `kdoctor` clean (`brew install kdoctor && kdoctor`).
- Branch `feature/cmp-ios-migration` checked out. Run `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` once and confirm it succeeds (~3-5 min on first run, much faster after).

---

## Step 1 — Register the iOS app in Firebase

1. Open [Firebase Console](https://console.firebase.google.com) → existing Schedule project → **Project settings** → **Add app** → **iOS+** icon.
2. Bundle ID: `ru.dvfu.appliances` (must match — already declared in `iosApp/iosApp/Info.plist`).
3. App nickname: `Schedule iOS`.
4. App Store ID: leave empty (fill in later when the app is in App Store Connect).
5. Click **Register app**.
6. Click **Download GoogleService-Info.plist**.
7. Move the downloaded file to: `/Users/merkost/AndroidStudioProjects/Schedule/iosApp/iosApp/GoogleService-Info.plist`.
8. The file is in `.gitignore` — do not commit it.

---

## Step 2 — Generate APNs Authentication Key

1. Go to [developer.apple.com → Certificates, Identifiers & Profiles → Keys](https://developer.apple.com/account/resources/authkeys/list).
2. Click the `+` button → name it `Schedule APNs` → check **Apple Push Notifications service (APNs)** → **Continue** → **Register**.
3. **Download the `.p8` file IMMEDIATELY** (you can only download it once).
4. Note the **Key ID** (10-char string shown on the page).
5. Note your **Team ID** (10-char string under your account name in the top right).
6. Go to Firebase Console → Project settings → **Cloud Messaging** tab → scroll to **Apple app configuration** → click **Upload** under APNs Authentication Key.
7. Upload the `.p8` file. Enter Key ID and Team ID. Save.

---

## Step 3 — Register the iOS app in App Store Connect

1. Go to [App Store Connect](https://appstoreconnect.apple.com) → **Apps** → `+` → **New App** (iOS).
2. **Bundle ID**: select `ru.dvfu.appliances` (must match exactly; if it doesn't appear, register it first at [Identifiers](https://developer.apple.com/account/resources/identifiers/list) with capabilities **Push Notifications** and **Sign in with Apple**).
3. **SKU**: `schedule-ios` (any unique string).
4. **Primary language**: Russian.
5. **Bundle ID**: `ru.dvfu.appliances`.
6. **App Name**: `Schedule` (or `Расписание`).
7. Click **Create**. Note the **Apple ID** number shown.

---

## Step 4 — Create the Xcode project

The Kotlin framework is already built. Now create the Xcode shell that hosts it.

1. Open Xcode → **File → New → Project**.
2. **iOS → App** → **Next**.
3. Settings:
   - Product Name: **`iosApp`**
   - Team: select your Apple Developer Team
   - Organization Identifier: `ru.dvfu` (so the bundle ID becomes `ru.dvfu.appliances` — verify this auto-fills correctly)
   - Bundle Identifier: must end up as `ru.dvfu.appliances` (visible in Build Settings after creation if it doesn't match what Xcode auto-generates)
   - Interface: **SwiftUI**
   - Language: **Swift**
   - Storage: **None**
   - Include Tests: **unchecked**
4. Save location: `/Users/merkost/AndroidStudioProjects/Schedule/iosApp/` (same folder where `iosApp/iosApp/` already lives — you'll merge the existing files into the new project below).
5. **Uncheck** "Create Git repository on my Mac" (the parent repo already has Git).
6. Xcode creates the project. It generates `iosApp.xcodeproj/` plus its own `iosApp/iOSApp.swift` and `iosApp/ContentView.swift` files in `iosApp/iosApp/`.

### Step 4a — Replace generated Swift files with the real ones

The pre-written Swift sources are in `iosApp/iosApp/` already. Xcode just generated overwrites:

1. In Xcode Project Navigator, **delete** (Move to Trash):
   - The auto-generated `iosAppApp.swift` (or `iosApp.swift` — whatever name Xcode gave the `@main` struct file)
   - The auto-generated `ContentView.swift`
   - The auto-generated `Info.plist` (if Xcode created one)
   - The auto-generated `Assets.xcassets` (or keep it; it's just empty)

2. In Xcode Project Navigator, right-click the `iosApp` group → **Add Files to "iosApp"** → select all of these existing files:
   - `iOSApp.swift`
   - `AppDelegate.swift`
   - `ContentView.swift` (the existing one — only if you deleted the Xcode-generated one)
   - `Info.plist`
   - `iosApp.entitlements`
   - `PrivacyInfo.xcprivacy`
   - `GoogleService-Info.plist` (downloaded in Step 1)

   In the Add Files dialog: **uncheck "Copy items if needed"**, **check "Add to targets: iosApp"**.

3. Verify the files appear in Project Navigator under `iosApp/iosApp/`.

### Step 4b — Set Code Signing Entitlements

1. Click the `iosApp` project in Navigator → `iosApp` target → **Build Settings** tab → search "Code Signing Entitlements" → set value to: `iosApp/iosApp.entitlements`.
2. Build Settings → search "Info.plist File" → set to: `iosApp/Info.plist`.
3. Build Settings → search "iOS Deployment Target" → set to **14.0**.

### Step 4c — Configure Signing

1. `iosApp` target → **Signing & Capabilities** tab.
2. Check **Automatically manage signing**.
3. **Team**: select your Apple Developer Team.
4. **Bundle Identifier**: confirm `ru.dvfu.appliances`.
5. Click `+ Capability` → add **Push Notifications**.
6. Click `+ Capability` → add **Sign in with Apple**.
7. Verify the entitlements file now contains both `com.apple.developer.applesignin` and `aps-environment` keys.

### Step 4d — Set version numbers

1. Target → **General** tab → **Identity** section:
   - **Version**: `1.1.0`
   - **Build**: `1`

---

## Step 5 — Add the ComposeApp framework

The Kotlin framework lives at `composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework`. Wire it into Xcode:

1. Build it once from terminal: `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`
2. In Xcode, target → **General** tab → **Frameworks, Libraries, and Embedded Content** → click `+` → **Add Other... → Add Files**.
3. Navigate to `composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework`. Select it.
4. **Embed**: set to **Do Not Embed** (the framework is static).

### Step 5a — Set Framework Search Paths

Build Settings → search "Framework Search Paths" → for **Debug | Any iOS Simulator SDK**:

```
$(SRCROOT)/../composeApp/build/bin/iosSimulatorArm64/debugFramework
```

For **Debug | Any iOS SDK** (physical device):

```
$(SRCROOT)/../composeApp/build/bin/iosArm64/debugFramework
```

For **Release | Any iOS SDK**:

```
$(SRCROOT)/../composeApp/build/bin/iosArm64/releaseFramework
```

### Step 5b — Auto-rebuild framework via Run Script

Make Xcode invoke Gradle on each build so the framework is always current.

1. Target → **Build Phases** tab → click `+` → **New Run Script Phase**.
2. **Name** the phase: "Build Kotlin Framework".
3. **Drag the new phase to the top** (above "Compile Sources").
4. **Shell**: `/bin/zsh`.
5. **Script**:

```zsh
cd "${SRCROOT}/.."
if [[ "$PLATFORM_NAME" == *simulator* ]]; then
    if [[ "$CONFIGURATION" == "Debug" ]]; then
        ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
    else
        ./gradlew :composeApp:linkReleaseFrameworkIosSimulatorArm64
    fi
else
    if [[ "$CONFIGURATION" == "Debug" ]]; then
        ./gradlew :composeApp:linkDebugFrameworkIosArm64
    else
        ./gradlew :composeApp:linkReleaseFrameworkIosArm64
    fi
fi
```

6. **Uncheck "Based on dependency analysis"** (always run).

### Step 5c — Add Crashlytics dSYM upload script

1. Target → Build Phases → `+` → **New Run Script Phase**.
2. Name: "Upload dSYMs to Crashlytics".
3. Place this phase **after** "Compile Sources" and "Link Binary".
4. Shell: `/bin/zsh`.
5. Script: paste the contents of `iosApp/scripts/upload_dsyms.sh` (it's already in the repo):

```zsh
#!/bin/zsh
if [ "${CONFIGURATION}" = "Release" ]; then
  "${BUILD_DIR%Build/*}SourcePackages/checkouts/firebase-ios-sdk/Crashlytics/run"
fi
```

6. **Input Files**:
   - `${DWARF_DSYM_FOLDER_PATH}/${DWARF_DSYM_FILE_NAME}/Contents/Resources/DWARF/${TARGET_NAME}`

---

## Step 6 — Add Swift Package Manager dependencies

Xcode → **File → Add Package Dependencies**.

### Firebase iOS SDK

- URL: `https://github.com/firebase/firebase-ios-sdk`
- Dependency Rule: **Up to Next Major Version** from `11.0.0`.
- Click **Add Package**.
- Select these products to add to **target `iosApp`**:
  - `FirebaseAuth`
  - `FirebaseFirestore`
  - `FirebaseDatabase`
  - `FirebaseStorage`
  - `FirebaseAnalytics`
  - `FirebaseCrashlytics`
  - `FirebaseRemoteConfig`
  - `FirebaseMessaging`
- **Do NOT** select `FirebasePerformance` and `FirebaseInAppMessagingSwift` (intentionally out of scope on iOS — see spec §3.4).

### GoogleSignIn (only if you want Google Sign-In on iOS)

- URL: `https://github.com/google/GoogleSignIn-iOS`
- Version: from `7.0.0`.
- Add product `GoogleSignIn` to target `iosApp`.

---

## Step 7 — First build & run

1. In Xcode toolbar: select scheme **`iosApp`**, destination **iPhone 16 Simulator** (iOS 18).
2. **Product → Build** (`⌘B`).
3. First build runs Gradle from the script — takes 3-5 min. Subsequent builds are seconds.
4. **Product → Run** (`⌘R`). Simulator launches; sign-in screen renders.

### If the build fails

Most common errors and fixes:

- **`No such module 'ComposeApp'`** — Framework Search Paths weren't set correctly (Step 5a) or framework wasn't linked (Step 5).
- **`Provisioning profile doesn't include Sign In with Apple capability`** — Step 4c — re-add the capability and confirm Team is selected.
- **`Reason: image not found` at runtime** — framework is static but Xcode tried to embed it. Set **Embed: Do Not Embed** in Step 5.
- **Gradle error in the build script** — run `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` from terminal first to surface the underlying error.

---

## Step 8 — Verify on a physical device

Plug in an iPhone (iOS 14+). Trust the developer profile via **Settings → General → VPN & Device Management → Apple Development**.

In Xcode, change destination to your device, **⌘R**.

Verify on device:

- Email sign-in → home screen appears.
- Google sign-in (if SPM dependency added) → Google sheet appears.
- Apple sign-in (Sign in with Apple capability) → Face ID prompt → home.
- Calendar renders, week view scrolls.
- Booking create flow.
- Image upload (PHPicker — wired via `composeApp/src/iosMain/kotlin/.../platform/ImagePicker.ios.kt`).
- Russian locale: Simulator → Settings → General → Language & Region → Russian. All UI in Cyrillic.
- Push notification: send from Firebase Console → Cloud Messaging → test push to the FCM token from `fcmToken` field in your Firestore user document.

---

## Step 9 — Archive and upload to TestFlight

When the device build runs cleanly:

1. Xcode toolbar → destination **Any iOS Device (arm64)**.
2. **Product → Archive**. Takes 5-10 min.
3. Xcode Organizer opens. Select the archive.
4. **Distribute App → App Store Connect → Upload**.
5. Keep all defaults (App Thinning: All compatible; Strip Swift symbols: Yes; Upload symbols: Yes).
6. Authenticate with Apple ID → **Upload**.
7. ~10-25 min later, App Store Connect → **TestFlight** → iOS — your build appears as **Processing**, then **Ready to Submit** or **Active**.
8. If asked about **Export Compliance**: select **No** (TLS-only encryption is exempt; Firebase uses TLS 1.2+).
9. **Internal Testing** → `+` → create group `Internal` → add yourself by Apple ID email. Install via TestFlight app on device.

---

## Useful commands

```bash
# Rebuild the iOS framework manually
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Inspect the generated ObjC header (to verify exported names match what Swift expects)
cat composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework/Headers/ComposeApp.h | grep -i "MainViewController\|GoogleSignIn\|AppleSignIn"

# Build for iPhone Simulator from CLI
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=latest' \
  -configuration Debug \
  CODE_SIGN_IDENTITY="" \
  CODE_SIGNING_REQUIRED=NO \
  build
```

---

## Known limitations on this first iOS build

- **Firebase Performance** and **Firebase In-App Messaging** are NOT on iOS by design (spec §9 out of scope).
- **`compose-calendar`** library is `java.time`-bound — calendar files run only on Android until that library is replaced or forked. The iOS framework will compile but calendar UI won't render natively. (Phase 6+ work.)
- **Apple Sign-In** code path requires a physical device (Simulator doesn't support real Face ID handshake).
- **FCM legacy HTTP send** endpoint is on a Google sunset timer (deprecated June 2024). Push delivery may stop without warning. Migrating to FCM HTTP v1 is documented as follow-up in spec §4.4.
- **Navigation back-stack persistence** on iOS process death is less battle-tested than Android (spec §7 Risk 6).

---

## Glossary

- **Branch**: `feature/cmp-ios-migration` on https://github.com/Merkost/Schedule.
- **Spec**: `docs/superpowers/specs/2026-05-05-compose-multiplatform-ios-migration-design.md`.
- **Plans**: `docs/superpowers/plans/2026-05-05-phase-{1..7}-*.md`.
