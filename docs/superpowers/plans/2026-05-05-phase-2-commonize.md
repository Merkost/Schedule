# Phase 2 — Common-ize — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Prerequisite:** Plan 1 (Phase 0+1) executed; `composeApp/` and `androidApp/` exist; Android build green. All ~140 Kotlin files live in `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/`.

**Goal:** Move ~62 Kotlin files to `composeApp/src/commonMain/kotlin/`, swap four library artifacts to KMP variants, migrate all `java.time.*` usages to `kotlinx.datetime`, replace Android string/drawable resources with Compose Resources (M-R), and convert Parcelable nav-arg data classes to `@Serializable` typed routes — leaving the Android build green throughout.

**Architecture:** All pure-Kotlin domain types, use-cases, ViewModels, and pure-Compose UI components move to `commonMain`. Files with `android.*`, `com.google.firebase.*`, `android.content.Context`, or `retrofit2.*` imports remain in `androidMain` untouched this phase. The kotlinx.datetime library replaces `java.time` in commonMain-bound files. Compose Resources (M-R) replaces `R.string.*` and `R.drawable.*` callsites using `org.jetbrains.compose.resources.stringResource(Res.string.*)`.

**Tech Stack:** Kotlin 2.3.20, Compose Multiplatform 1.9.x, kotlinx-datetime 0.6.x, kotlinx-serialization 1.8.x, Compose Resources (M-R, bundled with CMP 1.9), JetBrains Navigation Compose KMP, JetBrains lifecycle-viewmodel-compose KMP.

---

## kotlinx-datetime mapping table

| java.time class / API | kotlinx.datetime equivalent | Notes |
|---|---|---|
| `java.time.LocalDate` | `kotlinx.datetime.LocalDate` | Same fields. `LocalDate.now()` → `Clock.System.todayIn(TimeZone.currentSystemDefault())` |
| `java.time.LocalDateTime` | `kotlinx.datetime.LocalDateTime` | Same fields. `LocalDateTime.now()` → `Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())` |
| `java.time.LocalTime` | `kotlinx.datetime.LocalTime` | `LocalTime.now()` → `Clock.System.now().toLocalDateTime(...).time`. `LocalTime.MIN` → `LocalTime(0,0,0,0)`. `LocalTime.MAX` → `LocalTime(23,59,59,999_999_999)` |
| `java.time.Instant` | `kotlinx.datetime.Instant` | `Instant.ofEpochMilli(ms)` → `Instant.fromEpochMilliseconds(ms)`. `instant.toEpochMilli()` → `instant.toEpochMilliseconds()` |
| `java.time.ZoneId` | `kotlinx.datetime.TimeZone` | `ZoneId.of("Asia/Vladivostok")` → `TimeZone.of("Asia/Vladivostok")`. `ZoneId.systemDefault()` → `TimeZone.currentSystemDefault()` |
| `java.time.ZonedDateTime` | `Instant` + `TimeZone` | No direct equivalent; use `instant.toLocalDateTime(tz)` and `localDateTime.toInstant(tz)` |
| `java.time.Duration` | `kotlin.time.Duration` | `Duration.ofMinutes(30)` → `30.minutes`. `Duration.ofHours(1)` → `1.hours` |
| `ChronoUnit.MINUTES.between(a, b)` (LocalDateTime) | `(b.toInstant(tz) - a.toInstant(tz)).inWholeMinutes` | Needs TimeZone for LocalDateTime |
| `ChronoUnit.DAYS.between(a, b)` (LocalDate) | `a.until(b, DateTimeUnit.DAY)` | Returns `Int` |
| `java.time.format.DateTimeFormatter` | No equivalent | Use Format DSL or write explicit lambdas |
| `localDate.plusDays(n)` | `localDate.plus(n, DateTimeUnit.DAY)` | |
| `localDate.atStartOfDay()` | `localDate.atTime(0, 0)` | Returns LocalDateTime |
| `localDateTime.toLocalDate()` | `localDateTime.date` | Property, not method |
| `localDateTime.toLocalTime()` | `localDateTime.time` | Property, not method |
| `localDateTime.isAfter(other)` | `localDateTime > other` | Comparison operators native |
| `localTime.truncatedTo(ChronoUnit.HOURS)` | `LocalTime(localTime.hour, 0, 0, 0)` | Manual |
| `Instant.atZone(zone).toLocalDateTime()` | `instant.toLocalDateTime(zone)` | |
| `LocalDate.of(y, m, d)` | `LocalDate(y, m, d)` | Constructor |
| `LocalTime.of(h, m)` | `LocalTime(h, m)` | Constructor |
| `LocalDateTime.of(date, time)` | `LocalDateTime(date, time)` | Constructor |

**DateTimeFormatter replacement pattern:** `DateTimeFormatter.ofPattern("H:mm")` used as `time.format(formatter)` → replace with explicit helper lambda. Update every callsite from `localTime.format(EventTimeFormatter)` to `EventTimeFormatter(localTime)`:

```kotlin
val EventTimeFormatter: (LocalTime) -> String = {
    "${it.hour}:${it.minute.toString().padStart(2, '0')}"
}
val HourFormatter: (LocalTime) -> String = { it.hour.toString() }
val DayFormatter: (LocalDate) -> String = {
    val dow = it.dayOfWeek.name.take(2).lowercase().replaceFirstChar { c -> c.uppercase() }
    val mon = it.month.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() }
    "$dow, $mon ${it.dayOfMonth}"
}
```

---

## Resources migration — full example

**Step 1 — Source (Android, stays in place):** `app/src/main/res/values/strings.xml`
```xml
<string name="schedule">Расписание</string>
```

**Step 2 — Destination (M-R, new file):** `composeApp/src/commonMain/composeResources/values/strings.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="schedule">Расписание</string>
</resources>
```

**Step 3 — Russian override:** `composeApp/src/commonMain/composeResources/values-ru/strings.xml` (same format).

