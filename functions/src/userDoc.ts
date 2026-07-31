const ROLE_GUEST = 0

export interface UserRecordLike {
  uid: string
  email?: string
  displayName?: string
  photoURL?: string
  providerData: unknown[]
}

/**
 * Mirrors the Kotlin User entity. role defaults to GUEST; an admin elevates it
 * afterwards in Firestore. Returns null for anonymous (guest) accounts, which
 * the app tracks in its local datastore and must not get a users/ doc.
 */
export function buildUserDoc(u: UserRecordLike): Record<string, unknown> | null {
  const isAnonymous = (u.providerData?.length ?? 0) === 0
  if (isAnonymous) return null
  return {
    userId: u.uid,
    msgToken: "",
    userName: u.displayName ?? "",
    email: u.email ?? "",
    birthday: 0,
    role: ROLE_GUEST,
    anonymous: false,
    userPic: u.photoURL ?? "",
  }
}
