# Account Deletion In User Details Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an App Store-compliant in-app account deletion flow reachable from the signed-in user's details screen.

**Architecture:** Deletion is server-authoritative through a new authenticated Firebase callable so the client cannot delete or mutate other users. The callable deletes Firebase Auth, deletes `users/{uid}`, removes the uid from appliance membership fields, deletes the user's own booking events, and strips manager references where the user only acted as manager. The shared Compose UI exposes the destructive action only when `detailsUser.userId == currentUser.userId`; anonymous guest sessions get the same deletion action directly in Profile because they do not have a Firestore-backed `UserDetailsRoute`.

**Tech Stack:** Compose Multiplatform, Koin, Gitlive Firebase Auth/Functions, Firebase Cloud Functions v2, Firebase Admin SDK, Node test runner.

---

## File Map

- Create `functions/src/accountDeletion.ts`: pure deletion orchestration plus Firestore/Admin-backed store.
- Modify `functions/src/index.ts`: export `deleteCurrentAccount` callable.
- Create `functions/test/accountDeletion.test.mjs`: unit coverage for deletion order and self-only behavior.
- Modify `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/model/repository/UsersRepository.kt`: add `deleteCurrentAccount()`.
- Modify `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/model/datasource/FirebaseUsersRepositoryImpl.kt`: call `deleteCurrentAccount`, clear local user cache, sign out locally.
- Modify `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/viewmodels/UserDetailsViewModel.kt`: deletion state and action.
- Modify `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/home/UserDetails.kt`: self-only delete section and confirmation dialog.
- Modify `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/viewmodels/ProfileViewModel.kt`: anonymous guest deletion state and action.
- Modify `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/home/profile/Profile.kt`: add a clear Profile entry to the signed-in user's own `UserDetailsRoute`; show direct delete for anonymous guests.
- Modify `composeApp/src/commonMain/composeResources/values/strings.xml` and `values-ru/strings.xml`: localized deletion copy.
- Modify `docs/privacy.html`, `docs/APP_STORE_SUBMISSION.md`, and `docs/app-store-reviewer-notes.md`: remove email-only deletion wording and document the in-app path.

## Task 1: Backend Account Deletion Callable

**Files:**
- Create: `functions/src/accountDeletion.ts`
- Modify: `functions/src/index.ts`
- Test: `functions/test/accountDeletion.test.mjs`

- [ ] **Step 1: Write failing tests**

Create `functions/test/accountDeletion.test.mjs`:

```js
import { test } from "node:test"
import assert from "node:assert/strict"
import { deleteAccountData } from "../lib/accountDeletion.js"

test("deleteAccountData removes all app-owned user data before auth", async () => {
  const calls = []
  const store = {
    deleteUserDocument: async (uid) => calls.push(["deleteUserDocument", uid]),
    deleteOwnedEvents: async (uid) => calls.push(["deleteOwnedEvents", uid]),
    clearManagedEvents: async (uid) => calls.push(["clearManagedEvents", uid]),
    removeFromApplianceMembers: async (uid) => calls.push(["removeFromApplianceMembers", uid]),
    clearCreatedAppliances: async (uid) => calls.push(["clearCreatedAppliances", uid]),
    deleteAuthUser: async (uid) => calls.push(["deleteAuthUser", uid]),
  }

  await deleteAccountData("uid-1", store)

  assert.deepEqual(calls, [
    ["deleteOwnedEvents", "uid-1"],
    ["clearManagedEvents", "uid-1"],
    ["removeFromApplianceMembers", "uid-1"],
    ["clearCreatedAppliances", "uid-1"],
    ["deleteUserDocument", "uid-1"],
    ["deleteAuthUser", "uid-1"],
  ])
})

test("deleteAccountData rejects blank uid", async () => {
  await assert.rejects(
    () => deleteAccountData(" ", {
      deleteUserDocument: async () => {},
      deleteOwnedEvents: async () => {},
      clearManagedEvents: async () => {},
      removeFromApplianceMembers: async () => {},
      clearCreatedAppliances: async () => {},
      deleteAuthUser: async () => {},
    }),
    /missing uid/,
  )
})
```

Run:

```bash
npm --prefix functions run test
```

Expected: build fails because `accountDeletion.ts` does not exist.

- [ ] **Step 2: Implement deletion orchestration**

Create `functions/src/accountDeletion.ts`:

