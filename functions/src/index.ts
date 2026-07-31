import { onCall, HttpsError } from "firebase-functions/v2/https"
import { setGlobalOptions } from "firebase-functions/v2"
import { user as authUser } from "firebase-functions/v1/auth"
import { initializeApp } from "firebase-admin/app"
import { getFirestore } from "firebase-admin/firestore"
import { getMessaging, Message } from "firebase-admin/messaging"
import { createFirestoreAccountDeletionStore, deleteAccountData } from "./accountDeletion"
import { buildUserDoc, UserRecordLike } from "./userDoc"

initializeApp()
setGlobalOptions({ region: "asia-northeast1" })

/**
 * Server-side guarantee that every non-anonymous auth user has a users/{uid}
 * document. Fires on user creation for every provider (Google, Apple, email)
 * regardless of platform, app state, or client build. Idempotent: skips if the
 * doc already exists, so it never clobbers an admin-elevated role.
 */
export const createUserDocument = authUser().onCreate(async (u) => {
  const data = buildUserDoc(u as UserRecordLike)
  if (!data) return
  const ref = getFirestore().collection("users").doc(u.uid)
  if ((await ref.get()).exists) return
  await ref.set(data)
  console.log("createUserDocument wrote users/" + u.uid)
})

/**
 * Maps the legacy NotificationType enum (sent in payload.data.notificationType)
 * to the Android channel ids registered in Schedule.kt.
 */
function channelFor(type: string | undefined): string {
  switch (type) {
    case "APPLIANCE": return "channel_appliance"
    case "EVENT":     return "channel_event"
    case "NEW_EVENT": return "channel_new_event"
    case "MY_EVENT":  return "channel_my_event"
    default:          return "com.dvfu.appliances"
  }
}

interface LegacyPayload {
  to?: string
  notification?: { title?: string; body?: string }
  data?: { notificationType?: string }
}

function buildMessage(payload: LegacyPayload, token: string): Message {
  const type = payload.data?.notificationType
  return {
    token,
    notification: {
      title: payload.notification?.title ?? "",
      body: payload.notification?.body ?? "",
    },
    data: { notificationType: type ?? "DEFAULT" },
    android: {
      priority: "high",
      notification: {
        channelId: channelFor(type),
        defaultSound: true,
      },
    },
    apns: {
      payload: {
        aps: { sound: "default", contentAvailable: true },
      },
      headers: {
        "apns-priority": "10",
      },
    },
  }
}

/**
 * Generic mirror of the old NotificationApi.postNotification(...).
 * Accepts the same legacy body shape the client already builds.
 *
 * Auth: any signed-in user. Same threat model as today; tighten via
 * Firestore triggers + Security Rules in a future migration step.
 */
export const sendNotification = onCall(async (req) => {
  if (!req.auth) throw new HttpsError("unauthenticated", "sign in first")
  const payload = (req.data ?? {}) as LegacyPayload
  const token = payload.to
  if (!token) throw new HttpsError("invalid-argument", "missing 'to' (FCM token)")

  try {
    const messageId = await getMessaging().send(buildMessage(payload, token))
    return { ok: true, messageId }
  } catch (e: any) {
    const code = e?.errorInfo?.code ?? e?.code ?? "unknown"
    const msg = e?.errorInfo?.message ?? e?.message ?? String(e)
    console.error("FCM send failed:", { code, msg, tokenPrefix: token.slice(0, 12) })
    if (code === "messaging/registration-token-not-registered" ||
        code === "messaging/invalid-registration-token") {
      throw new HttpsError("failed-precondition", `dead token: ${code}`)
    }
    throw new HttpsError("internal", `send failed: ${code}: ${msg}`)
  }
})

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
