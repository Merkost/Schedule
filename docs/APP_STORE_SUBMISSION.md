# App Store submission — Schedule 1.1.4

Everything to paste into App Store Connect, with privacy nutrition + age
rating answers, in submission order.

---

## 0. Host privacy & support pages first

The two URLs Apple requires (Privacy Policy + Support) need to be reachable
on the public web. The repository already contains the pages at
`docs/privacy.html`, `docs/support.html`, `docs/index.html`.

To publish them via GitHub Pages:

1. Push `dev` (or whichever branch contains `docs/`) to `origin`.
2. GitHub → repository → **Settings → Pages**.
3. **Source**: deploy from branch. **Branch**: `dev`. **Folder**: `/docs`.
4. Wait ~1 min for the first deploy.

Resulting URLs (replace `Merkost/Schedule` if the repo moves again):

| Purpose                  | URL                                                           |
| ------------------------ | ------------------------------------------------------------- |
| Privacy Policy           | `https://merkost.github.io/Schedule/privacy.html`             |
| Support                  | `https://merkost.github.io/Schedule/support.html`             |
| (optional) Marketing URL | `https://merkost.github.io/Schedule/`                         |

---

## 1. App Information

| Field                              | Value                                          |
| ---------------------------------- | ---------------------------------------------- |
| **Name**                           | Приборы ДВФУ                                   |
| **Subtitle** (max 30 chars)        | Расписание приборов ДВФУ                       |
| **Bundle ID**                      | `ru.dvfu.appliances`                           |
| **SKU**                            | `schedule-fefu-001` (any unique slug)          |
| **Primary language**               | Russian                                        |
| **Primary category**               | Productivity                                   |
| **Secondary category** (optional)  | Utilities                                      |
| **Content rights**                 | Does not contain, show or access third-party content |

---

## 2. Pricing & Availability

| Field           | Value                                              |
| --------------- | -------------------------------------------------- |
| **Price**       | Free                                               |
| **Availability**| Russia (and any other countries with FEFU members) |

---

## 3. App Privacy

### 3.1 Privacy Policy URL

```
https://merkost.github.io/Schedule/privacy.html
```

### 3.2 Privacy Practices questionnaire (the "nutrition label")

Apple asks for each category whether you collect that data type. Answers
for Schedule are below.

> **Do you or your third-party partners use data to track users? — No.**
> The app does not use IDFA, no advertising, no cross-app analytics.