**Step 4 — Callsite swap:**
```kotlin
// Before (androidMain)
import androidx.compose.ui.res.stringResource
import ru.dvfu.appliances.R
Text(stringResource(R.string.schedule))

// After (commonMain)
import org.jetbrains.compose.resources.stringResource
import ru.dvfu.appliances.generated.resources.Res
import ru.dvfu.appliances.generated.resources.schedule
Text(stringResource(Res.string.schedule))
```

---

## Files that MUST remain in androidMain this phase

Do not move. These depend on Firebase, Android Context, Activity, or Retrofit and are addressed in later phases.

- `compose/ui/theme/Theme.kt` — Activity, WindowCompat, android.graphics.Color
- `compose/ScheduleAppStateHolder.kt` — android.content.res.Resources, bundleOf, NavController Parcelable helpers
- `compose/home/HomeScreen.kt` — `(context as MainActivity).finishAffinity()`, LocalContext, showToast
- `compose/MainActivity.kt`, `application/Schedule.kt`
- `model/utils/Utils.kt`, `model/utils/RepositoryCollections.kt`
- `model/datastore/UserPreferences.kt`
- All `model/datasource/*Impl.kt` — Firebase
- `model/repository/entity/notifications/RetrofitInstance.kt`, `NotificationApi.kt`
- `di/KoinModules.kt`
- `MyFirebaseMessagingService.kt`
- `ui/LoginActivity.kt`, `LoginScreen.kt`, `SplashScreen.kt`
- `Logger.kt`
- `compose/use_cases/GetEventTimeAvailabilityUseCase.kt`
- `compose/viewmodels/LoginViewModel.kt`
- `model/FirebaseMessagingViewModel.kt`
- `compose/utils/NotificationManagerImpl.kt`

---

## Phase 2 Tasks

### Group A — Build config: swap library artifacts to KMP variants

#### ⚙️ A1 — Add KMP-ready library versions to `gradle/libs.versions.toml`

- [ ] Under `[versions]`, add:
  ```toml
  kotlinxDatetime = "0.6.1"
  kotlinxSerializationJson = "1.8.1"
  cmpPlugin = "1.9.0"
  jetbrainsNavigation = "2.9.0-alpha15"
  jetbrainsLifecycle = "2.9.0-alpha15"
  constraintlayoutMultiplatform = "0.5.1"
  koinCore = "4.2.1"
  koinCompose = "4.2.1"
  ```
  Verify `org.jetbrains.androidx.navigation:navigation-compose` at `2.9.0-alpha15` is published. If not, use the latest published version.

- [ ] Under `[libraries]`, add:
  ```toml
  kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinxDatetime" }
  kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }
  jetbrains-navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "jetbrainsNavigation" }
  jetbrains-lifecycle-viewmodel-compose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "jetbrainsLifecycle" }
  jetbrains-lifecycle-viewmodel = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel", version.ref = "jetbrainsLifecycle" }
  constraintlayout-compose-multiplatform = { module = "tech.annexflow.compose:constraintlayout-compose-multiplatform", version.ref = "constraintlayoutMultiplatform" }
  koin-core = { module = "io.insert-koin:koin-core", version.ref = "koinCore" }
  koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koinCompose" }
  ```

- [ ] Under `[plugins]`, add:
  ```toml
  kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
  kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
  jetbrains-compose = { id = "org.jetbrains.compose", version.ref = "cmpPlugin" }
  ```

- [ ] Run `./gradlew help` — must succeed (validates TOML syntax).
- [ ] **Commit:** `⚙️ build: add KMP library versions and artifacts to libs.versions.toml`

#### ⚙️ A2 — Update `composeApp/build.gradle.kts` for KMP variants

- [ ] Apply `kotlin-serialization` plugin in plugins block.
- [ ] In `kotlin { sourceSets { commonMain.dependencies { } } }`, add:
  ```kotlin
  implementation(libs.kotlinx.datetime)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.jetbrains.navigation.compose)
  implementation(libs.jetbrains.lifecycle.viewmodel.compose)
  implementation(libs.jetbrains.lifecycle.viewmodel)
  implementation(libs.constraintlayout.compose.multiplatform)
  implementation(libs.koin.core)
  implementation(libs.koin.compose)
  ```
- [ ] Ensure `libs.kotlinx.coroutines.core` is in `commonMain.dependencies`.
- [ ] In `androidMain.dependencies`, keep:
  ```kotlin
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.play.services)
  implementation(libs.koin.android)
  implementation(libs.koin.androidx.compose)
  ```
- [ ] Remove from `androidMain.dependencies` (replaced by KMP variants now in commonMain):
  - `libs.androidx.navigation.compose`
  - `libs.androidx.lifecycle.viewmodel.compose`
  - `libs.androidx.constraintlayout.compose`
- [ ] Enable Compose Resources generation in `compose {}`:
  ```kotlin
  resources {
      generateResClass = always
  }
  ```
- [ ] Run `./gradlew :composeApp:assembleDebug` — green.
- [ ] **Commit:** `⚙️ build: wire KMP library variants into composeApp commonMain/androidMain`

---

### Group B — String and Drawable Resources Migration

#### 🔧 B1 — Create composeResources directory and migrate strings

- [ ] Create `composeApp/src/commonMain/composeResources/values/strings.xml`. Copy all entries from `app/src/main/res/values/strings.xml`. XML format identical.
- [ ] Create `composeApp/src/commonMain/composeResources/values-ru/strings.xml`. Copy from `app/src/main/res/values-ru/strings.xml`.
- [ ] Run `./gradlew :composeApp:generateCommonMainResClass`. Confirm `Res.kt` contains `Res.string.schedule`, etc.
- [ ] Run `./gradlew :composeApp:assembleDebug` — green.
- [ ] **Commit:** `🔧 resources: add composeResources/values/strings.xml (en + ru)`

#### 🔧 B2 — Migrate Compose-used drawables

