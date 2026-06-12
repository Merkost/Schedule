# App Review — sign-in instructions

## Short version (paste into App Review notes)

```
To access all features, sign in as admin:
1. On the login screen, tap the app logo 5 times quickly.
2. Enter  Email: appstore.review@dvfu.ru   Password: appstore.review.dvfu
3. Tap "Sign in".

Account deletion: please test with a guest session or disposable standard
account, not the shared admin demo account. Guest path: Profile → Delete
account. Standard account path: Profile → Account details → Delete account.
```

---

Paste this into **App Store Connect → App Review Information → Notes**, and put
the email/password in the **Sign-In Information** fields of the same section.

---

## Two ways to sign in

### 1. Standard sign-in (normal end user)

On the login screen, tap **“Continue with Apple”** and use your own Apple ID.
This is the regular user experience. It creates a standard (non-admin) account.

### 2. Full-feature demo account (administrator access)

To review all features, including the admin-only screens, use the built-in QA
sign-in:

1. Launch the app. You are on the **login screen** (app logo + “Sign in to
   continue”).
2. **Tap the app logo (the calendar icon in the centre) 5 times quickly** — all
   5 taps within about 2 seconds.
3. A **“QA sign-in”** dialog appears with Email and Password fields.
4. Enter:
   - **Email:** `appstore.review@dvfu.ru`
   - **Password:** `appstore.review.dvfu`
5. Tap **“Sign in”**.

You are signed in as an administrator. All admin-only screens, actions, and
management features are now accessible (Calendar, Appliances, Profile, and the
admin/management options).

---

## Account Deletion

Please verify account deletion with a guest session or a disposable standard
Apple/Google account, not the shared admin demo account.

For a standard account, use:

Profile → Account details → Delete account

For guest sessions, use:

Profile → Delete account

The delete action is shown only on the signed-in user's own details screen, or
directly in Profile for anonymous guests because guest sessions do not have a
Firestore profile document. It removes the Firebase Auth account, Firestore
profile when present, push token, appliance membership references, and
associated booking data.

---

## Notes

- The 5-tap gesture is intentionally hidden from normal users; it exists only so
  reviewers can reach the full-access demo account without a visible back door.
- If the QA dialog does not appear, make sure all 5 taps land on the logo within
  ~2 seconds, then try again.
- This account is a dedicated reviewer-only account; its password will be
  rotated after approval.
