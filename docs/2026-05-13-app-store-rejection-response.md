# App Store rejection response — 1.1.4

Submission `58a13140-3260-4f18-9951-35638c853888` (version 1.1.3 build 1) was
rejected on 2026-05-13 against three guidelines. This doc is the complete
record: what was rejected, what we considered, what we chose, what changed,
how to verify, and the reply text to paste into App Store Connect.

---

## 1. The rejection

Review device: iPad Air 11-inch (M3). Review date: 2026-05-13.

| Guideline | Apple's complaint                                                              |
| --------- | ------------------------------------------------------------------------------ |
| 4.8       | App uses Google sign-in but offers no equivalent login that satisfies the privacy criteria (limit to name/email, allow email private, no ad tracking). |
| 2.1(a)    | No way for reviewers to access all features — needs a demo account or demo mode. |
| 2.3.8     | App Store name is "Приборы ДВФУ", device name is "Schedule" — too dissimilar. |

---

## 2. Options considered

### For 4.8

| Option                                           | Picked? | Why / why not                                                                                                  |
| ------------------------------------------------ | ------- | -------------------------------------------------------------------------------------------------------------- |
| Keep Google only                                 | ✗       | Google fails the "email private" criterion. Auto-rejected.                                                     |
| Argue equivalence in reply                       | ✗       | No existing provider qualifies.                                                                                |
| **Sign in with Apple on iOS**                    | ✓       | Apple explicitly says this qualifies. Smallest code change. Native UX. Doubles as a reviewer-friendly login.   |
| Email + password (as 4.8 fix)                    | ✗ as sole fix | Would satisfy 4.8 but means building signup, password reset, email verification flows.                  |
| Email link / magic link                          | ✗       | Same reason as above. Heavier than SiwA for this use case.                                                     |

### For 2.1(a)

| Option                                           | Picked? | Why / why not                                                                                                  |
| ------------------------------------------------ | ------- | -------------------------------------------------------------------------------------------------------------- |
| Tell reviewer to use own Apple ID                | ✗       | Doesn't show admin features — admin role is per Firestore doc and reviewer's Apple ID has none.                 |
| Dummy Google account                             | ✗       | Apple reviewers sign in from Cupertino; Google's anomaly detection sends a verification code to *us*, not them. Common failure mode for resubmissions. |
| **Email/password + hidden 5-tap unlock**         | ✓       | Reliable (no 2FA / no IP heuristics), invisible to normal users, single account we can elevate via Firestore.   |
| "Promote on first sign-in" Cloud Function        | ✗       | Permanent backdoor — auditors don't like it.                                                                   |

### For 2.3.8

| Option                                           | Picked? | Why / why not                                                                                                  |
| ------------------------------------------------ | ------- | -------------------------------------------------------------------------------------------------------------- |
| Rename App Store listing to match "Schedule"     | ✗       | Russian name is the canonical marketing name.                                                                  |
| **Change `CFBundleDisplayName` → "Приборы ДВФУ"** | ✓       | Matches the listing. Bundle identifier unchanged.                                                              |
| Localized display name per locale                | ✗       | More work; rejection only complains about mismatch, not localization.                                          |

---

## 3. Code changes (1.1.4 build 4)

### Version bump

- `iosApp/iosApp/Info.plist` → `CFBundleShortVersionString = 1.1.4`,
  `CFBundleVersion = 4`.

### Display name (2.3.8)

- `iosApp/iosApp/Info.plist` → `CFBundleDisplayName = Приборы ДВФУ`.

### Sign in with Apple (4.8) — iOS only

**Swift:**

- `iosApp/iosApp/AppleSignInBridge.swift` (new) — `ASAuthorizationController`
  flow, generates a random nonce, SHA-256 hashes it for the Apple request,
  then calls `Auth.auth().signIn(with: OAuthProvider.appleCredential(...))`
  directly with the identity token + raw nonce + full name. Result is
  reported back to Kotlin as `error: String?` (null = success).
- `iosApp/iosApp/AppDelegate.swift` → `AppleSignInBridge.install()` after
  `GoogleSignInBridge.install()`.

**Why do Firebase sign-in in Swift, not Kotlin?**

The pattern mirrors `GoogleSignInBridge`, but for Apple we sign in to
Firebase from Swift directly instead of passing the token back to Kotlin.
Two reasons: (1) avoids any uncertainty about gitlive's `OAuthProvider`
API in version 2.4.0; (2) `UsersRepository.currentUser` is already a
flow that emits on Firebase auth state change, so the shared code picks
up the sign-in automatically — no extra plumbing needed.

