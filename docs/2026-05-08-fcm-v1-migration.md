# FCM v1 Migration via Firebase Cloud Functions

Status: **executing now**.
Captured: 2026-05-08.
Trigger: Google retired the legacy `/fcm/send` endpoint for this project — it returns `404` as of today.

## Decisions

| Question | Choice |
|---|---|
| Runtime | **Firebase Cloud Functions** (Gen 2, on Blaze plan with $0 expected spend) |
| Architecture **for this session** | **Callable mirror** — the existing `NotificationApi.postNotification(payload)` becomes a `Firebase.functions.httpsCallable("sendNotification")` invocation. Same payload shape, same fanout logic on the client. |
| Architecture **future** | The trigger-based architecture (originally drafted in `docs/2026-05-08-cloud-functions-migration.md`, now archived) can replace this incrementally with no client churn. Defer. |
| Region | `asia-northeast1` (Tokyo) — closer to Vladivostok user base |
| Auth on the callable | Firebase Auth — `request.auth` must be present; rejects unauthenticated calls. |

## Why callable mirror (not triggers) right now

- Pushes are **down**. Priority is restoring them, not refactoring.
- Callable mirror is a near-zero-risk drop-in: the body shape doesn't change, so there's nothing for the existing 8 sender call sites to learn.
- Triggers can be migrated later, one at a time. Each migration deletes a callable invocation from the client and adds a Firestore trigger on the server. The two architectures coexist while we transition.

## What you need to do

1. **Enable Blaze on the Firebase project.**
   Firebase Console → ⚙️ Project Settings → Usage and Billing → Modify plan → Blaze (Pay-as-you-go) → enter card details. The free tier is **2M function invocations/month, 360K GB-seconds, 5GB egress** — easily 100x more than this app uses. Set a **budget alert at $1/month** in the same dialog so you'll get an email if anything ever costs anything.

2. **Install firebase-tools and log in:**
   ```bash
   npm install -g firebase-tools
   firebase login
   ```

3. Tell me your **Firebase project ID** (Console → Project Settings → General → Project ID, looks like `schedule-xxxxx`). I'll plug it into `.firebaserc`.

That's it from your side until deploy time.

## What I'll do (in order)

1. **Scaffold the `functions/` subproject** — `firebase init functions` equivalent done by hand: `package.json`, `tsconfig.json`, `.eslintrc.js`, `src/index.ts`. TypeScript, Node 20.
2. **Implement two callables**:
   - `sendNotification({ to, notification, data })` — accepts the existing `PushNotification` shape, translates to FCM v1, sends via Admin SDK. Used by the 7 fanout flows.
   - `sendTestNotification()` — uses `request.auth.uid` → reads `users/<uid>.msgToken` → sends. Used by the Settings test button.
3. **Switch the client**:
   - Add `dev.gitlive:firebase-functions` to commonMain.
   - Bind `Firebase.functions("asia-northeast1")` in DI.
   - Rewrite `NotificationApi.postNotification` to call the `sendNotification` callable.
   - Rewrite `NotificationManagerImpl.sendTestNotificationToCurrentDevice` to call the `sendTestNotification` callable.
   - Delete `FCM_SERVER_KEY` from `local.properties`, `AppBuildConfig`, both `PlatformModule.kt` named bindings.
4. **Deploy** — `firebase deploy --only functions`. You run this since it requires `firebase login` on your machine, but I'll give you the exact command.
5. **Smoke test** the Settings → Send test notification flow on Android (real device or emulator) and iOS (real device).

## Cost honesty check

For this app's traffic — let's say worst case 1000 daily active users, each generates 5 push events / day = 5K invocations/day = 150K/month. Free tier is 2M. You'd use **7.5% of the free quota**. Charges only kick in if usage 13x's. Realistic monthly cost: **$0.00**.

If you ever exceed the free tier, the next $1 buys you another ~500K invocations. The pricing model is generous.

## Effort

| Step | Time | Done by |
|---|---|---|
| 1. Enable Blaze + budget alert | 5m | you |
| 2. firebase login on your machine | 2m | you |
| 3. Scaffold functions/ | 30m | me |
| 4. Implement two callables | 1h | me |
| 5. Switch client | 30m | me |
| 6. `firebase deploy` | 5m | you (then me to verify) |
| 7. Smoke test | 30m | you (run the app) |
| **Total** | **~3h** | mostly me |