- [ ] Copy `app/src/main/res/drawable/ic_color_off_24dp.xml` → `composeApp/src/commonMain/composeResources/drawable/ic_color_off_24dp.xml` (used by `ColorPicker.kt`).
- [ ] Copy `app/src/main/res/drawable/ic_google.xml` → `composeApp/src/commonMain/composeResources/drawable/ic_google.xml` (used by sign-in screen, pre-stages for Phase 4).
- [ ] Mipmap launcher icons stay in `androidApp/src/main/res/mipmap-*/` (Android system icons).
- [ ] Run `./gradlew :composeApp:assembleDebug` — green.
- [ ] **Commit:** `🔧 resources: migrate Compose-used drawables to composeResources/drawable/`

---

### Group C — Refactor StringOperation and SnackbarManager

#### 🔧 C1 — Replace `@StringRes Int` with `StringResource` in StringOperation

- [ ] Update `model/utils/StringOperation.kt`:
  ```kotlin
  package ru.dvfu.appliances.model.utils

  import org.jetbrains.compose.resources.StringResource

  interface StringOperation {
      val stringRes: StringResource
  }
  ```

- [ ] Update `compose/calendars/CalendarType.kt`:
  ```kotlin
  import org.jetbrains.compose.resources.StringResource
  import ru.dvfu.appliances.generated.resources.Res
  import ru.dvfu.appliances.generated.resources.week
  import ru.dvfu.appliances.generated.resources.month

  enum class CalendarType(override val stringRes: StringResource, val icon: ImageVector) : StringOperation {
      WEEK(Res.string.week, Icons.Default.DateRange),
      MONTH(Res.string.month, Icons.Default.CalendarViewMonth),
  }
  ```

- [ ] Update `model/repository/entity/Roles.kt` similarly with `Res.string.guest`, `Res.string.user`, etc.

- [ ] Update `model/repository/entity/Event.kt` `BookingStatus`:
  ```kotlin
  enum class BookingStatus(override val stringRes: StringResource, val color: Color, val icon: ImageVector) : StringOperation {
      NONE(Res.string.new_books, Color.Unspecified, Icons.Default.HourglassBottom),
      APPROVED(Res.string.approved_books, Green500, Icons.Default.CheckCircle),
      DECLINED(Res.string.declined_books, Red500, Icons.Default.Cancel),
      ;
  }
  ```

- [ ] Update `compose/components/ItemsSelection.kt` import: `androidx.compose.ui.res.stringResource` → `org.jetbrains.compose.resources.stringResource`.
- [ ] Run `./gradlew :composeApp:assembleDebug` — green.
- [ ] **Commit:** `🔧 refactor: StringOperation.stringRes Int → M-R StringResource in CalendarType, Roles, BookingStatus`

#### 🔧 C2 — Refactor SnackbarManager for commonMain

- [ ] Rewrite `application/SnackbarManager.kt`:
  ```kotlin
  package ru.dvfu.appliances.application

  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow
  import kotlinx.coroutines.flow.update
  import org.jetbrains.compose.resources.StringResource

  data class Message(val id: Long, val messageId: StringResource)

  object SnackbarManager {

      private val _messages = MutableStateFlow<List<Message>>(emptyList())
      val messages: StateFlow<List<Message>> get() = _messages.asStateFlow()

      fun showMessage(messageId: StringResource) {
          _messages.update { currentMessages ->
              currentMessages + Message(
                  id = currentMessages.hashCode().toLong() xor System.currentTimeMillis(),
                  messageId = messageId,
              )
          }
      }

      fun setMessageShown(messageId: Long) {
          _messages.update { currentMessages ->
              currentMessages.filterNot { it.id == messageId }
          }
      }
  }
  ```
  Replace `UUID.randomUUID().mostSignificantBits` (Java-only) with the `xor` expression. Or use `kotlin.uuid.Uuid` if available.

- [ ] In `compose/ScheduleApp.kt` (will move in K2), replace AppStateHolder-based snackbar collection with:
  ```kotlin
  val firstMessage = snackbarManager.messages.collectAsState().value.firstOrNull()
  if (firstMessage != null) {
      val text = stringResource(firstMessage.messageId)
      LaunchedEffect(firstMessage.id) {
          appStateHolder.snackbarHostState.showSnackbar(text)
          snackbarManager.setMessageShown(firstMessage.id)
      }
  }
  ```

- [ ] In `compose/ScheduleAppStateHolder.kt` (stays androidMain), remove the `coroutineScope.launch { snackbarManager.messages.collect { ... } }` from `AppStateHolder.init`. Remove the `resources: Resources` parameter once unused.

- [ ] Update every ViewModel calling `SnackbarManager.showMessage(R.string.foo)` → `SnackbarManager.showMessage(Res.string.foo)`. Find with `grep -rln 'SnackbarManager.showMessage' composeApp/src/androidMain/`.

- [ ] Run `./gradlew :composeApp:assembleDebug` — green.
- [ ] **Commit:** `🔧 refactor: SnackbarManager.showMessage @StringRes Int → M-R StringResource`

---

> **Review Gate 1** — Dispatch `feature-dev:code-reviewer` on Groups A+B+C diff.
>
> Checklist:
> - No `R.string.*` in any planned-commonMain file.
> - composeResources XML well-formed.
> - `libs.versions.toml` no duplicates (`./gradlew help` succeeds).
> - `SnackbarManager` no longer imports `androidx.annotation.StringRes`.
> - Android build green.

---

### Group D — Move Pure-Kotlin Model Files to commonMain

#### 🔧 D1 — Move pure state/result types

- [ ] `git mv composeApp/src/androidMain/kotlin/ru/dvfu/appliances/ui/BaseViewState.kt composeApp/src/commonMain/kotlin/ru/dvfu/appliances/ui/BaseViewState.kt`
- [ ] Repeat for: `Progress.kt`, `compose/components/UiState.kt`, `compose/utils/AvailabilityState.kt`, `model/utils/StringOperation.kt`, `model/utils/Constants.kt`, `application/SnackbarManager.kt`.
- [ ] Run `./gradlew :composeApp:assembleDebug` — green.

