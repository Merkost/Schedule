# FCM v1 + Cloud Functions Migration Plan

Status: **planned, not yet executed**.
Captured: 2026-05-08.

## Decisions

| Question | Choice |
|---|---|
| Architecture | **Hybrid** — Firestore triggers for 7 data-driven flows, single callable for `sendTestNotification` |
| Region | `asia-northeast1` (Tokyo) — closer to Vladivostok user base |
| Sequencing | Plan committed; execute in a future session |

## Why this migration

- Legacy FCM HTTP API (`/fcm/send` with `Authorization: key=…`) is being shut down by Google. Date keeps slipping but it will eventually break.
- Current `FCM_SERVER_KEY` is shipped inside the APK/IPA via `local.properties` → `AppBuildConfig` → embedded constant. Anyone who unzips the artifact can extract it and send arbitrary pushes as our project. v1 makes this impossible-to-ignore: it requires a service-account private key, which categorically can't be in a client app.
- v1 needs a backend gateway. Cloud Functions is the cheapest option and integrates natively with Firestore + Firebase Auth.

## Architecture: hybrid

**Firestore triggers** for everything driven by data writes (events / appliances / users). Client writes the document; server fans out push. Benefits: server can't be tricked into spoofing, fanout logic centralized, client gets ~150 LOC lighter.

**Single `httpsCallable`** for `sendTestNotification` — there's no Firestore write to react to; it's a manual "tap to verify push works" action.

## File layout

```
Schedule/
├─ functions/                          # NEW
│  ├─ package.json
│  ├─ tsconfig.json
│  ├─ .eslintrc.js
│  └─ src/
│     ├─ index.ts                       # exports all triggers + callables
│     ├─ push.ts                        # getMessaging().send wrapper, default channel id
│     ├─ formatting.ts                  # ru-RU date/time formatters mirroring TimeUtils.kt
│     ├─ events.ts                      # onCreated / onUpdated / onDeleted on events/{id}
│     ├─ appliances.ts                  # onDeleted on appliances/{id} (+ cascade event delete)
│     ├─ users.ts                       # onUpdated on users/{id} for role changes
│     └─ testNotification.ts            # callable
├─ firebase.json                        # NEW
├─ .firebaserc                          # NEW
├─ firestore.rules                      # NEW or moved
└─ firestore.indexes.json               # NEW or moved
```

All functions pinned to `region: "asia-northeast1"`.

## Per-path mapping

| Current `NotificationManager` method | Mechanism | Trigger | Recipients |
|---|---|---|---|
| `applianceDeleted(appliance)` | trigger | `onDocumentDeleted("appliances/{id}")` | `before.userIds ∪ before.superuserIds` minus actor |
| `eventUpdated(event, data)` | trigger | `onDocumentUpdated("events/{id}")` (commentary / non-time-non-status diff) | `event.userId` |
| `eventDeleted(event)` | trigger | `onDocumentDeleted("events/{id}")` | `event.userId` |
| `newEvent(event)` | trigger | `onDocumentCreated("events/{id}")` | superusers of `event.applianceId` minus actor |
| `newEventStatus(event, newStatus)` | trigger | `onDocumentUpdated` (when `status` changes) | `event.userId` |
| `eventTimeChanged(event, …)` | trigger | `onDocumentUpdated` (when `timeStart`/`timeEnd`/`date` change) | `event.userId ∪ event.managedById` |
| `newUserRole(user, role)` | trigger | `onDocumentUpdated("users/{id}")` (when `role` changes) | `user.userId` |
| `sendTestNotificationToCurrentDevice()` | **callable** | `onCall("sendTestNotification")` | request.auth.uid → `users/{uid}.msgToken` |

The `events/{id}` updates use **one** `onDocumentUpdated` handler that diffs `before` vs `after` and dispatches to `eventTimeChanged` / `newEventStatus` / `eventUpdated` flavors. Cleaner than three separate triggers.

## Actor exclusion

