# Profile, Settings, and iOS Promotion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a secure dependency and Android SDK verification update, Profile/Settings UX refresh, clickable/shareable iOS App Store promotion, and approved follow-up polish through PR #14 targeting `dev`.

**Architecture:** Keep the promotion UI in commonMain, keep URL opening and share-sheet presentation behind the existing `PlatformActions` expect/actual boundary, and store banner dismissal in the existing preferences-backed `UserDatastore`. The follow-up branch is based on the merged `dev`; security remediation is a source-level redaction in PR #14 and does not rewrite shared branch history.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material 3, Compose Resources, Android intents, iOS UIKit interop, Preferences DataStore, Gradle 9.4.1, AGP 9.2.1.

## Global Constraints

- [ ] Append all remaining implementation commits to PR #14; PR #12 is already merged.
- [ ] Preserve current feature work and local untracked QA/config; never stage them unless task requires.
- [ ] Keep dependency versions and Android SDK values in `gradle/libs.versions.toml`.
- [ ] Keep compileSdk 37, targetSdk 37, minSdk 26, AGP 9.2.1, Gradle 9.4.1, and Compose 1.11.1.
- [ ] Update Kotlin to 2.4.10 and Firebase BoM to 34.16.0.
- [ ] Use Material 3, safe-drawing Scaffold insets, semantic tokens, and no new code comments.
- [ ] Add user-facing strings in English and Russian Compose Resources.
- [ ] Never print, copy, or commit the leaked credential.
- [ ] Delete only proven stale clean worktrees and branches.

## Task 1: Sanitize history and preserve existing local build configuration

**Files:** `docs/2026-05-13-app-store-rejection-response.md`, `gradle.properties`, approved design and plan docs.

- [ ] Confirm the feature branch, clean tracked state, and the exact stale worktree metadata before rewriting.
- [ ] Create a local backup branch at the current feature head.
- [ ] Interactive-rebase only the feature commits after `origin/dev`, stopping at the GitGuardian-flagged commit `35374e6`.
- [ ] Replace only the credential-bearing line with a secure-source instruction without displaying the original value; amend and continue the rebase.
- [ ] Cherry-pick local commit `113c9ea` so the requested parallel Gradle sync setting is included in the feature branch.
- [ ] Scan tracked history and the current tree for password-like literals without printing sensitive values.
- [ ] Commit the approved design and implementation plan.

## Task 2: Update stable dependencies and verify Android SDK baseline

**Files:** `gradle/libs.versions.toml` and any directly required Gradle configuration discovered from live source.

- [ ] Update `kotlin = "2.4.10"` and `firebaseBom = "34.16.0"`.
- [ ] Verify `compileSdk = "37"`, `targetSdk = "37"`, `minSdk = "26"`, AGP `9.2.1`, Compose `1.11.1`, and wrapper Gradle `9.4.1` remain consistent.
- [ ] Verify the local Android API 37 platform/build tools are installed.
- [ ] Resolve the Android runtime dependency graph with `:androidApp:dependencies --configuration debugRuntimeClasspath`.
- [ ] Commit the dependency refresh.

## Task 3: Add tested common promotion and native platform actions

**Files:** `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/platform/PlatformActions.kt`, Android/iOS actuals, new `IosAppPromotion.kt`, and its common test.

- [ ] Add a failing common test first for the canonical App Store URL in the share message.
- [ ] Add the common URL constant and share-message builder.
- [ ] Add `openExternalUrl` and `shareText` to the common expect/actual seam.
- [ ] Implement Android URL opening with `ACTION_VIEW` and text sharing with an `ACTION_SEND` chooser.
- [ ] Implement iOS URL opening with `UIApplication` and sharing with `UIActivityViewController` from the active root view controller.
- [ ] Run the focused test and then the common test suite.
- [ ] Commit the cross-platform promotion actions.

## Task 4: Persist dismissal and refresh Profile/Settings UX

**Files:** `UserDatastore.kt`, `UserDatastoreImpl.kt`, new `IosAppPromotionBanner.kt`, `Settings.kt`, `Profile.kt`, `EditProfile.kt`, and English/Russian strings.

