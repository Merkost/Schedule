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
