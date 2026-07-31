# Release 1.1.4

Status: prepared on `dev`; not tagged, uploaded, submitted, or published.

## Version matrix

| Platform | Marketing version | Build |
| -------- | ----------------- | ----- |
| Android  | 1.1.4             | 11    |
| iOS      | 1.1.4             | 4     |

The public App Store version before this release is 1.1.3. App Store ID:
`6767415496`.

## Highlights

- Redesigned Profile and Settings with clearer account state and notification controls.
- Added Sign in with Apple on iOS.
- Added App Store opening and sharing actions.
- Improved account deletion, localization, safe-area handling, and push diagnostics.
- Updated Kotlin, Firebase Android BoM, and Android API 37 compatibility.
- Removed reviewer credentials from the tracked source tree.

## Release copy

### Russian

```
• Обновлены экраны профиля и настроек
• Добавлен вход через Apple на iOS
• Улучшены управление уведомлениями и диагностика push-уведомлений
• Улучшены удаление аккаунта, стабильность и безопасность
```

### English

```
• Redesigned Profile and Settings
• Added Sign in with Apple on iOS
• Improved notification controls and push diagnostics
• Improved account deletion, stability, and security
```

## Preparation checks

- [x] Android version is 1.1.4 (`versionCode` 11).
- [x] iOS version is 1.1.4 (`CFBundleVersion` 4).
- [x] App Store metadata and support-page version are aligned.
- [x] Reviewer credentials are referenced through secure storage only.
- [x] Android host tests pass.
- [x] Android release bundle builds and is signed with the configured release key.
- [x] Android Crashlytics mapping is uploaded by the release build.
- [x] iOS device Kotlin target compiles.
- [x] Xcode Release simulator build succeeds for arm64 without code signing.
- [ ] Xcode Release archive succeeds with the production team.
- [ ] Sign in with Apple and the reviewer account pass on a physical iPhone.
- [ ] Reviewer credential is rotated before submission.
- [ ] App Store Connect build 1.1.4 (4) is uploaded and selected.
- [ ] Google Play build 1.1.4 (11) is uploaded.

## Verified artifacts

### Android

- App Bundle: `androidApp/build/outputs/bundle/release/androidApp-release.aab`
- SHA-256: `600d2bd7c9c88ab9075e403db65a4dbdd6b8b4e202d092aebefba6bd0aa22408`
- Signature: verified with `jarsigner`.
- Manifest: verified with Bundletool 1.18.3 as package
  `ru.dvfu.appliances`, version 1.1.4 (11), target API 37.

### iOS

- The generated arm64 Release simulator app reports bundle
  `ru.dvfu.appliances`, version 1.1.4 (4).
- A generic dual-architecture simulator build is unsupported because the KMP
  module declares `iosSimulatorArm64()` but not `iosX64()`. Constraining the
  same build to the configured arm64 simulator target succeeds.
- The App Store device build remains subject to a signed Xcode archive and
  physical-device verification.

## Known build warnings

- Coil 3.4.0 resolves a Skiko version that differs from Compose's resolved
  Skiko version on iOS.
- The simulator linker reports an ICU object built for iOS Simulator 18.5
  while the app deployment target is iOS 16.0.
- Two Android dependency native libraries are packaged without stripping.

## Distribution

1. Rotate the dedicated reviewer credential and keep it outside this repository.
2. Verify the reviewer account still has the required Firestore role.
3. Build and inspect the signed Android App Bundle.
4. Archive the iOS app in Xcode and validate it in Organizer.
5. Run physical-device sign-in, notification, account-deletion, Profile, and Settings smoke tests.
6. Upload both artifacts and paste the localized release copy.
7. Release manually after store approval.

Do not create the release tag until both store artifacts are accepted.