#### 🔧 D2 — Move repository interfaces and UserDatastore interface

- [ ] Move repository interfaces (none import Firebase/Android directly):
  ```bash
  for f in BookingRepository AppliancesRepository Repository OfflineRepository UsersRepository; do
    git mv \
      composeApp/src/androidMain/kotlin/ru/dvfu/appliances/model/repository/${f}.kt \
      composeApp/src/commonMain/kotlin/ru/dvfu/appliances/model/repository/${f}.kt
  done
  ```
- [ ] `EventsRepository.kt`: replace `import java.time.LocalDate` with `import kotlinx.datetime.LocalDate`, then `git mv`.
- [ ] `git mv .../model/datastore/UserDatastore.kt → commonMain` (interface only).
- [ ] Run `./gradlew :composeApp:assembleDebug` — green.

#### 🔧 D3 — Move notification data models

- [ ] Move `Notification.kt`, `NotificationData.kt`, `PushNotification.kt` to commonMain (`.../model/repository/entity/notifications/`).
- [ ] `RetrofitInstance.kt` and `NotificationApi.kt` stay in androidMain (Phase 3).
- [ ] Run `./gradlew :composeApp:assembleDebug` — green.
- [ ] **Commit:** `🔧 refactor: move pure model types, repository interfaces, notification models to commonMain`

---

### Group E — java.time → kotlinx.datetime

#### 🔧 E1 — Mechanical import swap

```bash
find composeApp/src/androidMain/kotlin -name "*.kt" | \
  grep -E "(model/repository/entity|model/utils/TimeUtils|model/repository/EventsRepository|compose/utils/EventMapper|compose/use_cases|compose/viewmodels|compose/calendars|compose/home/booking_list|compose/home/AddEvent|model/datasource/mock)" | \
  xargs sed -i '' \
    -e 's|^import java\.time\.LocalDate$|import kotlinx.datetime.LocalDate|' \
    -e 's|^import java\.time\.LocalDateTime$|import kotlinx.datetime.LocalDateTime|' \
    -e 's|^import java\.time\.LocalTime$|import kotlinx.datetime.LocalTime|' \
    -e 's|^import java\.time\.Instant$|import kotlinx.datetime.Instant|' \
    -e 's|^import java\.time\.ZoneId$|import kotlinx.datetime.TimeZone|' \
    -e 's|^import java\.time\.ZonedDateTime$||' \
    -e 's|^import java\.time\.\*$|import kotlinx.datetime.*|' \
    -e 's|^import java\.time\.temporal\.ChronoUnit$|import kotlinx.datetime.DateTimeUnit|' \
    -e 's|^import java\.time\.format\.DateTimeFormatter$||' \
    -e 's|^import java\.time\.format\.FormatStyle$||' \
    -e 's|^import java\.time\.Duration$||'
```

Add `import kotlin.time.Duration.Companion.minutes` and `.hours` manually to files using `Duration.ofMinutes` / `.ofHours`.

- [ ] Run sed command.
- [ ] Run `./gradlew :composeApp:assembleDebug` — will fail with API mismatches. Expected; proceed to E2.

#### 🔧 E2 — Manual API fixups, file by file

Apply changes per the mapping table. Run build after each file.

**`model/utils/TimeUtils.kt`:**
- Change `val ZONE: ZoneId` → `val ZONE: TimeZone = TimeZone.of("Asia/Vladivostok")`.
- Add `import kotlin.time.Duration.Companion.minutes`/`.hours`.
- `Duration.ofMinutes(30)` → `30.minutes`. `Duration.ofHours(1)` → `1.hours`.
- Delete `val FULL_DATE_FORMAT: DateTimeFormatter`.
- `Long.toLocalDateTime()` body → `Instant.fromEpochMilliseconds(this).toLocalDateTime(ZONE)`.
- Delete `Long.toZonedDateTime()`.
- `Long.toLocalTime()` body → `Instant.fromEpochMilliseconds(this).toLocalDateTime(ZONE).time`.
- `Long.toLocalDate()` body → `Instant.fromEpochMilliseconds(this).toLocalDateTime(ZONE).date`.
- `LocalDateTime.toMillis` → `this.toInstant(ZONE).toEpochMilliseconds()`.
- `LocalDate.toMillis` → `this.atTime(0, 0).toInstant(ZONE).toEpochMilliseconds()`.
- `LocalTime.toHoursAndMinutes()` → `"$hour:${minute.toString().padStart(2, '0')}"`.
- Replace `formattedDate` with `russianMonthName` helper:
  ```kotlin
  private fun russianMonthName(month: kotlinx.datetime.Month): String = when (month) {
      kotlinx.datetime.Month.JANUARY -> "января"
      kotlinx.datetime.Month.FEBRUARY -> "февраля"
      kotlinx.datetime.Month.MARCH -> "марта"
      kotlinx.datetime.Month.APRIL -> "апреля"
      kotlinx.datetime.Month.MAY -> "мая"
      kotlinx.datetime.Month.JUNE -> "июня"
      kotlinx.datetime.Month.JULY -> "июля"
      kotlinx.datetime.Month.AUGUST -> "августа"
      kotlinx.datetime.Month.SEPTEMBER -> "сентября"
      kotlinx.datetime.Month.OCTOBER -> "октября"
      kotlinx.datetime.Month.NOVEMBER -> "ноября"
      kotlinx.datetime.Month.DECEMBER -> "декабря"
  }

  fun formattedDate(date: LocalDate): String = "${date.dayOfMonth} ${russianMonthName(date.month)}"
  ```

**`model/repository/entity/Event.kt`:**
- In `canBeRefused()`:
  ```kotlin
  import kotlinx.datetime.Clock
  import kotlinx.datetime.TimeZone
  val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
  val timeMins = (timeEnd.toInstant(TimeZone.currentSystemDefault()) -
      now.toInstant(TimeZone.currentSystemDefault())).inWholeMinutes
  ```