- [ ] Add a default-false persisted iOS-promotion dismissal preference.
- [ ] Build a Material 3 primary-container banner with open, share, dismiss actions, and a preview.
- [ ] Show the banner before notification permission content and persist dismissal.
- [ ] Replace fake booking/reminder switches with a truthful master permission switch; enabling requests permission and disabling opens system notification settings.
- [ ] Keep FCM diagnostics in a separate diagnostics section.
- [ ] Rework Profile into grouped semantic action rows while preserving existing navigation and account behavior.
- [ ] Localize existing hardcoded Profile/logout labels in English and Russian.
- [ ] Ensure Profile, Settings, and Edit Profile consume modifiers and use safe-drawing Scaffold insets.
- [ ] Run common and Android host tests.
- [ ] Commit the Profile/Settings UX refresh.

## Task 5: Verify PR, push rewritten history, and clean stale branches/worktrees

- [ ] Run `git diff --check origin/dev...HEAD`, tracked-secret scans, and final status checks.
- [ ] Run Android debug assembly and the iOS simulator Kotlin compile target.
- [ ] Push the rewritten feature branch with `git push --force-with-lease`.
- [ ] Record that PR #12 merged before the follow-up and verify PR #14 targets `dev`, contains the new head, and has current GitHub check results.
- [ ] Prune stale worktree metadata after confirming the directories are absent and their commits are reachable.
- [ ] Delete only the stale `worktree-agent-*` local branches; retain active development and repository-default branches.
- [ ] Report the PR URL, App Store URL, verification results, cleaned targets, and the separate need for credential rotation.

## Task 6: Apply the approved Profile and Settings polish pass

**Files:** `IosAppPromotionBanner.kt`, `Settings.kt`, `Profile.kt`, and the approved design/plan docs.

**Interfaces:**

- Consumes: `UserDatastore.getIosAppPromotionDismissed: Flow<Boolean>`, `NotificationPermissionResult`, and the existing Profile navigation callbacks.
- Produces: no new public API; this task refines rendering, semantics, and interaction feedback only.

- [ ] Change Settings dismissal collection to nullable initial state so a previously dismissed banner never flashes:

```kotlin
val iosPromotionDismissed by datastore.getIosAppPromotionDismissed.collectAsState(initial = null)

AnimatedVisibility(
    visible = iosPromotionDismissed == false,
    enter = EnterTransition.None,
    exit = fadeOut() + shrinkVertically(),
) {
    IosAppPromotionBanner(
        onOpen = { openExternalUrl(IOS_APP_STORE_URL) },
        onShare = { shareText(iosShareMessage) },
        onDismiss = {
            scope.launch { datastore.saveIosAppPromotionDismissed(true) }
        },
    )
}
```

- [ ] Disable the notification toggle until `permissionState` is loaded while retaining the platform-derived checked value:

```kotlin
SettingsToggleRow(
    checked = permissionState == NotificationPermissionResult.Granted,
    enabled = permissionState != null,
    onCheckedChange = { enabled ->
        if (enabled) requestPermission() else openAppNotificationSettings()
    },
)
```

- [ ] Give the banner close action a 48 dp target and make both actions share available width with a 48 dp minimum height:

```kotlin
IconButton(
    onClick = onDismiss,
    modifier = Modifier.size(48.dp),
)

Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    TextButton(
        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        onClick = onShare,
    ) {
        Icon(Icons.Default.Share, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text(stringResource(Res.string.ios_app_promotion_share))
    }
    Button(
        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        onClick = onOpen,
    ) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text(stringResource(Res.string.ios_app_promotion_open))
    }
}
```

- [ ] Add a semantic account-status chip, outlined avatar, 40 dp tonal icon containers, and an error-container danger group to Profile without changing navigation or deletion behavior.

- [ ] Run `git diff --check`, `:composeApp:testAndroidHostTest`, `:composeApp:compileKotlinIosSimulatorArm64`, and `:androidApp:assembleDebug`.

- [ ] Commit with `feat: polish profile and settings interactions`, push the existing branch, and verify PR #14 remains mergeable with GitGuardian passing.
