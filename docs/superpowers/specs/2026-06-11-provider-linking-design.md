# Provider Linking — Design Spec

**Date:** 2026-06-11
**Branch:** feature/cmp-ios-migration

---

## Overview

Add a "Linked accounts" screen reachable from the Profile screen that lets authenticated users (including anonymous/guest users) link a Google or Apple provider to their existing Firebase account. Guest users can use this screen to "save" their account by linking a real provider.

Scope: **link-only** (no unlink in v1). When a credential is already bound to a different account, show an error and stay put.

---

## User Flows

### Normal authenticated user

1. Profile → tap "Связанные аккаунты" button.
2. `LinkedAccountsScreen` lists two rows: Google and Apple (Apple row visible on iOS only).
3. Each row shows a provider icon + name + status: checkmark (already linked) or "Link" button.
4. Tapping "Link" launches the provider flow (Google consent, Apple sheet).
5. On success: row updates to show checkmark. No navigation change.
6. On conflict (credential already on another account): snackbar appears with message, row stays as "Link".
7. On cancel: no-op.

### Guest (anonymous) user

1. Profile shows a **"Сохранить аккаунт"** (`ColumnButton`) in place of `UserButtons` (which is hidden for anonymous).
2. Same `LinkedAccountsScreen`; title changes to "Сохранить аккаунт" and a one-line subtitle explains that linking converts the guest session into a permanent account with the same UID.
3. After successful link: guest flag is gone, normal profile buttons appear on the next Profile visit (stream from `usersRepository.currentUser` already handles this).
4. The ViewModel explicitly calls `userDocumentInitializer.ensure(user)` after a successful guest upgrade, because `authStateChanged` does **not** re-fire when the same UID transitions from anonymous to non-anonymous.

---

## Architecture

### New files

| File | Description |
|---|---|
| `composeApp/src/commonMain/…/model/datasource/ProviderLinkHelper.kt` | Pure functions: `linkedProviders(providerData)` and `classifyLinkError(Throwable)`. No Firebase imports — test in commonTest without mocking. |
| `composeApp/src/commonMain/…/compose/viewmodels/LinkedAccountsViewModel.kt` | ViewModel: provider state, `linkGoogle()`, `linkApple()`, loading, error event. |
| `composeApp/src/commonMain/…/ui/LinkedAccountsScreen.kt` | Composable screen + route composable. |
| `composeApp/src/commonMain/…/platform/AppleAuthLauncher.kt` (modified) | Add `link(): Result<Unit>` to the `expect` declaration. |
| `composeApp/src/iosMain/…/platform/AppleAuthLauncher.ios.kt` (modified) | `actual` for `link()` using a new bridge slot. |
| `composeApp/src/iosMain/…/platform/IosAppleAuthBridge.kt` (modified) | Add `linkImpl` slot alongside existing `signInImpl`. |
| `iosApp/iosApp/AppleSignInBridge.swift` (modified) | Add `installAppleLinkBridge` that calls `currentUser.link(with: appleCredential)` instead of `Auth.signIn`. |
| `composeApp/src/commonTest/…/model/datasource/ProviderLinkHelperTest.kt` | Unit tests for both pure functions. |

### Routes

Add `LinkedAccountsRoute` object to `navigation/Routes.kt` (`@Serializable object LinkedAccountsRoute`).

Wire `composable<LinkedAccountsRoute>` in `ScheduleApp.NavGraph`.

### Koin

`LinkedAccountsViewModel` is a standard Koin ViewModel — no extra DI needed; it takes `UsersRepository`, `GoogleAuthLauncher`, `AppleAuthLauncher`, and the `CoroutineScope` (for `userDocumentInitializer.ensure` delegation) via the existing `repositoryModule` bindings.

---

## Components

### `ProviderLinkHelper` (pure, no Firebase)

```kotlin
data class LinkedProviders(val google: Boolean, val apple: Boolean)

fun linkedProviders(providerData: List<String>): LinkedProviders

enum class LinkError { ALREADY_IN_USE, CANCELLED, GENERIC }

fun classifyLinkError(e: Throwable): LinkError
```

`linkedProviders` maps raw provider-ID strings (`"google.com"`, `"apple.com"`) to a `LinkedProviders` value. `classifyLinkError` maps Firebase exception error codes to the three-case enum. Neither function has side effects; both are tested without mocks.

### `LinkedAccountsViewModel`

State exposed via `StateFlow`:

```kotlin
data class LinkedAccountsState(
    val providers: LinkedProviders,
    val isGuest: Boolean,
    val googleLoading: Boolean,
    val appleLoading: Boolean,
)
sealed interface LinkedAccountsEvent {
    data class Error(val error: LinkError) : LinkedAccountsEvent
}
```