**`model/repository/entity/User.kt`:**
- In `canManageEvent`: `LocalDateTime.now()` → `Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())`.
- `event.timeEnd.isAfter(LocalDateTime.now())` → `event.timeEnd > Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())`.

**`compose/calendars/event_calendar/EventDataModifier.kt`:**
- Delete all `DateTimeFormatter` declarations.
- Add lambda formatters per mapping table (`EventTimeFormatter`, `HourFormatter`, `DayFormatter`).
- Update callsites: `.format(EventTimeFormatter)` → `EventTimeFormatter(localTime)`.

**`compose/calendars/event_calendar/ScheduleHeader.kt`:**
- `ChronoUnit.DAYS.between(minDate, maxDate)` → `minDate.until(maxDate, DateTimeUnit.DAY)`.
- `minDate.plusDays(i.toLong())` → `minDate.plus(i.toLong(), DateTimeUnit.DAY)`.
- `LocalDate.now()` → `Clock.System.todayIn(TimeZone.currentSystemDefault())`.

**`compose/calendars/event_calendar/ScheduleSideBar.kt`:**
- `ChronoUnit.MINUTES.between(minTime, maxTime)` → `(maxTime.toSecondOfDay() - minTime.toSecondOfDay()) / 60`.
- `minTime.truncatedTo(ChronoUnit.HOURS)` → `LocalTime(minTime.hour, 0, 0, 0)`.
- `firstHour.plusHours(1)` → `LocalTime(firstHour.hour + 1, 0, 0, 0)`.
- `startTime.plusHours(i.toLong())` → `LocalTime(startTime.hour + i, 0, 0, 0)`.

**`compose/calendars/event_calendar/BasicDayHeader.kt`, `BasicSidebarLabel.kt`:**
- `LocalDate.now()` → clock todayIn. `LocalTime.now()` → clock-based.
- `day.format(DayFormatter)` → `DayFormatter(day)`. Same for `HourFormatter`.

**`compose/calendars/event_calendar/Temp.kt`:**
- `ChronoUnit.DAYS.between` → `until(...,DAY)`.
- `LocalTime.MIN` → `LocalTime(0,0,0,0)`. `LocalTime.MAX` → `LocalTime(23,59,59,999_999_999)`.

**`compose/calendars/event_calendar/Schedule.kt`:**
- ChronoUnit between → `toSecondOfDay()` arithmetic.
- `LocalDate.now()` → clock-based.

**`compose/viewmodels/AddEventViewModel.kt`:**
- Add `import kotlin.time.Duration.Companion.minutes`/`.hours`.
- `Duration.ofMinutes(30)` → `30.minutes`. `Duration.ofHours(1)` → `1.hours`.
- `LocalDate.now()` / `LocalDateTime.now()` → clock-based.
- `LocalTime.of(h, m)` → `LocalTime(h, m)`.

**Other ViewModels (`WeekCalendarViewModel`, `BookingListViewModel`, `EventInfoViewModel`):**
- Same patterns: `.toLocalDate()` → `.date`, `.toLocalTime()` → `.time`. `Duration.ofMinutes` → `.minutes`. `LocalDate.now()` → clock.

**`model/datasource/mock/FakeData.kt`, `MockEventsRepository.kt`:**
- `LocalDate.of(y,m,d)` → `LocalDate(y,m,d)`.
- `LocalTime.of(h,m)` → `LocalTime(h,m)`.
- `LocalDateTime.of(d,t)` → `LocalDateTime(d,t)`.
- `LocalDate.now()` → clock-based.

**`compose/calendars/MonthCalendar.kt`, `MonthCalendarViews.kt`, `WeekCalendar.kt`:**
- Same patterns as above.

**`compose/home/AddEvent.kt`, booking list files:**
- Same.

- [ ] After all manual fixes, verify:
  ```bash
  grep -rn 'java\.time' \
    composeApp/src/androidMain/kotlin/ru/dvfu/appliances/compose/calendars/ \
    composeApp/src/androidMain/kotlin/ru/dvfu/appliances/compose/viewmodels/ \
    composeApp/src/androidMain/kotlin/ru/dvfu/appliances/compose/use_cases/ \
    composeApp/src/androidMain/kotlin/ru/dvfu/appliances/model/repository/entity/ \
    composeApp/src/androidMain/kotlin/ru/dvfu/appliances/model/utils/TimeUtils.kt \
    composeApp/src/androidMain/kotlin/ru/dvfu/appliances/model/datasource/mock/
  ```
  Expected: zero matches.

- [ ] Run `./gradlew :composeApp:assembleDebug` — green.
- [ ] **Commit:** `🔧 refactor: java.time → kotlinx.datetime in all commonMain-bound files`

---

> **Review Gate 2** — Dispatch `feature-dev:code-reviewer` on Groups D+E.
>
> Checklist:
> - Zero `java.time` in commonMain-bound files.
> - `DateTimeFormatter` removed; replaced with lambdas.
> - `.toLocalDate()` calls replaced with `.date`.
> - `LocalDate.now()` replaced with `Clock.System.todayIn(...)`.
> - `android.content.res.Resources` untouched in androidMain.
> - Android build green.

---

### Group F — Replace @Parcelize with @Serializable

#### 🔧 F1 — Convert nav-arg types to @Serializable

- [ ] In `model/repository/entity/Event.kt`:
  - Remove `import android.os.Parcelable`, `import kotlinx.parcelize.Parcelize`.
  - Remove `@Parcelize` and `: Parcelable` from `Event`.
  - Add `import kotlinx.serialization.Serializable` and `@Serializable`.
  - Remove from `CalendarEvent` similarly (no longer a nav arg).
  - Add `@Serializable` to `BookingStatus` enum.

- [ ] `Appliance.kt`: same pattern.
- [ ] `User.kt`: same pattern.
- [ ] In `composeApp/build.gradle.kts` plugins, remove `alias(libs.plugins.kotlin.parcelize)`. Same in `androidApp/build.gradle.kts` if present.
- [ ] Run `./gradlew :composeApp:assembleDebug` — green.
- [ ] **Commit:** `🔧 refactor: @Parcelize → @Serializable on Event, Appliance, User entities`

