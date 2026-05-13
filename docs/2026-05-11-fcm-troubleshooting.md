# FCM Push Notifications — Troubleshooting Playbook

Captured: 2026-05-11. Lessons from migrating off the legacy `/fcm/send` API onto FCM v1 via Cloud Functions, then debugging why pushes still didn't arrive.

## The full failure chain (in the order we discovered each)

Each fix in this list was necessary; pushes only arrived once all of them were in place. If pushes break again, walk this list top-to-bottom.

### 1. Legacy `/fcm/send` is dead — use FCM v1 via Cloud Functions

Symptom: `legacy /fcm/send returned 404` in Crashlytics / logcat. Cause: Google retired the endpoint. Fix: routes go through the `sendNotification` callable in `functions/src/index.ts`. Don't try to reintroduce the legacy path.

### 2. First Cloud Functions deploy fails on a missing build-service-account permission

Symptom from `firebase deploy --only functions`:
> Build failed with status: FAILURE. Could not build the function due to a missing permission on the build service account.

Cause: Google changed the default IAM on the Compute service account; new projects don't get the build permission automatically. Fix: re-run `firebase deploy --only functions` once. The CLI prompts to enable APIs / grant the role the second time. **If the second deploy succeeds but pushes still fail with 401**, see step 3.

### 3. Cloud Run service is private after a partial deploy

Symptom (Cloud Logging on `sendnotification`):
> The request was not authorized to invoke this service. The access token could not be verified.

Client side: `FirebaseFunctionsException: UNAUTHENTICATED`. Cause: Firebase CLI usually sets the `allUsers` invoker binding on first deploy, but if the first deploy failed it doesn't retry, and the second succeeds without setting it. Fix: in [Cloud Run console](https://console.cloud.google.com/run?project=schedule-4c151) → `sendnotification` → **Security** tab → flip **Authentication** to **Allow public access** → Save. (Firebase callable's own `request.auth` check inside the function still gates real auth — "public" here just means Cloud Run lets the request reach the function for inspection.)

Alternative via gcloud (needs `gcloud auth login` as a project Editor/Owner):
```bash
gcloud run services add-iam-policy-binding sendnotification --region=asia-northeast1 --member=allUsers --role=roles/run.invoker --project=schedule-4c151
```

### 4. App Check "No AppCheckProvider installed" is a red herring

Symptom (client logcat): `Error getting App Check token. FirebaseException: No AppCheckProvider installed.` Symptom (server log): `Allowing request with invalid AppCheck token because enforcement is disabled`.

This is **fine, ignore it.** App Check enforcement is opt-in per-function via `enforceAppCheck: true` in the function definition. Our function doesn't set that. The warning is the client SDK trying to attach an App Check token, failing, and falling back to send the request without one. The server doesn't reject. Only worry about this if you intentionally enable App Check.

### 5. APNs Authentication Key not uploaded → `messaging/third-party-auth-error`

**This was the actual final blocker.**

Symptom (server log, after redeploying with `console.error("FCM send failed:", ...)`):
```
FCM send failed: {
  code: 'messaging/third-party-auth-error',
  msg: 'Request is missing required authentication credential...',
  tokenPrefix: '...'
}
```

Symptom (client): `FirebaseFunctionsException: INTERNAL` — gitlive's iOS error translator strips the underlying detail; you only see it server-side.

Cause: FCM has no APNs credentials to forward the push to Apple. The `.p8` either wasn't uploaded to **this** Firebase project (each project needs its own upload — uploading to a different Firebase project doesn't help), or the Key ID / Team ID is wrong, or the .p8 was created restricted to a different Bundle ID at Apple Developer Portal.

