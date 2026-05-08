# FCM v1 Migration via Cloudflare Workers

Status: **planned, not yet executed**.
Captured: 2026-05-08.

## Decisions

| Question | Choice |
|---|---|
| Runtime | **Cloudflare Workers** (free tier, no credit card) |
| Architecture | **Translation proxy** — Worker exposes a `POST /fcm/send` endpoint that accepts the same legacy shape the client already sends, translates it to FCM v1, and forwards |
| FCM auth | Service-account JWT exchange cached in Cloudflare KV (1-hour TTL) |
| Caller auth | Firebase ID token in `Authorization: Bearer …`, verified against Google JWKs |

## Why not Cloud Functions / Hosting / Cloud Run

- **Cloud Functions Gen 2** (`firebase-functions/v2/...`): requires Blaze plan even to deploy, even if you stay under the free tier. No card → can't deploy.
- **Cloud Functions Gen 1**: deprecated; new projects can't create them.
- **Cloud Run**: same — Blaze billing required.
- **Firebase Hosting**: static files only on Spark. Rewrites to a backend require either Functions or Cloud Run, both of which need billing. Hosting alone can't run dynamic code.
- **Cloudflare Workers**: 100K requests/day free, no card on file. Web Crypto API for RS256 JWT signing. KV for free token caching. Plenty of headroom for this app.