```ts
import { getAuth } from "firebase-admin/auth"
import { DocumentReference, FieldValue, Firestore, QueryDocumentSnapshot, WriteBatch } from "firebase-admin/firestore"

export interface AccountDeletionStore {
  deleteOwnedEvents(uid: string): Promise<void>
  clearManagedEvents(uid: string): Promise<void>
  removeFromApplianceMembers(uid: string): Promise<void>
  clearCreatedAppliances(uid: string): Promise<void>
  deleteUserDocument(uid: string): Promise<void>
  deleteAuthUser(uid: string): Promise<void>
}

export async function deleteAccountData(uid: string, store: AccountDeletionStore): Promise<void> {
  if (!uid.trim()) throw new Error("missing uid")
  await store.deleteOwnedEvents(uid)
  await store.clearManagedEvents(uid)
  await store.removeFromApplianceMembers(uid)
  await store.clearCreatedAppliances(uid)
  await store.deleteUserDocument(uid)
  await store.deleteAuthUser(uid)
}

export function createFirestoreAccountDeletionStore(db: Firestore): AccountDeletionStore {
  return {
    async deleteOwnedEvents(uid) {
      const snapshot = await db.collection("events").where("userId", "==", uid).get()
      await commitDocs(db, snapshot.docs, (batch, ref) => batch.delete(ref))
    },
    async clearManagedEvents(uid) {
      const snapshot = await db.collection("events").where("managedById", "==", uid).get()
      await commitDocs(db, snapshot.docs, (batch, ref) => {
        batch.update(ref, {
          managedById: FieldValue.delete(),
          managedTime: FieldValue.delete(),
          managerCommentary: "",
        })
      })
    },
    async removeFromApplianceMembers(uid) {
      const userSnapshot = await db.collection("appliances").where("userIds", "array-contains", uid).get()
      const superuserSnapshot = await db.collection("appliances").where("superuserIds", "array-contains", uid).get()
      const refs = uniqueRefs([...userSnapshot.docs, ...superuserSnapshot.docs])
      await commitDocs(db, refs, (batch, ref) => {
        batch.update(ref, {
          userIds: FieldValue.arrayRemove(uid),
          superuserIds: FieldValue.arrayRemove(uid),
        })
      })
    },
    async clearCreatedAppliances(uid) {
      const snapshot = await db.collection("appliances").where("createdById", "==", uid).get()
      await commitDocs(db, snapshot.docs, (batch, ref) => batch.update(ref, { createdById: "" }))
    },
    async deleteUserDocument(uid) {
      await db.collection("users").doc(uid).delete()
    },
    async deleteAuthUser(uid) {
      await getAuth().deleteUser(uid)
    },
  }
}

type RefLike = QueryDocumentSnapshot | DocumentReference

function uniqueRefs(docs: QueryDocumentSnapshot[]): DocumentReference[] {
  return [...new Map(docs.map((doc) => [doc.ref.path, doc.ref])).values()]
}

async function commitDocs(
  db: Firestore,
  docsOrRefs: RefLike[],
  write: (batch: WriteBatch, ref: DocumentReference) => void,
): Promise<void> {
  let batch = db.batch()
  let count = 0
  for (const docOrRef of docsOrRefs) {
    const ref = "ref" in docOrRef ? docOrRef.ref : docOrRef
    write(batch, ref)
    count += 1
    if (count === 450) {
      await batch.commit()
      batch = db.batch()
      count = 0
    }
  }
  if (count > 0) await batch.commit()
}
```

- [ ] **Step 3: Export callable**

Modify `functions/src/index.ts`:

```ts
import { createFirestoreAccountDeletionStore, deleteAccountData } from "./accountDeletion"
```

Add after `sendNotification`:

```ts
export const deleteCurrentAccount = onCall(async (req) => {
  if (!req.auth) throw new HttpsError("unauthenticated", "sign in first")

  try {
    await deleteAccountData(req.auth.uid, createFirestoreAccountDeletionStore(getFirestore()))
    return { ok: true }
  } catch (e: any) {
    const msg = e?.message ?? String(e)
    console.error("deleteCurrentAccount failed:", { uid: req.auth.uid, msg })
    throw new HttpsError("internal", "account deletion failed")
  }
})
```

- [ ] **Step 4: Verify backend**

Run:

```bash
npm --prefix functions run test
```

Expected: all Node tests pass.

Commit:

```bash
git add functions/src/accountDeletion.ts functions/src/index.ts functions/test/accountDeletion.test.mjs
git commit -m "feat: add account deletion callable"
```