Functions:
- `linkGoogle()`: calls `GoogleAuthLauncher.signIn()` → `Firebase.auth.currentUser!!.link(GoogleAuthProvider.credential(...))` → on success, refreshes providers and calls `userDocumentInitializer.ensure`.
- `linkApple()`: calls `AppleAuthLauncher.link()` (iOS-only bridge) → same post-link logic.
- Provider state is derived from `Firebase.auth.currentUser?.providerData` and refreshed after each successful link.

### `AppleAuthLauncher` extension

Add `suspend fun link(): Result<Unit>` to the `expect class`. iOS `actual` delegates to `IosAppleAuthBridge.linkImpl`. Android `actual` returns `Result.failure(UnsupportedOperationException("Apple Sign In not available on Android"))` — the UI hides the Apple row on non-iOS anyway, so this path is never reachable in practice.

The Swift bridge in `AppleSignInBridge.swift` gains `installAppleLinkBridge` which runs the same `ASAuthorizationController` nonce flow but calls `Auth.auth().currentUser?.link(with: credential)` instead of `Auth.auth().signIn(with: credential)`.

### `LinkedAccountsScreen`

- `Scaffold` with back-arrow top bar.
- If `isGuest`: subtitle card explaining the upgrade.
- `GoogleRow`: always visible. Shows checkmark if `providers.google`, else "Привязать" button that triggers `linkGoogle()` with a circular progress while `googleLoading`.
- `AppleRow`: visible only when `AppleAuthLauncher.isAvailable`. Same pattern with `appleLoading`.
- Collects `LinkedAccountsEvent.Error` via `LaunchedEffect` → shows `SnackbarHostState`.

---

## Error handling

| Firebase error code | `LinkError` | Snackbar text (en) |
|---|---|---|
| `ERROR_CREDENTIAL_ALREADY_IN_USE` | `ALREADY_IN_USE` | "This account is already linked to another user." |
| `ERROR_CANCELLED` / `CancellationException` | `CANCELLED` | *(no snackbar — silent)* |
| anything else | `GENERIC` | "Something went wrong. Please try again." |

---

## Strings

New string keys needed in `values/strings.xml` and `values-ru/strings.xml`:

| Key | EN | RU |
|---|---|---|
| `linked_accounts` | Linked accounts | Связанные аккаунты |
| `save_account` | Save account | Сохранить аккаунт |
| `save_account_subtitle` | Link a sign-in method to keep your data permanently. | Привяжите способ входа, чтобы сохранить данные навсегда. |
| `link_provider` | Link | Привязать |
| `provider_linked` | Linked | Привязан |
| `link_error_already_in_use` | This account is already linked to another user. | Этот аккаунт уже привязан к другому пользователю. |
| `link_error_generic` | Something went wrong. Please try again. | Что-то пошло не так. Попробуйте ещё раз. |

---

## Profile screen changes

In `Profile.kt`:
- When `currentUser.anonymous`: render a `ColumnButton` ("Сохранить аккаунт") above `UserButtons` (or instead, since `UserButtons` is already hidden). Navigates to `LinkedAccountsRoute`.
- In `UserButtons`: add `ColumnButton` ("Связанные аккаунты") between "Редактировать профиль" and "Настройка уведомлений". Navigates to `LinkedAccountsRoute`.

---

## Testing

### Unit tests (`ProviderLinkHelperTest`, commonTest)

1. `linkedProviders` returns `google=true, apple=false` for `["google.com"]`.
2. `linkedProviders` returns `google=false, apple=false` for empty list.
3. `linkedProviders` returns both true for `["google.com", "apple.com"]`.
4. `classifyLinkError` returns `ALREADY_IN_USE` for an exception whose message contains `ERROR_CREDENTIAL_ALREADY_IN_USE`.
5. `classifyLinkError` returns `CANCELLED` for `CancellationException`.
6. `classifyLinkError` returns `GENERIC` for any other throwable.

### Manual verification

- **Android emulator**: sign in with QA account → Profile → "Связанные аккаунты" → Google row shows "Привязать" → tap → consent → row shows checkmark. Attempt to link the same Google account again → snackbar "already linked to another user".
- **iOS**: Apple-signed-in user → "Связанные аккаунты" → Apple row shows checkmark → Google row shows "Привязать" → tap → link → checkmark.
- **Guest upgrade (Android emulator)**: sign in anonymously (if the flow exists) → Profile → "Сохранить аккаунт" → link Google → app reflects permanent account, Firestore `users/{uid}` doc exists with `anonymous: false`.

---

## Out of scope (v1)

- Unlinking providers.
- Email/password provider linking.
- Conflict-merge: upgrading an anonymous account when the provider is already on a separate permanent account (Firebase requires a manual decision; show a clear ALREADY_IN_USE error).