---

### Group G — Navigation: Typed Routes

#### 🔧 G1 — Create typed route objects

- [ ] Create `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/navigation/Routes.kt`:
  ```kotlin
  package ru.dvfu.appliances.navigation

  import kotlinx.serialization.Serializable

  @Serializable object HomeRoute
  @Serializable data class AddEventRoute(val dateEpochDay: Long)
  @Serializable data class EventInfoRoute(val eventId: String)
  @Serializable object EditProfileRoute
  @Serializable data class ApplianceRoute(val applianceId: String)
  @Serializable data class AddUserToApplianceRoute(val applianceId: String)
  @Serializable data class AddSuperuserToApplianceRoute(val applianceId: String)
  @Serializable object AppliancesRoute
  @Serializable object NewApplianceRoute
  @Serializable data class UserDetailsRoute(val userId: String)
  @Serializable object UsersRoute
  @Serializable object BookingListRoute
  @Serializable object SettingsRoute
  ```

- [ ] Create `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/navigation/Destinations.kt`. Extract `MainDestinations` and `Arguments` from `ScheduleAppStateHolder.kt`:
  ```kotlin
  package ru.dvfu.appliances.navigation

  object MainDestinations {
      const val HOME_ROUTE = "home"
      const val ADD_EVENT = "add_event"
      const val EVENT_INFO = "event_info"
      const val EDIT_PROFILE = "edit_profile"
      const val APPLIANCE_ROUTE = "appliance"
      const val ADD_USER_TO_APPLIANCE = "add_user_to_appliance"
      const val ADD_SUPERUSER_TO_APPLIANCE = "add_superuser_to_appliance"
      const val APPLIANCES_ROUTE = "appliances"
      const val NEW_APPLIANCE_ROUTE = "new_appliance"
      const val USERS_ROUTE = "users"
      const val USER_DETAILS_ROUTE = "user_details"
      const val BOOKING_LIST = "booking_list_screen"
      const val SETTINGS_ROUTE = "settings"
      const val WEEK_CALENDAR = "week_calendar"
      const val EVENT_CALENDAR = "event_calendar"
      const val LOGIN_ROUTE = "login"
      const val SNACK_ID_KEY = "snackId"
  }

  object Arguments {
      const val DATE = "date_arg"
      const val EVENT = "event_arg"
      const val USER = "user_arg"
      const val APPLIANCE = "appliance_arg"
  }
  ```
  Remove duplicate declarations from `ScheduleAppStateHolder.kt`; add import.

#### 🔧 G2 — Update ScheduleApp.kt to use typed routes

- [ ] Change each `composable(MainDestinations.X)` to `composable<TypedRoute>` per JetBrains Navigation KMP API.
- [ ] Argument extraction pattern:
  ```kotlin
  composable<EventInfoRoute> {
      val route = it.toRoute<EventInfoRoute>()
      EventInfoScreen(navController, eventId = route.eventId, backPress)
  }
  ```
- [ ] For `AddEventRoute(dateEpochDay)`:
  ```kotlin
  composable<AddEventRoute> {
      val route = it.toRoute<AddEventRoute>()
      val selectedDate = Instant.fromEpochMilliseconds(route.dateEpochDay * 86_400_000L)
          .toLocalDateTime(TimeZone.currentSystemDefault()).date
      AddEvent(selectedDate = selectedDate, upPress)
  }
  ```
- [ ] For ID-based routes, pass id to ViewModel; ViewModel loads full object via existing use-case.
- [ ] Update navigation call sites: `navController.navigate(EventInfoRoute(eventId = calendarEvent.id))`.
- [ ] Remove `NavController.navigate(route, vararg args)` extension and `requiredArg<T>()` from `ScheduleAppStateHolder.kt`.
- [ ] Run `./gradlew :composeApp:assembleDebug` — green.
- [ ] **Commit:** `🔧 refactor: Parcelable nav args → @Serializable typed routes; extract Destinations.kt`

---

### Group H — Move Theme + UI Components

#### 🎨 H1 — Move theme files

- [ ] `git mv` to commonMain: `Color.kt`, `Shape.kt`, `Type.kt`, `CustomColors.kt`.
- [ ] `Theme.kt` stays in androidMain (Activity/WindowCompat/android.graphics.Color).
- [ ] Verify no `android.*` imports in moved files (`androidx.compose.*` is CMP-safe).
- [ ] Run `./gradlew :composeApp:assembleDebug` — green.
- [ ] **Commit:** `🎨 refactor: move Color, Shape, Type, CustomColors to commonMain`

#### 🎨 H2 — Move pure Compose components

For each: verify no `android.*`, update `R.string.*` → `Res.string.*`, swap stringResource import, then `git mv`.

- [ ] `compose/components/SwipeToDelete.kt` (no resources)
- [ ] `compose/components/PagerTabIndicator.kt`
- [ ] `compose/components/SettingsItems.kt`
- [ ] `compose/components/NavDrawerItem.kt` (titles hardcoded)
- [ ] `compose/components/ColorPicker.kt`:
  ```kotlin
  import org.jetbrains.compose.resources.painterResource
  import ru.dvfu.appliances.generated.resources.Res
  import ru.dvfu.appliances.generated.resources.ic_color_off_24dp
  painterResource(Res.drawable.ic_color_off_24dp)
  ```
- [ ] `compose/components/ApplianceSelection.kt`
- [ ] `compose/components/ItemsSelection.kt`
- [ ] `compose/components/views/DefaultTextViews.kt`
- [ ] `compose/components/views/Dialogs.kt` (R.string → Res.string)
- [ ] `compose/DefaultViews.kt`
- [ ] `compose/components/FloatingActionButtons.kt`
- [ ] Run `./gradlew :composeApp:assembleDebug` — green.
- [ ] **Commit:** `🎨 refactor: move UI component files and DefaultViews to commonMain`

