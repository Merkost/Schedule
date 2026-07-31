# Release 1.2.0

Status: prepared on `dev`; not tagged, uploaded, submitted, or published.

## Version matrix

| Platform | Marketing version | Build |
| -------- | ----------------- | ----- |
| Android  | 1.2.0             | 12    |
| iOS      | 1.2.0             | 5     |

App Store ID: `6767415496`.

This release supersedes the prepared 1.1.4 release.

## Highlights

- Redesigned Profile and Settings with clearer account state and notification controls.
- Added Sign in with Apple on iOS.
- Added App Store opening and sharing actions.
- Improved push notification delivery, tap routing, and token registration on Android and iOS.
- Updated the Android build stack and application dependencies for current platform compatibility.
- Improved account deletion, localization, safe-area handling, stability, and security.

## Release copy

### Russian

```
• Обновлены экраны профиля и настроек
• Добавлен вход через Apple на iOS
• Улучшены доставка и обработка push-уведомлений
• Обновлены системные компоненты для повышения стабильности и безопасности
```

### English

```
• Redesigned Profile and Settings
• Added Sign in with Apple on iOS
• Improved push notification delivery and handling
• Updated platform components for better stability and security
```

## Technical changes

- Migrated push notifications to the focused KMPNotifier 2.0 Firebase module.
- Updated Android cold-start and resumed-intent notification handling.
- Split notification-click and push-token listeners according to the KMPNotifier 2.0 API.
- Updated Firebase iOS SDK to the KMPNotifier-required version 12.14.0.
- Updated Gradle, Android Gradle Plugin, Firebase Android BoM, and supporting libraries.
- Kept Android compile and target API at 37.

## Preparation checks

- [x] Android version is 1.2.0 (`versionCode` 12).
- [x] iOS version is 1.2.0 (`CFBundleVersion` 5).
- [x] Support-page version is aligned.
- [ ] Android host tests pass.
- [ ] Android debug app assembles.
- [ ] Android release App Bundle builds and is signed with the configured release key.
- [ ] iOS device and simulator Kotlin targets compile.
- [ ] Xcode Release simulator build succeeds for arm64 without code signing.
- [ ] Xcode Release archive succeeds with the production team.
- [ ] Push receipt, tap routing, and token refresh pass on physical Android and iOS devices.
- [ ] App Store Connect build 1.2.0 (5) is uploaded and selected.
- [ ] Google Play build 1.2.0 (12) is uploaded.

## Distribution

1. Build and inspect the signed Android App Bundle.
2. Archive the iOS app in Xcode and validate it in Organizer.
3. Run physical-device sign-in, notification, account-deletion, Profile, and Settings smoke tests.
4. Upload both artifacts and paste the localized release copy.
5. Release manually after store approval.

Do not create the release tag until both store artifacts are accepted.