**Xcode capability (project.yml-managed, not hand-edited):**

- `iosApp/project.yml` → under `targets.Schedule.entitlements.properties`,
  added `com.apple.developer.applesignin: [Default]`. xcodegen regenerates
  `iosApp/iosApp/iosApp.entitlements` from this on every run, so editing
  the entitlements file directly was reverted on the first `xcodegen
  generate` — putting it in project.yml is the correct fix.

**KMP plumbing:**

- `composeApp/src/commonMain/.../platform/AppleAuthLauncher.kt` (new)
  — `expect class AppleAuthLauncher { val isAvailable; suspend fun signIn() }`
- `composeApp/src/iosMain/.../platform/IosAppleAuthBridge.kt` (new) —
  global hook installed by Swift, mirrors `IosGoogleAuthBridge`.
- `composeApp/src/iosMain/.../platform/AppleAuthLauncher.ios.kt` (new) —
  `isAvailable = true`, wraps the bridge callback in
  `suspendCancellableCoroutine`. Treats `"canceled"` as a
  `CancellationException`.
- `composeApp/src/androidMain/.../platform/AppleAuthLauncher.android.kt`
  (new) — `isAvailable = false`. Android UI never renders the button.
- `composeApp/src/iosMain/.../di/PlatformModule.kt` and `androidMain`
  counterpart — register `AppleAuthLauncher` as a Koin `single`.

**Login UI:**

- `composeApp/src/commonMain/.../ui/LoginScreen.kt` — added a black
  "Continue with Apple" button under the Google one, shown only when
  `appleLauncher.isAvailable`. Tapping it calls
  `appleLauncher.signIn()`; success path relies on the `currentUser` flow.
- `composeApp/src/commonMain/.../viewmodels/LoginViewModel.kt` — added
  `beginExternalSignIn()` to flip to loading state.

### Hidden QA email/password unlock (2.1(a))

- `composeApp/src/commonMain/.../viewmodels/LoginViewModel.kt` →
  `signInWithEmailPassword(email, password)` using
  `Firebase.auth.signInWithEmailAndPassword`.
- `composeApp/src/commonMain/.../ui/LoginScreen.kt`:
  - Logo `Image` is now `clickable` with `indication = null` (no ripple).
  - 5 taps within 2 seconds opens `QaSignInDialog` (Material3 `AlertDialog`
    with two `OutlinedTextField`s — email + password, password masked).
  - Reset rule: if the gap between taps is > 2 s, the counter restarts at 1.

### String resources

- `composeApp/src/commonMain/composeResources/values/strings.xml` and
  `values-ru/strings.xml`:
  - `sign_in_with_apple` — "Continue with Apple" / "Продолжить с Apple"
  - `qa_sign_in_title`, `qa_email_label`, `qa_password_label`,
    `qa_sign_in_action`, `qa_cancel` — for the hidden dialog.

---

## 4. External setup required (no code, just clicks)

Do these **before** uploading or the reply will fail review.

### 4.1 Apple Developer Portal

1. https://developer.apple.com → Certificates, Identifiers & Profiles →
   Identifiers → `ru.dvfu.appliances`.
2. Capabilities → check **Sign In with Apple** → Save.
3. Provisioning profiles regenerate automatically. Xcode will fetch on
   next archive.

### 4.2 Firebase Console

Project: same one referenced by `iosApp/iosApp/GoogleService-Info.plist`.

1. Authentication → Sign-in method → **Apple** → Enable.
   - Leave Services ID / Apple Team ID / Key ID / Private Key blank.
     Those are for *web* Apple sign-in. Native iOS only needs the toggle.
2. Authentication → Sign-in method → **Email/Password** → Enable.
3. Authentication → Users → Add user:
   - Email: `appstore.review@dvfu.ru`
   - Password: `<retrieve from secure local credentials>`
   - Save. Copy the resulting **User UID**.

### 4.3 Firestore admin elevation

The QA account needs `role = 2` (Roles.ADMIN.ordinal) in its `users` doc.

The user document is auto-created on first sign-in with `role = 0` (guest),
so:

1. On a local debug build, tap the logo 5 times on the login screen.
2. Sign in as `appstore.review@dvfu.ru` / `<retrieve from secure local credentials>`.
3. Firestore Console → `users` collection → find the doc with id =
   the UID from step 4.2 → edit field `role` (Int) → set to `2` → Save.
4. Sign out and back in — admin screens should now be visible.

### 4.4 Xcode project regeneration