#### 🎨 H3 — Move calendar UI files

- [ ] `git mv .../compose/calendars/CalendarType.kt → commonMain`.
- [ ] Move all under `compose/calendars/event_calendar/`: EventDataModifier, PositionedEvent, Temp, ScheduleHeader, ScheduleSideBar, BasicDayHeader, BasicSidebarLabel, CalendarEvent, Schedule.
- [ ] `git mv MonthCalendar.kt`, `MonthCalendarViews.kt`, `WeekCalendar.kt` to commonMain. Check for `LocalContext.current`; remove if present.
- [ ] Run `./gradlew :composeApp:assembleDebug` — green.
- [ ] **Commit:** `🎨 refactor: move all calendar composables to commonMain`

---

> **Review Gate 3** — Dispatch reviewer on Groups F+G+H.
>
> Checklist:
> - No `@Parcelize` or `import android.os.Parcelable` in commonMain.
> - No `androidx.compose.ui.res.stringResource` in commonMain.
> - No `R.string.*` or `R.drawable.*` in commonMain.
> - `Theme.kt` still in androidMain.
> - `Routes.kt` and `Destinations.kt` in commonMain.
> - Android build green.

---

### Group I — Move Entity Data Classes + Use-Cases

#### 🔧 I1 — Move entity data classes

- [ ] `git mv` to commonMain: `Event.kt`, `Appliance.kt`, `User.kt`, `Roles.kt`.
- [ ] Run `./gradlew :composeApp:assembleDebug` — green.
- [ ] **Commit:** `🔧 refactor: move entity data classes to commonMain`

#### 🔧 I2 — Move use-cases

- [ ] Move all use-cases except `GetEventTimeAvailabilityUseCase.kt` (uses Context):
  - `GetApplianceUseCase`, `GetAppliancesUseCase`, `GetUserUseCase`, `GetDateEventsUseCase`, `GetPeriodEventsUseCase`, `ChangeApplianceStatusUseCase`, `DeleteApplianceUseCase`, `UpdateEventUseCase`, `UpdateEventStatusUseCase`
  - `event/UpdateManagerCommentUseCase`, `event/UpdateEventUserCommentUseCase`, `event/UpdateTimeUseCase`
- [ ] Verify each: no `import com.google.firebase.*`, no `import android.*`.
- [ ] Run `./gradlew :composeApp:assembleDebug` — green.

#### 🔧 I3 — Move EventMapper, NotificationManager interface, TimeUtils, mock repos

- [ ] `git mv .../compose/utils/EventMapper.kt` and `NotificationManager.kt` (interface) to commonMain.
- [ ] `git mv .../model/utils/TimeUtils.kt` (E2 done).
- [ ] Move `model/datasource/mock/FakeData.kt`, `MockEventsRepository.kt`, `MockUsersRepository.kt`, `MockAppliancesRepository.kt`.
- [ ] Run `./gradlew :composeApp:assembleDebug` — green.
- [ ] **Commit:** `🔧 refactor: move use-cases, EventMapper, NotificationManager, TimeUtils, mock repos to commonMain`

---

### Group J — Move ViewModels

#### 🎨 J1 — Update Koin ViewModel injection import

```bash
find composeApp/src/androidMain/kotlin -name "*.kt" | \
  grep -v "LoginViewModel\|LoginActivity\|LoginScreen\|MainActivity\|Schedule\.kt" | \
  xargs sed -i '' \
    's|org\.koin\.androidx\.compose\.koinViewModel|org.koin.compose.viewmodel.koinViewModel|g'
```

#### 🎨 J2 — Move all ViewModels (except LoginViewModel)

- [ ] Move 14 ViewModels to commonMain:
  - `compose/home/MainScreenViewModel`
  - `compose/viewmodels/MainViewModel`, `AddEventViewModel`, `EventInfoViewModel`, `WeekCalendarViewModel`, `BookingListViewModel`, `AppliancesViewModel`, `ApplianceDetailsViewModel`, `AddUserViewModel`, `NewApplianceViewModel`, `UserDetailsViewModel`, `EditProfileViewModel`, `ProfileViewModel`, `ApplianceUsersViewModel`
- [ ] `LoginViewModel.kt` stays in androidMain.
- [ ] For each: verify no `android.*` or `firebase.*`; SnackbarManager updated; java.time gone; uses JetBrains lifecycle ViewModel/viewModelScope (in commonMain via JetBrains port).
- [ ] `AddEventViewModel` constructor takes `selectedDate: LocalDate` (kotlinx.datetime); convert from `dateEpochDay` at call site.
- [ ] Run `./gradlew :composeApp:assembleDebug` — green.
- [ ] **Commit:** `🎨 refactor: move 14 ViewModels to commonMain; fix koinViewModel import`

---

### Group K — Move Screen Composables

#### 🎨 K1 — Move booking, appliance, home screens

- [ ] Move to commonMain (update `R.string.*` → `Res.string.*`, remove LocalContext/showToast):
  - `compose/home/booking_list/BookingList.kt`, `BookingScreens.kt`, `BookingViews.kt`
  - `compose/home/AddEvent.kt`
  - `compose/appliance/TabItem.kt`, `ApplianceUsers.kt`, `ApplianceSuperUsers.kt`
- [ ] Scan other home screens; move those without Android-specific imports.
- [ ] `compose/home/HomeScreen.kt` stays in androidMain (casts Context to MainActivity).
- [ ] Run `./gradlew :composeApp:assembleDebug` — green.

#### 🎨 K2 — Move navigation entry points

- [ ] `git mv .../compose/Home.kt → commonMain` (R.string → Res.string).
- [ ] `git mv .../compose/ScheduleApp.kt → commonMain` (typed routes from G2; SnackbarManager from C2).
- [ ] Run `./gradlew :composeApp:assembleDebug` — green.
- [ ] **Commit:** `🎨 refactor: move screen composables and navigation entry points to commonMain`

---