**Data Linked to the User** (collected and tied to the user's identity):

| Category         | Type                  | Purpose                  |
| ---------------- | --------------------- | ------------------------ |
| Contact Info     | Email Address         | App Functionality        |
| Contact Info     | Name                  | App Functionality        |
| User Content     | Other User Content    | App Functionality (booking comments) |
| Identifiers      | User ID               | App Functionality        |

**Data Not Linked to the User** (collected but anonymised):

| Category    | Type                  | Purpose                  |
| ----------- | --------------------- | ------------------------ |
| Diagnostics | Crash Data            | App Functionality        |
| Diagnostics | Performance Data      | App Functionality        |
| Diagnostics | Other Diagnostic Data | App Functionality        |
| Usage Data  | Product Interaction   | Analytics                |

**Data Not Collected**: everything else (Health, Financial Info, Location,
Sensitive Info, Contacts, Photos, Audio Data, Search History, Browsing
History, Purchases, Gameplay Content, Customer Support — none).

For every "Linked to user" / "Not linked" category Apple also asks:

- *Used for tracking?* → **No.**
- *Shared with third parties?* → **No** (Firebase / Google Cloud are
  sub-processors per the privacy policy, which Apple treats as
  service-provider data flow rather than third-party sharing).

---

## 4. Localised App Listing — Russian

### 4.1 Subtitle (max 30 chars)

```
Расписание приборов ДВФУ
```

### 4.2 Promotional Text (max 170 chars, editable without re-review)

```
Бронируйте научные приборы ДВФУ в пару касаний: гибкий календарь, push-уведомления о подтверждениях, отдельные роли для суперпользователей лабораторий.
```

### 4.3 Description (max 4000 chars)

```
Schedule — приложение для ведения расписания работы научного оборудования
Дальневосточного Федерального Университета (ДВФУ). Студенты, аспиранты и
сотрудники могут забронировать время на общих приборах, а суперпользователи
лабораторий — управлять заявками со своего телефона.

ВОЗМОЖНОСТИ
• Месячный календарь с подсветкой дат, где у вас есть подтверждённые брони
• Создание бронирований за пару касаний — выбор прибора, даты, времени, комментарий
• Список заявок: ожидающие подтверждения, мои текущие и прошедшие
• Подтверждение и отклонение заявок (для суперпользователей и администраторов)
• Push-уведомления о новых заявках, подтверждениях, изменениях времени
• Профиль с ролью пользователя, привязанными приборами и аватаром
• Гостевой режим для просмотра расписания без регистрации

КАК ЭТО РАБОТАЕТ
1. Войдите через Google или продолжите как гость.
2. Откройте «Календарь» и нажмите «+», чтобы создать заявку.
3. Менеджер прибора получит push-уведомление и подтвердит её одним касанием.
4. Вам придёт уведомление об изменении статуса.

КОНФИДЕНЦИАЛЬНОСТЬ
Мы не используем рекламные идентификаторы, не отслеживаем вас между
приложениями и не передаём данные брокерам данных. Подробности —
в политике конфиденциальности.

Контакт: konstantin.merenkov@kttipay.com
```

### 4.4 Keywords (max 100 chars, comma-separated, NO spaces after commas)

```
двфу,расписание,бронирование,университет,приборы,лаборатория,календарь,fefu,booking,schedule
```

(Exactly 92 characters. The space-after-comma version uses 100+, so omit
spaces — Apple parses commas as the only separator.)

### 4.5 Support URL

```
https://merkost.github.io/Schedule/support.html
```

### 4.6 Marketing URL (optional, can leave blank)

```
https://merkost.github.io/Schedule/
```

---

## 5. Age Rating Questionnaire

All answers **None** except:

| Question                                                                           | Answer |
| ---------------------------------------------------------------------------------- | ------ |
| Cartoon or Fantasy Violence                                                        | None   |
| Realistic Violence                                                                 | None   |
| Sexual Content or Nudity                                                           | None   |
| Profanity or Crude Humor                                                           | None   |
| Alcohol, Tobacco, or Drug Use                                                      | None   |
| Mature/Suggestive Themes                                                           | None   |
| Horror/Fear Themes                                                                 | None   |
| Medical/Treatment Information                                                      | None   |
| Gambling                                                                           | None   |
| Contests                                                                           | None   |
| Unrestricted Web Access                                                            | No     |
| Made for Kids                                                                      | No     |
| User-generated content                                                             | No (comments are private to a single booking and visible only to participants) |

Result: **4+**.

---

## 6. App Review Information

### 6.1 Contact

| Field         | Value                                |
| ------------- | ------------------------------------ |
| First Name    | Konstantin                           |
| Last Name     | Merenkov                             |
| Phone number  | (Konstantin to fill in)              |
| Email         | konstantin.merenkov@kttipay.com      |

### 6.2 Demo Account

Enter the dedicated App Review account in App Store Connect. Retrieve its
email and password from the secure local credential source; never store the
password in this repository. Confirm that the account has `role = 2` in
Firestore before submission. Reviewers can still use **"Continue as guest"**
for the basic read-only flow.

### 6.3 Notes

```
=== ENGLISH ===

Schedule is a scientific-equipment booking app for the Far Eastern Federal
University (FEFU). Students and staff reserve time slots on shared lab
appliances, and the appliance "super-users" (lab managers) approve or
decline incoming requests.

HOW TO TEST WITHOUT AN ACCOUNT
The fastest way to review the app is the "Continue as guest" button on
the login screen. No credentials required. As a guest you can:
- Browse the monthly calendar of all bookings
- Open any booking and see its details
- Open the appliance list and view each appliance + its super-users

To exercise the full administrator feature set, use the Demo Account
credentials supplied in App Store Connect:
1. On the login screen, tap the app logo 5 times within 2 seconds.
2. Enter the supplied email and password in the "QA sign-in" dialog.

Standard end-user sign-in is also available through "Continue with Apple"
and "Continue with Google". New accounts join with the Guest role by default.

Account deletion path for review: please test with a guest session or a
disposable standard account, not the shared admin demo account. Guest path:
Profile → Delete account. Standard account path: Profile → Account details →
Delete account.
Deletion removes the Firebase Auth account, Firestore profile when present,
push token, appliance membership references, and associated booking data.

WHAT TO EXERCISE
1. Calendar tab — tap any day to see its bookings; tap a booking to open
   details (Home screen).
2. Appliances tab — list of equipment; tap an item to see super-users.
3. Profile tab — your account info, account details, account deletion, and
   sign-out live here.
4. "+" floating button — create a new booking (signed-in users only).

PERMISSIONS
- Push notifications: only used to alert users when their booking status
  changes (approved / declined / time changed) or when a new booking is
  created on an appliance they manage. Notifications are opt-in and the
  app works fully without them.
- Camera / photo library: declared for a future avatar feature; never
  requested in the current build.

CONTACT
Konstantin Merenkov — konstantin.merenkov@kttipay.com

=== РУССКИЙ ===

Schedule — приложение для ведения расписания работы научного оборудования
в Дальневосточном Федеральном Университете (ДВФУ). Студенты и сотрудники
бронируют временные слоты на общих приборах, а суперпользователи приборов
(заведующие лабораториями) подтверждают или отклоняют заявки.

Возможности:
- Создание, редактирование и отмена бронирований
- Комментарии к бронированиям
- Просмотр приборов и их суперпользователей
- Подтверждение/отклонение бронирований (для суперпользователей)
- Push-уведомления о новых бронированиях и изменениях статуса

КАК ПРОТЕСТИРОВАТЬ БЕЗ АККАУНТА
Кнопка «Продолжить как гость» на экране входа — доступ без учётной записи.
Для проверки функций администратора используйте Demo Account из App Store
Connect: пять раз нажмите на логотип приложения в течение двух секунд и
введите выданные email и пароль в окне «QA sign-in». Обычный пользователь
также может войти через Apple или Google.

Konstantin Merenkov — konstantin.merenkov@kttipay.com
```

### 6.4 Attachment

Optional. Skip unless the reviewer asks.

---

## 7. Version Release

| Field                                  | Value                                            |
| -------------------------------------- | ------------------------------------------------ |
| **App Store version**                  | 1.1.4                                            |
| **iOS build**                          | 4                                                |
| **Android version**                    | 1.1.4 (`versionCode` 11)                         |
| **What's New in This Version**         | См. ниже                                         |
| **Manual release after approval**      | Recommended (so you can announce on your own schedule) |

**What's New — Russian:**

```
• Обновлены экраны профиля и настроек
• Добавлен вход через Apple на iOS
• Улучшены управление уведомлениями и диагностика push-уведомлений
• Улучшены удаление аккаунта, стабильность и безопасность
```

**What's New — English:**

```
• Redesigned Profile and Settings
• Added Sign in with Apple on iOS
• Improved notification controls and push diagnostics
• Improved account deletion, stability, and security
```

---

## 8. Screenshots

- **Required:** 6.9" iPhone display (1320×2868 px). Apple auto-scales for
  smaller iPhones.
- **iPad screenshots are not required:** `TARGETED_DEVICE_FAMILY` is `"1"`
  in `iosApp/project.yml`.
- 3–6 screenshots is the sweet spot. Recommended set:
  1. **Home (calendar)** with a day pinned and several events visible
  2. **Booking detail** for an approved or pending event
  3. **Appliances** list
  4. **Add Event** screen mid-flow
  5. *(optional)* Profile screen
  6. *(optional)* Settings screen showing notifications

Capture via Xcode simulator (`⌘+S`) on **iPhone 16 Pro Max** — auto-clean
status bar at 9:41.

---

## 9. Pre-submission checklist

- [ ] `dev` (or `master`) pushed with `docs/` content
- [ ] GitHub Pages enabled — Privacy + Support URLs return HTTP 200
- [ ] Dedicated reviewer credential rotated and stored outside the repository
- [ ] Demo Account credentials entered in App Store Connect and verified
- [ ] iOS build archived (Xcode → Product → Archive) and uploaded via Organizer
- [ ] In App Store Connect the new build (1.1.4 / 4) appears under the version
- [ ] All fields above filled
- [ ] 6.9" iPhone screenshots uploaded
- [ ] Age rating saved (= 4+)
- [ ] Privacy nutrition label saved
- [ ] Reviewer notes pasted into App Review Information → Notes
- [ ] Submit for Review