Fix:
1. [Firebase Console → schedule-4c151 → Project Settings → Cloud Messaging tab](https://console.firebase.google.com/project/schedule-4c151/settings/cloudmessaging)
2. Apple app configuration → APNs Authentication Key → Upload (or replace) the `.p8`
3. Paste **Key ID** (10 chars from https://developer.apple.com/account/resources/authkeys/list) and **Team ID** (10 chars from your Apple membership page)
4. If the .p8 was created with App ID restriction, edit at https://developer.apple.com/account/resources/authkeys/list → click the key → add `ru.dvfu.appliances` to allowed App IDs

The same `.p8` works for all apps on the same Apple team — but each Firebase project needs the upload step separately.

## Other gotchas worth remembering

### iOS-specific

- **Simulator can't receive APNs pushes.** APNs only delivers to real hardware. Test on a physical iPhone.
- **`aps-environment` entitlement must be present.** Pinned in `iosApp/project.yml` via `entitlements.properties` because Xcode wipes it from the file on every build when the Push Notifications capability isn't declared at the target level. Don't manually edit `iosApp/iosApp/iosApp.entitlements` — `xcodegen generate` regenerates it from `project.yml`.
- **`UIBackgroundModes` must include `remote-notification`.** In `Info.plist`. Without it, FCM/APNs background delivery is dropped.
- **`registerForRemoteNotifications()` must be called explicitly.** In `AppDelegate.didFinishLaunchingWithOptions`. Plus an explicit `didRegisterForRemoteNotificationsWithDeviceToken` handler that does `Messaging.messaging().apnsToken = deviceToken` — covers cases where Firebase swizzling silently doesn't pick up the registration.
- **iOS error path eats the server error message.** gitlive's iOS `FirebaseFunctionsException` translation discards the HttpsError message. Always check `firebase functions:log` for the real reason on iOS failures.

### Android-specific

- **`POST_NOTIFICATIONS` runtime permission required on T+.** Handled via moko-permissions in Settings.
- **Notification channels must exist** at the matching `channelId` before pushes can be displayed. Created in `Schedule.onCreate()` via `createNotificationChannels()`. The Cloud Function's `channelFor()` helper in `functions/src/index.ts` must map `notificationType` values to the same channel ids.
- **`GoogleApiManager: DEVELOPER_ERROR / Unknown calling package`** is usually noise unrelated to push delivery. We saw it during the debugging session but it didn't turn out to be a cause. If you do hit it as an actual blocker, register your debug build's SHA-1 and SHA-256 in Firebase Console → Settings → General → Your Android app → "SHA certificate fingerprints".

### Cross-platform

- **FCM tokens rotate.** Don't trust a cached token; re-upload to `users/<uid>.msgToken` on every Home screen entry. The wiring is in `HomeScreen.kt` via `LaunchedEffect(Unit) { usersRepository.uploadCurrentMessagingToken() }`.
- **One device active at a time.** We chose single-token (latest device wins) over multi-device. If you want both iOS and Android to receive simultaneously, you'd need to migrate `User.msgToken: String` → `msgTokens: List<String>` and update fanout to call `getMessaging().sendEachForMulticast({ tokens: [...], message: ... })`.

## Cheat sheet

```bash
# Deploy / redeploy
firebase deploy --only functions

# Watch logs while testing
firebase functions:log --only sendNotification | tail -20

# Find the actual FCM error code (server-side; client gets opaque INTERNAL)
firebase functions:log --only sendNotification | grep "FCM send failed"

# Re-check IAM if you suspect public-invoker got revoked
gcloud run services get-iam-policy sendnotification --region=asia-northeast1 --project=schedule-4c151

# Switch firebase CLI account
firebase login:add
firebase login:use <email>
firebase login:list

# Get debug build SHA fingerprints (for Firebase Console SHA registration if ever needed)
./gradlew :androidApp:signingReport 2>&1 | grep -E "^(Variant|SHA1|SHA-256):" | head -20
```

## Useful URLs

- Firebase Console — project: https://console.firebase.google.com/project/schedule-4c151
- Cloud Functions logs: https://console.cloud.google.com/functions/list?project=schedule-4c151
- Cloud Run services: https://console.cloud.google.com/run?project=schedule-4c151
- Cloud Logging (full text search): https://console.cloud.google.com/logs/query?project=schedule-4c151
- Apple Developer Keys: https://developer.apple.com/account/resources/authkeys/list
- Apple Developer Membership: https://developer.apple.com/account/#!/membership