Trade-off vs the original Cloud Functions plan:
- ❌ No Firestore triggers — the client still calls the proxy after each notification-worthy mutation, same as today.
- ❌ Actor-impersonation risk unchanged (a compromised client can still ask the proxy to push to anyone's token).
- ✅ **FCM server key out of the APK/IPA** — the main goal. Service account lives in Worker secrets, never reaches the client.
- ✅ Free, no card, no billing dashboard to babysit.
- ✅ Smaller scope — `~3h` vs `~9h`.

If the app ever moves to Blaze, the trigger-based architecture from the original plan can replace this proxy with no client changes (the Worker can stay deployed as-is during the migration; just point the client at the Functions endpoint when ready).

## Architecture: stateless translation proxy

```
[App]  ── POST /fcm/send + Bearer <Firebase ID token> ──>  [Cloudflare Worker]
                                                              │
                                                              │ (cached for 55 min in KV)
                                                              ▼
                                                            OAuth2 access token
                                                              │
                                                              ▼
                  [FCM HTTP v1] ◄── POST /v1/projects/X/messages:send ──┘
```

The Worker:
1. Verifies the `Authorization: Bearer <ID token>` against Firebase Auth using Google's public JWKs (cached in KV for 1h).
2. Reads or refreshes the FCM v1 OAuth2 access token using a service account JWT (cached in KV, refresh just-before-expiry).
3. Translates the legacy-shape body the client sends:
   ```json
   { "to": "<fcmToken>",
     "notification": { "title": "…", "body": "…" },
     "data": { "notificationType": "MY_EVENT" } }
   ```
   into the v1 shape:
   ```json
   { "message": {
       "token": "<fcmToken>",
       "notification": { "title": "…", "body": "…" },
       "data": { "notificationType": "MY_EVENT" },
       "android": { "notification": { "channel_id": "schedule_my_events" } },
       "apns": { "payload": { "aps": { "sound": "default" } } }
   } }
   ```
4. Forwards to FCM v1, returns 204 / 4xx.

Client-side change: just `NotificationApi`'s base URL and auth header.

## File layout

```
Schedule/
├─ worker/                          # NEW — Cloudflare Worker
│  ├─ wrangler.toml
│  ├─ package.json
│  ├─ tsconfig.json
│  └─ src/
│     ├─ index.ts                    # request router (just /fcm/send)
│     ├─ auth.ts                     # verify Firebase ID token
│     ├─ fcm.ts                      # OAuth2 token mint + cache + v1 send
│     ├─ jwt.ts                      # RS256 sign helper (Web Crypto)
│     └─ jwks.ts                     # Google JWKs fetch + cache
└─ docs/2026-05-08-fcm-v1-migration.md
```

## Worker secrets / config (set via `wrangler secret put`)

| Name | Source | Purpose |
|---|---|---|
| `FIREBASE_PROJECT_ID` | Firebase project settings | URL path + JWT audience |
| `SERVICE_ACCOUNT_EMAIL` | `client_email` from the service-account JSON | JWT issuer/subject |
| `SERVICE_ACCOUNT_PRIVATE_KEY` | `private_key` from the service-account JSON | JWT signing |

KV namespace bound as `CACHE` for:
- `fcm_token` — cached OAuth2 token (TTL 55 min)
- `google_jwks` — cached signing keys for Firebase ID-token verification (TTL 1h)

Generate the service account JSON from Firebase Console → Project Settings → Service accounts → "Generate new private key". Paste the email + key into Worker secrets. **Don't commit the JSON.**

## Worker sketch (TypeScript)

```ts
// worker/src/index.ts
import { verifyFirebaseIdToken } from "./auth"
import { sendV1 } from "./fcm"

export interface Env {
  CACHE: KVNamespace
  FIREBASE_PROJECT_ID: string
  SERVICE_ACCOUNT_EMAIL: string
  SERVICE_ACCOUNT_PRIVATE_KEY: string
}

export default {
  async fetch(req: Request, env: Env): Promise<Response> {
    if (req.method !== "POST" || new URL(req.url).pathname !== "/fcm/send") {
      return new Response("not found", { status: 404 })
    }
    const auth = req.headers.get("authorization") ?? ""
    if (!auth.startsWith("Bearer ")) return new Response("unauthorized", { status: 401 })
    const idToken = auth.slice("Bearer ".length)

    try {
      await verifyFirebaseIdToken(idToken, env)
    } catch (e) {
      return new Response(`invalid token: ${e}`, { status: 401 })
    }

    const legacy = await req.json() as {
      to: string
      notification: { title: string; body: string }
      data?: Record<string, string>
    }
    if (!legacy.to) return new Response("missing 'to'", { status: 400 })

    const message = {
      token: legacy.to,
      notification: legacy.notification,
      data: legacy.data ?? {},
      android: { notification: { channel_id: channelFor(legacy.data?.notificationType) } },
      apns: { payload: { aps: { sound: "default" } } },
    }
    await sendV1(message, env)
    return new Response(null, { status: 204 })
  },
} satisfies ExportedHandler<Env>

function channelFor(type: string | undefined): string {
  switch (type) {
    case "APPLIANCE": return "schedule_appliances"
    case "EVENT":     return "schedule_events"
    case "MY_EVENT":  return "schedule_my_events"
    case "NEW_EVENT": return "schedule_new_events"
    default:          return "schedule_default"
  }
}
```

`auth.ts`, `fcm.ts`, `jwt.ts`, `jwks.ts` are ~300 LOC total. Standard "verify ID token + mint OAuth2 token" boilerplate; nothing project-specific.

## Client changes

Tiny — just point `NotificationApi` at the Worker and attach the user's ID token:

```kotlin
class NotificationApi(
    private val client: HttpClient,
    private val baseUrl: String,                // e.g. "https://schedule-fcm.your-account.workers.dev"
) {
    suspend fun postNotification(payload: PushNotification) {
        val token = Firebase.auth.currentUser?.getIdToken(false) ?: return
        client.post("$baseUrl/fcm/send") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
    }
}
```

Wire-up changes:
1. Drop `FCM_SERVER_KEY` from `local.properties`, `AppBuildConfig`, both `PlatformModule.kt` named bindings.
2. Add a `WORKER_BASE_URL` constant (or reuse `AppBuildConfig` mechanism if you want a debug-vs-prod split).
3. Constructor of `NotificationApi` takes `baseUrl` instead of `fcmServerKey`. Update `NetworkModule` accordingly.
4. Everything else (the 8 send paths inside `NotificationManagerImpl`) stays exactly the same — same payload shape.

Net diff: a few lines.

## Local dev

```bash
cd worker
npm install
# put secrets locally for `wrangler dev`
echo '<service-account-email>' | wrangler secret put SERVICE_ACCOUNT_EMAIL
echo '<service-account-private-key>' | wrangler secret put SERVICE_ACCOUNT_PRIVATE_KEY
wrangler secret put FIREBASE_PROJECT_ID
# optional: bind to a local KV namespace for dev
wrangler dev
```

The app points at `http://localhost:8787` in debug builds, the deployed `*.workers.dev` URL in release builds.

`wrangler deploy` ships it. Logs via `wrangler tail` or the Cloudflare dashboard.

## Migration sequence

1. **Set up Cloudflare account + Worker scaffold.** `npm create cloudflare@latest worker`, choose Hello World TypeScript. Add KV binding. Verify `wrangler deploy` works. ~30m.
2. **Implement the proxy.** Auth verification, OAuth2 mint, payload translation, KV caching. ~1.5h.
3. **Switch the client.** Drop `FCM_SERVER_KEY`, change `NotificationApi` to use the Worker URL + ID token. ~30m. **Legacy FCM gone.**
4. **Smoke-test all 8 notification paths** end-to-end on Android + iOS. ~30m.
5. **Drop `local.properties` `FCM_SERVER_KEY` + `AppBuildConfig.FCM_SERVER_KEY`** from the repo. ~5m.

**Total: ~3 hours.** Single session.

## Open items at execution time

1. **Service-account JSON download** — needs to happen once via Firebase Console.
2. **Worker URL** — pick a subdomain (`schedule-fcm.<account>.workers.dev` or a custom domain if you have one).
3. **Rate limiting** — Worker free tier is 100K req/day. If misused (e.g. compromised user spamming), you'll burn through it. Add a simple per-uid rate limit (KV-backed, e.g. 100 pushes/min/uid). ~30m extra.
4. **Russian channel/message localization** — already happens client-side in `formattedX(...)` helpers; nothing to change.
5. **Failure handling** — Worker should pass FCM error responses through so the client can react (e.g. invalid token → drop it from `users/{uid}.msgToken`). The current `NotificationApi` ignores response status; consider improving while we're here.
6. **Cloudflare Workers' free tier limits** — 100K req/day, 10ms CPU/req, 128 MB RAM. JWT signing + token verify takes ~5ms; well under. KV reads cached, so most requests skip the OAuth round-trip entirely.

## What this does NOT solve

These were nice-to-haves in the original Cloud Functions plan but are unreachable without billing:

- **Firestore triggers** — server can't react to data changes. Client still drives fanout.
- **Actor-impersonation hardening** — Worker only knows "the caller is signed in", not "the caller is allowed to push to this token". A compromised client can still spam any user's device. Same threat model as today.
- **Server-side state** for things like "drop FCM tokens that return UNREGISTERED". Possible in the Worker but more work; deferred.

If/when the app moves to Blaze (or another billed runtime), the trigger architecture from the original plan can replace this proxy with no client changes — the Worker becomes a fallback or gets retired entirely.

## Effort summary

| Step | Time | Outcome |
|---|---|---|
| 1 | 0.5h | Worker deployed, "hello world" returns 200 |
| 2 | 1.5h | Proxy validates ID token, mints OAuth2, forwards to v1 |
| 3 | 0.5h | Client points at Worker, FCM key gone from repo |
| 4 | 0.5h | All 8 paths smoke-tested |
| 5 | 0.1h | Cleanup |
| **Total** | **~3h** | FCM key off the device, $0 forever, no card on file |