> **Review Gate 4** — Dispatch reviewer on Groups I+J+K.
>
> Checklist:
> - Every commonMain file has zero `import android.*`.
> - Every commonMain file has zero `import com.google.firebase.*`.
> - `koinViewModel()` import is `org.koin.compose.viewmodel.koinViewModel`.
> - `viewModelScope` compiles.
> - `find composeApp/src/commonMain/kotlin -name '*.kt' | wc -l` ≥ 55.
> - Android build green.

---

### Group L — Koin Stub + Verification

#### ⚙️ L1 — Stub expect/actual platformModule

- [ ] Create `composeApp/src/commonMain/kotlin/ru/dvfu/appliances/di/PlatformModule.kt`:
  ```kotlin
  package ru.dvfu.appliances.di

  import org.koin.core.module.Module

  expect fun platformModule(): Module
  ```

- [ ] Create `composeApp/src/androidMain/kotlin/ru/dvfu/appliances/di/PlatformModule.kt`:
  ```kotlin
  package ru.dvfu.appliances.di

  import org.koin.core.module.Module

  actual fun platformModule(): Module = repositoryModule + application + mainActivity
  ```

- [ ] Run `./gradlew :composeApp:assembleDebug` — green.
- [ ] **Commit:** `⚙️ di: add expect/actual platformModule stub for future iOS`

#### ⚙️ L2 — Final purity verification

```bash
echo "=== No androidx.compose.ui.res.stringResource in commonMain ==="
grep -rn 'androidx\.compose\.ui\.res\.stringResource' composeApp/src/commonMain/

echo "=== No R.string or R.drawable in commonMain ==="
grep -rn 'import ru\.dvfu\.appliances\.R' composeApp/src/commonMain/

echo "=== No java.time in commonMain ==="
grep -rn 'java\.time' composeApp/src/commonMain/

echo "=== No android imports in commonMain ==="
grep -rn '^import android\.' composeApp/src/commonMain/

echo "=== No Parcelable in commonMain ==="
grep -rn 'kotlinx\.parcelize\|android\.os\.Parcelable' composeApp/src/commonMain/
```

All five must produce zero output.

- [ ] Confirm `Res.string.app_name` works in en + ru.
- [ ] Run `./gradlew :composeApp:assembleDebug` — green.
- [ ] **Commit:** `⚙️ verify: commonMain is clean of android/java.time/Parcelable/R imports`

---

### Group M — commonTest + Final Smoke Test

#### 🔧 M1 — commonTest for TimeUtils

- [ ] Create `composeApp/src/commonTest/kotlin/ru/dvfu/appliances/model/utils/TimeUtilsTest.kt`:
  ```kotlin
  package ru.dvfu.appliances.model.utils

  import kotlin.test.Test
  import kotlin.test.assertEquals
  import kotlin.test.assertTrue

  class TimeUtilsTest {

      @Test
      fun epochMillisToLocalDateTimeRoundTrip() {
          val ms = 1_700_000_000_000L
          val ldt = ms.toLocalDateTime()
          assertEquals(ms, ldt.toMillis)
      }

      @Test
      fun epochMillisToLocalDateRoundTrip() {
          val ms = 1_700_000_000_000L
          val date = ms.toLocalDate()
          val dateMsBack = date.toMillis
          val dateFromBack = dateMsBack.toLocalDate()
          assertEquals(date, dateFromBack)
      }

      @Test
      fun formattedTimeContainsColon() {
          val start = 1_700_000_000_000L.toLocalDateTime()
          val end = 1_700_003_600_000L.toLocalDateTime()
          val result = formattedTime(start, end)
          assertTrue(result.contains(":"))
      }

      @Test
      fun formattedDateNotBlank() {
          val date = 1_700_000_000_000L.toLocalDate()
          val result = formattedDate(date)
          assertTrue(result.isNotBlank())
      }
  }
  ```

- [ ] Run `./gradlew :composeApp:testDebugUnitTest` — 4 pass.
- [ ] **Commit:** `🔧 test: commonTest TimeUtils round-trip tests`

#### M2 — Manual device smoke test

- [ ] `./gradlew :androidApp:installDebug`.
- [ ] App launches, sign-in renders.
- [ ] Sign in → home calendar week view.
- [ ] Switch to month view.
- [ ] Tap date → AddEvent opens with correct date.
- [ ] Open Bookings → list renders.
- [ ] Open Settings → toggles respond.
- [ ] Set device locale to Russian → all strings Cyrillic.
- [ ] Create new booking end-to-end.

---

> **Review Gate 5 (Final)** — Dispatch reviewer on full Phase 2 diff.
>
> - `! grep -rn 'import android\.' composeApp/src/commonMain/` → zero
> - `! grep -rn 'java\.time' composeApp/src/commonMain/` → zero
> - `! grep -rn 'import ru\.dvfu\.appliances\.R' composeApp/src/commonMain/` → zero
> - `! grep -rn 'kotlinx\.parcelize\|android\.os\.Parcelable' composeApp/src/commonMain/` → zero
> - `find composeApp/src/commonMain/kotlin -name '*.kt' | wc -l` ≥ 55
> - `find composeApp/src/androidMain/kotlin -name '*.kt' | wc -l` ≤ 40
> - `./gradlew :composeApp:testDebugUnitTest` passes
> - `./gradlew :androidApp:assembleRelease` green
> - Russian locale smoke test passed

- [ ] **Final commit:** `🎨 phase2 complete: ~60% of code in commonMain, Android build green`
- [ ] `git push origin feature/cmp-ios-migration`

---

## Appendix: full commonMain file list (post-Phase-2)

~92 files in commonMain; ~35 in androidMain (Firebase impls, DI, MainActivity, Application, DataStore impl, Logger, notification service impl, Retrofit, login activity).

**Phase 2 exit:** Android app installs and runs identically to pre-Phase-2; ~60% of code in commonMain; commonTest with 4 tests; all five purity grep checks zero; branch pushed and ready for Phase 3.