Several existing flows skip notifying the actor (e.g., I cancel my own booking → I don't get notified). The trigger doesn't have client context, so we plumb it through the document:

- Add `_meta: { actorId: <uid> }` to every Firestore write in repos.
- Triggers read `after.data()._meta.actorId` (or `before.data()._meta.actorId` for deletes that record actor on a final mutation before the delete) and exclude that uid from recipients.
- Security Rules require `request.resource.data._meta.actorId == request.auth.uid` so the actor field can't be spoofed.

## Security Rules sketch

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function signedIn() { return request.auth != null }
    function isAdmin() { return signedIn() &&
      get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 3 }
    function isSelf(uid) { return signedIn() && request.auth.uid == uid }
    function isActor() { return request.resource.data._meta.actorId == request.auth.uid }

    match /users/{uid} {
      allow read: if signedIn();
      allow update: if (isSelf(uid) || isAdmin()) && isActor();
    }
    match /events/{eventId} {
      allow read: if signedIn();
      allow create: if signedIn() && isActor() &&
        request.resource.data.userId == request.auth.uid;
      allow update: if signedIn() && isActor();
      allow delete: if signedIn() &&
        (resource.data.userId == request.auth.uid || isAdmin());
    }
    match /appliances/{id} {
      allow read: if signedIn();
      allow write: if isAdmin() && isActor();
    }
  }
}
```

Read whatever's currently in the Firebase console first; merge rather than overwrite.

## Client changes (commonMain)

1. Drop `FCM_SERVER_KEY` from `local.properties`, `AppBuildConfig`, both `PlatformModule.kt` named bindings.
2. Add gitlive dep: `dev.gitlive:firebase-functions`. Bind `Firebase.functions` in DI.
3. `NotificationManager` interface trims to **just** `sendTestNotificationToCurrentDevice()` and `subscribeCurrentUser()` (or drop subscribe entirely — see below).
4. `NotificationManagerImpl` becomes ~30 lines:
   ```kotlin
   override suspend fun sendTestNotificationToCurrentDevice(): Result<String> = runCatching {
       val token = NotifierManager.getPushNotifier().getToken()
       check(!token.isNullOrBlank()) { "FCM token unavailable on this device" }
       Firebase.functions("asia-northeast1")
           .httpsCallable("sendTestNotification")
           .invoke()
       token
   }
   ```
5. Delete:
   - `network/NotificationApi.kt`
   - `network/NotificationHttpClient.kt` (if unused after #5)
   - `entity/notifications/PushNotification.kt`, `Notification.kt`, `NotificationData.kt` (or keep just for `NotificationConstants.NotificationType` enum which the channelId code still uses)
   - All `notificationManager.{newEvent,eventDeleted,eventUpdated,…}` call sites — 7 places across ViewModels and use cases.
   - `ru.dvfu.appliances.di.networkModule`'s `NotificationApi` binding.
6. Add `_meta.actorId` to writes in:
   - `EventsRepositoryImpl.addNewEvent / setNewTimeEnd / setNewEventStatus / updateEvent`
   - `AppliancesRepositoryImpl.deleteUserFromAppliance / deleteSuperUserFromAppliance / changeApplianceStatus / addUsersToAppliance / addSuperUsersToAppliance`
   - `FirebaseUsersRepositoryImpl.setNewProfileData / updateUserField / updateCurrentUserField`
7. The `subscribeCurrentUser → "weather"` topic is dead code; drop it.

Net diff: deletes more code than it adds.

## Local dev

```
# one-time
npm install -g firebase-tools
cd functions && firebase init functions      # TypeScript
firebase init emulators                       # Functions + Firestore + Auth

# loop
cd functions && npm run build
firebase emulators:start

# deploy
firebase deploy --only functions
firebase functions:log
```

App points at emulator in debug builds (gated by `AppDebug.isDebug`):
```kotlin
if (AppDebug.isDebug) {
    Firebase.functions("asia-northeast1").useEmulator(
        host = if (Platform.isAndroid) "10.0.2.2" else "localhost",
        port = 5001,
    )
    Firebase.firestore.useEmulator("10.0.2.2"/"localhost", 8080)
    Firebase.auth.useEmulator("10.0.2.2"/"localhost", 9099)
}
```

(`Platform.isAndroid` — small expect/actual or use the existing `AppContextHolder` pattern.)

## Migration sequence

Each step keeps Android + iOS green. Commit after each.

1. **Scaffold Functions project + deploy a no-op `sendTestNotification`.** Verify `firebase deploy` works end-to-end. ~1h.
2. **Switch `sendTestNotificationToCurrentDevice` to the callable.** Add gitlive functions dep, swap the impl, delete `FCM_SERVER_KEY`, delete `NotificationApi`. **Legacy FCM is gone after this step.** ~1.5h.
3. **Add `_meta.actorId` to all writes.** Doesn't break anything yet. ~30m.
4. **Roll out triggers in low-to-high-risk order:**
   1. `newUserRole` (single recipient, rare)
   2. `eventDeleted`
   3. `newEventStatus`
   4. `eventTimeChanged`
   5. `newEvent` (multi-recipient fanout)
   6. `applianceDeleted` (multi-recipient + cascade)
   For each: deploy trigger → remove the corresponding `notificationManager.X(...)` call from client → manually verify no double-notification on staging. ~4-6h total.
5. **Tighten Firestore Security Rules** to require `_meta.actorId == auth.uid` on every write. ~1h.
6. **Final cleanup** — delete `NotificationManagerImpl` body, drop the topic-subscribe leftover, drop unused imports. ~30m.

**Total: ~9 hours of focused work, ideally 2 sessions** (steps 1-2 in one; 3-6 in another).

## Open items to resolve at execution time

1. **Read existing Firestore Rules** before writing new ones — merge rather than replace.
2. **Channel IDs** — current Android `Schedule.onCreate()` creates channels via `Constants.NotificationType.channelId`. The Cloud Function payload must set `android.notification.channelId` to the same value. Hard-code in `functions/src/push.ts` to keep it from drifting:
   ```ts
   const CHANNEL_BY_TYPE: Record<string, string> = {
     DEFAULT: "schedule_default",
     APPLIANCE: "schedule_appliances",
     EVENT: "schedule_events",
     MY_EVENT: "schedule_my_events",
     NEW_EVENT: "schedule_new_events",
   }
   ```
3. **Russian month/day formatters** — `formattedDate`, `formattedDateTime`, etc. from `TimeUtils.kt` need a TS port. ~30 LOC. Or pass pre-formatted strings from the client via Firestore (uglier, but possible).
4. **Functions cold start** on `asia-northeast1` is ~1.5s. Test notifications will feel slightly laggier than the current direct-FCM path. Acceptable for this app; if not, set `minInstances: 1` on the test callable (~$1.50/month).
5. **Service account JSON** is auto-managed by Firebase. Never commit it. Functions runtime mounts it at deploy time.

## Effort summary

| Step | Time | Outcome |
|---|---|---|
| 1 | 1h | Functions project deployed, callable works |
| 2 | 1.5h | **Legacy FCM API gone**, server key deleted |
| 3 | 0.5h | `_meta.actorId` on all writes |
| 4 | 4-6h | All 6 triggers live, client fanout calls deleted |
| 5 | 1h | Security Rules locked down |
| 6 | 0.5h | Cleanup |
| **Total** | **~9h** | Pure backend gateway, no FCM key in app, smaller client codebase |