## Task 2: Shared Repository API

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/model/repository/UsersRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/model/datasource/FirebaseUsersRepositoryImpl.kt`

- [ ] **Step 1: Add repository contract**

In `UsersRepository.kt`, add:

```kotlin
suspend fun deleteCurrentAccount(): Result<Unit>
```

- [ ] **Step 2: Implement Firebase repository method**

In `FirebaseUsersRepositoryImpl.kt`, add imports:

```kotlin
import dev.gitlive.firebase.functions.functions
```

Add property:

```kotlin
private val functions by lazy { Firebase.functions("asia-northeast1") }
```

Add method:

```kotlin
override suspend fun deleteCurrentAccount(): Result<Unit> = runCatching {
    Firebase.auth.currentUser?.getIdToken(true) ?: error("No signed-in user")
    functions.httpsCallable("deleteCurrentAccount").invoke()
    userDatastore.saveUser(User())
    Firebase.auth.signOut()
}
```

- [ ] **Step 3: Verify shared compile**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: compile succeeds.

Commit:

```bash
git add composeApp/src/commonMain/kotlin/ru/dvfu/appliances/model/repository/UsersRepository.kt composeApp/src/commonMain/kotlin/ru/dvfu/appliances/model/datasource/FirebaseUsersRepositoryImpl.kt
git commit -m "feat: expose current account deletion in users repository"
```

## Task 3: UserDetails Self-Deletion UI

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/viewmodels/UserDetailsViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/home/UserDetails.kt`
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`
- Modify: `composeApp/src/commonMain/composeResources/values-ru/strings.xml`

- [ ] **Step 1: Add strings**

Add English strings:

```xml
<string name="account_details">Account details</string>
<string name="delete_account">Delete account</string>
<string name="delete_account_description">Permanently delete your sign-in account, profile, push token, and booking data.</string>
<string name="delete_account_title">Delete account?</string>
<string name="delete_account_message">This permanently deletes your account and associated booking data. This action cannot be undone.</string>
<string name="delete_account_confirm">Delete account</string>
<string name="delete_account_failed">Couldn\'t delete account. Please try again.</string>
```

Add Russian strings:

```xml
<string name="account_details">Данные аккаунта</string>
<string name="delete_account">Удалить аккаунт</string>
<string name="delete_account_description">Навсегда удалить аккаунт, профиль, push-токен и данные бронирований.</string>
<string name="delete_account_title">Удалить аккаунт?</string>
<string name="delete_account_message">Аккаунт и связанные данные бронирований будут удалены без возможности восстановления.</string>
<string name="delete_account_confirm">Удалить аккаунт</string>
<string name="delete_account_failed">Не удалось удалить аккаунт. Попробуйте ещё раз.</string>
```

- [ ] **Step 2: Add view-model action**

In `UserDetailsViewModel.kt`, add:

```kotlin
private val _accountDeletionState = MutableStateFlow<UiState>(UiState.Success)
val accountDeletionState = _accountDeletionState.asStateFlow()
```

Add:

```kotlin
fun deleteCurrentAccount(onDeleted: () -> Unit) {
    viewModelScope.launch {
        val current = currentUser.value
        val details = detailsUser.value
        if (current.userId == "0" || current.userId != details.userId) return@launch

        _accountDeletionState.value = UiState.InProgress
        usersRepository.deleteCurrentAccount().fold(
            onSuccess = {
                _accountDeletionState.value = UiState.Success
                onDeleted()
            },
            onFailure = {
                _accountDeletionState.value = UiState.Error
                SnackbarManager.showMessage(Res.string.delete_account_failed)
            },
        )
    }
}
```

- [ ] **Step 3: Add self-only delete section in `UserDetails.kt`**

Import:

```kotlin
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
```

In `UserDetails`, collect state:

```kotlin
val accountDeletionState by viewModel.accountDeletionState.collectAsState()
var isDeleteAccountDialogOpen by remember { mutableStateOf(false) }
val isOwnAccount = currentUser.userId != "0" && currentUser.userId == detailsUser.userId
```

Inside the `Column`, after role/appliance content:

```kotlin
if (isOwnAccount) {
    DeleteAccountCard(
        deleting = accountDeletionState is UiState.InProgress,
        onClick = { isDeleteAccountDialogOpen = true },
    )
}
```

Add dialog near the role dialog:

```kotlin
if (isDeleteAccountDialogOpen) {
    DeleteAccountDialog(
        deleting = accountDeletionState is UiState.InProgress,
        onDismiss = { if (accountDeletionState !is UiState.InProgress) isDeleteAccountDialogOpen = false },
        onConfirm = {
            viewModel.deleteCurrentAccount {
                isDeleteAccountDialogOpen = false
            }
        },
    )
}
```

Add composables in the same file:

```kotlin
@Composable
private fun DeleteAccountCard(deleting: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.delete_account),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = stringResource(Res.string.delete_account_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            if (deleting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                OutlinedButton(onClick = onClick) {
                    Text(stringResource(Res.string.delete))
                }
            }
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    deleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp),
            )
        },
        title = { Text(stringResource(Res.string.delete_account_title)) },
        text = { Text(stringResource(Res.string.delete_account_message)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !deleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                if (deleting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.delete_account_confirm))
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !deleting) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
```

- [ ] **Step 4: Add previews for new composables**

Add in `UserDetails.kt`:

```kotlin
@Preview
@Composable
private fun DeleteAccountCardPreview() {
    MaterialTheme {
        DeleteAccountCard(deleting = false, onClick = {})
    }
}