Already done in this session, but if you pull these changes fresh:

```
cd iosApp && xcodegen generate
```

---

## 5. Reviewer credentials

| Field    | Value                       |
| -------- | --------------------------- |
| Email    | `appstore.review@dvfu.ru`   |
| Password | `<retrieve from secure local credentials>` |

This is a dedicated reviewer-only account. Rotate the password after the
app is approved; the account itself can stay so future submissions don't
have to re-bootstrap admin state.

---

## 6. Gotchas worth knowing

- **Apple sends name/email only on first sign-in.** Subsequent sign-ins
  return just the user identifier. The Swift bridge passes
  `credential.fullName` into `OAuthProvider.appleCredential(...)` so
  Firebase sets `displayName` once on first sign-in.
- **iOS Simulator quirks.** Sign in with Apple sometimes fails on the
  simulator with cryptic errors. If a smoke test fails there, try a real
  device before debugging the code.
- **Bundle ID consistency.** Apple Developer portal, Firebase iOS app
  config (`GoogleService-Info.plist → BUNDLE_ID`), and Xcode
  (`PRODUCT_BUNDLE_IDENTIFIER`) must all be `ru.dvfu.appliances`.
- **xcodegen overwrites `iosApp.entitlements`.** Don't hand-edit it; put
  capabilities under `targets.Schedule.entitlements.properties` in
  `project.yml`.
- **The Apple-logo glyph in the button** uses U+F8FF (Apple's private-use
  glyph; renders only on Apple platforms). Fine here because the button is
  hidden on Android.
- **The QA dialog is invisible to normal users.** Discoverability requires
  the 5-tap gesture or knowing the gesture from this doc / the review
  notes. Don't document it in user-facing help.

---

## 7. App Review reply

Paste into App Store Connect → App Review → Reply.

```
Hello App Review team,

Thank you for the detailed feedback on submission
58a13140-3260-4f18-9951-35638c853888. We have addressed all three issues
in version 1.1.4 (build 4).

---

Guideline 4.8 — Login Services

We added Sign in with Apple as an equivalent login option alongside
Sign in with Google on iOS. It meets all three privacy requirements:

- Collects only the user's name and email.
- Allows the user to keep their email private via Apple's relay service.
- Does not collect interactions with the app for advertising purposes.

The new "Continue with Apple" button is shown on the login screen on iOS.

---

Guideline 2.1(a) — Information Needed

A demo account with full administrator functionality is available via a
QA sign-in built into the app:

1. On the login screen, tap the app logo 5 times within 2 seconds.
2. A "QA sign-in" dialog will appear.
3. Enter the following credentials:
     Email:    appstore.review@dvfu.ru
      password: <retrieve from secure local credentials>

This account has the administrator role configured in our backend, so all
admin-only screens, actions, and management features are accessible.

You may also test the standard end-user flow using "Continue with Apple"
with your own Apple ID.

---

Guideline 2.3.8 — Accurate Metadata

The on-device display name has been updated from "Schedule" to
"Приборы ДВФУ", matching the App Store listing. The bundle identifier
(ru.dvfu.appliances) is unchanged.

---

Please let us know if you need any additional information.

Best regards,
DVFU Appliances team
```

---

## 8. Pre-submission checklist

External setup:

- [ ] Apple Dev portal — Sign in with Apple capability enabled on App ID
- [ ] Firebase — Apple provider enabled (Authentication → Sign-in method)
- [ ] Firebase — Email/Password provider enabled
- [ ] Firebase — `appstore.review@dvfu.ru` user created
- [ ] Firestore — `users/<uid>.role = 2` for the QA account
- [ ] `xcodegen generate` re-run in `iosApp/` (already done in-session)

Smoke tests on a debug build:

- [ ] Login screen shows the black "Continue with Apple" button on iOS
- [ ] Tap → Apple sheet appears → sign in works → app lands signed in
- [ ] Firebase Console shows a new user with provider "Apple"
- [ ] Tap logo 5× → QA dialog appears
- [ ] Sign in with `appstore.review@dvfu.ru` works → admin screens visible
- [ ] On-device home screen icon label reads "Приборы ДВФУ"
- [ ] On Android, the Apple button is NOT visible (no regression)

Upload:

- [ ] Build 1.1.4 (build 4) archived in Xcode and uploaded
- [ ] Build selected for the resubmission in App Store Connect
- [ ] Reply pasted into App Review thread

Post-approval:

- [ ] Rotate the QA account password
- [ ] Keep the replacement password in the secure credential store only
