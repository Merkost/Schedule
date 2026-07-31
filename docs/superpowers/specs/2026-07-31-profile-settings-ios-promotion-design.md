# Profile, Settings, and iOS Promotion Design

**Status:** Approved for implementation by the user on 2026-07-31.

## Goal

Improve the Profile and Settings experience, promote the Schedule iOS app from Settings with a clickable and shareable App Store link, update the stable dependency baseline, and deliver every remaining change through PR #14 targeting `dev`. PR #12 was merged while implementation was in progress, so the completed follow-up is necessarily carried by PR #14.

## Current context

- `Profile.kt` uses full-width outlined buttons, contains hardcoded Russian labels, and keeps account actions visually flat.
- `Settings.kt` combines notification permission, notification diagnostics, and appearance controls in one long surface. Its booking-update and reminder switches are local UI state and do not persist or affect notification delivery.
- The shared platform action seam only opens Android notification settings and has no external URL or share operation.
- The branch already contains the broad migration upgrade to API 37, Kotlin 2.4.0, Compose Multiplatform 1.11.1, Ktor 3.5.0, and Firebase BoM 34.14.1.
- GitGuardian identified a hardcoded credential in the PR history. The credential must be removed from the branch history without printing it or committing a replacement secret.

## Design

### Profile

Keep the existing identity card and logout action, but replace the flat outlined-button stack with grouped Material 3 action cards. Registered users see Account details, Edit profile, Linked accounts, and Settings as semantic rows with icons, supporting labels, and trailing navigation affordances. Admin-only Users remains in a separate management group. Guest users see Save account as the primary action and Delete account in a separated error-colored danger group.

All visible labels, including the existing Russian-only labels and logout dialog copy, move to Compose Resources in both English and Russian. The screen consumes its `modifier` and uses safe-drawing insets through `Scaffold`.

### Settings

Place a dismissible iOS promotion card at the top of the scroll content, before permission and notification sections. The card contains:

- title: “Schedule is now on iOS”;
- supporting copy explaining that the iPhone app can be opened or shared;
- Open App Store action;
- Share action that sends the App Store URL through the platform share sheet;
- close action persisted in the existing preferences DataStore.

The canonical URL is `https://apps.apple.com/app/id6767415496`.

Remove the non-persistent booking-update and reminder switches. Keep the system notification permission state truthful by deriving the master notification switch from the platform permission result; enabling requests permission and disabling opens system settings. Keep FCM diagnostics available in a separate Diagnostics section so troubleshooting controls do not compete with normal notification settings.

### Platform actions

Extend the existing `PlatformActions` expect/actual seam with:

```kotlin
expect fun openExternalUrl(url: String)
expect fun shareText(text: String)
```

Android uses `ACTION_VIEW` and an `ACTION_SEND` chooser. iOS uses `UIApplication` for URL opening and presents `UIActivityViewController` from the active key window root controller. The common UI owns the URL and share message; platform code owns only native presentation.

### Preferences

Add a boolean `IOS_APP_PROMOTION_DISMISSED` preference to `UserDatastore`. Its default is `false`, and dismissing the card writes `true`. No network or account data is introduced.

### Dependencies and Android SDK

Update the stable patch-level dependencies that are behind current official releases:

- Kotlin `2.4.0` to `2.4.10`;
- Firebase Android BoM `34.14.1` to `34.16.0`.

Keep AGP `9.2.1`, Gradle `9.4.1`, Compose Multiplatform `1.11.1`, and API 37. The branch already declares `compileSdk = 37` and `targetSdk = 37`; implementation will verify those values and the installed SDK instead of moving to an unreleased API.

### Security remediation

Sanitize the credential-bearing App Store review document in the follow-up diff. The document contains instructions to obtain credentials from a secure local source, never a password. GitGuardian must pass on PR #14. Because PR #12 merged the earlier history into `dev`, credential rotation remains an external operational action and rewriting shared `dev` history is outside this change.

## Approved follow-up polish

The user approved a second focused UI/UX pass on 2026-07-31.

### Promotion card

- Avoid a dismissed-banner flash by treating the first DataStore value as unknown and rendering only after a real `false` value arrives.
- Keep the first appearance static, then use a restrained fade-and-shrink exit after dismissal.
- Give the close control a minimum 48 dp hit target.
- Give Open App Store and Share equal responsive width, at least 48 dp height, and enough spacing for English and Russian labels.
- Preserve Open App Store as the visually primary action and Share as the secondary action.

### Notification state

- Keep the permission switch disabled until the platform permission state has loaded.
- Continue deriving the checked state exclusively from the platform permission result.
- Preserve the existing request/open-settings behavior and separate Diagnostics section.

### Profile hierarchy

- Add a compact account-status chip for Guest, Admin, or User beneath the identity details.
- Add a subtle semantic outline to the profile image.
- Place action icons in 40 dp tonal containers for stronger scanning and optical balance.
- Use an error-container surface for the guest danger group while preserving the existing deletion confirmation flow.
- Keep row touch targets at least 48 dp and retain all existing navigation destinations.

## Verification

- Common promotion helper tests cover the canonical URL and share-message construction.
- Gradle compilation covers Android and iOS simulator Kotlin targets.
- Existing common and Android host tests are rerun.
- Source scans confirm no password-like literal remains in tracked files or the PR diff.
- GitHub PR #14 is checked for the new head, `dev` base, mergeability, and security-check result.
- Worktree metadata and local branches are cleaned only after clean-status and reachability checks.