@Preview
@Composable
private fun DeleteAccountDialogPreview() {
    MaterialTheme {
        DeleteAccountDialog(
            deleting = false,
            onDismiss = {},
            onConfirm = {},
        )
    }
}
```

- [ ] **Step 5: Verify UI compile**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: compile succeeds.

Commit:

```bash
git add composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/viewmodels/UserDetailsViewModel.kt composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/home/UserDetails.kt composeApp/src/commonMain/composeResources/values/strings.xml composeApp/src/commonMain/composeResources/values-ru/strings.xml
git commit -m "feat: add self account deletion in user details"
```

## Task 4: Reviewer-Discoverable Profile Entry And Guest Fallback

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/viewmodels/ProfileViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/home/profile/Profile.kt`

- [ ] **Step 1: Add guest deletion state to `ProfileViewModel`**

Add imports:

```kotlin
import ru.dvfu.appliances.application.SnackbarManager
import ru.dvfu.appliances.compose.components.UiState
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.*
```

Add state:

```kotlin
private val _accountDeletionState = MutableStateFlow<UiState>(UiState.Success)
val accountDeletionState = _accountDeletionState.asStateFlow()
```

Add action:

```kotlin
fun deleteCurrentAccount(onDeleted: () -> Unit) {
    viewModelScope.launch {
        _accountDeletionState.value = UiState.InProgress
        usersRepository.deleteCurrentAccount().fold(
            onSuccess = {
                _accountDeletionState.value = UiState.Success
                onDeleted()
            },
            onFailure = {
                _accountDeletionState.value = UiState.Error
                SnackbarManager.showMessage(Res.string.delete_account_failed)
            },
        )
    }
}
```

- [ ] **Step 2: Add route and loading imports**

Add:

```kotlin
import androidx.compose.material3.ButtonDefaults
import ru.dvfu.appliances.compose.components.UiState
import ru.dvfu.appliances.compose.components.views.ModalLoadingDialog
import ru.dvfu.appliances.navigation.UserDetailsRoute
```

- [ ] **Step 3: Add Profile state and anonymous delete dialog**

Inside `Profile`, after `currentUser`:

```kotlin
val accountDeletionState by viewModel.accountDeletionState.collectAsState()
var isDeleteAccountDialogOpen by rememberSaveable { mutableStateOf(false) }
```

Before `Scaffold`, add:

```kotlin
if (accountDeletionState is UiState.InProgress) {
    ModalLoadingDialog()
}
if (isDeleteAccountDialogOpen) {
    DefaultDialog(
        primaryText = stringResource(Res.string.delete_account_title),
        secondaryText = stringResource(Res.string.delete_account_message),
        negativeButtonText = stringResource(Res.string.cancel),
        onNegativeClick = { isDeleteAccountDialogOpen = false },
        positiveButtonText = stringResource(Res.string.delete_account_confirm),
        positiveButtonColor = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
        onPositiveClick = {
            viewModel.deleteCurrentAccount {
                isDeleteAccountDialogOpen = false
            }
        },
        onDismiss = { isDeleteAccountDialogOpen = false },
    )
}
```

- [ ] **Step 4: Add own account details button for registered users**

In `UserButtons`, before edit profile:

