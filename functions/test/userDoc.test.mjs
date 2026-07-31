import { test } from "node:test"
import assert from "node:assert/strict"
import { buildUserDoc } from "../lib/userDoc.js"

const providerUser = {
  uid: "uid-1",
  email: "a@b.c",
  displayName: "New User",
  photoURL: "http://pic",
  providerData: [{ providerId: "apple.com" }],
}

test("maps a provider user to the User entity with GUEST role", () => {
  assert.deepEqual(buildUserDoc(providerUser), {
    userId: "uid-1",
    msgToken: "",
    userName: "New User",
    email: "a@b.c",
    birthday: 0,
    role: 0,
    anonymous: false,
    userPic: "http://pic",
  })
})

test("returns null for anonymous users (no provider data)", () => {
  assert.equal(buildUserDoc({ uid: "anon", providerData: [] }), null)
})

test("defaults missing name/email/photo to empty strings", () => {
  const doc = buildUserDoc({ uid: "uid-2", providerData: [{ providerId: "google.com" }] })
  assert.equal(doc.userName, "")
  assert.equal(doc.email, "")
  assert.equal(doc.userPic, "")
})
