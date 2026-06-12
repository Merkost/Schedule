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