```kotlin
ColumnButton(Icons.Default.AccountCircle, stringResource(Res.string.account_details)) {
    navController.navigate(UserDetailsRoute(userId = currentUser.userId))
}
```

This gives Apple reviewers a simple path: Profile -> Account details -> Delete account.

- [ ] **Step 5: Add anonymous guest deletion action**

In `Profile`, replace the anonymous branch with:

```kotlin
if (currentUser.anonymous) {
    ColumnButton(Icons.Default.Link, stringResource(Res.string.save_account)) {
        navController.navigate(LinkedAccountsRoute)
    }
    Spacer(Modifier.height(12.dp))
    ColumnButton(Icons.Default.DeleteForever, stringResource(Res.string.delete_account)) {
        isDeleteAccountDialogOpen = true
    }
} else {
    UserButtons(navController, currentUser)
}
```

This gives guests a complete path: Profile -> Delete account.

- [ ] **Step 6: Verify compile**

Run:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: compile succeeds.

Commit:

```bash
git add composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/viewmodels/ProfileViewModel.kt composeApp/src/commonMain/kotlin/ru/dvfu/appliances/compose/home/profile/Profile.kt
git commit -m "feat: link profile to account deletion paths"
```

## Task 5: Policy And Review Notes

**Files:**
- Modify: `docs/privacy.html`
- Modify: `docs/APP_STORE_SUBMISSION.md`
- Modify: `docs/app-store-reviewer-notes.md`

- [ ] **Step 1: Update privacy policy deletion wording**

Change both Russian and English retention/deletion sections from email-only deletion to in-app deletion:

```html
<p>Пользователь может удалить учётную запись в приложении: Профиль → Данные аккаунта → Удалить аккаунт.
Для гостевой сессии: Профиль → Удалить аккаунт.
При удалении удаляются профиль, push-токен, учётная запись Firebase Authentication и связанные бронирования.</p>
```

```html
<p>Users can delete their account in the app: Profile → Account details → Delete account.
For guest sessions: Profile → Delete account.
Deletion removes the profile, push token, Firebase Authentication account, and associated booking data.</p>
```

- [ ] **Step 2: Update App Store reviewer notes**

Add to `docs/app-store-reviewer-notes.md`:

```markdown
## Account Deletion

The demo account can verify in-app account deletion from:

Profile → Account details → Delete account

For guest sessions, use:

Profile → Delete account

The delete action is shown only on the signed-in user's own details screen, or directly in Profile for anonymous guests because guest sessions do not have a Firestore profile document. It removes the Firebase Auth account, Firestore profile when present, push token, appliance membership references, and associated booking data.
```

- [ ] **Step 3: Update submission checklist**

In `docs/APP_STORE_SUBMISSION.md`, add a review note under demo-account instructions:

```markdown
Account deletion path for review: Profile → Account details → Delete account. Guest path: Profile → Delete account.
```

Commit:

```bash
git add docs/privacy.html docs/APP_STORE_SUBMISSION.md docs/app-store-reviewer-notes.md
git commit -m "docs: document in-app account deletion"
```

## Task 6: End-To-End Verification

- [ ] **Step 1: Run all local checks**

```bash
npm --prefix functions run test
./gradlew :composeApp:compileDebugKotlinAndroid
./gradlew :androidApp:assembleDebug
```

Expected: all pass.

- [ ] **Step 2: Deploy callable**

```bash
firebase deploy --only functions:deleteCurrentAccount
```

Expected: Firebase deploy succeeds for `asia-northeast1-deleteCurrentAccount`.

- [ ] **Step 3: Manual reviewer flow**

Use a disposable QA account, not the real reviewer account:

1. Sign in.
2. Open Profile.
3. Tap Account details.
4. Confirm Delete account.
5. Confirm app returns to login.
6. In Firebase Console, verify Auth user is gone.
7. In Firestore, verify `users/{uid}` is gone, owned `events` are gone, appliance `userIds`/`superuserIds` no longer contain the uid, and `createdById` no longer equals the uid.

Repeat once with Continue as guest:

1. Sign in as guest.
2. Open Profile.
3. Confirm Delete account.
4. Confirm app returns to login.
5. In Firebase Console, verify the anonymous Auth user is gone.

- [ ] **Step 4: Build release after verification**

```bash
./gradlew :androidApp:bundleRelease
```

For iOS, archive/upload from Xcode after the callable is deployed and the privacy policy page is published.

Commit:

```bash
git status --short
git commit --allow-empty -m "chore: verify account deletion flow"
```
